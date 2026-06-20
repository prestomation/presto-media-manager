package com.presto.mediamanager.ui.nav

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.presto.mediamanager.ui.editor.EditorScreen
import com.presto.mediamanager.ui.feed.ReviewFeedScreen
import com.presto.mediamanager.ui.settings.SettingsScreen

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

object Routes {
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{uri}"

    // Base64 (URL-safe) the document URI so its '/', ':' and '%' don't collide
    // with route parsing or get double-decoded by Navigation.
    fun editor(uri: String): String =
        "editor/${Base64.encodeToString(uri.toByteArray(), BASE64_FLAGS)}"
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
            val encoded = backStackEntry.arguments?.getString("uri").orEmpty()
            val uri = String(Base64.decode(encoded, BASE64_FLAGS))
            EditorScreen(
                uri = uri,
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }
    }
}
