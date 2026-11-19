// Run from the functions/ directory to use its node_modules
const admin = require('firebase-admin');
const projectId = process.env.FIREBASE_PROJECT_ID || 'your-firebase-project-id';
admin.initializeApp({ projectId });
const db = admin.firestore();

db.collection('system').doc('deliveryCharges').set({
  freeDeliveryThreshold: 5000.0,
  defaultRate: 500.0,
  provinceRates: {
    'Western': 250.0,
    'Central': 400.0,
    'Southern': 450.0,
    'North Western': 400.0,
    'North Central': 500.0,
    'Uva': 550.0,
    'Sabaragamuwa': 450.0,
    'Eastern': 600.0,
    'Northern': 650.0
  }
}).then(() => {
  console.log('✅ Delivery charges seeded to Firestore!');
  process.exit(0);
}).catch(e => {
  console.error('❌ Error:', e.message);
  process.exit(1);
});
