package com.example.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import com.example.model.RealFileSystem
import com.example.model.VirtualItem
import com.example.model.ZipFileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    // Style constants
    companion object {
        const val STYLE_SINGLE_PANE = "PAINEL_UNICO"
        const val STYLE_DUAL_PANE = "PAINEL_DUPLO"
    }

    // 1. Filesystem State (Refreshes dynamically based on directories browsed)
    private val _fileSystem = MutableStateFlow<List<VirtualItem>>(emptyList())
    val fileSystem: StateFlow<List<VirtualItem>> = _fileSystem.asStateFlow()

    // 2. Active Browsing Paths
    private val _currentPathLeft = MutableStateFlow("/storage/emulated/0")
    val currentPathLeft: StateFlow<String> = _currentPathLeft.asStateFlow()

    private val _currentPathRight = MutableStateFlow("/storage/emulated/0")
    val currentPathRight: StateFlow<String> = _currentPathRight.asStateFlow()

    // 0 = Left Pane, 1 = Right Pane (focused/active panel in Dual Pane)
    private val _activePane = MutableStateFlow(0)
    val activePane: StateFlow<Int> = _activePane.asStateFlow()

    // 3. App Settings Flow
    private val _explorerStyle = MutableStateFlow(STYLE_SINGLE_PANE)
    val explorerStyle: StateFlow<String> = _explorerStyle.asStateFlow()

    private val _darkThemeMode = MutableStateFlow("SYSTEM") // "SYSTEM", "LIGHT", "DARK"
    val darkThemeMode: StateFlow<String> = _darkThemeMode.asStateFlow()

    private val _accentColorTheme = MutableStateFlow("DYNAMIC") // "DYNAMIC", "AMBER", "TEAL", "PINK", "BLUE"
    val accentColorTheme: StateFlow<String> = _accentColorTheme.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _sortOption = MutableStateFlow("NAME") // "NAME", "DATE", "SIZE"
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    // 4. Selections & Clipboard state
    private val _selectedItems = MutableStateFlow<Set<VirtualItem>>(emptySet())
    val selectedItems: StateFlow<Set<VirtualItem>> = _selectedItems.asStateFlow()

    private val _clipboardItems = MutableStateFlow<List<VirtualItem>>(emptyList())
    val clipboardItems: StateFlow<List<VirtualItem>> = _clipboardItems.asStateFlow()

    private val _isMoveOperation = MutableStateFlow(false)
    val isMoveOperation: StateFlow<Boolean> = _isMoveOperation.asStateFlow()

    // 5. Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Active path based on active pane (for single-pane/focused actions)
    val activePath: String
        get() = if (_explorerStyle.value == STYLE_SINGLE_PANE) _currentPathLeft.value else (if (_activePane.value == 0) _currentPathLeft.value else _currentPathRight.value)

    val privateAppPath: String by lazy {
        getApplication<Application>().getExternalFilesDir(null)?.absolutePath 
            ?: getApplication<Application>().filesDir.absolutePath
    }

    init {
        // Find best dynamic starting path based on readability
        val initialPath = getAccessibleDefaultDirectory()
        _currentPathLeft.value = initialPath
        _currentPathRight.value = initialPath
        refreshFileSystem()
    }

    fun getAccessibleDefaultDirectory(): String {
        val extDir = "/storage/emulated/0"
        val extFile = File(extDir)
        return if (extFile.exists() && extFile.canRead()) {
            extDir
        } else {
            privateAppPath
        }
    }

    // Checking of real Android Storage permissions
    fun hasStoragePermission(): Boolean {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Refresh contents of active directories
    fun refreshFileSystem() {
        val leftPath = _currentPathLeft.value
        val rightPath = _currentPathRight.value

        val leftItems = loadDirectoryFiles(leftPath)
        val rightItems = if (_explorerStyle.value == STYLE_DUAL_PANE && rightPath != leftPath) {
            loadDirectoryFiles(rightPath)
        } else {
            emptyList()
        }

        // Merge active files so they are visible to list views in Jetpack Compose
        _fileSystem.value = (leftItems + rightItems).distinctBy { it.path }
    }

    private fun loadDirectoryFiles(path: String): List<VirtualItem> {
        // If it's a path within a ZIP file
        if (path.contains("::")) {
            val parts = path.split("::")
            val zipPath = parts[0]
            val innerPath = if (parts.size > 1) parts[1] else ""
            return ZipFileSystem.listZipContents(zipPath, innerPath)
        }

        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            return RealFileSystem.listRealFiles(path)
        }
        return emptyList()
    }

    // Helper to get parent directory of a path
    private fun getParentPath(path: String): String {
        if (path.contains("::")) {
            val parts = path.split("::")
            val zipPath = parts[0]
            val innerPath = if (parts.size > 1) parts[1].trim('/') else ""
            if (innerPath.isEmpty()) {
                // Exit the zip and go to the directory holding the zip file
                val file = File(zipPath)
                return file.parent ?: "/storage/emulated/0"
            } else {
                val lastSlash = innerPath.lastIndexOf('/')
                return if (lastSlash == -1) {
                    "$zipPath::/"
                } else {
                    val parentInner = innerPath.substring(0, lastSlash)
                    "$zipPath::/$parentInner"
                }
            }
        }

        if (path == "/storage") return "/storage"
        val lastIdx = path.lastIndexOf('/')
        if (lastIdx <= 0) return "/storage"
        return path.substring(0, lastIdx)
    }

    // Navigation Methods
    fun navigateTo(path: String, paneIndex: Int) {
        if (paneIndex == 0) {
            _currentPathLeft.value = path
        } else {
            _currentPathRight.value = path
        }
        // Deselect items when navigating and refresh live files
        clearSelection()
        refreshFileSystem()
    }

    fun goUp(paneIndex: Int) {
        val path = if (paneIndex == 0) _currentPathLeft.value else _currentPathRight.value
        val parent = getParentPath(path)
        navigateTo(parent, paneIndex)
    }

    fun setActivePane(paneIndex: Int) {
        _activePane.value = paneIndex
        refreshFileSystem()
    }

    // Toggle Settings
    fun toggleExplorerStyle() {
        if (_explorerStyle.value == STYLE_SINGLE_PANE) {
            _explorerStyle.value = STYLE_DUAL_PANE
            _showHiddenFiles.value = true
        } else {
            _explorerStyle.value = STYLE_SINGLE_PANE
            _showHiddenFiles.value = false
        }
        clearSelection()
        refreshFileSystem()
    }

    fun setExplorerStyle(style: String) {
        _explorerStyle.value = style
        if (style == STYLE_DUAL_PANE) {
            _showHiddenFiles.value = true
        } else {
            _showHiddenFiles.value = false
        }
        clearSelection()
        refreshFileSystem()
    }

    fun setDarkThemeMode(mode: String) {
        _darkThemeMode.value = mode
    }

    fun setAccentColorTheme(theme: String) {
        _accentColorTheme.value = theme
    }

    fun toggleShowHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
    }

    // Selection management
    fun toggleSelectItem(item: VirtualItem) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(item)) {
            current.remove(item)
        } else {
            current.add(item)
        }
        _selectedItems.value = current
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    // Real-Time physical and virtual File Operations
    fun createFolder(name: String, parentPath: String) {
        val nameClean = name.trim()
        if (nameClean.isEmpty() || parentPath.contains("::")) return

        RealFileSystem.createFolder(parentPath, nameClean)
        refreshFileSystem()
    }

    fun createFile(name: String, extension: String, sizeText: String, parentPath: String) {
        val nameClean = name.trim()
        if (nameClean.isEmpty() || parentPath.contains("::")) return

        val ext = extension.trim().removePrefix(".")
        val fullName = if (ext.isNotEmpty()) "$nameClean.$ext" else nameClean

        RealFileSystem.createFile(parentPath, fullName)
        refreshFileSystem()
    }

    suspend fun deleteItems(items: List<VirtualItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        items.forEach { item ->
            // Prevent deletion of virtual files inside a zip
            if (!item.path.contains("::")) {
                RealFileSystem.deleteRecursively(item.path)
            }
        }
        clearSelection()
        refreshFileSystem()
    }

    fun startCopyOperation(items: List<VirtualItem>) {
        _clipboardItems.value = items
        _isMoveOperation.value = false
        clearSelection()
    }

    fun startMoveOperation(items: List<VirtualItem>) {
        _clipboardItems.value = items
        _isMoveOperation.value = true
        clearSelection()
    }

    fun clearClipboard() {
        _clipboardItems.value = emptyList()
        _isMoveOperation.value = false
    }

    suspend fun pasteClipboard(targetParentPath: String) = withContext(Dispatchers.IO) {
        val itemsToPaste = _clipboardItems.value
        if (itemsToPaste.isEmpty() || targetParentPath.contains("::")) return@withContext

        itemsToPaste.forEach { sourceItem ->
            if (sourceItem.path.contains("::")) {
                // Source is inside a zip file - extract it into physical target folder!
                val parts = sourceItem.path.split("::")
                val zipFilePath = parts[0]
                val entryPath = parts[1].trim('/')

                if (sourceItem.isDirectory) {
                    extractZipFolder(zipFilePath, entryPath, targetParentPath)
                } else {
                    ZipFileSystem.extractEntry(zipFilePath, entryPath, targetParentPath)
                }
            } else {
                // Standard physical operation
                if (_isMoveOperation.value) {
                    RealFileSystem.move(sourceItem.path, targetParentPath)
                } else {
                    RealFileSystem.copy(sourceItem.path, targetParentPath)
                }
            }
        }

        clearClipboard()
        clearSelection()
        refreshFileSystem()
    }

    // Helper for folder extraction inside zip
    private fun extractZipFolder(zipFilePath: String, entryFolder: String, targetFolder: String) {
        try {
            val archive = File(zipFilePath)
            val outputDir = File(targetFolder, File(entryFolder).name)
            if (!outputDir.exists()) outputDir.mkdirs()

            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                val prefix = if (entryFolder.endsWith("/")) entryFolder else "$entryFolder/"
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith(prefix)) {
                        val relPath = entry.name.substring(prefix.length)
                        val outFile = File(outputDir, relPath)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                java.io.FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Direct Extraction of the selected archive
    suspend fun extractAllArchive(zipFilePath: String, targetFolderPath: String): Boolean = withContext(Dispatchers.IO) {
        val result = ZipFileSystem.extractAll(zipFilePath, targetFolderPath)
        refreshFileSystem()
        result
    }

    // Direct Extraction of the selected archive via Foreground Service
    fun startExtractionService(archivePath: String, targetFolderPath: String) {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.service.ExtractionService::class.java).apply {
            action = com.example.service.ExtractionService.ACTION_START_EXTRACTION
            putExtra(com.example.service.ExtractionService.EXTRA_ARCHIVE_PATH, archivePath)
            putExtra(com.example.service.ExtractionService.EXTRA_TARGET_FOLDER, targetFolderPath)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // Compress item to a zip/7z archive
    suspend fun compressItem(item: VirtualItem, extension: String): Boolean = withContext(Dispatchers.IO) {
        if (item.path.contains("::")) return@withContext false
        val sourceFile = File(item.path)
        if (!sourceFile.exists()) return@withContext false

        val parentDir = sourceFile.parent ?: return@withContext false
        val baseName = if (sourceFile.isDirectory) sourceFile.name else sourceFile.nameWithoutExtension.ifEmpty { sourceFile.name }
        val targetZipPath = "$parentDir/$baseName.$extension"

        val success = ZipFileSystem.zip(item.path, targetZipPath)
        refreshFileSystem()
        success
    }

    // Rename an item
    fun renameItem(item: VirtualItem, newName: String) {
        val newNameClean = newName.trim()
        if (newNameClean.isEmpty() || newNameClean == item.name || item.path.contains("::")) return

        RealFileSystem.rename(item.path, newNameClean)
        clearSelection()
        refreshFileSystem()
    }

    // Clipboard Paste from Left Pane to Right Pane (or vice versa) in Dual Pane Mode
    suspend fun mirrorClipboardToOtherPane() {
        val sourcePane = _activePane.value
        val sourcePath = if (sourcePane == 0) _currentPathLeft.value else _currentPathRight.value
        val targetPath = if (sourcePane == 0) _currentPathRight.value else _currentPathLeft.value

        val localList = loadDirectoryFiles(sourcePath)
        if (localList.isNotEmpty()) {
            _clipboardItems.value = localList
            _isMoveOperation.value = false
            pasteClipboard(targetPath)
        }
    }

    // Search query setters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
