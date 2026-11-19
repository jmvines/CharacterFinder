package com.characterfinder.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.characterfinder.app.R
import com.characterfinder.app.ui.messageRes
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onCharacterClick: (Int) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val view = LocalView.current
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is SearchUiState.Success) {
            val count = state.characters.size
            view.announceForAccessibility(
                view.context.resources.getQuantityString(R.plurals.search_results_count, count, count),
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val searchFieldFocus = remember { FocusRequester() }
            var initialFocusRequested by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (initialFocusRequested) return@LaunchedEffect
                initialFocusRequested = true
                val accessibility = view.context.getSystemService(AccessibilityManager::class.java)
                if (accessibility?.isTouchExplorationEnabled == true) {
                    searchFieldFocus.requestFocus()
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(searchFieldFocus),
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            )
            StatusFilterRow(
                selected = statusFilter,
                onSelect = viewModel::onStatusFilterChange,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                val state = uiState
                if (state is SearchUiState.Success) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { isTraversalGroup = true },
                    ) {
                        val resultsLabel = pluralStringResource(
                            R.plurals.results_group_label,
                            state.characters.size,
                            state.characters.size,
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .semantics(mergeDescendants = true) {
                                    traversalIndex = -1f
                                    contentDescription = resultsLabel
                                },
                        )
                        Box(Modifier.padding(1.dp)) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics {
                                        collectionInfo = CollectionInfo(
                                            rowCount = state.characters.size,
                                            columnCount = 1,
                                        )
                                    },
                            ) {
                                itemsIndexed(state.characters, key = { _, character -> character.id }) { index, character ->
                                    CharacterRow(
                                        character = character,
                                        onClick = { onCharacterClick(character.id) },
                                        modifier = Modifier.semantics {
                                            collectionItemInfo = CollectionItemInfo(
                                                rowIndex = index,
                                                rowSpan = 1,
                                                columnIndex = 0,
                                                columnSpan = 1,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (state is SearchUiState.Loading) {
                    val loadingLabel = stringResource(R.string.loading)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = loadingLabel
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
                StatusMessage(
                    state = state,
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: CharacterStatus?,
    onSelect: (CharacterStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(R.string.filter_group_label, statusFilterOptions.size)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .semantics(mergeDescendants = true) {
                    traversalIndex = -1f
                    contentDescription = groupLabel
                },
        )
        Box(Modifier.padding(1.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 15.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                statusFilterOptions.forEach { (status, labelRes) ->
                    FilterChip(
                        selected = selected == status,
                        onClick = { onSelect(status) },
                        label = { Text(stringResource(labelRes)) },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    )
                }
            }
        }
    }
}

private val statusFilterOptions = listOf(
    null to R.string.filter_all,
    CharacterStatus.ALIVE to R.string.filter_alive,
    CharacterStatus.DEAD to R.string.filter_dead,
    CharacterStatus.UNKNOWN to R.string.filter_unknown,
)

@Composable
private fun StatusMessage(
    state: SearchUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (state) {
        SearchUiState.Idle -> stringResource(R.string.search_idle)
        SearchUiState.Empty -> stringResource(R.string.search_empty)
        is SearchUiState.Error -> stringResource(state.error.messageRes())
        SearchUiState.Loading, is SearchUiState.Success -> ""
    }
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state is SearchUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun CharacterRow(
    character: Character,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowDescription = stringResource(
        R.string.character_row_description,
        character.name,
        character.species,
        character.status,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .clearAndSetSemantics { contentDescription = rowDescription }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = character.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${character.species} · ${character.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
