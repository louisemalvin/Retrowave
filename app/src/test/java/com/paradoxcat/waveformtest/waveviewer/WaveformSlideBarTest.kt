package com.paradoxcat.waveformtest.waveviewer

import com.google.common.truth.Truth.assertThat
import com.paradoxcat.waveviewer.model.Point
import com.paradoxcat.waveviewer.waveviewer.WaveformSlideBar
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// TODO: Fix 'posix:permissions' not supported as initial attribute' error //
@RunWith(RobolectricTestRunner::class)
class WaveformSlideBarTest {

    companion object {
        const val SAMPLE_WIDTH = 1000
        const val SAMPLE_HEIGHT = 300
        const val MIDDLE_VALUE = 150
    }

    private lateinit var intArray: IntArray
    private lateinit var points: Array<Point>

    @Before
    fun setUp() {
        intArray = intArrayOf(0, 10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000)
    }

    fun init_point_noStep() {
        points = WaveformSlideBar.calculatePoints(intArray, SAMPLE_WIDTH, SAMPLE_HEIGHT, 1)
    }

    @Test
    fun calculatePoints_size() {
        init_point_noStep()
        assertThat(points.size).isEqualTo(intArray.size)
    }

    @Test
    fun getPathChunk_not_empty() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 0, 1)
        assertThat(pathChunk.isEmpty).isFalse()
    }

    @Test
    fun getPathChunk_invalid_startIndex_equals_endIndex() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 0, 0)
        assertThat(pathChunk.isEmpty).isTrue()
    }

    @Test
    fun getPathChunk_invalid_endIndex_lessThan_startIndex() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 1, 0)
        assertThat(pathChunk.isEmpty).isTrue()
    }

    @Test
    fun getPathChunk_invalid_startIndex_lessThan_zero() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, -1, 0)
        assertThat(pathChunk.isEmpty).isTrue()
    }

    @Test
    fun getPathChunk_invalid_endIndex_greaterThan_size() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 0, points.size + 1)
        assertThat(pathChunk.isEmpty).isTrue()
    }




}