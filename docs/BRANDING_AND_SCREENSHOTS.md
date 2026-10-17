# Official Am I Scholar logo and launcher icon (621/24)

The selected source artwork is the black/gold academic logo with graduation cap,
fountain-pen A, globe, books, and the 621/24 signature.

**Use the emblem-only crop for small app icons.** The full wordmark is not legible
at 48 px and is reserved for branding, documentation, and promotional material.

## Sources and generated assets

- `brand/icon-master.webp`: actual selected artwork, emblem-only crop (256 px, compressed).
- `docs/brand/icon-master.webp`: identical GitHub Pages source.
- `app/src/main/res/drawable-nodpi/ic_scholar_foreground.webp`: same image for Android.
- `brand/generate_assets.py`: reproducible Pillow-based PNG generation.
- `brand/icon-{size}.png`: favicons/PWA, 16 through 512 px.
- `brand/icon-maskable-192.png`, `brand/icon-maskable-512.png`: inset for OS masking.
- `brand/apple-touch-icon.png`: iPhone/iPad Home Screen icon.
- `app/src/main/res/mipmap-{density}/ic_launcher.png` and `ic_launcher_round.png`: Android legacy launcher sizes.
- `app/src/main/res/drawable/ic_scholar_foreground_safe.xml`: adaptive icon safe-zone inset.
- Root and `docs/` PWA files use the same brand paths.

The GitHub Actions `Generate approved brand icons` workflow runs on source changes
and commits the generated PNGs. The always-available WebP is the primary web favicon,
sidebar icon, and Android foreground, so the app can build before generation finishes.

## Integrity

This is an image of the approved artwork, not a traced substitute.
Raster upscaling does not create additional photographic detail. For production
store listing graphics, use the full-resolution source from the separately provided
brand-assets package. Android adaptive launcher masks may crop peripheral details;
test on real launchers and dark/light themes.

No app data, Room schema, backup formats, privacy behavior, or research modules
are changed by this branding update.
