package com.paradoxcat.waveviewer.util.di

import android.media.MediaPlayer
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Reusable
    fun provideMediaPlayer() = MediaPlayer()
}