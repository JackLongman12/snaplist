package com.listingprep.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.LocalContext
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import com.listingprep.data.readyToShare
import com.listingprep.data.isActive
import com.listingprep.data.Regions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.listingprep.data.CONDITIONS
import com.listingprep.data.Item
import com.listingprep.data.Photo
import com.listingprep.data.Status

// ---- settings -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(state: UiState, vm: AppViewModel, onBack: () -> Unit) {
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(uri)
    }
    val context = LocalContext.current
    val idle = state.busy == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCards(state, vm)

            ModelSection(state, vm, onImportFile = { modelPicker.launch(arrayOf("*/*")) })

            HorizontalDivider()
            PrivacySection(state, vm)

            HorizontalDivider()
            Text("AI writing style", style = MaterialTheme.typography.titleMedium)
            Text(
                "Applies to every description the AI writes. Facts still come only from the photos, codes and your notes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STYLE_PRESETS.forEach { (label, text) ->
                    FilterChip(
                        selected = state.settings.writingStyle == text,
                        onClick = { vm.updateSettings { it.copy(writingStyle = text) } },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = state.settings.writingStyle,
                onValueChange = { v -> vm.updateSettings { it.copy(writingStyle = v.take(1000)) } },
                label = { Text("Style instructions") },
                placeholder = { Text("e.g. Short and casual. Mention it's from a smoke-free home. Use Australian spelling.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SettingSwitch(
                title = "Write in the background",
                detail = "After you take photos, the AI writes the new descriptions on its own. Leave the app if you like; " +
                    "a notification says when they're ready.",
                checked = state.settings.autoWrite,
                onChange = { on -> vm.updateSettings { it.copy(autoWrite = on) } },
            )
            SettingSwitch(
                title = "Look up barcodes online",
                detail = "Only when the AI can't tell what an item is. Sends just the barcode number (not photos) over HTTPS " +
                    "to upcitemdb.com, Open Food Facts or Open Library (books).",
                checked = state.settings.onlineLookup,
                onChange = { on -> vm.updateSettings { it.copy(onlineLookup = on) } },
            )

            HorizontalDivider()
            Text("Location", style = MaterialTheme.typography.titleMedium)
            var picking by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Text(Regions.describe(state.settings), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "Sets the currency for prices and the spelling and units in descriptions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (picking) LocationPicker(
                onPicked = { loc ->
                    picking = false
                    vm.updateSettings { it.copy(region = loc.country, area = loc.area, areaName = loc.areaName, city = loc.city) }
                },
                onDismiss = { picking = false },
            )

            HorizontalDivider()
            Text("Phone's built-in AI", style = MaterialTheme.typography.titleMedium)
            SettingSwitch(
                title = "Use Gemini Nano when available",
                detail = when (state.nano) {
                    com.listingprep.ml.Nano.State.READY -> "Ready on this phone. Runs on the phone's AI chip, nothing is uploaded, no extra download."
                    com.listingprep.ml.Nano.State.DOWNLOADING -> "Android is downloading it (it manages this itself)…"
                    com.listingprep.ml.Nano.State.DOWNLOADABLE -> "This phone supports it; Android will download it when this is on."
                    com.listingprep.ml.Nano.State.UNAVAILABLE -> "Not available on this phone (or Android hasn't enabled it yet). The 4.4 GB model is used instead."
                    null -> "Checking…"
                },
                checked = state.settings.useNano,
                onChange = { on -> vm.updateSettings { it.copy(useNano = on) }; if (on) vm.refreshNano(download = true) },
            )
            TextButton(onClick = { vm.refreshNano(download = state.settings.useNano) }) { Text("Check again") }

            HorizontalDivider()
            CloudSection(state, vm)

            HorizontalDivider()
            Text("Updates and crash reports", style = MaterialTheme.typography.titleMedium)
            SettingSwitch(
                title = "Update automatically",
                detail = "Checks this app's GitHub releases, downloads a new build and installs it when you leave the app. " +
                    "Only builds signed with the same key as this one are accepted.",
                checked = state.settings.autoUpdate,
                onChange = { on -> vm.updateSettings { it.copy(autoUpdate = on) } },
            )
            if (com.listingprep.CrashLog.canSend) {
                SettingSwitch(
                    title = "Send crash reports automatically",
                    detail = "If the app crashes, a report is posted to the app's public issue board on GitHub. It only has the " +
                        "app build, Android version, phone model, the day and which lines of code failed: no error messages, " +
                        "photos, listings or anything about you. Off: you're offered a copy instead.",
                    checked = state.settings.crashReports,
                    onChange = { on -> vm.updateSettings { it.copy(crashReports = on) } },
                )
            }
            OutlinedButton(onClick = { vm.checkForUpdate() }, enabled = !state.updatePopup) {
                Text("Check for updates (now on build ${remember { com.listingprep.update.Updater.installedBuild(context) }})")
            }

            HorizontalDivider()
            Text("Listing defaults", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.settings.footer,
                onValueChange = { v -> vm.updateSettings { it.copy(footer = v) } },
                label = { Text("Footer added to every description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Default condition for new items", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CONDITIONS.forEach { c ->
                    FilterChip(
                        selected = state.settings.defaultCondition == c,
                        onClick = { vm.updateSettings { it.copy(defaultCondition = c) } },
                        label = { Text(c) },
                    )
                }
            }
        }
    }
}

/** Optional cloud writing with Claude: your own Anthropic API key, off by default. */
@Composable
private fun CloudSection(state: UiState, vm: AppViewModel) {
    var editing by remember { mutableStateOf(false) }
    Text("Better descriptions (cloud)", style = MaterialTheme.typography.titleMedium)
    Text(
        "Claude, a large AI from Anthropic, looks at up to ${com.listingprep.ml.Claude.MAX_PHOTOS} photos of each item and " +
            "writes far more accurate listings than the phone's AI. It needs your own Anthropic API key " +
            "(console.anthropic.com) and costs about 5 cents per listing, billed to that key.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingSwitch(
        title = "Write with Claude",
        detail = if (state.hasClaudeKey) {
            "When on, the item's photos (shrunk), what was read from them and your notes are sent over HTTPS to " +
                "api.anthropic.com. Nothing else leaves the phone. If Claude can't be reached, the phone's AI writes it instead."
        } else {
            "Add an API key first."
        },
        checked = state.settings.cloudWriting && state.hasClaudeKey,
        onChange = { on ->
            if (on && !state.hasClaudeKey) editing = true
            else vm.updateSettings { it.copy(cloudWriting = on) }
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { editing = true }) { Text(if (state.hasClaudeKey) "Change API key" else "Add API key") }
        if (state.hasClaudeKey) TextButton(onClick = { vm.saveClaudeKey(null) }) { Text("Remove key") }
    }
    if (editing) {
        var key by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Anthropic API key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Create one at console.anthropic.com → API keys, then paste it here. It's stored encrypted " +
                            "with a key locked in the phone's hardware, and only ever sent to api.anthropic.com. " +
                            "Set a monthly spend limit in the Console if you like.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it.trim() },
                        label = { Text("sk-ant-…") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = key.startsWith("sk-ant-") && key.length > 20,
                    onClick = {
                        vm.saveClaudeKey(key)
                        vm.updateSettings { it.copy(cloudWriting = true) }
                        editing = false
                    },
                ) { Text("Save and turn on") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }
}
