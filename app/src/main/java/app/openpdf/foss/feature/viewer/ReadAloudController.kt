package app.openpdf.foss.feature.viewer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Reads page text aloud with Android's on-device TTS. One page per utterance;
 * [onPageFinished] fires on the main thread when an utterance completes so the
 * caller can feed the next page.
 */
class ReadAloudController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onPageFinished: (() -> Unit)? = null

    fun speak(text: String) {
        if (text.isBlank()) {
            _isSpeaking.value = false
            onPageFinished?.invoke()
            return
        }
        val engine = tts
        if (engine != null && ready) {
            doSpeak(engine, text)
            return
        }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.let { doSpeak(it, text) }
            } else {
                _isSpeaking.value = false
            }
        }
    }

    private fun doSpeak(engine: TextToSpeech, text: String) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "page-end") onPageFinished?.invoke()
            }
        })
        _isSpeaking.value = true
        // TTS input is capped (~4000 chars); split long pages into queued chunks.
        val chunks = text.chunked(3500)
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = if (index == chunks.lastIndex) "page-end" else "chunk-$index"
            engine.speak(chunk, mode, null, id)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        _isSpeaking.value = false
    }
}
