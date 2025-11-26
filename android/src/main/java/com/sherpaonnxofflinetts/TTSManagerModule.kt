package com.sherpaonnxofflinetts

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.k2fsa.sherpa.onnx.*
import android.content.Context
import kotlin.concurrent.thread
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ModelLoader(private val context: Context) {

    @Throws(IOException::class)
    fun loadModelFromAssets(assetPath: String, outputFileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(assetPath)
        val outFile = File(context.filesDir, outputFileName)
        FileOutputStream(outFile).use { output -> inputStream.copyTo(output) }
        inputStream.close()
        return outFile.absolutePath
    }

    @Throws(IOException::class)
    fun copyAssetDirectory(assetDir: String, outputDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetDir) ?: return
        if (!outputDir.exists()) outputDir.mkdirs()
        for (file in files) {
            val assetPath = if (assetDir.isEmpty()) file else "$assetDir/$file"
            val outFile = File(outputDir, file)
            if (assetManager.list(assetPath)?.isNotEmpty() == true) {
                copyAssetDirectory(assetPath, outFile)
            } else {
                assetManager.open(assetPath).use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}

class TTSManagerModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var tts: OfflineTts? = null
    private var realTimeAudioPlayer: AudioPlayer? = null
    private val modelLoader = ModelLoader(reactContext)

    private var isStopped = false
    private var currentPromise: Promise? = null
    private var currentOnDone: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun getName(): String = "TTSManager"

    // Initialize TTS
    @ReactMethod
    fun initializeTTS(sampleRate: Double, channels: Int, modelId: String, debug: Boolean, threadsUsed:Int) {
        realTimeAudioPlayer = AudioPlayer(sampleRate.toInt(), channels, object : AudioPlayerDelegate {
            override fun didUpdateVolume(volume: Float) { sendVolumeUpdate(volume) }
            override fun didFinishPlaying() { handlePlaybackFinished() }
        })

        val jsonObject = JSONObject(modelId)
        val modelPath = jsonObject.getString("modelPath")
        val tokensPath = jsonObject.getString("tokensPath")
        val dataDirPath = jsonObject.getString("dataDirPath")

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelPath,
                    tokens = tokensPath,
                    dataDir = dataDirPath
                ),
                numThreads = threadsUsed,
                debug = debug
            )
        )

        tts = OfflineTts(config = config)
        realTimeAudioPlayer?.start()
    }

    // Generate and play text
@ReactMethod
fun generateAndPlay(text: String, sid: Int, speed: Double, promise: Promise) {
    val trimmedText = text.trim()
    if (trimmedText.isEmpty()) {
        promise.reject("EMPTY_TEXT", "Input text is empty")
        return
    }

    // If a previous utterance is running, notify it as "overwritten"
    currentOnDone?.invoke("overwritten")
    currentPromise?.resolve("overwritten")  // or reject if you prefer

    // Stop previous playback
    isStopped = true
    realTimeAudioPlayer?.stopPlayer()

    // Reset state
    isStopped = false
    currentPromise = promise

    // Register new onDone
    currentOnDone = { result: String? ->
        if (result == null) {
            promise.resolve("PlaybackFinished")
        } else {
            promise.resolve(result) // e.g., "overwritten"
        }
        currentPromise = null
        sendEvent("TTS_FINISHED")
    }

    val sentences = splitText(trimmedText, 15)

    thread {
        try {
            for (sentence in sentences) {
                if (isStopped) return@thread
                val processedSentence = if (sentence.endsWith(".")) sentence else "$sentence."
                generateAudio(processedSentence, sid, speed.toFloat())
            }

            // If no audio queued, call onDone immediately
            if (sentences.isEmpty()) {
                mainHandler.post { currentOnDone?.invoke(null) }
            }
        } catch (e: Exception) {
            promise.reject("GENERATION_ERROR", e.message)
        }
    }
}


    // Stop playback
    @ReactMethod
    fun stop() {
        isStopped = true
        currentOnDone = null
        currentPromise = null
        realTimeAudioPlayer?.stopPlayer()
    }

    // Deinitialize TTS
    @ReactMethod
    fun deinitialize() {
        realTimeAudioPlayer?.stopPlayer()
        realTimeAudioPlayer = null
        tts?.release()
        tts = null
    }

    // Split text into chunks
    private fun splitText(text: String, maxWords: Int): List<String> {
        val sentences = mutableListOf<String>()
        val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        var currentIndex = 0
        val totalWords = words.size

        while (currentIndex < totalWords) {
            val endIndex = (currentIndex + maxWords).coerceAtMost(totalWords)
            var chunk = words.subList(currentIndex, endIndex).joinToString(" ")
            val lastPeriod = chunk.lastIndexOf('.')
            val lastComma = chunk.lastIndexOf(',')

            when {
                lastPeriod != -1 -> {
                    val sentence = chunk.substring(0, lastPeriod + 1).trim()
                    sentences.add(sentence)
                    currentIndex += sentence.split("\\s+".toRegex()).size
                }
                lastComma != -1 -> {
                    val sentence = chunk.substring(0, lastComma + 1).trim()
                    sentences.add(sentence)
                    currentIndex += sentence.split("\\s+".toRegex()).size
                }
                else -> {
                    sentences.add(chunk.trim())
                    currentIndex += maxWords
                }
            }
        }

        return sentences
    }

    // Generate audio chunk
    private fun generateAudio(text: String, sid: Int, speed: Float) {
        val audio = tts?.generate(text, sid, speed) ?: return
        realTimeAudioPlayer?.enqueueAudioData(audio.samples, audio.sampleRate)
    }

    // Called by AudioPlayer when playback finishes
    private fun handlePlaybackFinished() {
        if (isStopped) return
        currentOnDone?.invoke()
        currentOnDone = null
    }

    // Volume update
    private fun sendVolumeUpdate(volume: Float) {
        if (!reactContext.hasActiveCatalystInstance()) return
        val params = Arguments.createMap()
        params.putDouble("volume", volume.toDouble())
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("VolumeUpdate", params)
    }

    // Send general events
    private fun sendEvent(eventName: String) {
        if (!reactContext.hasActiveCatalystInstance()) return
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, null)
    }
}
