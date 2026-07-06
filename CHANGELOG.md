# Changelog

## v0.7.0

- Password protect: save an AES-256 encrypted copy of any document
- Remove password from encrypted documents (once opened with the password)
- Document info: view and edit title, author, subject, and keywords
- Fixed save verification for encrypted output

## v0.6.0

- Create PDFs, fully offline: from gallery images, from camera scans
  (multi-page, optional black & white filter), or from typed text
- Camera scanning needs no camera permission — it uses your camera app
- EXIF-aware image rotation and memory-bounded decoding for large photos

## v0.5.0

- Organize pages: thumbnail grid with multi-select — rotate, duplicate,
  delete, move (reorder), and extract pages to a new PDF
- Merge multiple PDFs into one (Home → Merge PDFs)
- Split / extract page ranges like "1-3, 7" to a new PDF (Home → Split PDF)
- All page operations build a fresh verified file before touching anything

## v0.4.0

- Fill & sign: tap interactive form fields to fill them (text, checkboxes,
  radio buttons, dropdowns) — saved into the PDF
- Signature: draw once, then tap to place it anywhere (stored as vector ink)
- Text boxes (free text) and shapes (rectangle, ellipse, line) with colors
- Form fields get a subtle hint highlight so they're easy to spot

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
