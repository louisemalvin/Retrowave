package com.paradoxcat.waveformtest.view

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test


class WaveformSlideBarTest {

    companion object {
        const val SAMPLE_WIDTH = 1000
        const val SAMPLE_HEIGHT = 300
        const val MIDDLE_VALUE = 150
        const val SAMPLE_PROCESSED_ARRAY_SIZE =
            10 * 4 // 4 times the size, 1 sample -> x0, y0, x1, y1
    }

    private lateinit var intArray: IntArray

    @Before
    fun setUp() {
        intArray = intArrayOf(0, 10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000)
    }

    fun init_mirrored_false(): FloatArray {
        return WaveformSlideBar.calculateDrawLinesPoints(
            intArray,
            false,
            SAMPLE_WIDTH,
            SAMPLE_HEIGHT,
            1
        )
    }

    fun init_mirrored_true(): FloatArray {
        return WaveformSlideBar.calculateDrawLinesPoints(
            intArray,
            true,
            SAMPLE_WIDTH,
            SAMPLE_HEIGHT,
            1
        )
    }

    @Test
    fun calculatePoints_output_size() {
        val processedPoints = init_mirrored_true()
        assertThat(processedPoints.size).isEqualTo(SAMPLE_PROCESSED_ARRAY_SIZE)
    }

    @Test
    fun calculatePoints_output_size_step() {
        val processedPoints = WaveformSlideBar.calculateDrawLinesPoints(
            intArray,
            true,
            SAMPLE_WIDTH,
            SAMPLE_HEIGHT,
            2
        )
        assertThat(processedPoints.size).isEqualTo(SAMPLE_PROCESSED_ARRAY_SIZE / 2)
    }

    @Test
    fun calculatePoints_empty() {
        val processedPoints =
            WaveformSlideBar.calculateDrawLinesPoints(
                intArrayOf(),
                true,
                SAMPLE_WIDTH,
                SAMPLE_HEIGHT,
                1
            )
        assertThat(processedPoints.size).isEqualTo(0)
    }

    @Test
    fun calculatePoints_zero_mirrored_false() {
        val processedPoints = init_mirrored_false()
        assertThat(processedPoints[3]).isEqualTo(MIDDLE_VALUE)
    }

    @Test
    fun calculatePoints_someNumber_mirrored_false() {
        val processedPoints = init_mirrored_false()
        assertThat(processedPoints[5]).isNotEqualTo(MIDDLE_VALUE)
    }

    @Test
    fun calculatePoints_zero_mirrored_true() {
        val processedPoints = init_mirrored_true()
        assertThat(processedPoints[3] + processedPoints[1]).isEqualTo(2 * MIDDLE_VALUE)
    }

    @Test
    fun calculatePoints_someNumber_mirrored_true() {
        val processedPoints = init_mirrored_true()
        assertThat(processedPoints[5] + processedPoints[7]).isEqualTo(2 * MIDDLE_VALUE)
    }

    @Test
    fun calculatePoints_x_equal() {
        val processedPoints = init_mirrored_true()
        assertThat(processedPoints[0]).isEqualTo(processedPoints[2])
    }


}