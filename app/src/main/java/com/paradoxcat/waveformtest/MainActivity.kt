package com.paradoxcat.waveformtest

import android.media.AudioFormat
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.paradoxcat.waveformtest.viewmodel.MediaPlayerViewModel
import com.paradoxcat.waveformtest.waveviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"

        // A real gravitational wave from https://www.gw-openscience.org/audio/
        // It was a GW150914 binary black hole merger event that LIGO has detected,
        // waveform template derived from GR, whitened, frequency shifted +400 Hz
//         const val EXAMPLE_AUDIO_FILE_NAME = "gravitational_wave_mono_44100Hz_16bit.wav" // takes forever, but loads eventually
//        const val EXAMPLE_AUDIO_FILE_NAME = "whistle_mono_44100Hz_16bit.wav" // small enough to load
        const val EXAMPLE_AUDIO_FILE_NAME =
            "music_mono_44100Hz_16bit.wav" // too large to load currently!

        const val EXPECTED_NUM_CHANNELS = 1
        const val EXPECTED_SAMPLE_RATE = 44100
        const val EXPECTED_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private lateinit var _binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        val mediaPlayerViewModel: MediaPlayerViewModel by viewModels()
        val assetFileDescriptor = assets.openFd(EXAMPLE_AUDIO_FILE_NAME)

        // Set default audio to play
        mediaPlayerViewModel.setMedia(assetFileDescriptor)


        _binding.playButton.setOnClickListener {
            mediaPlayerViewModel.togglePlayPause()
        }

        // Observers
        mediaPlayerViewModel.title.observe(this) { title ->
            _binding.titleTextView.text = title
        }
        mediaPlayerViewModel.timestamp.observe(this) { timestamp ->
            _binding.timestampTextView.text = timestamp
        }
        mediaPlayerViewModel.duration.observe(this) { duration ->
            _binding.durationTextView?.text = duration.toString()
        }
        mediaPlayerViewModel.waveformData.observe(this) { waveformData ->
            _binding.waveformView.setData(waveformData)
        }
    }
}
