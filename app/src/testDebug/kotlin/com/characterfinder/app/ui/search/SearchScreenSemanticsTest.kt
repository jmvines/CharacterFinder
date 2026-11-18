package com.characterfinder.app.ui.search

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.characterfinder.app.ui.theme.CharacterFinderTheme
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifica las semantics de accesibilidad de [SearchScreen] (lo que TalkBack recibe),
 * en JVM con Robolectric: corre con `./gradlew test`, sin emulador.
 */
// application genérica: estos tests no usan Koin (los ViewModels se inyectan a mano)
// y la Application real llamaría a startKoin en cada test, chocando con el GlobalContext.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class SearchScreenSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeCharacterRepository : CharacterRepository {
        var responseDelayMillis = 0L
        var resultsForQuery: (String) -> List<Character> = { emptyList() }

        override suspend fun search(query: String): List<Character> {
            delay(responseDelayMillis)
            return resultsForQuery(query)
        }

        override suspend fun getCharacter(id: Int): Character = error("sin uso en estos tests")
    }

    private val repository = FakeCharacterRepository()

    private fun setScreen(): SearchViewModel {
        val viewModel = SearchViewModel(SearchCharactersUseCase(repository))
        compose.setContent {
            CharacterFinderTheme {
                SearchScreen(onCharacterClick = {}, viewModel = viewModel)
            }
        }
        return viewModel
    }

    /** retry() reemite el query sin esperar al debounce: lleva la pantalla al estado final ya. */
    private fun SearchViewModel.search(query: String) {
        onQueryChange(query)
        retry()
        compose.waitForIdle()
    }

    @Test
    fun `el campo de busqueda esta etiquetado`() {
        setScreen()

        compose.onNodeWithText("Busca un personaje").assertExists()
    }

    @Test
    fun `el mensaje de estado inicial es una region viva`() {
        setScreen()

        compose.onNodeWithText("Escribe un nombre para buscar personajes de Rick y Morty")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `el spinner de carga anuncia Cargando como region viva`() {
        repository.responseDelayMillis = 60_000
        setScreen().search("rick")

        compose.onNodeWithContentDescription("Cargando")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `la lista expone el numero total de elementos`() {
        repository.resultsForQuery = { listOf(stub(1, "Rick Sanchez"), stub(2, "Alien Rick"), stub(3, "Aqua Rick")) }
        setScreen().search("rick")

        val collectionInfo = compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
            .fetchSemanticsNode()
            .config[SemanticsProperties.CollectionInfo]
        assertEquals(3, collectionInfo.rowCount)
        assertEquals(1, collectionInfo.columnCount)
    }

    @Test
    fun `cada fila expone su posicion en la lista`() {
        repository.resultsForQuery = { listOf(stub(1, "Rick Sanchez"), stub(2, "Alien Rick"), stub(3, "Aqua Rick")) }
        setScreen().search("rick")

        val itemInfo = compose.onNodeWithText("Alien Rick")
            .fetchSemanticsNode()
            .config[SemanticsProperties.CollectionItemInfo]
        assertEquals(1, itemInfo.rowIndex)
    }

    @Test
    fun `cada fila es un unico boton con nombre especie y estado fusionados`() {
        repository.resultsForQuery = { listOf(stub(1, "Rick Sanchez")) }
        setScreen().search("rick")

        compose.onNode(hasText("Rick Sanchez") and hasText("Human · Alive"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun `sin resultados anuncia el mensaje de vacio como region viva`() {
        repository.resultsForQuery = { emptyList() }
        setScreen().search("nadie")

        compose.onNodeWithText("Sin resultados para esa búsqueda")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `el error se anuncia como region viva y ofrece reintentar`() {
        repository.resultsForQuery = { error("fallo de red simulado") }
        setScreen().search("rick")

        compose.onNodeWithText("Algo ha fallado. Revisa tu conexión e inténtalo de nuevo.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithText("Reintentar").assertHasClickAction()
    }
}

private fun stub(id: Int, name: String) = Character(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    gender = "Male",
    imageUrl = "https://example.com/$id.jpeg",
    origin = "Earth (C-137)",
    lastLocation = "Citadel of Ricks",
    episodeCount = 51,
)
