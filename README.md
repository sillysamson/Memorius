# Memorius (native Android, Java)

Homework reminder app. Pure Java, no web view.

## Build the APK with GitHub
1. Create a GitHub repo and upload everything in this folder (branch `main`).
2. Open the **Actions** tab. Wait for "Build Memorius APK" to turn green.
3. Open the finished run, download the artifact **memorius-apk**, unzip it.
4. Copy `app-debug.apk` to your Android phone and install it (allow "install unknown apps" once).

You can also open the folder in Android Studio and press Run.

## Android features used
- **Edge to edge:** the app draws behind transparent status and navigation bars. Controls move with the system bar sizes, and the keyboard lifts the forms.
- **Predictive back:** needs Android 13 or newer (the full preview on 14+). Turn on "Predictive back animations" in Developer options on Android 13. While a card, form, menu or project is open, the back swipe previews closing it. On the Cards screen the system plays its own back-to-home preview.
- **Material Expressive motion:** spring physics (one set of numbers in `Motion.java`), tonal active indicator in the nav pill, larger rounded corners. Everything switches to instant when Android "remove animations" is on.

## Notes
- Needs Android 7.0 (API 24) or newer. Portrait only.
- Reminders use a daily alarm at your reminder time. They fire when the app is closed and again after a reboot. Android may delay them by a few minutes to save battery.
- Android 13+ asks for notification permission the first time. If you deny it, the "Allow notifications" button in Settings opens the system settings.
- Reading: PDF (rendered page by page), TXT, DOCX (text only), HTML-based DOC. Old binary .doc files cannot be read.
- Writing: TXT, DOC (HTML-based Word file), PDF (plain text pages). Export goes to your storage folder, or asks where to save if none is chosen.
- Card attachments are kept as links to your files. If you move or delete the original, the link stops working.
