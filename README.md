# Memorius (native Android, Java)

Homework reminder app. Pure Java, no web view.

## Build the APK
- Automatic (Ubuntu / Debian / WSL): `bash build.sh` gives `~/Memorius-debug.apk`.
- Or with GitHub, below.

## Build the APK with GitHub
1. Create a GitHub repo and upload everything in this folder (branch `main`).
2. Open the **Actions** tab. Wait for "Build Memorius APK" to turn green.
3. Open the finished run, download the artifact **memorius-apk**, unzip it.
4. Copy `app-debug.apk` to your Android phone and install it (allow "install unknown apps" once).

You can also open the folder in Android Studio and press Run.

## Icon
- Adaptive icon built from your two artworks: the light one by day, the dark one when the phone is in dark mode (the launcher may need a moment or a restart to refresh it).
- Android 13+ themed icon is included (card shape with the wordmark cut out).
- Android 7.x gets a rounded-square version.

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

## Beta 1.1.0 (this version)
- Cards: Standby (title, short description, time left), Preview (hold 0.5 s, snaps between an upper and a lower position, release returns it), Fullview (tap; Edit, Open File, Finish; tap outside or predictive back collapses it).
  One shared card view and one progress value drive the transitions (`MainActivity.CardFloat`, `CardDrawable`).
- Navigation: icon-only pill you can drag; the spotlight follows the finger and snaps on release. Header shows the page title.
- Settings: index of categories, each with its own page (`SettingsPage`). Same draft + Apply banner as before.
- Theme: text size 75-130%, increased contrast, paper / concrete / grain textures, colours, gradients.
- Display: light/dark (dark is the default), warmth reading mode, battery saver (asks Android for 30 FPS).
- Notifications: permission, Android settings link, separate sounds for general and card-deadline notifications (`Notifier`).
- Reminders: Off, On due day, D-X, Every day until done; hour, minute, second. Exact alarms need the alarm permission.
- Languages: 15, with Arabic and Hebrew right-to-left (`Str`, `Dialects`). The four fun dialects are rule based; the other translations should be proofread by native speakers.
- Window blur and blur-in-motion need Android 12+. Older phones get plain surfaces and no blur.
- Directory, Projects and Schedule keep their behaviour.

## Liquid glass (bottom bar, switches, sliders, buttons)
Java re-implementation of the look and feel of the "Backdrop" catalog components you supplied
(LiquidBottomTabs, LiquidToggle, LiquidSlider, LiquidButton). No Compose and no extra libraries.
- `LiquidBar` + `GlassFrame`: glass tab bar. Blur + saturation (Android 12+), lens at the rim (Android 13+, AGSL in `Glass33`).
  Hold the thumb: it swells (78/56), bends the light, stretches with speed; the bar leans up to 4 dp.
- `LiquidToggle`, `LiquidSlider`: white thumb that turns into glass while held (`GlassThumb`).
- `GlassButton`: every button; `Interactive` gives the swell / follow-the-finger / stretch physics. The ⋯ button uses the same physics over real glass.
- `LiquidMath`: all the numbers (tested without a phone).
- Older phones fall back to plain translucent shapes. Battery saver and "Window blur" off also switch the glass off.
- Check the licence of the original library before publishing the app.

## 1.2 Stable
- Filled buttons (Edit...) pick their text colour for contrast on their own fill; the whitening sheen is gone from filled buttons.
- Cards: base is the theme background (lighter in dark mode, darker in light mode). No outlines or borders. The colour strip on the start edge stays as the card colour marker.
- Theme page and the other Settings pages leave room for the dock, the Apply banner and the system navigation area.
- Text-size slider thumb: one lens, no colour splitting, one highlight, no second outline.
- No sliding between screens: tabs, Settings categories and the page title change immediately. Swiping between tabs with a finger still works.
- Concrete texture removed (Paper and Grain stay).
- The ⋯ button grows into its popup (and shrinks back into it); the rotating icon is gone.
- Blur: blur-in removed. Motion blur follows real speed, runs along the direction of travel only, is capped at about 2.5 dp, and disappears when things stop. Page blur behind an open card is lighter (8 dp max). Glass blur and lens are gentler.
- Version shown in About: 1.2 Stable.

## Visual styles (Settings > Theme > Visual style)
Flat (default), Humanist, Material Design 3, Swiss Modernism, Corporate Simplicity. The choice is applied at once and saved.
- `Style` holds every token: palette, radii, border rule, spacing, elevation, what is allowed (textures, gradients, glass).
- `Surface` is the one rounded-shape drawable. All buttons, selectors, inputs, panels, sheets, swatches and the popup use it.
  Root cause of the corner / border / gradient bugs: shapes came from many separate setups, each with its own radius, stroke and clipping.
  Now fill, gradient, stroke and outline are one path built from the real bounds (radius clamped to half the short side, stroke drawn inside the bounds).
- Each style has its own card (`CardDrawable`), button (`GlassButton`), navigation (`LiquidBar` / `GlassFrame`), switch (`LiquidToggle`), slider (`LiquidSlider`), dialogs, menus and press behaviour.
- Typography is untouched. To style fonts per theme, read `Style.id` (0 Flat, 1 Humanist, 2 Material, 3 Swiss, 4 Corporate).
- Horizontal swiping between tabs is off. The tab bar (tap or drag) changes pages.
- Blur: continuous and eased (`Blur.progressive`), updated in quarter-pixel steps; effects are rebuilt only when something changed.

## 1.3.2
- Updated the launcher artwork to the latest supplied Memorius logo across adaptive, themed, and legacy launcher icons.
- Updated the Android version name to 1.3.2 (version code 4) and the About screen version label.
