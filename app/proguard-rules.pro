# MuPDF uses JNI; keep its classes so native method registration keeps working.
-keep class com.artifex.mupdf.fitz.** { *; }

# PdfBox-Android optionally uses a JPEG2000 decoder we don't bundle.
-dontwarn com.gemalto.jp2.**
