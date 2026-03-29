const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

process.env.GOOGLE_APPLICATION_CREDENTIALS = "/Users/theekshanasudeepa/AndroidStudioProjects/LexoraFix/functions/serviceAccountKey.json"; // Need valid credentials, wait, functions use default credentials when deployed, but locally it needs it. Oh wait, we can just use the emulator? No, it's hitting live.
// if without credential, default credentials might not be set.
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
