package com.paradoxcat.waveformtest.waveviewer

import android.content.Context
import android.util.AttributeSet
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.paradoxcat.waveformtest.MainActivity
import com.paradoxcat.waveformtest.model.Point
import org.junit.Before
import org.junit.Test
import org.junit.internal.builders.JUnit4Builder
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock

// TODO: Fix 'posix:permissions' not supported as initial attribute' error //
@RunWith(AndroidJUnit4::class)
class WaveformSlideBarInstrumentedTest {

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
    fun getPathChunk_invalid_index_equals() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 0, 0)
        assertThat(pathChunk.isEmpty).isTrue()
    }

    @Test
    fun getPathChunk_invalid_index_lessThan() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 1, 0)
        assertThat(pathChunk.isEmpty).isTrue()
    }

    @Test
    fun getPathChunk_value() {
        init_point_noStep()
        val pathChunk = WaveformSlideBar.getPathChunk(points, 0, 1)
        assertThat(pathChunk.toString()).isEqualTo("WHAT")
    }




}