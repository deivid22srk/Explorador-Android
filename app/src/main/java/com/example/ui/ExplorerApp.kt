package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.VirtualItem
import com.example.viewmodel.ExplorerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerApp(
    viewModel: ExplorerViewModel = viewModel()
) {
    // Collect ViewModel States
    val fileSystem by viewModel.fileSystem.collectAsState()
    val pathLeft by viewModel.currentPathLeft.collectAsState()
    val pathRight by viewModel.currentPathRight.collectAsState()
    val activePane by viewModel.activePane.collectAsState()
    val explorerStyle by viewModel.explorerStyle.collectAsState()
    val activePath = if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) pathLeft else (if (activePane == 0) pathLeft else pathRight)
    val darkThemeMode by viewModel.darkThemeMode.collectAsState()
    val accentColorTheme by viewModel.accentColorTheme.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val isMoveOperation by viewModel.isMoveOperation.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()

    // Screen-level state variables
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<VirtualItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameItem by remember { mutableStateOf<VirtualItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { mutableStateOf(viewModel.hasStoragePermission()) }

    val manageStorageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        hasPermission = viewModel.hasStoragePermission()
        viewModel.refreshFileSystem()
    }

    val legacyStorageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        viewModel.refreshFileSystem()
    }

    val requestPermissionAction = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                manageStorageLauncher.launch(intent)
            } catch (e: java.lang.Exception) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (ex: java.lang.Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Não foi possível abrir as configurações de permissão.")
                    }
                }
            }
        } else {
            legacyStorageLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Determine color schemes
    val isDarkTheme = when (darkThemeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val customColorScheme = when (accentColorTheme) {
        "AMBER" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFFB300),
                    secondary = Color(0xFFFFCC80),
                    surface = Color(0xFF1E1C16),
                    background = Color(0xFF12110D)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFFF8F00),
                    secondary = Color(0xFFFFE082),
                    surface = Color(0xFFFFFDF9),
                    background = Color(0xFFFCF8F2)
                )
            }
        }
        "TEAL" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = Color(0xFF00BFA5),
                    secondary = Color(0xFF80CBC4),
                    surface = Color(0xFF16201E),
                    background = Color(0xFF0E1413)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF00796B),
                    secondary = Color(0xFFE0F2F1),
                    surface = Color(0xFFF4FAF9),
                    background = Color(0xFFEDF5F4)
                )
            }
        }
        "PINK" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = Color(0xFFEC407A),
                    secondary = Color(0xFFF48FB1),
                    surface = Color(0xFF22161A),
                    background = Color(0xFF160E11)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFD81B60),
                    secondary = Color(0xFFFCE4EC),
                    surface = Color(0xFFFFF9FA),
                    background = Color(0xFFFAF2F4)
                )
            }
        }
        "BLUE" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = Color(0xFF42A5F5),
                    secondary = Color(0xFF90CAF9),
                    surface = Color(0xFF171A21),
                    background = Color(0xFF101217)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1976D2),
                    secondary = Color(0xFFE3F2FD),
                    surface = Color(0xFFF5F9FC),
                    background = Color(0xFFEDF2F7)
                )
            }
        }
        else -> MaterialTheme.colorScheme // Fallback to system / Material Theme defaults mapping
    }

    MaterialTheme(
        colorScheme = customColorScheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { paddingValues ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. TOP APP BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isSearchActive) {
                                // Search Input Header
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Pesquisar arquivos e pastas...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .testTag("search_input_field"),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Busca"
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.setSearchActive(false) }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Fechar busca"
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                // Standard Interactive Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Left icon based on Explorer style
                                    if (explorerStyle == ExplorerViewModel.STYLE_DUAL_PANE) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Gesto lateral de Menu no modo Painel Duplo!"
                                                    )
                                                }
                                            },
                                            modifier = Modifier.testTag("hamburger_menu_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Menu"
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (activePath != "/storage") {
                                                    viewModel.goUp(0)
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Você já está na raiz do sistema!")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("back_navigation_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Voltar"
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                // Quick parent chooser dropdown mock or popup can go here
                                            }
                                    ) {
                                        Text(
                                            text = if (viewModel.selectedItems.value.isNotEmpty()) {
                                                "${viewModel.selectedItems.value.size} selecionados"
                                            } else {
                                                if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) {
                                                    "Explorador de Arquivos"
                                                } else {
                                                    "Painel Duplo"
                                                }
                                            },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) {
                                                activePath
                                            } else {
                                                // Dual pane summarizes metadata
                                                "Pastas: 65 | Arqs: 23 | Disco: 140,7G / 230,3G"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Header action buttons
                                Row(horizontalArrangement = Arrangement.End) {
                                    IconButton(
                                        onClick = { viewModel.setSearchActive(true) },
                                        modifier = Modifier.testTag("search_toggle_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Pesquisar"
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleExplorerStyle() },
                                        modifier = Modifier.testTag("layout_style_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) {
                                                Icons.Default.GridOn
                                            } else {
                                                Icons.Default.List
                                            },
                                            contentDescription = "Tocar estilo"
                                        )
                                    }

                                    IconButton(
                                        onClick = { showSettingsDialog = true },
                                        modifier = Modifier.testTag("settings_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Configurações"
                                        )
                                    }
                                }
                            }
                        }

                        // Alert Banner for simulated vs real storage
                        if (!hasPermission) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable { requestPermissionAction() }
                                    .testTag("permission_alert_banner"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Alerta de Permissão",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Exibindo Pastas Simuladas",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Toque para conceder permissão de armazenamento e explorar pastas reais do seu celular.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                            lineHeight = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { requestPermissionAction() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Permitir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Inside ZIP file navigation banner
                        val isInsideZip = activePath.contains("::")
                        AnimatedVisibility(visible = isInsideZip) {
                            val activeZipPath = activePath.substringBefore("::")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = "Visualizador ZIP",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Dentro do Arquivo ZIP",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Selecione arquivos/pastas para copiar e colar fora, ou descompacte todo o conteúdo.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                            lineHeight = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            val targetDir = java.io.File(activeZipPath).parent ?: viewModel.privateAppPath
                                            val success = viewModel.extractAllArchive(activeZipPath, targetDir)
                                            coroutineScope.launch {
                                                if (success) {
                                                    snackbarHostState.showSnackbar("Arquivo descompactado em: $targetDir")
                                                } else {
                                                    snackbarHostState.showSnackbar("Erro ao descompactar.")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Extrair Tudo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Options Toolbar when in Selection Mode
                        AnimatedVisibility(visible = selectedItems.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    IconButton(onClick = { viewModel.startCopyOperation(selectedItems.toList()) }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar")
                                            Text("Copiar", fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.startMoveOperation(selectedItems.toList()) }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Mover")
                                            Text("Recortar", fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = {
                                        if (selectedItems.size == 1) {
                                            renameItem = selectedItems.first()
                                            showRenameDialog = true
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Selecione apenas 1 item para renomear")
                                            }
                                        }
                                    }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Renomear")
                                            Text("Renomear", fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteItems(selectedItems.toList())
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Itens excluídos com sucesso!")
                                        }
                                    }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                                            Text("Excluir", fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(onClick = {
                                        if (selectedItems.size == 1) {
                                            detailItem = selectedItems.first()
                                            showDetailsDialog = true
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Selecione 1 item para ver detalhes")
                                            }
                                        }
                                    }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Info, contentDescription = "Detalhes")
                                            Text("Detalhes", fontSize = 10.sp)
                                        }
                                    }
                                    val containsZip = selectedItems.size == 1 && selectedItems.first().name.endsWith(".zip", ignoreCase = true)
                                    if (containsZip) {
                                        IconButton(onClick = {
                                            val zipItem = selectedItems.first()
                                            val currentFolder = activePath
                                            val success = viewModel.extractAllArchive(zipItem.path, currentFolder)
                                            viewModel.clearSelection()
                                            coroutineScope.launch {
                                                if (success) {
                                                    snackbarHostState.showSnackbar("Arquivo ZIP extraído com sucesso na pasta atual!")
                                                } else {
                                                    snackbarHostState.showSnackbar("Erro ao extrair arquivo ZIP.")
                                                }
                                            }
                                        }) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(imageVector = Icons.Default.Unarchive, contentDescription = "Extrair")
                                                Text("Extrair", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { viewModel.clearSelection() }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Desmarcar")
                                            Text("Limpar", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. CHOOSE EXPLORER STYLE VIEW
                        if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) {
                            // ----------------- STYLE 1: SINGLE PANE -----------------
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Real-time horizontal mounts / short-cuts (from the screenshots!)
                                ShortcutSection(
                                    onShortcutClick = { rootPath ->
                                        viewModel.navigateTo(rootPath, 0)
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )

                                // Current active Directory Title Header in List View (e.g. "..", "Alarms", "Android")
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Arquivos locais",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Files List
                                val filteredList = remember(fileSystem, pathLeft, searchQuery, showHiddenFiles, sortOption) {
                                    filterAndSortFiles(fileSystem, pathLeft, searchQuery, showHiddenFiles, sortOption)
                                }

                                if (filteredList.isEmpty()) {
                                    EmptyStateView(
                                        message = "Pasta vazia ou sem resultados para a pesquisa."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .testTag("single_pane_lazy_col")
                                    ) {
                                        // Parent navigation ".." (shown only when we coordinates aren't /storage and search is inactive)
                                        if (pathLeft != "/storage" && searchQuery.isEmpty()) {
                                            item {
                                                ParentNavigationRow(
                                                    onClick = { viewModel.goUp(0) }
                                                )
                                            }
                                        }

                                        items(filteredList) { fItem ->
                                            FileListItem(
                                                item = fItem,
                                                isSelected = selectedItems.contains(fItem),
                                                showColorfulIcons = true,
                                                onItemClick = { clickedItem ->
                                                    if (selectedItems.isNotEmpty()) {
                                                        viewModel.toggleSelectItem(clickedItem)
                                                    } else {
                                                        if (clickedItem.isDirectory) {
                                                            viewModel.navigateTo(clickedItem.path, 0)
                                                        } else if (clickedItem.name.endsWith(".zip", ignoreCase = true)) {
                                                            viewModel.navigateTo("${clickedItem.path}::/", 0)
                                                        } else {
                                                            // Show details or file snackbar
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("Arquivo: ${clickedItem.name} (${clickedItem.itemDetails})")
                                                            }
                                                        }
                                                    }
                                                },
                                                onItemLongClick = { clickedItem ->
                                                    viewModel.toggleSelectItem(clickedItem)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // ----------------- STYLE 2: DUAL PANE SPLIT SCREEN -----------------
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Split Pane Screen
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    // LEFT COLUMN MODULE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(
                                                width = if (activePane == 0) 2.dp else 0.5.dp,
                                                color = if (activePane == 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                                    alpha = 0.3f
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (activePane == 0) MaterialTheme.colorScheme.surface.copy(
                                                    alpha = 0.95f
                                                ) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                            )
                                            .clickable { viewModel.setActivePane(0) }
                                            .padding(4.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Left Pane Path indicator header card
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (activePane == 0) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(2.dp)
                                            ) {
                                                Text(
                                                    text = pathLeft,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (activePane == 0) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            val leftPaneList = remember(fileSystem, pathLeft, searchQuery, showHiddenFiles, sortOption) {
                                                filterAndSortFiles(fileSystem, pathLeft, searchQuery, showHiddenFiles, sortOption)
                                            }

                                            if (leftPaneList.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Vazio", fontSize = 12.sp, color = Color.Gray)
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .testTag("left_pane_lazy_col")
                                                ) {
                                                    // Parent ".."
                                                    if (pathLeft != "/storage" && searchQuery.isEmpty()) {
                                                        item {
                                                            ParentNavigationRow(
                                                                onClick = { viewModel.goUp(0) }
                                                            )
                                                        }
                                                    }

                                                    items(leftPaneList) { fItem ->
                                                        FileListItem(
                                                            item = fItem,
                                                            isSelected = selectedItems.contains(fItem),
                                                            showColorfulIcons = false, // Monochromatic list matching Style 2
                                                            onItemClick = { clickedItem ->
                                                                viewModel.setActivePane(0)
                                                                if (selectedItems.isNotEmpty()) {
                                                                    viewModel.toggleSelectItem(clickedItem)
                                                                } else {
                                                                    if (clickedItem.isDirectory) {
                                                                        viewModel.navigateTo(clickedItem.path, 0)
                                                                    } else if (clickedItem.name.endsWith(".zip", ignoreCase = true)) {
                                                                        viewModel.navigateTo("${clickedItem.path}::/", 0)
                                                                    } else {
                                                                        coroutineScope.launch {
                                                                            snackbarHostState.showSnackbar("Arquivo: ${clickedItem.name}")
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onItemLongClick = { clickedItem ->
                                                                viewModel.setActivePane(0)
                                                                viewModel.toggleSelectItem(clickedItem)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // RIGHT COLUMN MODULE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(
                                                width = if (activePane == 1) 2.dp else 0.5.dp,
                                                color = if (activePane == 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                                    alpha = 0.3f
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (activePane == 1) MaterialTheme.colorScheme.surface.copy(
                                                    alpha = 0.95f
                                                ) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                            )
                                            .clickable { viewModel.setActivePane(1) }
                                            .padding(4.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Right Pane Path indicator
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (activePane == 1) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(2.dp)
                                            ) {
                                                Text(
                                                    text = pathRight,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (activePane == 1) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            val rightPaneList = remember(fileSystem, pathRight, searchQuery, showHiddenFiles, sortOption) {
                                                filterAndSortFiles(fileSystem, pathRight, searchQuery, showHiddenFiles, sortOption)
                                            }

                                            if (rightPaneList.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Vazio", fontSize = 12.sp, color = Color.Gray)
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .testTag("right_pane_lazy_col")
                                                ) {
                                                    // Parent ".."
                                                    if (pathRight != "/storage" && searchQuery.isEmpty()) {
                                                        item {
                                                            ParentNavigationRow(
                                                                onClick = { viewModel.goUp(1) }
                                                            )
                                                        }
                                                    }

                                                    items(rightPaneList) { fItem ->
                                                        FileListItem(
                                                            item = fItem,
                                                            isSelected = selectedItems.contains(fItem),
                                                            showColorfulIcons = false,
                                                            onItemClick = { clickedItem ->
                                                                viewModel.setActivePane(1)
                                                                if (selectedItems.isNotEmpty()) {
                                                                    viewModel.toggleSelectItem(clickedItem)
                                                                } else {
                                                                    if (clickedItem.isDirectory) {
                                                                        viewModel.navigateTo(clickedItem.path, 1)
                                                                    } else if (clickedItem.name.endsWith(".zip", ignoreCase = true)) {
                                                                        viewModel.navigateTo("${clickedItem.path}::/", 1)
                                                                    } else {
                                                                        coroutineScope.launch {
                                                                            snackbarHostState.showSnackbar("Arquivo: ${clickedItem.name}")
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onItemLongClick = { clickedItem ->
                                                                viewModel.setActivePane(1)
                                                                viewModel.toggleSelectItem(clickedItem)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Dual Pane bottom actions panel bar (from screenshot 3)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    IconButton(
                                        onClick = { viewModel.goUp(activePane) },
                                        modifier = Modifier.testTag("dual_pane_left_arrow_nav")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Voltar Direitinho"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            // Action context simulation or detail open
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Visualizador em Painel Duplo ativo no diretório: $activePath")
                                            }
                                        },
                                        modifier = Modifier.testTag("dual_pane_right_arrow_nav")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Avançar"
                                        )
                                    }

                                    IconButton(
                                        onClick = { showCreateDialog = true },
                                        modifier = Modifier.testTag("dual_pane_add_nav")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Criar novo item"
                                        )
                                    }

                                    // Special cloning file-transfer swapping operation (Swap items from active pane down to the other side!)
                                    IconButton(
                                        onClick = {
                                            viewModel.mirrorClipboardToOtherPane()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Sincronizando/Copiando conteúdo para o painel vizinho!")
                                            }
                                        },
                                        modifier = Modifier.testTag("dual_pane_swap_nav")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Copiar para Painel Vizinho"
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.goUp(activePane) },
                                        modifier = Modifier.testTag("dual_pane_up_nav")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Subir nível"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. FLOATING CLIPBOARD NOTIFICATION STRIP FOR SELESS COPY/PASTE
                    AnimatedVisibility(
                        visible = clipboardItems.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isMoveOperation) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                                        contentDescription = "Mover ou Copiar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${clipboardItems.size} item(ns) prontos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Row {
                                    IconButton(onClick = { viewModel.clearClipboard() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancelar transferência",
                                            tint = Color.Red
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val currentPane = if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) 0 else activePane
                                            val targetParent = if (currentPane == 0) pathLeft else pathRight
                                            viewModel.pasteClipboard(targetParent)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Itens mesclados na pasta de destino!")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Colar",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Colar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. FLOATING ACTION BUTTON (Visible only in Single Pane Style, style 1)
                    if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) {
                        FloatingActionButton(
                            onClick = { showCreateDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp)
                                .testTag("create_item_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Criar novo item"
                            )
                        }
                    }
                }

                // ------------------- DYNAMIC DIALOG MODALS -------------------

                // A. CREATION DIALOG (FOLDER / FILE)
                if (showCreateDialog) {
                    var creationType by remember { mutableStateOf("FOLDER") } // "FOLDER" or "FILE"
                    var inputName by remember { mutableStateOf("") }
                    var fileExtension by remember { mutableStateOf("txt") }
                    var fileSizeString by remember { mutableStateOf("12 KB") }

                    AlertDialog(
                        onDismissRequest = { showCreateDialog = false },
                        title = { Text(text = "Criar Novo Item") },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Toggle Folders or Files
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { creationType = "FOLDER" },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (creationType == "FOLDER") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) {
                                        Text("Pasta")
                                    }
                                    OutlinedButton(
                                        onClick = { creationType = "FILE" },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (creationType == "FILE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) {
                                        Text("Arquivo")
                                    }
                                }

                                OutlinedTextField(
                                    value = inputName,
                                    onValueChange = { inputName = it },
                                    label = { Text("Nome do item") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("creator_name_input"),
                                    singleLine = true
                                )

                                if (creationType == "FILE") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Tipo de extensão:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    
                                    val extensionsList = listOf("txt", "pdf", "png", "mp3", "apk", "xlsx", "docx")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        extensionsList.forEach { ext ->
                                            val isSelected = fileExtension == ext
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { fileExtension = ext }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = ext.uppercase(),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = fileSizeString,
                                        onValueChange = { fileSizeString = it },
                                        label = { Text("Tamanho simulado (ex: 2.1 MB)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val currentPane = if (explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE) 0 else activePane
                                    val targetParent = if (currentPane == 0) pathLeft else pathRight

                                    if (creationType == "FOLDER") {
                                        viewModel.createFolder(inputName, targetParent)
                                    } else {
                                        viewModel.createFile(inputName, fileExtension, fileSizeString, targetParent)
                                    }
                                    showCreateDialog = false
                                },
                                modifier = Modifier.testTag("confirm_create_btn")
                            ) {
                                Text("Criar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                // B. SETTINGS DIALOG MODAL
                if (showSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings Icon")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Configurações")
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Tocar Estilo de Explorador (Requested feature!)
                                Column {
                                    Text("Estilo de Visualização", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = explorerStyle == ExplorerViewModel.STYLE_SINGLE_PANE,
                                            onClick = { viewModel.setExplorerStyle(ExplorerViewModel.STYLE_SINGLE_PANE) },
                                            modifier = Modifier.testTag("radio_single_pane")
                                        )
                                        Text("Painel Único (Inovação Expressiva)", fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = explorerStyle == ExplorerViewModel.STYLE_DUAL_PANE,
                                            onClick = { viewModel.setExplorerStyle(ExplorerViewModel.STYLE_DUAL_PANE) },
                                            modifier = Modifier.testTag("radio_dual_pane")
                                        )
                                        Text("Painel Duplo (Multitarefa Clássico)", fontSize = 12.sp)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // 2. Dark theme mode
                                Column {
                                    Text("Modo Escuro", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tocar estilo tema:", fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (darkThemeMode == "LIGHT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { viewModel.setDarkThemeMode("LIGHT") }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Claro", fontSize = 10.sp, color = if (darkThemeMode == "LIGHT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (darkThemeMode == "DARK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { viewModel.setDarkThemeMode("DARK") }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Escuro", fontSize = 10.sp, color = if (darkThemeMode == "DARK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (darkThemeMode == "SYSTEM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { viewModel.setDarkThemeMode("SYSTEM") }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Sistema", fontSize = 10.sp, color = if (darkThemeMode == "SYSTEM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // 3. Material theme accents
                                Column {
                                    Text("Visual Expressivo - Sotaque de Paleta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val colorsList = listOf(
                                            "DYNAMIC" to Color.Gray,
                                            "AMBER" to Color(0xFFFF8F00),
                                            "TEAL" to Color(0xFF00796B),
                                            "PINK" to Color(0xFFD81B60),
                                            "BLUE" to Color(0xFF1976D2)
                                        )

                                        colorsList.forEach { (themeName, color) ->
                                            val isSelected = accentColorTheme == themeName
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.setAccentColorTheme(themeName) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // 4. Sort selection option
                                Column {
                                    Text("Ordenar por:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("NAME" to "Nome", "DATE" to "Data Modificada", "SIZE" to "Tamanho").forEach { (option, label) ->
                                            val isSelected = sortOption == option
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { viewModel.setSortOption(option) }
                                                )
                                                Text(text = label, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // 5. Hidden Files Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Mostrar Arquivos Ocultos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Exibe arquivos iniciados por pontos .", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = showHiddenFiles,
                                        onCheckedChange = { viewModel.toggleShowHiddenFiles() },
                                        modifier = Modifier.testTag("switch_hidden_files")
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showSettingsDialog = false },
                                modifier = Modifier.testTag("close_settings_btn")
                            ) {
                                Text("Pronto")
                            }
                        }
                    )
                }

                // C. DETAIL VIEWER MODAL
                if (showDetailsDialog && detailItem != null) {
                    val item = detailItem!!
                    AlertDialog(
                        onDismissRequest = { showDetailsDialog = false },
                        title = { Text("Detalhes do Item") },
                        text = {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Nome: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Tipo: ${if (item.isDirectory) "Pasta / Diretório" else "Arquivo de Mídia"}", fontSize = 13.sp)
                                    Text("Caminho: ${item.path}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Modificado: ${item.lastModified}", fontSize = 12.sp)
                                    Text("Tamanho / Conteúdo: ${item.itemDetails}", fontSize = 12.sp)
                                    Text("Arquivo Oculto: ${if (item.isHidden) "Sim" else "Não"}", fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showDetailsDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // D. RENAME DIALOG MODAL
                if (showRenameDialog && renameItem != null) {
                    val item = renameItem!!
                    var newNameInput by remember { mutableStateOf(item.name) }

                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Renomear Item") },
                        text = {
                            OutlinedTextField(
                                value = newNameInput,
                                onValueChange = { newNameInput = it },
                                label = { Text("Novo Nome") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rename_input_field"),
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.renameItem(item, newNameInput)
                                    showRenameDialog = false
                                },
                                modifier = Modifier.testTag("confirm_rename_btn")
                            ) {
                                Text("Renomear")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

// =================================== REUSABLE SUB-COMPONENTS ===================================

@Composable
fun ShortcutSection(
    onShortcutClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appPrivatePath = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath

    Column {
        Text(
            text = "Categorias e Dispositivos",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutCard(
                title = "Pasta do App (Real)",
                subtitle = "Escrita e leitura real",
                path = appPrivatePath,
                icon = Icons.Default.Folder,
                color = Color(0xFF00BFA5),
                onClick = onShortcutClick
            )

            // Mocks the mount card indicators from Style 1 screenshots
            ShortcutCard(
                title = "Memória Interna",
                subtitle = "89.61GB / 230.35GB",
                path = "/storage/emulated/0",
                icon = Icons.Default.Storage,
                color = Color(0xFF4CAF50),
                onClick = onShortcutClick
            )

            ShortcutCard(
                title = "Cartão SD",
                subtitle = "77.69GB / 116.50GB",
                path = "/storage/FFE7-7614",
                icon = Icons.Default.SdCard,
                color = Color(0xFF8BC34A),
                onClick = onShortcutClick
            )

            ShortcutCard(
                title = "Download",
                subtitle = "Baixados do sistema",
                path = "/storage/emulated/0/Download",
                icon = Icons.Default.Download,
                color = Color(0xFFFF9800),
                onClick = onShortcutClick
            )

            ShortcutCard(
                title = "Músicas",
                subtitle = "Biblioteca MP3",
                path = "/storage/emulated/0/Music",
                icon = Icons.Default.MusicNote,
                color = Color(0xFFFFC107),
                onClick = onShortcutClick
            )

            ShortcutCard(
                title = "Documentos",
                subtitle = "Arquivos DOC e PDF",
                path = "/storage/emulated/0/Documents",
                icon = Icons.Default.Description,
                color = Color(0xFF03A9F4),
                onClick = onShortcutClick
            )
        }
    }
}

@Composable
fun ShortcutCard(
    title: String,
    subtitle: String,
    path: String,
    icon: ImageVector,
    color: Color,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(185.dp)
            .clickable { onClick(path) }
            .testTag("shortcut_card_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Render folder list row
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: VirtualItem,
    isSelected: Boolean,
    showColorfulIcons: Boolean,
    onItemClick: (VirtualItem) -> Unit,
    onItemLongClick: (VirtualItem) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        }, label = "fBck"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("file_item_${item.name.lowercase().replace(".", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Checkbox indicator inside selections
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp)
                )
            } else if (showColorfulIcons && isSelected) {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Não selecionado",
                    tint = Color.Gray,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp)
                )
            }

            // Beautiful Expressive Icons mapping
            val (icon, tint) = getExpressiveIconDetails(item, showColorfulIcons)

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Arquivo ou Pasta",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isHidden) Color.Gray else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.itemDetails,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Action or Date indicator
        Text(
            text = item.lastModified,
            fontSize = 11.sp,
            color = Color.Gray.copy(alpha = 0.75f)
        )
    }
}

// Special parent ".." row navigator
@Composable
fun ParentNavigationRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("parent_dots_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Subir nível",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "..",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun EmptyStateView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Pasta vazia",
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

// helper logical function to assign expressive icons following screenshot styles
fun getExpressiveIconDetails(item: VirtualItem, useColors: Boolean): Pair<ImageVector, Color> {
    if (!useColors) {
        // Monochromatic / subtle slate theme colors as depicted in screenshot 3
        val icon = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description
        return Pair(icon, Color.Gray)
    }

    if (item.isDirectory) {
        // Custom colorful expressive badge folders from screenshots (Alarms, Android, Audiobooks, Music, etc.)
        return when (item.name.lowercase()) {
            "alarms" -> Pair(Icons.Default.WatchLater, Color(0xFFFF9800)) // Clock inside alarms folder
            "android" -> Pair(Icons.Default.Android, Color(0xFF8BC34A)) // Android icon folder
            "music", "audiobooks", "músicas" -> Pair(Icons.Default.MusicNote, Color(0xFFE91E63)) // Music folder
            "download", "appdownload" -> Pair(Icons.Default.Download, Color(0xFF2196F3)) // Download folder
            "documents" -> Pair(Icons.Default.Description, Color(0xFF03A9F4)) // Document folder
            else -> {
                // Amber theme folders (Style 1 screenshot style)
                Pair(Icons.Default.Folder, Color(0xFFFFB300))
            }
        }
    } else {
        // File extension mappings
        val ext = item.name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp3" -> Pair(Icons.Default.AudioFile, Color(0xFF9C27B0))
            "pdf" -> Pair(Icons.Default.Description, Color(0xFFE53935))
            "png", "jpg" -> Pair(Icons.Default.Description, Color(0xFF4CAF50))
            "apk" -> Pair(Icons.Default.Android, Color(0xFF8BC34A))
            "zip", "rar" -> Pair(Icons.Default.FolderZip, Color(0xFF607D8B))
            else -> Pair(Icons.Default.Description, Color(0xFF5C6BC0))
        }
    }
}

// Logical helper to compute current list based on filters/sorts
fun filterAndSortFiles(
    allFiles: List<VirtualItem>,
    currentRelativePath: String,
    searchQuery: String,
    showHidden: Boolean,
    sortMode: String
): List<VirtualItem> {
    // 1. Filter: If search is active, do a global search. Otherwise, list immediate folder children
    var list = if (searchQuery.isNotEmpty()) {
        allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    } else {
        allFiles.filter { f ->
            // Parent of item should be current path
            val lastSlash = f.path.lastIndexOf('/')
            val parentPath = if (lastSlash == 0) "/" else if (lastSlash > 0) f.path.substring(0, lastSlash) else ""
            parentPath == currentRelativePath
        }
    }

    // 2. Filter hidden folders starting with dot if showHidden is false
    if (!showHidden) {
        list = list.filter { !it.isHidden }
    }

    // 3. Sort options
    return when (sortMode) {
        "DATE" -> list.sortedWith(compareByDescending<VirtualItem> { it.isDirectory }.thenBy { it.lastModified })
        "SIZE" -> list.sortedWith(compareByDescending<VirtualItem> { it.isDirectory }.thenBy { it.itemDetails })
        else -> list.sortedWith(compareByDescending<VirtualItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
}
