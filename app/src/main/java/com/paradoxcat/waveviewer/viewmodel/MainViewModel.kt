package com.paradoxcat.waveviewer.viewmodel

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxcat.waveviewer.util.TimeConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val assetManager: AssetManager,
    private val mediaPlayer: MediaPlayer
) : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
        const val REFRESH_RATE = 17L

        // A real gravitational wave from https://www.gw-openscience.org/audio/
        // It was a GW150914 binary black hole merger event that LIGO has detected,
        // waveform template derived from GR, whitened, frequency shifted +400 Hz
        const val EXAMPLE_AUDIO_FILE_NAME_SMALL = "whistle_mono_44100Hz_16bit.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_MEDIUM = "gravitational_wave_mono_44100Hz_16bit.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_LARGE = "music_mono_44100Hz_16bit.wav"
        const val EXPECTED_NUM_CHANNELS = 1
        const val EXPECTED_SAMPLE_RATE = 44100
        const val EXPECTED_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val DEFAULT_TOAST_ERROR_MESSAGE = "Preparing media player..."
        enum class MediaPlayerState {
            INIT, PREPARED, PLAYING
        }
    }

    private val _waveformData = MutableLiveData(intArrayOf())
    private val _currentWaveformIndex = MutableLiveData(0)
    private val _title = MutableLiveData("")
    private val _state = MutableLiveData(MediaPlayerState.INIT)
    private val _timestamp = MutableLiveData(0L)
    private val _duration = MutableLiveData(0L)
    private val _errorMessage = MutableLiveData("")

    private var currentMedia = 0

    val waveformData: LiveData<IntArray> get() = _waveformData
    val currentWaveform: LiveData<Int> get() = _currentWaveformIndex
    val title: LiveData<String> get() = _title
    val state: LiveData<MediaPlayerState> get() = _state
    val timestamp: LiveData<Long> get() = _timestamp
    val duration: LiveData<Long> get() = _duration
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        // set default audio to play
        setMedia()
    }

    override fun onCleared() {
        mediaPlayer.release()
        super.onCleared()
    }

    /**
     * Converts raw audio data to an array of integers.
     *
     * Raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM).
     * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate,
     * 16-bits per sample, and is already without WAV header.
     */
    private fun transformRawData(buffer: ByteBuffer): IntArray {
        val nSamples = buffer.limit() / 2 // assuming 16-bit PCM mono
        val waveform = IntArray(nSamples)
        for (i in 1 until buffer.limit() step 2) {
            waveform[i / 2] = (buffer[i].toInt() shl 8) or buffer[i - 1].toInt()
        }
        return waveform
    }

    /**
     * Extracts raw audio data from an asset file descriptor.
     */
    private fun extractRawData(assetFileDescriptor: AssetFileDescriptor) {
        // allocate a buffer
        var fileSize = assetFileDescriptor.length // in bytes
        if (fileSize==AssetFileDescriptor.UNKNOWN_LENGTH) {
            fileSize =
                30 * 1024 * 1024 // 30 MB would accommodate ~6 minutes of 44.1 KHz, 16-bit uncompressed audio
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
            Log.e(TAG, "No media tracks found, aborting initialization")
            return
        }
        val mime = mediaExtractor.getTrackFormat(0)
        if (mime.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
            mime.getInteger(MediaFormat.KEY_PCM_ENCODING)!=EXPECTED_AUDIO_FORMAT
        ) {
            Log.e(
                TAG, "Expected AudioFormat ${EXPECTED_AUDIO_FORMAT}, got AudioFormat ${
                    mime.getInteger(
                        MediaFormat.KEY_PCM_ENCODING
                    )
                }"
            )
            return
        }
        if (mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT) &&
            mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT)!=EXPECTED_NUM_CHANNELS
        ) {
            Log.e(
                TAG, "Expected ${EXPECTED_NUM_CHANNELS} channels, got ${
                    mime.getInteger(
                        MediaFormat.KEY_CHANNEL_COUNT
                    )
                }"
            )
            return
        }
        if (mime.containsKey(MediaFormat.KEY_SAMPLE_RATE) &&
            mime.getInteger(MediaFormat.KEY_SAMPLE_RATE)!=EXPECTED_SAMPLE_RATE
        ) {
            Log.e(
                TAG, "Expected ${EXPECTED_SAMPLE_RATE} sample rate, got ${
                    mime.getInteger(
                        MediaFormat.KEY_SAMPLE_RATE
                    )
                }"
            )
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

    /**
     * Resets media player and metadata.
     */
    private fun reset() {
        mediaPlayer.reset()
        _waveformData.value = intArrayOf()
        _currentWaveformIndex.value = 0
        _title.value = ""
        _state.value = MediaPlayerState.INIT
        _timestamp.value = 0L
        _duration.value = 0L
    }

    /**
     * Returns true if media player is playing, false otherwise.
     */
    private fun update() {
        try {
            if (mediaPlayer.isPlaying) {
                _state.value = MediaPlayerState.PLAYING
                return
            } else {
                _state.value = MediaPlayerState.PREPARED
            }
        } catch (e: Exception) {
            _state.value = MediaPlayerState.INIT
            Log.e(TAG, "Media player error: ${e.message}")
            _errorMessage.value = e.message
        }
    }

    /**
     * Updates timestamp and waveform index while playing.
     *
     * This function refresh rate depends on REFRESH_RATE constant.
     */
    private fun updatePlayLoop() {
        update()
        viewModelScope.launch {
            while (_state.value==MediaPlayerState.PLAYING) {
                updateTimestampAndWaveformIndex()
                delay(REFRESH_RATE)
                update()
            }
            updateTimestampAndWaveformIndex()
            Log.i(TAG, "Stopped updating timestamp and waveform index")
        }
    }

    /**
     * Updates timestamp and waveform index.
     *
     * Call this function whenever the timestamp changes.
     */
    private fun updateTimestampAndWaveformIndex() {
        // update timestamp
        _timestamp.value = mediaPlayer.currentPosition.toLong()

        // update waveform index
        if (_waveformData.value==null) return

        val waveformLength = _waveformData.value!!.size
        val progress = TimeConverter.millisecondsToProgress(_timestamp.value!!, _duration.value!!)
        val index =
            (waveformLength.toFloat() * progress.toFloat() / TimeConverter.MAX_PROGRESS_VALUE).toInt() - 1
        if (index < 0) return
        _currentWaveformIndex.value = index
    }

    /**
     * Sets audio to play using default audio file in assets.
     */
    private fun setMedia(title: String) {
        // pre-condition check:
        // make sure media player is on idle state
        reset()
        try {
            Log.d(TAG, "Trying to load media: $title")
            // load file from assets to prepare media player and waveform data
            val assetFileDescriptor = assetManager.openFd(title)
            mediaPlayer.setDataSource(assetFileDescriptor)
            extractRawData(assetFileDescriptor)
            // asset no longer needed, close it
            assetFileDescriptor.close()
            // wait media player to be prepared before setting metadata
            mediaPlayer.prepare()
            // set metadata
            _title.value = title
            _timestamp.value = mediaPlayer.currentPosition.toLong()
            _duration.value = mediaPlayer.duration.toLong()
            update()
            Log.d(TAG, "State after loading file: ${_state.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set media: ${e.message}")
            _errorMessage.value = e.message
        }
    }

    /**
     * Sets audio to play using default audio file in assets.
     */
    fun setMedia() {
        when (currentMedia) {
            0 -> setMedia(EXAMPLE_AUDIO_FILE_NAME_LARGE)
            1 -> setMedia(EXAMPLE_AUDIO_FILE_NAME_MEDIUM)
            2 -> setMedia(EXAMPLE_AUDIO_FILE_NAME_SMALL)
        }
        currentMedia = (currentMedia + 1) % 3
    }

    /**
     * Sets audio to play using default audio file in assets.
     */
    fun setMedia(assetFileDescriptor: AssetFileDescriptor, title: String) {
        // pre-condition check:
        // make sure media player is on idle state
        reset()

        try {
            // load file from assets to prepare media player and waveform data
            Log.d(TAG, "Trying to load external media: $title")
            mediaPlayer.setDataSource(assetFileDescriptor)
            extractRawData(assetFileDescriptor)
            // asset no longer needed, close it
            // wait media player to be prepared before setting metadata
            mediaPlayer.prepare()
            // set metadata
            _title.value = title
            _timestamp.value = mediaPlayer.currentPosition.toLong()
            _duration.value = mediaPlayer.duration.toLong()
            update()
            Log.d(TAG, "State after loading external file: ${_state.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set media: ${e.message}")
            _errorMessage.value = e.message
        }
    }

    /**
     * Sets media player to play or pause state.
     */
    fun togglePlayPause() {
        // pre-condition check:
        update()
        if (_state.value==MediaPlayerState.INIT) {
            Log.e(TAG, "Media player is not ready")
            _errorMessage.value = DEFAULT_TOAST_ERROR_MESSAGE
            return
        }
        if (_state.value==MediaPlayerState.PLAYING) {
            // pause and update state
            mediaPlayer.pause()
            return
        }
        if (_state.value==MediaPlayerState.PREPARED) {
            // start and call loop to update timestamp & waveform index
            mediaPlayer.start()
            updatePlayLoop()
        }
    }

    /**
     * Sets media player to pause state if it is playing.
     */
    fun stop() {
        if (_state.value==MediaPlayerState.INIT) {
            Log.e(TAG, "Media player is not initialized")
            _errorMessage.value = DEFAULT_TOAST_ERROR_MESSAGE
            return
        }
        if (_state.value==MediaPlayerState.PLAYING) {
            mediaPlayer.pause()
        }
        mediaPlayer.seekTo(0)
        update()
        updateTimestampAndWaveformIndex()
    }

    /**
     * Sets media player to certain timestamp.
     */
    fun seekTo(milliseconds: Long) {
        // pre-condition check:
        // return if media player is not ready
        if (_state.value==MediaPlayerState.INIT) {
            Log.e(TAG, "Media player is not ready")
            _errorMessage.value = DEFAULT_TOAST_ERROR_MESSAGE
            return
        }

        Log.i(TAG, "try seek to $milliseconds")
        mediaPlayer.seekTo(milliseconds.toInt())
        Log.i(TAG, "result: ${mediaPlayer.currentPosition}")
        updateTimestampAndWaveformIndex()
    }
}