package com.paradoxcat.waveformtest

import android.media.AudioFormat
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.paradoxcat.waveformtest.util.TimeConverter.getFormattedTime
import com.paradoxcat.waveformtest.util.TimeConverter.millisecondsToProgress
import com.paradoxcat.waveformtest.util.TimeConverter.progressToMilliseconds
import com.paradoxcat.waveformtest.viewmodel.MediaPlayerViewModel
import com.paradoxcat.waveformtest.waveviewer.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {

        const val TAG = "MainActivity"

        // A real gravitational wave from https://www.gw-openscience.org/audio/
        // It was a GW150914 binary black hole merger event that LIGO has detected,
        // waveform template derived from GR, whitened, frequency shifted +400 Hz
        // const val EXAMPLE_AUDIO_FILE_NAME = "gravitational_wave_mono_44100Hz_16bit.wav" // takes forever, but loads eventually
//        const val EXAMPLE_AUDIO_FILE_NAME = "whistle_mono_44100Hz_16bit.wav" // small enough to load
        const val EXAMPLE_AUDIO_FILE_NAME = "music_mono_44100Hz_16bit.wav" // too large to load currently!
        const val MAX_PROGRESS_VALUE = 10000
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

        // SeekBar listener when user drags the thumb
        _binding.playbackSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Update the media player
                    mediaPlayerViewModel.duration.observe(this@MainActivity) { duration ->
                        mediaPlayerViewModel.seekTo(progressToMilliseconds(duration, progress))
                    }
                    // Update the timestamp
                    mediaPlayerViewModel.updateTimestamp()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // observers
        mediaPlayerViewModel.title.observe(this) { title ->
            _binding.titleTextView.text = title
        }
        mediaPlayerViewModel.timestamp.observe(this) { timestamp ->
            _binding.timestampTextView.text = getFormattedTime(timestamp)
            // update seekbar position as well
            mediaPlayerViewModel.duration.observe(this) { duration ->
                _binding.playbackSeekBar.progress =
                    millisecondsToProgress(timestamp, duration.toInt())
            }
        }
        mediaPlayerViewModel.duration.observe(this) { duration ->
            _binding.durationTextView?.text = getFormattedTime(duration)
        }
        mediaPlayerViewModel.waveformData.observe(this) { waveformData ->
            _binding.waveformView.setData(waveformData)
        }
    }
}
