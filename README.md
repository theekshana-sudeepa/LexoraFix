<div align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" alt="LexoraFix Logo" width="120" />
  
  # LexoraFix
  
  **Your All-in-One Home Tools & Services Platform**
  
  [![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
  [![Language](https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white)](https://www.java.com/)
  [![Backend](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)
  
  <p align="center">
    LexoraFix brings the hardware store and service professionals right to your fingertips. Buy top-quality home tools, book expert installation, repair, and maintenance services, and manage everything effortlessly from a modern Android application.
  </p>
</div>

---

## 🌟 Key Features

### 🛒 E-Commerce for Home Tools
- **Explore & Search:** Browse through categorized lists of tools and featured products.
- **Rich Details:** View detailed product descriptions, checking stock status, and watch product demo videos.
- **Cart & Checkout:** Seamlessly add items to your cart, review order summaries, and place orders with real-time delivery fee calculations.
- **Wishlist & Reviews:** Add items to your wishlist and leave helpful reviews for products.

### 🛠️ Service Booking
- **Diverse Services:** Schedule installations, standard repairs, or routine maintenance.
- **Flexible Scheduling:** Select your preferred date and time, and leave additional booking notes for the technician.
- **Real-Time Tracking:** Stay updated on your booking status via instant push notifications.
- **Locate Service Centers:** Find nearby service centers natively integrated with maps to get directions or call providers directly.

### 📱 User Experience
- **Smooth Authentication:** Secure login & registration using Email/Password, Password Reset, and Google Sign-In options.
- **Dynamic Notifications:** Stay informed with categorized notifications (Order Updates, Booking Updates, Reminders, Promotions).
- **Interactive UI:** Discover features interactively, like the "Shake device to quickly open cart" function!

### 👔 Admin Dashboard (Dedicated Module)
- **Order Management:** Efficiently confirm, decline, or update status for incoming user orders.
- **Booking Management:** Track and manage service booking requests.
- **Push Notifications Control:** Send out promotional alerts and targeted reminders directly to customers.

---

## 🏗️ Architecture & Tech Stack

The project is built as a single Android monorepo containing multiple modules for clean separation of concerns.

- **`app` Module:** The primary customer-facing Android application.
- **`admin` Module:** A dedicated Android application for administrators and store managers to oversee operations.
- **`functions` Module:** Node.js based Firebase Cloud Functions handling background tasks, securing server-side logic, and triggering push notifications.

### Technologies
- **UI/UX:** XML Layouts, Material Design Components.
- **Local Storage:** SQLite for local state management and offline caching.
- **Cloud Backend:** 
  - **Firebase Authentication:** Handles secure user identity and Google Sign-In.
  - **Cloud Firestore:** Real-time NoSQL database for synchronizing products, orders, bookings, and chat messages.
  - **Firebase Cloud Messaging (FCM):** Delivers instantaneous background data and notification payloads.
  - **Firebase Storage:** Hosts product thumbnail imagery, demo videos, and user avatars.

---

## 🚀 Getting Started

### Prerequisites
1. **Android Studio:** Ensure you have the latest version of Android Studio installed.
2. **Java Development Kit (JDK):** Version 17 or higher.
3. **Firebase Account:** You will need your own Firebase Project configuration to build and run this app locally.

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/LexoraFix.git
   cd LexoraFix
   ```

2. **Configure Firebase:**
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App for both the `app` module (`com.lexoratech.lexorafix`) and the `admin` module.
   - Download the generated `google-services.json` files and place them inside the respective module root directories (`/app/` and `/admin/`).

3. **Deploy Firebase Settings:**
   - Deploy the provided security rules to your Firestore and Storage buckets:
     ```bash
     firebase deploy --only firestore:rules
     firebase deploy --only storage
     ```
   - Deploy backend Cloud Functions:
     ```bash
     cd functions
     npm install
     firebase deploy --only functions
     ```

4. **Build and Run:**
   - Open the project in Android Studio.
   - Sync the project with Gradle files.
   - Select the target module (`app` or `admin`) from the run configuration and hit the **Run** button.

---

<div align="center">
  <b>Built with ❤️ by Lexoratech </b>
</div>
