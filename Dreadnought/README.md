# Memorius (Kotlin/JS)

Homework reminder app. Web app (PWA), installable on Android.

## Build with GitHub
1. Make a new GitHub repo. Upload everything in this folder. Branch: `main`.
2. Repo > Settings > Pages > Source: **GitHub Actions**.
3. Go to the Actions tab. Wait for "Build Memorius" to finish (green).
4. Open the Pages link on your Android phone in Chrome.
5. Chrome menu > **Add to Home screen** (or Install app).
6. Open the app > Settings > pick a notification option > allow notifications.

## Limits (web, not native)
- Reminders fire only when the app is open or recently running. For always-on reminders you need a native Android app.
- Words: .doc export is an HTML-based Word file. PDF export uses the print dialog (Save as PDF). .docx is read, not written.
- Old binary .doc files cannot be read. HTML-based .doc, .docx, .txt, .pdf can.
- Files up to 2 MB each. Storage folder works only in browsers that support it (desktop Chrome/Edge). Android Chrome downloads instead.
- PDF/Word reading loads two libraries from a CDN, so it needs internet.
