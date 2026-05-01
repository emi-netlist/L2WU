Screen Control Service (L2WU) is an Android application that can wake your phone by lifting it
up, and lock the screen by touching an on-screen notification.

    It was made speciffically for the SONY Xperia 10 VI (Android 16), which misses a "lift to wake up" feature and the previous feature that old Xperia phones had, to lock the screen while double tapping on the wallpaper. This app addresses this options.

## Screenshots
![Main Screen](screenshots/app_1.png)
![Main Screen](screenshots/app_2.png)

While this operations(**Waking the screen** from the background and **Locking the screen** programatically) are two restricted features of Android, it can be done by using Android modern standards:

1. **A Foreground Service:** To keep the sensor listener alive.
2. **Sensor Manager:** To detect the "Lift" gesture (Accelerometer)
3. **Accessibility Service:** This is the only modern way to lock the screen without requiring the user to grant "Device Admin" (which is being phased out)
4. **Jetpack Compose:** For the UI.

## How to use this app:

    Grant Notification Permission: When you run the app, you must click the button to trigger the system dialog and tap "Allow". If you don't, Android 16 will silently block notifications.

    Accessibility Permission: Click the button to go to Settings. Find your app under "Downloaded Apps" or "Installed Services" and turn it ON. This allows the app to lock the screen.

    (Maybe optional) Battery Optimization: To prevent Android from killing the app, you should go to App Info -> Battery -> set to "Unrestricted".
    
    Click "Start Service".

    Test Lock: Click the persistent notification. The screen should turn off.

    Test Wake: Lock the phone, lay it flat on a table, then lift it towards your face. The screen should turn on.
    

