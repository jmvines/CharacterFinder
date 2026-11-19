package com.characterfinder.app.ui.detail

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.characterfinder.app.R
import com.characterfinder.app.ui.messageRes
import com.characterfinder.domain.model.Character
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    characterId: Int,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(characterId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val view = LocalView.current
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        val accessibility = view.context.getSystemService(AccessibilityManager::class.java)
        if (accessibility?.isTouchExplorationEnabled == true) {
            titleFocus.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        modifier = Modifier
                            .semantics { heading() }
                            .focusRequester(titleFocus)
                            .focusable(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            DetailUiState.Loading -> {
                val loadingLabel = stringResource(R.string.loading)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = loadingLabel
                            liveRegion = LiveRegionMode.Polite
                        },
                    )
                }
            }
            is DetailUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = stringResource(state.error.messageRes()),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::retry) {
                    Text(stringResource(R.string.retry))
                }
            }
            is DetailUiState.Success -> CharacterDetail(
                character = state.character,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CharacterDetail(character: Character, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DetailField(stringResource(R.string.detail_status), character.status)
        DetailField(stringResource(R.string.detail_species), character.species)
        DetailField(stringResource(R.string.detail_gender), character.gender)
        DetailField(stringResource(R.string.detail_origin), character.origin)
        DetailField(stringResource(R.string.detail_last_location), character.lastLocation)
        DetailField(stringResource(R.string.detail_episodes), character.episodeCount.toString())
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {}
        .padding(vertical = 6.dp)) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
