const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

// Set GOOGLE_APPLICATION_CREDENTIALS in your environment before running locally:
// export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
try {
  initializeApp();
} catch(e) { console.error("Initialize failed", e.message); }

const db = getFirestore();

async function check() {
  const users = await db.collection("users").get();
  let found = false;
  for (const doc of users.docs) {
    const notifs = await db.collection("users").doc(doc.id).collection("notifications").get();
    if (notifs.size > 0) {
      found = true;
      console.log(`User ${doc.id} has ${notifs.size} notifications.`);
      notifs.forEach(n => console.log(n.id, n.data()));
    }
  }
  if (!found) console.log("No users have any notifications in their subcollection.");
}
check().catch(console.error);
