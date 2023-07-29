package com.paradoxcat.waveformtest.view

import android.graphics.Canvas
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RuntimeEnvironment


class WaveformSlideBarTest {

    companion object {
        const val SAMPLE_PROCESSED_ARRAY_SIZE =
            10 * 4 // 4 times the size, 1 sample -> x0, y0, x1, y1
    }

    private lateinit var intArray: IntArray

    @Before
    fun setUp() {
        intArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    }

    @Test
    fun calculatePoints_output_size() {
        val processed_points = WaveformSlideBar.calculatePoints(intArray, false)
        assertThat(processed_points.size).isEqualTo(SAMPLE_PROCESSED_ARRAY_SIZE)
    }

    @Test
    fun calculatePoints_empty() {
        val processed_points = WaveformSlideBar.calculatePoints(intArrayOf(), false)
        assertThat(processed_points.size).isEqualTo(0)
    }

    @Test
    fun calculatePoints_mirrored_false() {
        val processed_points = WaveformSlideBar.calculatePoints(intArray, false)
        assertThat(processed_points[3]).isEqualTo(0.0f)
    }

    @Test
    fun calculatePoints_mirrored_true() {
        val processed_points = WaveformSlideBar.calculatePoints(intArray, false)
        assertThat(processed_points[3] + processed_points[1]).isEqualTo(0.0f)
    }

    @Test
    fun calculatePoints_x_equal() {
        val processed_points = WaveformSlideBar.calculatePoints(intArray, false)
        assertThat(processed_points[0]).isEqualTo(processed_points[2])
    }




}