package com.characterfinder.app.ui.search

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.characterfinder.app.ui.theme.CharacterFinderTheme
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class SearchScreenSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeCharacterRepository : CharacterRepository {
        var responseDelayMillis = 0L
        var resultsForQuery: (String) -> Either<DomainError, List<Character>> = { Either.Right(emptyList()) }

        override suspend fun search(query: String, status: CharacterStatus?): Either<DomainError, List<Character>> {
            delay(responseDelayMillis)
            return resultsForQuery(query)
        }

        override suspend fun getCharacter(id: Int): Either<DomainError, Character> =
            error("sin uso en estos tests")
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
    fun `el titulo es el encabezado de la pantalla`() {
        setScreen()

        compose.onNodeWithText("Buscar personajes")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `el teclado ofrece la accion Buscar en el campo`() {
        setScreen()

        compose.onNodeWithText("Busca un personaje")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, ImeAction.Search))
    }

    @Test
    fun `los chips de filtro exponen la seleccion con Todos activo por defecto`() {
        setScreen()

        compose.onNodeWithText("Todos").assertIsSelected()
        compose.onNodeWithText("Vivos").assertIsNotSelected()
        compose.onNodeWithText("Muertos").assertIsNotSelected()
        compose.onNodeWithText("Desconocido").assertIsNotSelected()
    }

    @Test
    fun `la fila de filtros es un grupo de seleccion unica etiquetado y los chips tienen rol de radio`() {
        setScreen()

        compose.onNodeWithContentDescription("Filtro de estado del personaje, 4 opciones")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, -1f))
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
        compose.onNodeWithText("Vivos")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun `elegir un filtro mueve la seleccion a su chip`() {
        setScreen()

        compose.onNodeWithText("Vivos").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Vivos").assertIsSelected()
        compose.onNodeWithText("Todos").assertIsNotSelected()
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
    fun `la lista de resultados tiene una supercaja con el recuento como parada previa`() {
        repository.resultsForQuery = { Either.Right(listOf(stub(1, "Rick Sanchez"), stub(2, "Alien Rick"), stub(3, "Aqua Rick"))) }
        setScreen().search("rick")

        compose.onNodeWithContentDescription("Lista de resultados, 3 personajes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, -1f))
    }

    @Test
    fun `la lista expone el numero total de elementos`() {
        repository.resultsForQuery = { Either.Right(listOf(stub(1, "Rick Sanchez"), stub(2, "Alien Rick"), stub(3, "Aqua Rick"))) }
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
        repository.resultsForQuery = { Either.Right(listOf(stub(1, "Rick Sanchez"), stub(2, "Alien Rick"), stub(3, "Aqua Rick"))) }
        setScreen().search("rick")

        val itemInfo = compose
            .onNodeWithContentDescription("Nombre: Alien Rick. Especie: Human. Estado: Alive.")
            .fetchSemanticsNode()
            .config[SemanticsProperties.CollectionItemInfo]
        assertEquals(1, itemInfo.rowIndex)
    }

    @Test
    fun `cada fila es un unico boton que anuncia sus campos etiquetados`() {
        repository.resultsForQuery = { Either.Right(listOf(stub(1, "Rick Sanchez"))) }
        setScreen().search("rick")

        compose.onNodeWithContentDescription("Nombre: Rick Sanchez. Especie: Human. Estado: Alive.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun `el texto visual de la fila queda fuera de semantics para no leerse dos veces`() {
        repository.resultsForQuery = { Either.Right(listOf(stub(1, "Rick Sanchez"))) }
        setScreen().search("rick")

        compose.onNode(hasText("Human · Alive")).assertDoesNotExist()
    }

    @Test
    fun `sin resultados anuncia el mensaje de vacio como region viva`() {
        repository.resultsForQuery = { Either.Right(emptyList()) }
        setScreen().search("nadie")

        compose.onNodeWithText("Sin resultados para esa búsqueda")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `el error se anuncia como region viva y ofrece reintentar`() {
        repository.resultsForQuery = { Either.Left(DomainError.Unexpected) }
        setScreen().search("rick")

        compose.onNodeWithText("Algo ha fallado. Inténtalo de nuevo.")
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
