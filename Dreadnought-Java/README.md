# Memorius (native Android, Java)

Homework reminder app. Pure Java, no web view.

## Build the APK with GitHub
1. Create a GitHub repo and upload everything in this folder (branch `main`).
2. Open the **Actions** tab. Wait for "Build Memorius APK" to turn green.
3. Open the finished run, download the artifact **memorius-apk**, unzip it.
4. Copy `app-debug.apk` to your Android phone and install it (allow "install unknown apps" once).

You can also open the folder in Android Studio and press Run.

## Notes
- Needs Android 7.0 (API 24) or newer. Portrait only.
- Reminders use a daily alarm at your reminder time. They fire when the app is closed and again after a reboot. Android may delay them by a few minutes to save battery.
- Android 13+ asks for notification permission the first time. If you deny it, the "Allow notifications" button in Settings opens the system settings.
- Reading: PDF (rendered page by page), TXT, DOCX (text only), HTML-based DOC. Old binary .doc files cannot be read.
- Writing: TXT, DOC (HTML-based Word file), PDF (plain text pages). Export goes to your storage folder, or asks where to save if none is chosen.
- Card attachments are kept as links to your files. If you move or delete the original, the link stops working.
