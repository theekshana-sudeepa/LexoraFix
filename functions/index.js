/**
 * Firebase Cloud Functions for LexoraFix
 * 
 * These server-side functions run automatically in response to Firestore document
 * changes (triggers). They handle push notifications for:
 *   - New bookings (notifies workers)
 *   - Booking status changes (notifies customers)
 *   - Chat messages (notifies the other participant)
 *   - New e-commerce orders (notifies admin)
 *   - Order status changes (notifies customers)
 *   - Worker registrations (notifies admin)
 *   - Worker approvals (notifies worker)
 * 
 * Each function listens to a specific Firestore collection path using
 * onDocumentCreated or onDocumentUpdated triggers from Firebase Functions v2.
 */

const { onDocumentUpdated, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

// Initialize the Firebase Admin SDK (required for Firestore and FCM access)
initializeApp();

/**
 * When a new booking is CREATED → notify all workers
 */
exports.onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
  const booking = event.data?.data();
  if (!booking) return null;

  const db = getFirestore();
  const serviceType = booking.serviceType || "Service";
  const customerName = booking.userName || "A customer";

  // Get all workers' FCM tokens
  const workersSnap = await db.collection("users")
    .where("role", "==", "worker")
    .where("adminApproved", "==", true)
    .get();

  const tokens = [];
  workersSnap.forEach((doc) => {
    const fcmToken = doc.data().fcmToken;
    if (fcmToken) tokens.push(fcmToken);
  });

  if (tokens.length === 0) {
    console.log("No worker FCM tokens found");
    return null;
  }

  const message = {
    notification: {
      title: "🔔 New Job Available!",
      body: `${customerName} booked a ${serviceType} service. Tap to accept!`,
    },
    data: {
      title: "🔔 New Job Available!",
      body: `${customerName} booked a ${serviceType} service. Tap to accept!`,
      type: "new_booking",
      bookingId: event.params.bookingId,
      serviceType: serviceType,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "lexorafix_admin_notifications",
      },
    },
    tokens: tokens,
  };

  try {
    const response = await getMessaging().sendEachForMulticast(message);
    console.log(`Sent to ${response.successCount}/${tokens.length} workers`);

    // Clean up invalid tokens
    response.responses.forEach((resp, idx) => {
      if (!resp.success && resp.error?.code === "messaging/registration-token-not-registered") {
        console.log(`Removing invalid token for worker`);
      }
    });
  } catch (error) {
    console.error("Error sending worker notifications:", error);
  }

  return null;
});

/**
 * When a booking's STATUS CHANGES → notify the customer AND workers
 */
