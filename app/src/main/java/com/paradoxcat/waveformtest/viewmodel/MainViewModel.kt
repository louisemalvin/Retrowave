package com.paradoxcat.waveformtest.viewmodel

import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxcat.waveformtest.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MainViewModel : ViewModel() {
    companion object {
        const val REFRESH_RATE = 17L
    }

    private val _waveformData = MutableLiveData<IntArray>()
    private val _title = MutableLiveData<String>()
    private val _isPlaying = MutableLiveData<Boolean>()
    private val _timestamp = MutableLiveData<Long>()
    private val _duration = MutableLiveData<Long>()

    val waveformData: LiveData<IntArray> get() = _waveformData
    val title: LiveData<String> get() = _title
    val isPlaying: LiveData<Boolean> get() = _isPlaying
    val timestamp: LiveData<Long> get() = _timestamp
    val duration: LiveData<Long> get() = _duration

    private val mediaPlayer = MediaPlayer()
    private var mediaPlayerExist = false

    fun setMedia(assetFileDescriptor: AssetFileDescriptor, title: String) {
        if (!mediaPlayerExist) {
            mediaPlayer.setDataSource(assetFileDescriptor)
            mediaPlayer.prepareAsync()
            extractRawData(assetFileDescriptor)
            setMetadata()
            _title.value = title
            mediaPlayerExist = true
        }
    }

    fun setMetadata() {
        _timestamp.value = mediaPlayer.currentPosition.toLong()
        _duration.value = mediaPlayer.duration.toLong()
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _isPlaying.value = mediaPlayer.isPlaying
        } else {
            mediaPlayer.start()
            _isPlaying.value = mediaPlayer.isPlaying
            updatePlaybackTimestamp()
        }
    }

    fun updateTimestamp() {
        _timestamp.value = mediaPlayer.currentPosition.toLong()
    }

    private fun updatePlaybackTimestamp() {
        viewModelScope.launch {
            while (mediaPlayer.isPlaying) {
                updateTimestamp()
                delay(REFRESH_RATE)
            }
            updateTimestamp()
        }
    }

    fun seekTo(milliseconds: Long) {
        mediaPlayer.seekTo(milliseconds.toInt())
    }

    private fun extractRawData(assetFileDescriptor: AssetFileDescriptor) {
        // allocate a buffer
        var fileSize = assetFileDescriptor.length // in bytes
        if (fileSize == AssetFileDescriptor.UNKNOWN_LENGTH) {
            fileSize = 30 * 1024 * 1024 // 30 MB would accommodate ~6 minutes of 44.1 KHz, 16-bit uncompressed audio
        } else if (fileSize > Int.MAX_VALUE) {
            fileSize = Int.MAX_VALUE.toLong()
        }
        val rawAudioBuffer = ByteBuffer.allocate(fileSize.toInt())

        // extract raw audio samples from file into the buffer
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(assetFileDescriptor)
        assetFileDescriptor.close()

        // Assuming single track, PCM, 1-channel, and 16-bit format in the buffer.
        // Otherwise we would have to first decode it e.g. using MediaCodec
        // Sanity checks:
        if (mediaExtractor.trackCount < 1) {
            Log.e(MainActivity.TAG, "No media tracks found, aborting initialization")
            return
        }
        val mime = mediaExtractor.getTrackFormat(0)
        if (mime.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
            mime.getInteger(MediaFormat.KEY_PCM_ENCODING) != MainActivity.EXPECTED_AUDIO_FORMAT
        ) {
            Log.e(
                MainActivity.TAG, "Expected AudioFormat ${MainActivity.EXPECTED_AUDIO_FORMAT}, got AudioFormat ${mime.getInteger(
                    MediaFormat.KEY_PCM_ENCODING)}")
            return
        }
        if (mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT) &&
            mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != MainActivity.EXPECTED_NUM_CHANNELS
        ) {
            Log.e(
                MainActivity.TAG, "Expected ${MainActivity.EXPECTED_NUM_CHANNELS} channels, got ${mime.getInteger(
                    MediaFormat.KEY_CHANNEL_COUNT)}")
            return
        }
        if (mime.containsKey(MediaFormat.KEY_SAMPLE_RATE) &&
            mime.getInteger(MediaFormat.KEY_SAMPLE_RATE) != MainActivity.EXPECTED_SAMPLE_RATE
        ) {
            Log.e(
                MainActivity.TAG, "Expected ${MainActivity.EXPECTED_SAMPLE_RATE} sample rate, got ${mime.getInteger(
                    MediaFormat.KEY_SAMPLE_RATE)}")
            return
        }

        // dump raw data into a buffer
        mediaExtractor.selectTrack(0)
        var bufferSize = 0
        var samplesRead = mediaExtractor.readSampleData(rawAudioBuffer, 0)
        while (samplesRead > 0) {
            bufferSize += samplesRead
            mediaExtractor.advance()
            samplesRead = mediaExtractor.readSampleData(rawAudioBuffer, bufferSize)
        }
        mediaExtractor.release()
        _waveformData.value = transformRawData(rawAudioBuffer)
    }

    private fun transformRawData(buffer: ByteBuffer): IntArray {
        val nSamples = buffer.limit() / 2 // assuming 16-bit PCM mono
        val waveForm = IntArray(nSamples)
        for (i in 1 until buffer.limit() step 2) {
            waveForm[i / 2] = (buffer[i].toInt() shl 8) or buffer[i - 1].toInt()
        }
        return waveForm
    }

    override fun onCleared() {
        mediaPlayer.release()
        super.onCleared()
    }
}