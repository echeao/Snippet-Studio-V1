package com.feige.snippetstudio

import android.app.Application
import com.feige.snippetstudio.di.AppContainer

class SnippetStudioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
