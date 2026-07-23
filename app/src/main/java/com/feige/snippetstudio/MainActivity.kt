package com.feige.snippetstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.feige.snippetstudio.ui.AppScaffold
import com.feige.snippetstudio.ui.nav.AppNavGraph
import com.feige.snippetstudio.ui.theme.SnippetStudioTheme
import com.feige.snippetstudio.util.LocaleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SnippetStudioApp).container

        setContent {
            val settings by appContainer.settingsRepository.settingsFlow.collectAsState(initial = com.feige.snippetstudio.model.AppSettings())

            val localeContext = remember(settings.lang) {
                LocaleHelper.setLocale(this@MainActivity, settings.lang)
            }

            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                SnippetStudioTheme(themeSetting = settings.theme) {
                    val navController = rememberNavController()
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    val showSnackbar: (String) -> Unit = { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }

                    AppScaffold(
                        navController = navController,
                        snackbarHostState = snackbarHostState
                    ) { innerPadding ->
                        AppNavGraph(
                            navController = navController,
                            appContainer = appContainer,
                            onShowSnackbar = showSnackbar,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
