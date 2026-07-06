package app.openpdf.foss.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.openpdf.foss.core.files.SafFileManager
import app.openpdf.foss.feature.home.HomeScreen
import app.openpdf.foss.feature.settings.SettingsScreen
import app.openpdf.foss.feature.viewer.ViewerScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun AppNavHost(
    incomingUris: Flow<Uri>,
    fileManager: SafFileManager,
) {
    val navController = rememberNavController()

    // PDFs arriving from other apps (ACTION_VIEW / ACTION_SEND) open directly.
    LaunchedEffect(incomingUris) {
        incomingUris.collect { uri ->
            navController.navigate(ViewerRoute(uri.toString()))
        }
    }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenDocument = { uri ->
                    fileManager.tryPersistReadPermission(uri)
                    navController.navigate(ViewerRoute(uri.toString()))
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<ViewerRoute> {
            ViewerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
