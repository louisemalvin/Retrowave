package com.paradoxcat.waveviewer.util

import com.paradoxcat.waveviewer.MainActivity
import java.util.concurrent.TimeUnit

object TimeConverter {
    /**
     * Convert SeekBar progress value to milliseconds
     * @param milliseconds total duration of the audio file
     * @param progress current SeekBar progress value
     */
    fun progressToMilliseconds(milliseconds: Long, progress: Int): Long {
        return (milliseconds * progress / MainActivity.MAX_PROGRESS_VALUE)
    }

    /**
     * Convert milliseconds to SeekBar progress value
     * @param currentMilliseconds current timestamp of the audio file
     * @param totalMilliseconds total duration of the audio file
     */
    fun millisecondsToProgress(currentMilliseconds: Long, totalMilliseconds: Int): Int {
        return (currentMilliseconds * MainActivity.MAX_PROGRESS_VALUE / totalMilliseconds).toInt()
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