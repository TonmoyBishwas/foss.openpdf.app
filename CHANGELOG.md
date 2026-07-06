# Changelog

## v0.1.0

- PDF viewer MVP powered by MuPDF: open PDFs from the file picker or from
  other apps (ACTION_VIEW / share), continuous scroll, pinch and double-tap
  zoom, go-to-page, page indicator
- Password-protected PDFs supported with retry dialog
- Recent files on Home with page count, last-opened time, and resume at last page
- Keep-screen-on while reading

## v0.0.1

- Project scaffold: Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore
- Calm Material theme (light + dark) derived from brand seed `#2C3E50`
- Home screen placeholder and Settings with theme selection
- MuPDF engine dependency wired in (viewer lands in v0.1.0)
- CI (build + tests) and signed-release workflows
