package com.ivy.aiassistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberUpdatedState
import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiProvider
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen() {
    val viewModel: AiSettingsViewModel = screenScopedViewModel()
    val state = viewModel.uiState()
    AiSettingsUi(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiSettingsUi(
    state: AiSettingsState,
    onEvent: (AiSettingsEvent) -> Unit,
) {
    val nav = navigation()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(state.infoMessage, state.errorMessage) {
        val msg = state.errorMessage ?: state.infoMessage
        if (!msg.isNullOrEmpty()) {
            scope.launch {
                snackbar.showSnackbar(msg)
                currentOnEvent(AiSettingsEvent.DismissMessages)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("ai_settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("AI assistant settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(AiSettingsEvent.ResetAll) }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset all")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { padding ->
        SettingsBody(state = state, paddingValues = padding, onEvent = onEvent)
    }
}

@Composable
@Suppress("LongMethod")
private fun SettingsBody(
    state: AiSettingsState,
    paddingValues: PaddingValues,
    onEvent: (AiSettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("General")
        ToggleRow(
            label = "Enable AI assistant",
            description = "Turn the assistant on or off completely.",
            value = state.enabled,
            onChange = { onEvent(AiSettingsEvent.ToggleEnabled(it)) },
            testTag = "ai_settings_enable",
        )

        HorizontalDivider()

        SectionTitle("Provider")
        ProviderDropdown(
            current = state.provider,
            available = state.availableProviders,
            onChange = { onEvent(AiSettingsEvent.ProviderChanged(it)) },
        )

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = { onEvent(AiSettingsEvent.BaseUrlChanged(it)) },
            label = { Text("Endpoint URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ApiKeyField(
            value = state.apiKey,
            requiresKey = state.provider.requiresApiKey,
            onChange = { onEvent(AiSettingsEvent.ApiKeyChanged(it)) },
        )

        ModelField(
            currentModel = state.model,
            availableModels = state.availableModels.map { it.id }.toPersistentList(),
            supportsListing = state.provider.supportsModelListing,
            fetching = state.fetchingModels,
            onModelChange = { onEvent(AiSettingsEvent.ModelChanged(it)) },
            onFetch = { onEvent(AiSettingsEvent.FetchModels) },
        )

        HorizontalDivider()

        SectionTitle("Generation")
        SliderRow(
            label = "Temperature",
            value = state.temperature,
            valueRange = 0f..2f,
            valueLabel = String.format(java.util.Locale.US, "%.2f", state.temperature),
            onValueChange = { onEvent(AiSettingsEvent.TemperatureChanged(it)) },
        )
        SliderRow(
            label = "Max tokens",
            value = state.maxTokens.toFloat(),
            valueRange = 64f..8192f,
            valueLabel = state.maxTokens.toString(),
            steps = 0,
            onValueChange = { onEvent(AiSettingsEvent.MaxTokensChanged(it.toInt())) },
        )
        ToggleRow(
            label = "Streaming responses",
            description = "Show text as it arrives instead of waiting for the full reply.",
            value = state.streaming,
            onChange = { onEvent(AiSettingsEvent.StreamingToggled(it)) },
        )
        OutlinedTextField(
            value = state.systemPrompt,
            onValueChange = { onEvent(AiSettingsEvent.SystemPromptChanged(it)) },
            label = { Text("System prompt") },
            placeholder = { Text(AiConfig.DEFAULT_SYSTEM_PROMPT) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
        )

        HorizontalDivider()

        SectionTitle("Capabilities")
        ToggleRow(
            label = "Tool calling",
            description = "Allow the AI model to request app tools (e.g. fetch summaries).",
            value = state.toolCallingEnabled,
            onChange = { onEvent(AiSettingsEvent.ToolCallingToggled(it)) },
        )
        ToggleRow(
            label = "Execute app actions",
            description = "Permit the AI to invoke real in-app actions. Stays off by default.",
            value = state.appActionsEnabled,
            onChange = { onEvent(AiSettingsEvent.AppActionsToggled(it)) },
        )
        ToggleRow(
            label = "Offline-only mode",
            description = "Block remote providers; only use local servers like Ollama or LM Studio.",
            value = state.offlineModeOnly,
            onChange = { onEvent(AiSettingsEvent.OfflineModeToggled(it)) },
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { onEvent(AiSettingsEvent.ResetAll) }) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp).padding(end = 8.dp))
            Text("Reset all AI settings")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdown(
    current: AiProvider,
    available: ImmutableList<AiProvider>,
    onChange: (AiProvider) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = current.displayName, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Expand providers")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            available.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName) },
                    onClick = {
                        expanded = false
                        onChange(p)
                    },
                )
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    requiresKey: Boolean,
    onChange: (String) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(if (requiresKey) "API key" else "API key (optional)")
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key",
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelField(
    currentModel: String,
    availableModels: ImmutableList<String>,
    supportsListing: Boolean,
    fetching: Boolean,
    onModelChange: (String) -> Unit,
    onFetch: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = currentModel,
            onValueChange = onModelChange,
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("ai_settings_model"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onFetch,
                enabled = supportsListing && !fetching,
                modifier = Modifier.testTag("ai_settings_fetch_models"),
            ) {
                if (fetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text("Fetch Models")
                }
            }
        }
        when {
            availableModels.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxWidth().height(140.dp),
            ) {
                items(availableModels) { id ->
                    AssistChip(
                        onClick = { onModelChange(id) },
                        label = { Text(id) },
                        modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                    )
                }
            }

            !supportsListing -> FilterChip(
                selected = false,
                onClick = {},
                enabled = false,
                label = { Text("Provider does not expose a list-models endpoint") },
            )
        }
    }
}
