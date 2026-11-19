const { initializeApp, cert, applicationDefault } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const fs = require("fs");
const path = require("path");

const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || path.join(__dirname, "serviceAccountKey.json");
if (fs.existsSync(keyPath)) {
  const serviceAccount = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  initializeApp({ credential: cert(serviceAccount) });
} else {
  initializeApp({ credential: applicationDefault() });
}

async function run() {
  const db = getFirestore();
  
  // Check the latest booking
  const bookingsSnap = await db.collection("bookings").orderBy("timestamp", "desc").limit(3).get();
  console.log("=== LATEST BOOKINGS ===");
  for (const doc of bookingsSnap.docs) {
    const d = doc.data();
    console.log(`Booking ${doc.id}:`);
    console.log(`  status: ${d.status}`);
    console.log(`  firebaseUid: ${d.firebaseUid}`);
    console.log(`  serviceType: ${d.serviceType}`);
    console.log(`  assignedWorkerName: ${d.assignedWorkerName}`);
    
    // Check customer's FCM token
    if (d.firebaseUid) {
      const userDoc = await db.collection("users").doc(d.firebaseUid).get();
      if (userDoc.exists) {
        const u = userDoc.data();
        console.log(`  Customer fcmToken: ${u.fcmToken ? u.fcmToken.substring(0, 30) + "..." : "MISSING!"}`);
        console.log(`  Customer role: ${u.role}`);
      } else {
        console.log(`  Customer document DOES NOT EXIST!`);
      }
    } else {
      console.log(`  firebaseUid is MISSING from booking!`);
    }
    console.log("---");
  }
  
  // Check all users and their notification counts
  console.log("\n=== USERS ===");
  const usersSnap = await db.collection("users").get();
  for (const doc of usersSnap.docs) {
    const u = doc.data();
    const notifsSnap = await db.collection("users").doc(doc.id).collection("notifications").get();
    console.log(`User ${doc.id} (${u.name || "?"}, role: ${u.role}): fcmToken=${u.fcmToken ? "YES" : "NO"}, notifications=${notifsSnap.size}`);
  }
}
run();
