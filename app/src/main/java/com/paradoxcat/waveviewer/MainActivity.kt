package com.paradoxcat.waveviewer

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.paradoxcat.waveviewer.databinding.ActivityMainBinding
import com.paradoxcat.waveviewer.util.TimeConverter.getFormattedTime
import com.paradoxcat.waveviewer.util.TimeConverter.millisecondsToProgress
import com.paradoxcat.waveviewer.util.TimeConverter.progressToMilliseconds
import com.paradoxcat.waveviewer.view.WaveformSlideBar
import com.paradoxcat.waveviewer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var _binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri==null) {
                    return@registerForActivityResult
                }
                try {
                    val fileName = queryName(this.contentResolver, uri)
                    val assetFileDescriptor = this.contentResolver.openAssetFileDescriptor(uri, "r")
                    if (assetFileDescriptor==null && fileName==null) {
                        return@registerForActivityResult
                    }
                    mainViewModel.setMedia(assetFileDescriptor!!, fileName!!)
                    assetFileDescriptor.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading file", e)
                    Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show()
                }

            }

        /* Listeners */

        // load button listener
        _binding.loadButton.setOnClickListener {
            mainViewModel.setMedia()
//            getContent.launch("audio/wav")
        }

        // play button listener
        _binding.playButton.setOnClickListener {
            mainViewModel.play()
            // get lock state, if media is playing, keep the button pressed
            val currentPlayState = mainViewModel.isPlaying.value!!
            // animate button press
            _binding.playButton.press(currentPlayState)

        }

        // pause button listener
        _binding.pauseButton.setOnClickListener {
            mainViewModel.pause()
            // animate button press
            _binding.pauseButton.press(false)
        }

        // Seekbar listener
        _binding.playbackSeekBar.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    // pre-condition check
                    // only update if seekbar is changed by user
                    if (!fromUser) {
                        return
                    }

                    val totalDuration = mainViewModel.duration.value!!
                    // update the media player
                    mainViewModel.seekTo(progressToMilliseconds(totalDuration, progress))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // nothing to track currently
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // nothing to track currently
                }
            })

        /* Observers */

        // title name observer
        mainViewModel.title.observe(this) { title ->
            _binding.titleTextView.text = title
        }

        // total audio duration observer
        mainViewModel.duration.observe(this) { duration ->
            _binding.durationTextView.text = getFormattedTime(duration)
        }

        // audio playback state observer
        mainViewModel.isPlaying.observe(this) { isPlaying ->
            // update playback indicator & play button lock state
            _binding.playbackIndicatorView.setData(isPlaying)
            _binding.playButton.press(isPlaying)
        }

        // timestamp observer
        mainViewModel.timestamp.observe(this) { timestamp ->
            // update timestamp & seekbar progress
            _binding.timestampTextView.text = getFormattedTime(timestamp)
            val duration = mainViewModel.duration.value
            _binding.playbackSeekBar.progress = millisecondsToProgress(timestamp, duration!!)
        }

        // waveform data observer
        mainViewModel.waveformData.observe(this) { waveformData ->
            _binding.waveformView.setData(waveformData)
        }

        // current waveform index observer
        mainViewModel.currentWaveform.observe(this) { currentWaveform ->
            // pre-condition check
            if (currentWaveform >= mainViewModel.waveformData.value!!.size) {
                return@observe
            }
            
            val waveform = mainViewModel.waveformData.value!![currentWaveform]
            val pressScaleFactor = abs(waveform)
            if (pressScaleFactor in 5000..10000) {
                _binding.loadButton.press(pressScaleFactor / 10000f, false)
            }
            if (pressScaleFactor in 10000..15000) {
                val isPlaying = mainViewModel.isPlaying.value!!
                _binding.playButton.press(pressScaleFactor / 10000f, isPlaying)
            } else {
                _binding.pauseButton.press(pressScaleFactor / WaveformSlideBar.MAX_VALUE, false)
            }
        }

        // error message observer
        mainViewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }

    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        val returnCursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }
}
