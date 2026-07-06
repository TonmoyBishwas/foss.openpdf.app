# OpenPDF

**A completely free, open-source PDF app for Android.** View, annotate, fill & sign, organize, and create PDFs — with no ads, no subscriptions, no accounts, and no internet access. Everything happens on your device.

OpenPDF aims to be a full alternative to Adobe Acrobat Reader, including the features Acrobat locks behind a subscription.

## Features

**Available now**
- 🎨 Calm Material design — light & dark themes

**Planned (in milestone order)**
- 📄 Fast PDF viewing: smooth zoom & scroll, night mode, outline, bookmarks, search
- 🔊 Read aloud (on-device text-to-speech)
- ✏️ Annotations saved into the PDF: highlight, underline, strikethrough, notes, drawing, text boxes
- 📝 Fill & sign forms
- 🗂️ Organize pages: rotate, reorder, delete, extract, merge, split
- 📷 Create PDFs from images, camera scans, or text
- 🔒 Password protect & remove passwords
- 🚫 What we'll never add: ads, tracking, paywalls, or cloud dependencies

## Privacy

OpenPDF requests **no internet permission** — it is technically incapable of sending your documents anywhere. No analytics, no telemetry, no accounts.

## Building

Requirements: JDK 17+, Android SDK (compileSdk 36).

```
./gradlew assembleDebug
```

## Tech

Kotlin · Jetpack Compose · Material 3 · [MuPDF](https://mupdf.com/) (AGPL-3.0) · [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) (Apache-2.0)

## License

[GPL-3.0](LICENSE). OpenPDF bundles MuPDF, which is licensed under AGPL-3.0; the combined work is distributed under terms compatible with both.
