# AGENTS.md

Convenciones operativas de este repositorio para herramientas de codificación automatizadas.
El contexto funcional, la arquitectura y el por qué de cada decisión están en
[README.md](README.md); aquí solo está lo que hay que respetar al tocar el código, y sobre todo
lo que parece mejorable y no lo es.

## Estructura y regla de dependencias

```
:app  (Android, Compose)  ──▶  :domain  (Kotlin puro)  ◀──  :data  (Kotlin puro)
```

- `:domain` y `:data` aplican solo el plugin `kotlin-jvm`, así que cualquier uso de una API de
  Android no compila. Eso es lo único que impide el toolchain: las librerías de infraestructura
  (red, DI, serialización) **sí compilarían** si alguien las añadiera a `:domain`. Que no estén
  es una convención que se revisa a mano en `domain/build.gradle.kts`; no hay test de
  arquitectura que la vigile.
- `:data` depende de `:domain` (`api`), nunca al revés.
- `:app` incluye `:data` en Gradle solo para registrar su módulo de Koin al arrancar
  (`CharacterFinderApp`). Fuera de ese punto no toca Retrofit, los DTOs ni la caché: las
  pantallas y los ViewModels hablan únicamente con casos de uso y modelos de dominio.

## Comandos

```bash
./gradlew test --rerun-tasks          # tests de los tres módulos
./gradlew :app:testDebugUnitTest      # solo presentación, incluida la semántica de accesibilidad
./gradlew build                       # compila, pasa los tests y el lint de Android
./gradlew installDebug                # instala en el dispositivo conectado
```

- En local usa `--rerun-tasks` (o `cleanTest test`) y comprueba los XML de
  `*/build/test-results/**/TEST-*.xml`: un `UP-TO-DATE` de Gradle puede dar por bueno un run que
  no ha ejecutado nada, y la salida de consola no lo distingue. El CI corre `./gradlew test` a
  secas porque cada run clona el repositorio limpio y no hay `build/` previo que reutilizar.
- `./gradlew build` ejecuta también los tests (`build` → `check` → `test`): no es solo compilar.
- Los tests de semántica solo existen en la variante debug, así que un `testReleaseUnitTest`
  verde no dice nada sobre ellos.
- `installDebug` instala en un dispositivo real: pide confirmación antes de lanzarlo.
- Requiere JDK 17. `compileSdk` 37, `targetSdk` 36, `minSdk` 26.

## Convenciones de código

- **Sin comentarios ni KDoc** en `.kt` ni en `.gradle.kts`. El razonamiento va al README o al
  mensaje del commit, no al source.
- Identificadores de producción en inglés. **Los nombres de test son la excepción**: una frase en
  español entre backticks y sin tildes, describiendo el comportamiento
  (``fun `el debounce agrupa el tecleo en una sola busqueda`()``). Los 54 siguen ese patrón.
- La UI es monolingüe en español: `app/src/main/res/values/strings.xml` es el único locale, no
  hay `values-en`. Un string nuevo se escribe en español y se lee con `stringResource` /
  `pluralStringResource`; no hay literales de texto en el código.
  - Excepción deliberada: el recuento de resultados se anuncia de forma imperativa con
    `view.announceForAccessibility(...)` + `getQuantityString` dentro de un `LaunchedEffect`
    (`ui/search/SearchScreen.kt`), porque una live region no vuelve a anunciar un texto que no
    cambia.
  - `Character.status` y `Character.species` se muestran tal y como llegan de la API, en inglés
    ("Alive", "Human"), mientras los chips de filtro están traducidos. Si alguna vez se
    traducen, el mapeo va en presentación y hay que actualizar las aserciones de los tests.
- Dependencias siempre por el catálogo `gradle/libs.versions.toml` (`libs.*`), nunca coordenadas
  literales en un `build.gradle.kts`.
- Estado de UI: `sealed interface` inmutable expuesta como `StateFlow`; nunca un flow mutable.
  El estado derivado de un pipeline se publica con `stateIn` (`SearchViewModel`) y el que se
  asigna a mano, con un `MutableStateFlow` privado más `asStateFlow()` (`DetailViewModel`).
- Errores: `Either<DomainError, T>` atravesando capas, no excepciones. El mapeo a texto está
  centralizado en `DomainError.messageRes()` (`ui/ErrorMessages.kt`); si añades un caso a
  `DomainError`, el `when` exhaustivo te obliga a darle mensaje.
- La validez de una búsqueda vive **solo** en el value class `SearchQuery` del dominio. No
  dupliques el predicado en ViewModels ni en casos de uso.
- Dobles de test escritos a mano (fakes), sin librerías de mocking.

## Dos refactors "obvios" que son bugs

