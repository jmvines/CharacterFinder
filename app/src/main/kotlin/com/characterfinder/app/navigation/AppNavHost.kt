package com.characterfinder.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.characterfinder.app.ui.detail.DetailScreen
import com.characterfinder.app.ui.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
private data object SearchKey : NavKey

@Serializable
private data class DetailKey(val characterId: Int) : NavKey

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(SearchKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SearchKey> {
                SearchScreen(
                    onCharacterClick = { id ->
                        if (backStack.lastOrNull() != DetailKey(id)) backStack.add(DetailKey(id))
                    },
                )
            }
            entry<DetailKey> { key ->
                DetailScreen(
                    characterId = key.characterId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
