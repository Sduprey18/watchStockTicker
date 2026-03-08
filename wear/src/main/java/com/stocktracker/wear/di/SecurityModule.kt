package com.stocktracker.wear.di

import android.content.Context
import com.stocktracker.wear.data.SecureApiKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureApiKeyManager(
        @ApplicationContext context: Context
    ): SecureApiKeyManager = SecureApiKeyManager(context)
}
