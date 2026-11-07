# AutoSnipe - Android Floating Auto-Clicker & Price Sniper APK

AutoSnipe is an Android accessibility and overlay utility that places two draggable floating crosshair buttons anywhere on your phone screen:
- **Target 1 (Clicker / Watcher)**: Continuously clicks the screen (e.g. Refresh, Re-check, or Buy Box) at your configured interval (e.g. 250ms).
- **Target 2 (Place Order)**: Instantly fires the purchase/order click the moment the screen price meets your target condition (e.g. price drops to <= $99.00).

---

## 🚀 How to Build the APK (3 Easy Ways)

### Method 1: Android Studio (Recommended)
1. Download and extract this project folder.
2. Open Android Studio -> Select **"Open"** -> Choose this project folder.
3. Wait for Gradle sync to complete.
4. Go to top menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio compiles `app-debug.apk` in `app/build/outputs/apk/debug/`.
6. Transfer `app-debug.apk` to your Android phone and install!

### Method 2: GitHub Actions (Zero Software Required!)
1. Create a free GitHub repository.
2. Push or upload these files to your repository.
3. GitHub Actions will automatically run the included `.github/workflows/build-apk.yml` script.
4. Click the **"Actions"** tab -> Select the latest run -> Download the ready-to-install **`AutoSnipe-Debug-APK`** directly to your phone!

### Method 3: Command Line (Gradle)
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Android Permissions Setup (Crucial!)
To allow the floating buttons to draw over other apps and dispatch automated clicks, enable these two Android permissions:
1. **Display over other apps (SYSTEM_ALERT_WINDOW)**:
   - When launching AutoSnipe, tap "Grant Overlay Permission".
   - Find AutoSnipe in the list and toggle **"Allow display over other apps"** ON.
2. **Accessibility Service (BIND_ACCESSIBILITY_SERVICE)**:
   - Tap "Grant Accessibility Permission".
   - In Settings > Accessibility > Installed Apps / Downloaded Services, select **AutoSnipe Service** and turn it **ON**.
   - (Note: Accessibility Service is what enables synthetic touch clicks at Target 1 and Target 2 coordinates without root).

---

## 🎯 How to Use
1. Launch AutoSnipe and set your **Target Price** (e.g. `99.00`) and condition (`<=`).
2. Tap **"Launch Floating Overlay"**.
3. Open your shopping app, sneaker drop, flight booking, or ticket site.
4. Drag **Target 1** over the Refresh / Reload button.
5. Drag **Target 2** over the "Place Order" or "Buy Now" button.
6. Tap **START** on the floating HUD.
7. AutoSnipe will click Target 1 repeatedly. As soon as the price satisfies your target, it halts Target 1 and instantly clicks Target 2!
