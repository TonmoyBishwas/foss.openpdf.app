# Changelog

## v0.3.0

- Annotations are now written into the PDF file itself (not an overlay):
  highlight, underline, and strikethrough from text selection; sticky
  notes; freehand drawing with color choices; eraser for existing annotations
- Save in place (when the file allows it) or save a copy, with an atomic
  save pipeline that verifies the output before touching your original
- Unsaved-changes indicator and annotation toolbar

## v0.2.0

- Full-text search with match highlighting and previous/next navigation
- Table of contents (outline) and per-document bookmarks
- Text selection by long-press drag, with copy to clipboard
- Night and sepia reading modes
- Horizontal page-by-page mode alongside vertical scroll
- Read aloud with on-device text-to-speech (auto-advances pages)
- Share and print

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
