package com.characterfinder.app.ui.detail

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.characterfinder.app.ui.theme.CharacterFinderTheme
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.usecase.GetCharacterDetailUseCase
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifica las semantics de accesibilidad de [DetailScreen] (lo que TalkBack recibe),
 * en JVM con Robolectric: corre con `./gradlew test`, sin emulador.
 */
// application genérica: estos tests no usan Koin (los ViewModels se inyectan a mano)
// y la Application real llamaría a startKoin en cada test, chocando con el GlobalContext.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class DetailScreenSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeCharacterRepository : CharacterRepository {
        var responseDelayMillis = 0L
        var characterForId: (Int) -> Character = { id -> stub(id, "Rick Sanchez") }

        override suspend fun search(query: String): List<Character> = error("sin uso en estos tests")

        override suspend fun getCharacter(id: Int): Character {
            delay(responseDelayMillis)
            return characterForId(id)
        }
    }

    private val repository = FakeCharacterRepository()

    private fun setScreen(characterId: Int = 1) {
        val viewModel = DetailViewModel(GetCharacterDetailUseCase(repository), characterId)
        compose.setContent {
            CharacterFinderTheme {
                DetailScreen(characterId = characterId, onBack = {}, viewModel = viewModel)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `el nombre del personaje es un encabezado`() {
        setScreen()

        compose.onNodeWithText("Rick Sanchez")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `cada campo fusiona etiqueta y valor en un unico foco`() {
        setScreen()

        compose.onNode(hasText("Género") and hasText("Male")).assertExists()
        compose.onNode(hasText("Última ubicación") and hasText("Citadel of Ricks")).assertExists()
    }

    @Test
    fun `el boton de volver esta etiquetado`() {
        setScreen()

        compose.onNodeWithContentDescription("Volver").assertHasClickAction()
    }

    @Test
    fun `el spinner de carga anuncia Cargando como region viva`() {
        repository.responseDelayMillis = 60_000
        setScreen()

        compose.onNodeWithContentDescription("Cargando")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `el error se anuncia como region viva y reintentar recupera el detalle`() {
        var failing = true
        repository.characterForId = { id ->
            if (failing) error("fallo de red simulado") else stub(id, "Rick Sanchez")
        }
        setScreen()

        compose.onNodeWithText("Algo ha fallado. Revisa tu conexión e inténtalo de nuevo.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        failing = false
        compose.onNodeWithText("Reintentar").assertHasClickAction().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Rick Sanchez").assertExists()
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
