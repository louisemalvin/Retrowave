package com.paradoxcat.waveviewer

import android.animation.ObjectAnimator
import android.media.AudioFormat
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.paradoxcat.waveviewer.databinding.ActivityMainBinding
import com.paradoxcat.waveviewer.util.TimeConverter.getFormattedTime
import com.paradoxcat.waveviewer.util.TimeConverter.millisecondsToProgress
import com.paradoxcat.waveviewer.util.TimeConverter.progressToMilliseconds
import com.paradoxcat.waveviewer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
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
        const val MAX_PROGRESS_VALUE = 10000
        const val EXPECTED_NUM_CHANNELS = 1
        const val EXPECTED_SAMPLE_RATE = 44100
        const val EXPECTED_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val SEEK_ANIMATION_DURATION = 200L
    }

    private lateinit var _binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        val mainViewModel: MainViewModel by viewModels()
        val assetFileDescriptor = assets.openFd(EXAMPLE_AUDIO_FILE_NAME)

//        // Set default audio to play
//        _binding.loadFile.setOnClickListener {
//            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//                type = "audio/wav"
//                startActivityForResult(this, 0)
//            }
//        }

        mainViewModel.setMedia(assetFileDescriptor, EXAMPLE_AUDIO_FILE_NAME)

        _binding.playButton.setOnClickListener {
            mainViewModel.togglePlayPause()
        }

        // SeekBar listener when user drags the thumb
        _binding.playbackSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Update the media player
                    animateSeekTo(progress)
                    mainViewModel.duration.observe(this@MainActivity) { duration ->
                        mainViewModel.seekTo(progressToMilliseconds(duration, progress))
                    }
                    // Update the timestamp
                    mainViewModel.updateTimestamp()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        // observers
        mainViewModel.title.observe(this) { title ->
            _binding.titleTextView.text = title
        }
        mainViewModel.timestamp.observe(this) { timestamp ->
            _binding.timestampTextView.text = getFormattedTime(timestamp)
            // update seekbar position as well
            mainViewModel.duration.observe(this) { duration ->
                _binding.playbackSeekBar.progress =
                    millisecondsToProgress(timestamp, duration.toInt())
            }
        }
        mainViewModel.isPlaying.observe(this) { isPlaying ->
                _binding.playbackIndicatorView.setData(isPlaying)
        }
        mainViewModel.duration.observe(this) { duration ->
            _binding.durationTextView.text = getFormattedTime(duration)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            mainViewModel.waveformData.observe(this@MainActivity) { waveformData ->
                _binding.waveformView.setData(waveformData)
            }
        }

    }

    private fun animateSeekTo(endValue: Int) {
        val animator = ObjectAnimator.ofInt(
            _binding.playbackSeekBar,
            "progress",
            _binding.playbackSeekBar.progress,
            endValue
        )
        animator.duration = SEEK_ANIMATION_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        Log.i(TAG, "animateSeekTo: ${_binding.playbackSeekBar.progress} -> $endValue")
    }
}