# Memorius 1.3.2 — change and audit notes

## Applied
- Replaced the adaptive launcher foreground in both day/night resource directories with the supplied latest `logo.png` artwork.
- Regenerated the Android 13+ monochrome themed-icon asset from the new artwork.
- Regenerated legacy launcher PNGs for mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi.
- Set Android `versionName` to `1.3.2` and incremented `versionCode` from 3 to 4.
- Updated the Settings > About version label to `1.3.2`.
- Added the 1.3.2 entry to the README.

## Static checks
- Parsed every XML file under `app/src/main` successfully.
- Verified all launcher image dimensions against their density targets.
- Confirmed the version metadata and About label are aligned.

## Not verified
- A Gradle/Android build was not run in this environment because Gradle and the Android SDK are not installed. The project remains configured for the existing Gradle 8.7 / Android Gradle Plugin 8.5.2 GitHub Actions build path.
- No broad application logic or dependency changes were made in this release-specific pass; the existing dependency set was preserved.

## Visual-system refinement pass
- Kept the native Java / Android component architecture and existing dependency set.
- Refined the shared primary icon renderer with theme-specific stroke construction; Material and Corporate now use folder, dashboard/stack, and gear-shaped primary glyphs, while the multifunction control uses three horizontal controls.
- Increased the Flat card marker's reserved gutter and inset its rounded accent to follow the card silhouette; added a restrained Flat card boundary.
- Kept full-view actions in a dedicated, non-scrolling action rail, ensured 48 dp minimum targets, disabled clipping around the rail, and brought its fade-in forward so actions become legible earlier in the expansion.
- Cached and quantized native blur effects in half-pixel increments to avoid allocating a new RenderEffect on every animation sample while preserving eased blur progression.
- XML resources parse successfully and edited Java files have balanced braces. A device/Gradle build and interactive rendering tests could not be run in this environment because Gradle and the Android SDK are unavailable.