exports.onBookingStatusChange = onDocumentUpdated("bookings/{bookingId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return null;

  // Only trigger if status actually changed
  if (before.status === after.status) return null;

  const db = getFirestore();
  const bookingId = event.params.bookingId;
  const newStatus = after.status;
  const serviceType = after.serviceType || "Service";
  const customerUid = after.firebaseUid;
  const workerName = after.assignedWorkerName || "A worker";

  console.log(`Booking ${bookingId} status: ${before.status} → ${newStatus}`);

  // 1. Notify the CUSTOMER about the status change
  if (customerUid) {
    const customerDoc = await db.collection("users").doc(customerUid).get();
    const customerToken = customerDoc.data()?.fcmToken;

    if (customerToken) {
      let title, body;
      switch (newStatus) {
        case "Accepted":
          title = "🤝 Worker Assigned!";
          body = `${workerName} accepted your ${serviceType} booking!`;
          break;
        case "In Progress":
          title = "🔧 Work Started!";
          body = `Your ${serviceType} booking is now in progress!`;
          break;
        case "Completed":
          title = "✅ Job Completed!";
          body = `Your ${serviceType} booking has been completed!`;
          break;
        default:
          title = "📋 Booking Updated";
          body = `Your ${serviceType} booking is now: ${newStatus}`;
      }

      try {
        await getMessaging().send({
          notification: {
            title: title,
            body: body,
          },
          data: {
            title: title,
            body: body,
            type: "status_update",
            bookingId: bookingId,
            newStatus: newStatus,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "lexorafix_notifications",
            },
          },
          token: customerToken,
        });
        console.log(`Notified customer ${customerUid}`);

        // Add to customer's in-app NotificationFragment
        await getFirestore().collection("users").doc(customerUid).collection("notifications").add({
            title: title,
            message: body,
            type: "booking_update",
            timestamp: FieldValue.serverTimestamp(),
            read: false,
            bookingId: bookingId
        });
        console.log(`Logged booking update notification for customer ${customerUid}`);

      } catch (error) {
        console.error("Error notifying customer:", error);
      }
    }
  }

  // 2. If a booking was ACCEPTED, notify OTHER workers it's no longer available
  if (newStatus === "Accepted") {
    const workersSnap = await db.collection("users")
      .where("role", "==", "worker")
      .where("adminApproved", "==", true)
      .get();

    const tokens = [];
    workersSnap.forEach((doc) => {
      // Don't notify the worker who accepted it
      if (doc.id !== after.assignedWorkerId && doc.data().fcmToken) {
        tokens.push(doc.data().fcmToken);
      }
    });

    if (tokens.length > 0) {
      try {
        await getMessaging().sendEachForMulticast({
          notification: {
            title: "📋 Job Taken",
            body: `The ${serviceType} job has been accepted by another worker.`,
          },
          data: {
            title: "📋 Job Taken",
            body: `The ${serviceType} job has been accepted by another worker.`,
            type: "booking_taken",
            bookingId: bookingId,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "lexorafix_admin_notifications",
            },
          },
          tokens: tokens,
        });
        console.log(`Notified ${tokens.length} other workers about taken job`);
      } catch (error) {
        console.error("Error notifying other workers:", error);
      }
    }
  }

  return null;
});

exports.onChatMessage = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
  const messageData = event.data.data();
  if (!messageData) return null;

  const chatId = event.params.chatId;
  const senderId = messageData.senderId;
  const senderName = messageData.senderName || "Someone";
  const messageText = messageData.text || "New message";

  try {
    // 1. Get the chat document to find participants and booking info
    const chatDoc = await getFirestore().collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return null;

    const chatData = chatDoc.data();
    const participants = chatData.participants || [];
    const customerName = chatData.customerName || "Customer";
    const workerName = chatData.workerName || "Worker";
    const bookingId = chatData.bookingId || "";

    // 2. Identify the recipient (the participant who is NOT the sender)
    let recipientId = null;
    let senderRole = "worker"; // Default assumption, will verify below
    
    // We expect the chat document to have `participants` array, or we can look up booking
    if (participants.length > 0) {
      recipientId = participants.find(id => id !== senderId);
    } 
    
    // Fallback logic if participants array is missing or incomplete
    if (!recipientId && bookingId) {
      const bookingDoc = await getFirestore().collection("bookings").doc(bookingId).get();
      if (bookingDoc.exists) {
         const bookingData = bookingDoc.data();
         if (senderId === bookingData.assignedWorkerId) {
             recipientId = bookingData.firebaseUid; // FIXED: It's firebaseUid in LexoraFix, not customerId
             senderRole = "worker";
         } else if (senderId === bookingData.firebaseUid) {
             recipientId = bookingData.assignedWorkerId;
             senderRole = "customer";
         }
      }
    }

    if (!recipientId) {
        console.log(`Could not identify recipient for message ${event.params.messageId} in chat ${chatId}`);
        return null;
    }

    // 3. Get the recipient's FCM tokens
    const recipientDoc = await getFirestore().collection("users").doc(recipientId).get();
    if (!recipientDoc.exists) return null;

    const recipientData = recipientDoc.data();
    const tokens = recipientData.fcmTokens || (recipientData.fcmToken ? [recipientData.fcmToken] : []);

    if (tokens.length === 0) {
      console.log(`No FCM tokens found for user ${recipientId}`);
      return null;
    }

    // 4. Construct Notification Payload
    const title = senderName;
    const body = messageText;

    const payload = {
      notification: {
        title: title,
        body: body,
      },
      data: {
        title: title,
        body: body,
        type: "chat_message",
        chatId: chatId,
        bookingId: bookingId,
        senderId: senderId,
        senderName: senderName,
        senderRole: senderRole
      },
      android: {
        priority: "high",
        notification: {
          channelId: "lexorafix_admin_notifications",
        },
      },
      tokens: tokens,
    };

    // 5. Send FCM
    const response = await getMessaging().sendMulticast(payload);
    console.log(`Sent chat notification from ${senderName}. Success: ${response.successCount}, Failures: ${response.failureCount}`);

    // === BONUS: Log to Customer's NotificationFragment if Recipient is Customer ===
    // We identify a customer if the recipient role is customer. The senderRole above gives us a hint.
    if (senderRole === "worker") {
        // Log this notification to the recipient's notifications sub-collection
        await getFirestore().collection("users").doc(recipientId).collection("notifications").add({
            title: title,
            message: body,
            type: "chat_message",
            timestamp: FieldValue.serverTimestamp(),
            read: false,
            bookingId: bookingId,
            chatId: chatId,
            senderId: senderId
        });
        console.log(`Logged chat notification for customer ${recipientId}`);
    }

  } catch (error) {
    console.error("Error sending chat notification:", error);
  }
  
  return null;
});

