package com.paradoxcat.waveformtest.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.paradoxcat.waveviewer.viewmodel.MainViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainViewModelInstrumentedTest {

    companion object {
        const val EXAMPLE_AUDIO_FILE_NAME_SMALL = "whistle_mono_44100Hz_16bit.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_MEDIUM = "gravitational_wave_mono_44100Hz_16bit.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_LARGE = "music_mono_44100Hz_16bit.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_WILDCARD = "random_file.wav"
        const val EXAMPLE_AUDIO_FILE_NAME_WRONG_FILETYPE = "wrong_format.mp3"
        const val EXAMPLE_AUDIO_LENGTH_SMALL = 792
        const val EXAMPLE_AUDIO_WILDCAD_LENGTH = 201348
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var context: Context

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        mainViewModel = MainViewModel()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun setMedia_smallAudioFile_metadata() {
        mainViewModel.setMedia(
            context.assets.openFd(EXAMPLE_AUDIO_FILE_NAME_SMALL),
            EXAMPLE_AUDIO_FILE_NAME_SMALL
        )
        assertThat(mainViewModel.duration.value).isEqualTo(EXAMPLE_AUDIO_LENGTH_SMALL)
        assertThat(mainViewModel.title.value).isEqualTo(EXAMPLE_AUDIO_FILE_NAME_SMALL)
        assertThat(mainViewModel.state.value).isEqualTo(false)
        assertThat(mainViewModel.timestamp.value).isEqualTo(0)
        assertThat(mainViewModel.waveformData.value).isNotNull()
    }

    @Test
    fun setMedia_wildCardAudioFile() {
        mainViewModel.setMedia(
            context.assets.openFd(EXAMPLE_AUDIO_FILE_NAME_WILDCARD),
            EXAMPLE_AUDIO_FILE_NAME_WILDCARD
        )
        assertThat(mainViewModel.duration.value).isEqualTo(EXAMPLE_AUDIO_WILDCAD_LENGTH)

    }

    @Test
    fun setMedia_wrongFileType() {
        mainViewModel.setMedia(
            context.assets.openFd(EXAMPLE_AUDIO_FILE_NAME_WRONG_FILETYPE),
            EXAMPLE_AUDIO_FILE_NAME_WRONG_FILETYPE
        )
        val exception = IllegalArgumentException()
        assertThat(exception).hasMessageThat().startsWith("File type not supported")

    }

    @Test
    fun setMedia_double_mediaFile() {
        mainViewModel.setMedia(
            context.assets.openFd(EXAMPLE_AUDIO_FILE_NAME_MEDIUM),
            EXAMPLE_AUDIO_FILE_NAME_MEDIUM
        )
        mainViewModel.setMedia(
            context.assets.openFd(EXAMPLE_AUDIO_FILE_NAME_SMALL),
            EXAMPLE_AUDIO_FILE_NAME_SMALL
        )

        assertThat(mainViewModel.duration.value).isEqualTo(EXAMPLE_AUDIO_LENGTH_SMALL)
    }

}