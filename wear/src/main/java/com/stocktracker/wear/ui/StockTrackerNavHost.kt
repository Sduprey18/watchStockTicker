package com.stocktracker.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

private const val WATCHLIST_ROUTE = "watchlist"
private const val ADD_SYMBOL_ROUTE = "add_symbol"

@Composable
fun StockTrackerNavHost(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController = rememberSwipeDismissableNavController()
) {
    SwipeDismissableNavHost(
        modifier = modifier,
        navController = navController,
        startDestination = WATCHLIST_ROUTE
    ) {
        composable(WATCHLIST_ROUTE) {
            WatchlistScreen(
                onSymbolClick = { symbol -> navController.navigate("detail/$symbol") },
                onAddClick = { navController.navigate(ADD_SYMBOL_ROUTE) }
            )
        }
        composable(
            route = "detail/{symbol}",
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }
        composable(ADD_SYMBOL_ROUTE) {
            AddSymbolScreen(onBack = { navController.popBackStack() })
        }
    }
}
