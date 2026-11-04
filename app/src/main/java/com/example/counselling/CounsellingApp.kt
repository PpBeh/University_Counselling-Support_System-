// Application | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling

import android.app.Application
import com.example.counselling.core.AppContainer

class CounsellingApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
