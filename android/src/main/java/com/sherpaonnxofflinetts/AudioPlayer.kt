package com.sherpaonnxofflinetts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

class AudioPlayer(
    private val sampleRate: Int,
    private val channels: Int,
    private val delegate: AudioPlayerDelegate?
) {
    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<FloatArray>()
    @Volatile private var isRunning = false
    @Volatile private var sentCompletion = false
    @Volatile private var inputCompleted = false
    @Volatile private var totalFramesWritten = 0
    private var sendFinish = false
    private var playbackThread: Thread? = null

    private val chunkDurationMs = 200L
    private val samplesPerChunk = ((sampleRate * channels * chunkDurationMs) / 1000).toInt()
    private val accumulationBuffer = mutableListOf<Float>()
    private val volumesQueue = LinkedBlockingQueue<Float>()

    private val volumeUpdateIntervalMs: Long = 200
    private val scalingFactor = 0.42f

    private val mainHandler = Handler(Looper.getMainLooper())

    private val volumeUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            volumesQueue.poll() // Optional: send to JS here if needed
            if (isRunning) mainHandler.postDelayed(this, volumeUpdateIntervalMs)
        }
    }

    private val startCompletionChecker = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (inputCompleted && !sendFinish){
                checkCompletion()  // check if finished
            }

            // If still waiting for input completion, schedule next check
            if (isRunning) {
                mainHandler.postDelayed(this, volumeUpdateIntervalMs) 
            }
        }
        }

    fun start() {
        val channelConfig = if (channels == 1)
            AudioFormat.CHANNEL_OUT_MONO
        else
            AudioFormat.CHANNEL_OUT_STEREO

        val bufferSizeInSamples = (sampleRate * 20) / 1000
        val bufferSizeInBytes = bufferSizeInSamples * 4 * channels
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_FLOAT)
        val finalBufferSize = maxOf(bufferSizeInBytes, minBufferSize)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(finalBufferSize)
            .build()

        audioTrack?.play()
        isRunning = true
        mainHandler.post(volumeUpdateRunnable)
        mainHandler.post(startCompletionChecker)

        playbackThread = Thread {
            Log.d("AudioPlayer", "Playback thread started.")
            while (isRunning) {
                try {
                    val samples = audioQueue.take()
                    synchronized(this) {
                        accumulationBuffer.addAll(samples.asList())
                        processAccumulatedSamples()
                        totalFramesWritten += samples.size / channels
                    }
                    audioTrack?.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        playbackThread?.start()
    }

    private fun processAccumulatedSamples() {
        while (accumulationBuffer.size >= samplesPerChunk) {
            val chunk = accumulationBuffer.subList(0, samplesPerChunk).toFloatArray()
            accumulationBuffer.subList(0, samplesPerChunk).clear()
            val volume = computePeak(chunk) * scalingFactor
            volumesQueue.offer(volume)
        }
    }

    fun enqueueAudioData(samples: FloatArray, sr: Int) {
        if (sr != sampleRate) throw IllegalArgumentException("Sample rate mismatch")
        synchronized(this) { sentCompletion = false }
        audioQueue.offer(samples)
    }

    fun markInputComplete() {
        synchronized(this) {
            inputCompleted = true
        }
    }

    private fun computePeak(data: FloatArray): Float {
        var maxVal = 0f
        for (sample in data) maxVal = maxOf(maxVal, abs(sample))
        return maxVal
    }

    private fun checkCompletion() {
        synchronized(this) {
            if (!sentCompletion && inputCompleted && audioQueue.isEmpty() && audioTrack?.playbackHeadPosition ?: 0 >0 && audioTrack?.playbackHeadPosition ?: 0 >= totalFramesWritten) {
                sentCompletion = true
                inputCompleted = false
                sendFinish = true
                mainHandler.post { delegate?.didFinishPlaying("PlaybackFinished") }
            }
        }
    }

    fun clearQueue() {
        audioQueue.clear()
        sendFinish = false
        synchronized(this) {
            accumulationBuffer.clear()
            volumesQueue.clear()
            sentCompletion = true
            inputCompleted = false
            totalFramesWritten=0
        }
        audioTrack?.stop()
        audioTrack?.flush()
        audioTrack?.play()
        mainHandler.post { delegate?.didFinishPlaying("Stopped") }
    }

    fun stopPlayer() {
        playbackThread?.interrupt()
        sendFinish = false
        playbackThread?.join()
        mainHandler.removeCallbacks(volumeUpdateRunnable)
        mainHandler.removeCallbacks(startCompletionChecker)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        synchronized(this) {
            accumulationBuffer.clear()
            volumesQueue.clear()
            totalFramesWritten=0
            isRunning = false
        }
        mainHandler.post { delegate?.didFinishPlaying("Stopped") }
    }
}
