package com.characterfinder.app

import android.app.Application
import com.characterfinder.app.di.appModule
import com.characterfinder.data.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CharacterFinderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CharacterFinderApp)
            modules(dataModule, appModule)
        }
    }
}