/**
 * When a new E-commerce ORDER is CREATED → notify Admin
 */
exports.onOrderCreated = onDocumentCreated("orders/{orderId}", async (event) => {
  const order = event.data?.data();
  if (!order) return null;

  const db = getFirestore();
  const orderId = order.orderId || event.params.orderId;
  const customerName = order.userName || "A customer";
  const totalPrice = order.totalPrice || 0;

  // Get admin FCM tokens
  const adminsSnap = await db.collection("users")
    .where("role", "==", "admin")
    .get();

  const tokens = [];
  adminsSnap.forEach((doc) => {
    const fcmToken = doc.data().fcmToken;
    if (fcmToken) tokens.push(fcmToken);
  });

  if (tokens.length === 0) {
    console.log("No admin FCM tokens found for order notification");
    return null;
  }

  const message = {
    notification: {
      title: "🛒 New Order Received!",
      body: `${customerName} placed an order (#${orderId}) for LKR ${totalPrice.toFixed(2)}`,
    },
    data: {
      title: "🛒 New Order Received!",
      body: `${customerName} placed an order (#${orderId}) for LKR ${totalPrice.toFixed(2)}`,
      type: "new_order",
      orderId: String(orderId),
    },
    android: {
      priority: "high",
      notification: {
        channelId: "lexorafix_admin_notifications",
      },
    },
    tokens: tokens,
  };

  try {
    const response = await getMessaging().sendEachForMulticast(message);
    console.log(`Order notification sent to ${response.successCount}/${tokens.length} admins`);
  } catch (error) {
    console.error("Error sending order notification:", error);
  }

  return null;
});

/**
 * When an ORDER STATUS CHANGES → notify the Customer
 */
