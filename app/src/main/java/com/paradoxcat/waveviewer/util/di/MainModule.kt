package com.paradoxcat.waveviewer.util.di

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Reusable
    fun provideMediaPlayer() = MediaPlayer()

    @Provides
    @Reusable
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager = context.assets
}