- **El pipeline de `SearchViewModel` son dos ramas con `merge`, no un `combine`.** El texto
  entra debounceado y el filtro entra inmediato, cada rama emparejando el valor *actual* de la
  otra. Con `combine`, cambiar el filtro a media escritura emparejaría el filtro nuevo con el
  último texto ya debounceado —el anterior— y lanzaría la petición con el texto viejo. Ese bug
  existió y está cubierto por un test dedicado en `SearchViewModelTest` ("cambiar el filtro
  con texto a medio escribir no lanza busqueda con el texto anterior").
- **`SearchCache` usa `LinkedHashMap(16, 0.75f, true)` y un reloj monótono inyectado.** El
  tercer parámetro activa el orden por acceso: es lo que convierte el mapa en LRU al descartar
  la entrada más antigua al pasar de 64. Y el reloj es `System.nanoTime()`, no
  `currentTimeMillis`, para que un cambio de hora del dispositivo no altere la expiración;
  llega como `() -> Long` para que los tests muevan el tiempo sin esperas. No "simplifiques"
  ninguna de las tres cosas.

## Tests

| Dónde | Qué va ahí |
|---|---|
| `domain/src/test`, `data/src/test` | lógica de dominio, caché y repositorio, con fakes |
| `app/src/test` | ViewModels, con `coroutines-test` + Turbine |
| `app/src/testDebug` | contrato de accesibilidad con Robolectric + `compose-ui-test` |

- La semántica vive en `testDebug` porque `ui-test-manifest` entra como `debugImplementation`:
  en la variante release no hay actividad de test donde montar el Compose.
- Esos tests llevan `@Config(sdk = [35], application = android.app.Application::class)`. La
  `Application` genérica es deliberada: `CharacterFinderApp` llama a `startKoin`, y arrancarlo
  por partida doble en cada test rompe la ejecución.
- **Los tests de semántica asertan los literales de `strings.xml`**, no los ids de recurso:
  buscan los nodos con `onNodeWithText("Busca un personaje")` o por la etiqueta completa del
  grupo de filtros. Cambiar la redacción de un string rompe esas aserciones y hay que
  actualizarlas en el mismo commit: el fallo no significa que la UI esté mal.
- Un arreglo de bug entra con un test que lo reproduzca primero.
- No reduzcas asserts ni borres tests para poner el build en verde. Si un test de accesibilidad
  falla, lo que se corrige es la UI, no el test.

## Accesibilidad: lo que parece residuo y no lo es

La zona de filtros y la lista de resultados se anuncian con una parada propia antes de entrar
elemento a elemento ("Filtro de estado del personaje, 4 opciones" / "Lista de resultados,
N personajes"). El montaje es el mismo en los dos sitios:

- Un `Box` padre marcado con `isTraversalGroup = true`.
- Dentro, un `Box` hermano con `matchParentSize()` y
  `semantics(mergeDescendants = true) { traversalIndex = -1f; contentDescription = … }`: ese nodo
  vacío **es** la parada, y el `traversalIndex` negativo la coloca delante de sus hermanos.
- El contenido real envuelto en `Box(Modifier.padding(1.dp))`. Ese inset de 1 dp evita que los
  hijos queden con los mismos límites que la superficie etiquetada; sin él la parada no se
  anuncia como nodo propio.
- Por eso la fila de chips lleva `padding(horizontal = 15.dp)` y no 16: 1 + 15 cuadra con los
  16 dp del título y del campo de búsqueda.

**Qué cubren los tests y qué no.** Los tests de `app/src/testDebug` fijan las etiquetas, el
`traversalIndex` de las dos paradas, el `selectableGroup`, el rol radio de los chips, las live
regions y el `collectionInfo` / `collectionItemInfo`. **No** fijan las medidas: no hay ninguna
aserción sobre el inset de 1 dp ni sobre los 15 dp, y no puede haberla de forma significativa,
porque `compose-ui-test` lee el árbol de semantics de Compose y la deduplicación de nodos con
límites idénticos ocurre más abajo, en el `AccessibilityNodeInfo` que consume TalkBack. Si los
normalizas a 16 dp la suite sigue verde y el recorrido se rompe en el dispositivo: esa parte se
verifica a mano con un lector de pantalla.

Lo mismo con el foco inicial (campo de búsqueda al abrir, título al entrar al detalle): se pide
con `FocusRequester` solo cuando hay exploración táctil activa, y bajo Robolectric esa rama no
se ejecuta. El test únicamente comprueba que el nodo es enfocable.

## Git

- Haz stage **por fichero explícito**. Nunca `git add .` ni por directorio: el árbol de trabajo
  puede tener cambios en curso que no son tuyos.
- Mensajes de commit en español, asunto breve y descriptivo, sin trailers.
- No commitees nada de lo que ya cubre `.gitignore` (`local.properties`, `.idea/`, `.kotlin/`,
  `build/`, `.gradle/`).
- No reescribas historia ya publicada ni hagas `force-push` a `main`.

## Alcance de los cambios

- Cambio mínimo que resuelva lo pedido. No refactorices ni reformatees código adyacente que no
  haya que tocar; si ves algo roto fuera del alcance, menciónalo en vez de arreglarlo.
- Lo que está fuera por decisión, no por olvido: persistencia en disco, paginación de
  resultados, librerías de mocking y DI por generación de código. Están justificados en
  "Decisiones principales" del README; no los introduzcas sin que se pidan.
