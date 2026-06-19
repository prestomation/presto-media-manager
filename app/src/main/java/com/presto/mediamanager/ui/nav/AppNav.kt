package com.presto.mediamanager.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.presto.mediamanager.ui.editor.EditorScreen
import com.presto.mediamanager.ui.feed.ReviewFeedScreen
import com.presto.mediamanager.ui.settings.SettingsScreen

object Routes {
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{uri}"
    fun editor(uri: String) = "editor/${Uri.encode(uri)}"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.FEED) {
        composable(Routes.FEED) {
            ReviewFeedScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onReview = { item -> nav.navigate(Routes.editor(item.uri)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uri = Uri.decode(backStackEntry.arguments?.getString("uri").orEmpty())
            EditorScreen(
                uri = uri,
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }
    }
}
