package com.paradoxcat.waveviewer

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.paradoxcat.waveviewer.databinding.ActivityMainBinding
import com.paradoxcat.waveviewer.util.TimeConverter.getFormattedTime
import com.paradoxcat.waveviewer.view.WaveformSlideBar
import com.paradoxcat.waveviewer.viewmodel.MainViewModel
import com.paradoxcat.waveviewer.viewmodel.MainViewModel.Companion.MediaPlayerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val OPEN_FILE_ERROR = "Error opening file"
        const val EXTERNAL_FILE_TRIGGER = 1000L
    }

    private lateinit var _binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels()
    private var showHint = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        // set up file picker
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { selectedUri ->
                    try {
                        val fileName = getFileName(contentResolver, selectedUri)
                        val assetFileDescriptor =
                            contentResolver.openAssetFileDescriptor(selectedUri, "r")
                        if (assetFileDescriptor!=null && fileName!=null) {
                            mainViewModel.setMedia(assetFileDescriptor, fileName)
                            assetFileDescriptor.close()
                        } else {
                            throw Exception(OPEN_FILE_ERROR)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, OPEN_FILE_ERROR, e)
                        Toast.makeText(this, OPEN_FILE_ERROR, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        // set up listeners
        setupListeners(getContent)

        // set up observers
        setupObservers()

    }

    /**
     * Set up observers for LiveData from MainViewModel.
     */
    private fun setupObservers() {
        setupTitleObserver()
        setupDurationObserver()
        setupStateObserver()
        setupTimestampObserver()
        setupWaveformDataObserver()
        setupCurrentWaveformObserver()
        setupErrorMessageObserver()
    }

    /**
     * Update UI with audio file name.
     */
    private fun setupTitleObserver() {
        mainViewModel.title.observe(this) { title ->
            _binding.titleTextView.text = title
        }
    }

    /**
     * Update UI and seekbar max value with total audio duration.
     */
    private fun setupDurationObserver() {
        mainViewModel.duration.observe(this) { duration ->
            _binding.durationTextView.text = getFormattedTime(duration)
            _binding.playbackSeekBar.max = duration?.toInt() ?: 1
        }
    }

    /**
     * Update play button and indicator with current media player state.
     */
    private fun setupStateObserver() {
        mainViewModel.state.observe(this) { state ->
            when (state) {
                MediaPlayerState.PLAYING -> {
                    _binding.playbackIndicatorView.setData(true)
                    _binding.playButton.lock(true)
                }
                else -> {
                    _binding.playbackIndicatorView.setData(false)
                    _binding.playButton.lock(false)
                    _binding.playButton.press(0.0f)
                }
            }
        }
    }

    /**
     * Update timestamp and seekbar with current media player position.
     */
    private fun setupTimestampObserver() {
        mainViewModel.timestamp.observe(this) { timestamp ->
            _binding.timestampTextView.text = getFormattedTime(timestamp)
            _binding.playbackSeekBar.progress = timestamp?.toInt() ?: 0
        }
    }

    /**
     * Update waveform slide bar with current waveform data.
     */
    private fun setupWaveformDataObserver() {
        mainViewModel.waveformData.observe(this@MainActivity) { waveformData ->
            lifecycleScope.launch {
                _binding.waveformView.setData(waveformData)
            }
        }
    }

    /**
     * Manipulate buttons height with current waveform data.
     */
    private fun setupCurrentWaveformObserver() {
        mainViewModel.currentWaveform.observe(this) { currentWaveform ->
            if (currentWaveform >= mainViewModel.waveformData.value!!.size) {
                return@observe
            }

            val waveform = mainViewModel.waveformData.value!![currentWaveform]
            val pressScale = abs(waveform) / WaveformSlideBar.MAX_VALUE

            // Check pressScale condition before performing actions
            if (pressScale < 0.1f) {
                return@observe
            }

            val pressScaleFactor = pressScale * 2.0f

            when (waveform % 3) {
                0 -> _binding.stopButton.press(pressScaleFactor)
                1 -> _binding.playButton.press(pressScaleFactor)
                else -> _binding.loadButton.press(pressScaleFactor)
            }
        }
    }

    /**
     * Show error message if any.
     */
    private fun setupErrorMessageObserver() {
        mainViewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Set up event listeners for MainActivity.
     * @param getContent -- ActivityResultLauncher for file picker
     */
    private fun setupListeners(getContent: ActivityResultLauncher<String>) {
        setupLoadButtonListener(getContent)
        setupPlayButtonListener()
        setupStopButtonListener()
        setupSeekBarListener()
    }

    /**
     * Load button listener.
     *
     * Long press to load from file, short press to switch to audio from assets.
     * @param getContent -- ActivityResultLauncher for file picker
     */
    private fun setupLoadButtonListener(getContent: ActivityResultLauncher<String>) {
        var externalRead = false
        // create timer for long press
        val timer = object : CountDownTimer(EXTERNAL_FILE_TRIGGER, EXTERNAL_FILE_TRIGGER) {
            override fun onTick(millisUntilFinished: Long) {
                // nothing to do
            }
            override fun onFinish() {
                getContent.launch("audio/*")
                externalRead = true
            }
        }

        _binding.loadButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (showHint) {
                        Toast.makeText(
                            this,
                            "Long press to load from file",
                            Toast.LENGTH_LONG
                        ).show()
                        showHint = false
                    }
                    _binding.loadButton.pressHold()
                    timer.start()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    timer.cancel()
                    _binding.loadButton.performClick()
                    _binding.loadButton.pressRelease()
                    if (externalRead) {
                        externalRead = false
                    } else {
                        mainViewModel.setMedia()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Play button listener.
     *
     * Any press toggle play/pause depending on current media player state.
     */
    private fun setupPlayButtonListener() {
        _binding.playButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    _binding.playButton.pressHold()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainViewModel.togglePlayPause()
                    val isPlaying = mainViewModel.state.value == MediaPlayerState.PLAYING
                    _binding.playbackIndicatorView.setData(isPlaying)
                    _binding.playButton.pressRelease()
                    _binding.playButton.performClick()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Stop button listener.
     *
     * Reset timestamp to 0 and pause media player.
     */
    private fun setupStopButtonListener() {
        _binding.stopButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    _binding.stopButton.pressHold()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainViewModel.stop()
                    _binding.stopButton.pressRelease()
                    _binding.stopButton.performClick()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Seek bar listener.
     *
     * Call seek to function when user move the seek bar.
     */
    private fun setupSeekBarListener() {
        _binding.playbackSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) {
                        return
                    }
                    mainViewModel.seekTo(progress.toLong())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Nothing to track currently
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // Nothing to track currently
                }
            }
        )
    }

    /**
     * Helper function to get file name from uri
     */
    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        val returnCursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }
}
