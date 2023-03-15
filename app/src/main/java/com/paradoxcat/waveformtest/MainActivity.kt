package com.paradoxcat.waveformtest

import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.paradoxcat.waveformtest.waveviewer.databinding.ActivityMainBinding
import java.io.IOException
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"

        // A real gravitational wave from https://www.gw-openscience.org/audio/
        // It was a GW150914 binary black hole merger event that LIGO has detected,
        // waveform template derived from GR, whitened, frequency shifted +400 Hz
//        const val EXAMPLE_AUDIO_FILE_NAME = "gravitational_wave_mono_44100Hz_16bit.wav" // takes forever, but loads eventually
        const val EXAMPLE_AUDIO_FILE_NAME = "whistle_mono_44100Hz_16bit.wav" // small enough to load
//        const val EXAMPLE_AUDIO_FILE_NAME = "music_mono_44100Hz_16bit.wav" // too large to load currently!

        const val EXPECTED_NUM_CHANNELS = 1
        const val EXPECTED_SAMPLE_RATE = 44100
        const val EXPECTED_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private lateinit var _binding: ActivityMainBinding
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        try {
            val assetFileDescriptor = assets.openFd(EXAMPLE_AUDIO_FILE_NAME)

            // initialize media player
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(assetFileDescriptor)
            mediaPlayer.prepareAsync()
            _binding.playButton.setOnClickListener {
                try {
                    if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.start()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Could not start playing", e)
                }
            }

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
                Log.e(TAG, "No media tracks found, aborting initialization")
                return
            }
            val mime = mediaExtractor.getTrackFormat(0)
            if (mime.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                mime.getInteger(MediaFormat.KEY_PCM_ENCODING) != EXPECTED_AUDIO_FORMAT
            ) {
                Log.e(TAG, "Expected AudioFormat $EXPECTED_AUDIO_FORMAT, got AudioFormat ${mime.getInteger(MediaFormat.KEY_PCM_ENCODING)}")
                return
            }
            if (mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT) &&
                mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != EXPECTED_NUM_CHANNELS
            ) {
                Log.e(TAG, "Expected $EXPECTED_NUM_CHANNELS channels, got ${mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT)}")
                return
            }
            if (mime.containsKey(MediaFormat.KEY_SAMPLE_RATE) &&
                mime.getInteger(MediaFormat.KEY_SAMPLE_RATE) != EXPECTED_SAMPLE_RATE
            ) {
                Log.e(TAG, "Expected $EXPECTED_SAMPLE_RATE sample rate, got ${mime.getInteger(MediaFormat.KEY_SAMPLE_RATE)}")
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

            // draw it
            _binding.waveformView.setData(rawAudioBuffer)
        } catch (e: IOException) {
            Log.e(TAG, "Exception while reading audio file $EXAMPLE_AUDIO_FILE_NAME", e)
        }
    }
}