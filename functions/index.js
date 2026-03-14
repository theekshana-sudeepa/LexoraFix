const { onDocumentUpdated, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

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
      type: "new_booking",
      bookingId: event.params.bookingId,
      serviceType: serviceType,
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
          notification: { title, body },
          data: {
            type: "status_update",
            bookingId: bookingId,
            newStatus: newStatus,
          },
          token: customerToken,
        });
        console.log(`Notified customer ${customerUid}`);
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
            type: "booking_taken",
            bookingId: bookingId,
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
