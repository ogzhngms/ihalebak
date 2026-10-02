package com.ogzhngms.ihalebak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ogzhngms.ihalebak.ui.App
import com.ogzhngms.ihalebak.ui.IhaleBakTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        WatchWorker.schedule(this, Settings(this))
        openFromNotification(intent)
        enableEdgeToEdge()
        setContent {
            IhaleBakTheme { App(viewModel) }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshIfStale()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }

    private fun openFromNotification(intent: Intent?) {
        intent?.getStringExtra(EXTRA_PROVINCE)?.let(viewModel::showCity)
    }

    companion object {
        const val EXTRA_PROVINCE = "province"
    }
}
