package com.characterfinder.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.characterfinder.app.ui.detail.DetailScreen
import com.characterfinder.app.ui.search.SearchScreen

private const val ROUTE_SEARCH = "search"
private const val ARG_CHARACTER_ID = "characterId"
private const val ROUTE_DETAIL = "detail/{$ARG_CHARACTER_ID}"

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = ROUTE_SEARCH) {
        composable(ROUTE_SEARCH) {
            SearchScreen(onCharacterClick = { id -> navController.navigate("detail/$id") })
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_CHARACTER_ID) { type = NavType.IntType }),
        ) { backStackEntry ->
            val characterId = requireNotNull(backStackEntry.arguments).getInt(ARG_CHARACTER_ID)
            DetailScreen(
                characterId = characterId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