exports.onOrderStatusChange = onDocumentUpdated("orders/{orderId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return null;

  // Only trigger if status actually changed
  if (before.status === after.status) return null;

  const db = getFirestore();
  const orderId = after.orderId || event.params.orderId;
  const newStatus = after.status;
  const customerUid = after.firebaseUid;

  console.log(`Order ${orderId} status: ${before.status} → ${newStatus}`);

  if (!customerUid) return null;

  const customerDoc = await db.collection("users").doc(customerUid).get();
  const customerToken = customerDoc.data()?.fcmToken;

  if (!customerToken) {
    console.log(`No FCM token for customer ${customerUid}`);
    return null;
  }

  let title, body;
  switch (newStatus) {
    case "Processing":
      title = "📦 Order Processing";
      body = `Your order #${orderId} is now being processed!`;
      break;
    case "Shipped":
      title = "🚚 Order Shipped!";
      body = `Your order #${orderId} has been shipped and is on its way!`;
      break;
    case "Delivered":
      title = "✅ Order Delivered!";
      body = `Your order #${orderId} has been delivered. Enjoy!`;
      break;
    case "Cancelled":
      title = "❌ Order Cancelled";
      body = `Your order #${orderId} has been cancelled.`;
      break;
    default:
      title = "📋 Order Updated";
      body = `Your order #${orderId} status is now: ${newStatus}`;
  }

  try {
    await getMessaging().send({
      notification: { title, body },
      data: {
        title, body,
        type: "order_status_update",
        orderId: String(orderId),
        newStatus: newStatus,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "lexorafix_notifications"
        },
      },
      apns: {
        payload: {
          aps: {
            contentAvailable: true,
            sound: "default"
          }
        }
      },
      token: customerToken,
    });
    console.log(`Order status notification sent to customer ${customerUid}`);

    // Log to customer's in-app notifications
    await db.collection("users").doc(customerUid).collection("notifications").add({
      title: title,
      message: body,
      type: "order_update",
      timestamp: FieldValue.serverTimestamp(),
      read: false,
      orderId: String(orderId),
    });
    console.log(`Logged order update notification for customer ${customerUid}`);
  } catch (error) {
    console.error("Error notifying customer about order status:", error);
  }

  return null;
});

/**
 * When a Worker's adminApproved changes to true → notify the Worker
 */
exports.onWorkerApproved = onDocumentUpdated("users/{userId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return null;

  // Only trigger if adminApproved flipped from false to true AND role is worker
  if (after.role !== "worker") return null;
  if (before.adminApproved === true || after.adminApproved !== true) return null;

  const userId = event.params.userId;
  const workerToken = after.fcmToken;
  const workerName = after.name || "Worker";

  console.log(`Worker ${userId} (${workerName}) has been approved`);

  if (!workerToken) {
    console.log(`No FCM token for worker ${userId}`);
    return null;
  }

  const title = "🎉 Account Approved!";
  const body = `Congratulations ${workerName}! Your LexoraFix worker account has been approved. You can now start accepting jobs!`;

  try {
    await getMessaging().send({
      notification: { title, body },
      data: {
        title, body,
        type: "worker_approved",
        userId: userId,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "lexorafix_admin_notifications",
        },
      },
      token: workerToken,
    });
    console.log(`Approval notification sent to worker ${userId}`);
  } catch (error) {
    console.error("Error sending worker approval notification:", error);
  }

  return null;
});

/**
 * When a new Worker registers (role=worker, adminApproved=false) → notify Admin
 */
exports.onWorkerRegistered = onDocumentCreated("users/{userId}", async (event) => {
  const userData = event.data?.data();
  if (!userData) return null;

  // Only trigger for new worker registrations
  if (userData.role !== "worker") return null;
  if (userData.adminApproved === true) return null;

  const db = getFirestore();
  const workerName = userData.name || "Unknown";
  const workerEmail = userData.email || "";

  console.log(`New worker registered: ${workerName} (${workerEmail})`);

  // Get admin FCM tokens
  const adminsSnap = await db.collection("users")
    .where("role", "==", "admin")
    .get();

  const tokens = [];
  adminsSnap.forEach((doc) => {
    const fcmToken = doc.data().fcmToken;
    if (fcmToken) tokens.push(fcmToken);
  });

  if (tokens.length === 0) {
    console.log("No admin FCM tokens found for worker registration notification");
    return null;
  }

  const title = "📋 New Worker Application";
  const body = `${workerName} has applied as a service worker. Review in the Admin app.`;

  const message = {
    notification: { title, body },
    data: {
      title, body,
      type: "new_worker_registration",
      workerName: workerName,
      workerEmail: workerEmail,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "lexorafix_admin_notifications",
      },
    },
    tokens: tokens,
  };

  try {
    const response = await getMessaging().sendEachForMulticast(message);
    console.log(`Worker registration notification sent to ${response.successCount}/${tokens.length} admins`);
  } catch (error) {
    console.error("Error sending worker registration notification:", error);
  }

  return null;
});
