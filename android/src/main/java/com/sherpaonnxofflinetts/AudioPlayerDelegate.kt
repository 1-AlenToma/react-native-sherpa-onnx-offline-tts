package com.sherpaonnxofflinetts

interface AudioPlayerDelegate {
    fun didUpdateVolume(volume: Float)
    fun didFinishPlaying()
}
