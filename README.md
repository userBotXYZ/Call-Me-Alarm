# Call-Me Event Alarm (Native Android App)

Triggers a full-screen incoming call screen **30 minutes before** your scheduled event on selected days of the week, with **Answer & Direct Cellular Call (`ACTION_CALL`)**, **Reject**, and **5-Minute / 10-Minute Follow-Up Call** options.

## How to Get Your `.apk` Without Installing Android Studio (GitHub Actions)

1. Extract this `.zip` folder on your computer.
2. Create a new repository on [GitHub](https://github.com/new) and upload/push all extracted files (make sure the `.github/workflows/build-apk.yml` file is included).
3. Go to the **Actions** tab in your GitHub repository and select **Build CallMeAlarm APK**.
4. Once the build completes (~2 minutes), scroll to the **Artifacts** section at the bottom of the workflow summary and download **`CallMeAlarm-debug-apk.zip`**, which contains your installable `app-debug.apk`.

## How to Build in Android Studio

1. Extract this `.zip` folder.
2. Open Android Studio -> **Open** -> select the extracted folder.
3. Wait for Gradle Sync to finish, then select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
