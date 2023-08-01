package com.paradoxcat.waveviewer.util

import java.util.concurrent.TimeUnit

object TimeConverter {
    const val MAX_PROGRESS_VALUE = 10000
    /**
     * Convert SeekBar progress value to milliseconds
     * @param milliseconds total duration of the audio file
     * @param progress current SeekBar progress value
     */
    fun progressToMilliseconds(milliseconds: Long, progress: Int): Long {
        return (milliseconds * progress / MAX_PROGRESS_VALUE)
    }

    /**
     * Convert milliseconds to SeekBar progress value
     * @param currentMilliseconds current timestamp of the audio file
     * @param totalMilliseconds total duration of the audio file
     */
    fun millisecondsToProgress(currentMilliseconds: Long, totalMilliseconds: Long): Int {
        return (currentMilliseconds * MAX_PROGRESS_VALUE / totalMilliseconds).toInt()
    }

    /**
     * Convert milliseconds to mm:ss string
     */
    fun getFormattedTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
}