package com.example.model

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader

typealias ProgressCallback = (extractedCount: Int, totalCount: Int, currentFileName: String) -> Unit

data class ArchiveEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val time: Long
)

object ZipFileSystem {

    // Lists the files and subfolders within a specific archive given an inner path prefix (supports .zip, .7z, .rar)
    fun listZipContents(zipFilePath: String, innerPath: String): List<VirtualItem> {
        val archiveFile = File(zipFilePath)
        if (!archiveFile.exists() || !archiveFile.isFile) return emptyList()

        val entriesList = mutableListOf<ArchiveEntry>()
        val ext = zipFilePath.lowercase(Locale.getDefault())

        try {
            if (ext.endsWith(".zip")) {
                ZipFile(archiveFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        entriesList.add(ArchiveEntry(
                            name = entry.name.replace('\\', '/'),
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            time = entry.time
                        ))
                    }
                }
            } else if (ext.endsWith(".7z")) {
                SevenZFile(archiveFile).use { s7 ->
                    var entry = s7.nextEntry
                    while (entry != null) {
                        entriesList.add(ArchiveEntry(
                            name = entry.name.replace('\\', '/'),
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            time = entry.lastModifiedDate?.time ?: 0L
                        ))
                        entry = s7.nextEntry
                    }
                }
            } else if (ext.endsWith(".rar")) {
                Archive(archiveFile).use { rar ->
                    val headers = rar.fileHeaders
                    for (fh in headers) {
                        entriesList.add(ArchiveEntry(
                            name = fh.fileName.replace('\\', '/'),
                            isDirectory = fh.isDirectory,
                            size = fh.fullUnpackSize,
                            time = fh.mTime?.time ?: 0L
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        val results = mutableListOf<VirtualItem>()
        val seenDirectories = mutableSetOf<String>()
        val normalizedInner = innerPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }

        for (entry in entriesList) {
            val entryName = entry.name
            if (entryName.startsWith(normalizedInner) && entryName != normalizedInner) {
                val relativePath = entryName.substring(normalizedInner.length)
                if (relativePath.isEmpty()) continue

                val firstSlash = relativePath.indexOf('/')
                if (firstSlash == -1) {
                    val isDir = entry.isDirectory
                    val name = relativePath
                    val details = if (isDir) "Diretório" else RealFileSystem.formatFileSize(entry.size)
                    val formattedDate = try {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.time))
                    } catch (e: Exception) {
                        "---"
                    }
                    val itemPath = "$zipFilePath::$normalizedInner$name"

                    results.add(VirtualItem(
                        path = itemPath,
                        name = name,
                        isDirectory = isDir,
                        itemDetails = details,
                        lastModified = formattedDate,
                        isHidden = name.startsWith(".")
                    ))
                } else {
                    val dirName = relativePath.substring(0, firstSlash)
                    if (!seenDirectories.contains(dirName)) {
                        seenDirectories.add(dirName)
                        val itemPath = "$zipFilePath::$normalizedInner$dirName"
                        results.add(VirtualItem(
                            path = itemPath,
                            name = dirName,
                            isDirectory = true,
                            itemDetails = "Diretório",
                            lastModified = try {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.time))
                            } catch (e: Exception) {
                                "---"
                            },
                            isHidden = dirName.startsWith(".")
                        ))
                    }
                }
            }
        }

        return results.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
    }

    // Extracts a single file entry from the archive to the target folder (supports .zip, .7z, .rar)
    fun extractEntry(zipFilePath: String, entryPathInZip: String, targetFolder: String): Boolean {
        try {
            val archiveFile = File(zipFilePath)
            val outputDir = File(targetFolder)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val ext = zipFilePath.lowercase(Locale.getDefault())

            if (ext.endsWith(".zip")) {
                ZipFile(archiveFile).use { zip ->
                    val entry = zip.getEntry(entryPathInZip) ?: return false
                    val fileName = File(entry.name).name
                    val outFile = File(outputDir, fileName)
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                return true
            } else if (ext.endsWith(".7z")) {
                SevenZFile(archiveFile).use { s7 ->
                    var entry = s7.nextEntry
                    while (entry != null) {
                        if (entry.name == entryPathInZip) {
                            val fileName = File(entry.name).name
                            val outFile = File(outputDir, fileName)
                            FileOutputStream(outFile).use { output ->
                                val buffer = ByteArray(8192)
                                var readBy: Int
                                while (s7.read(buffer).also { readBy = it } != -1) {
                                    output.write(buffer, 0, readBy)
                                }
                            }
                            return true
                        }
                        entry = s7.nextEntry
                    }
                }
                return false
            } else if (ext.endsWith(".rar")) {
                Archive(archiveFile).use { rar ->
                    val headers = rar.fileHeaders
                    for (fh in headers) {
                        if (fh.fileName == entryPathInZip || fh.fileName.replace('\\', '/') == entryPathInZip) {
                            val fileName = File(fh.fileName).name
                            val outFile = File(outputDir, fileName)
                            FileOutputStream(outFile).use { output ->
                                rar.extractFile(fh, output)
                            }
                            return true
                        }
                    }
                }
                return false
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Extracts all entries to the destination directory
    fun extractAll(zipFilePath: String, targetFolderPath: String): Boolean {
        return extractAllWithProgress(zipFilePath, targetFolderPath) { _, _, _ -> }
    }

    // Extracts all entries to the destination directory with a real-time progress callback (supports .zip, .7z, .rar)
    fun extractAllWithProgress(zipFilePath: String, targetFolderPath: String, progressUpdate: ProgressCallback): Boolean {
        val ext = zipFilePath.lowercase(Locale.getDefault())
        try {
            val archiveFile = File(zipFilePath)
            val outputDir = File(targetFolderPath)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            if (ext.endsWith(".zip")) {
                ZipFile(archiveFile).use { zip ->
                    val entriesList = mutableListOf<ZipEntry>()
                    val entriesEnum = zip.entries()
                    while (entriesEnum.hasMoreElements()) {
                        entriesList.add(entriesEnum.nextElement())
                    }
                    val totalEntries = entriesList.size
                    var extractedCount = 0

                    for (entry in entriesList) {
                        val outFile = File(outputDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        extractedCount++
                        progressUpdate(extractedCount, totalEntries, entry.name)
                    }
                }
                return true
            } else if (ext.endsWith(".7z")) {
                // Step 1: Count entries
                val entryNames = mutableListOf<String>()
                SevenZFile(archiveFile).use { s7 ->
                    var entry = s7.nextEntry
                    while (entry != null) {
                        entryNames.add(entry.name ?: "")
                        entry = s7.nextEntry
                    }
                }
                val totalEntries = entryNames.size
                if (totalEntries == 0) return true

                // Step 2: Extract with progress reporting
                var extractedCount = 0
                SevenZFile(archiveFile).use { s7 ->
                    var entry = s7.nextEntry
                    while (entry != null) {
                        val entryName = entry.name ?: ""
                        val outFile = File(outputDir, entryName)

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { output ->
                                val buffer = ByteArray(8192)
                                var readBytes: Int
                                while (s7.read(buffer).also { readBytes = it } != -1) {
                                    output.write(buffer, 0, readBytes)
                                }
                            }
                        }
                        extractedCount++
                        progressUpdate(extractedCount, totalEntries, entryName)
                        entry = s7.nextEntry
                    }
                }
                return true
            } else if (ext.endsWith(".rar")) {
                Archive(archiveFile).use { rar ->
                    val fileHeaders = rar.fileHeaders ?: return false
                    val totalEntries = fileHeaders.size
                    var extractedCount = 0

                    for (fh in fileHeaders) {
                        val entryName = fh.fileName.replace('\\', '/')
                        val outFile = File(outputDir, entryName)

                        if (fh.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { output ->
                                rar.extractFile(fh, output)
                            }
                        }
                        extractedCount++
                        progressUpdate(extractedCount, totalEntries, entryName)
                    }
                }
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Compresses a file or folder into a ZIP file (legacy compatibility)
    fun zip(sourcePath: String, zipFilePath: String): Boolean {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return false

            val zipFile = File(zipFilePath)
            zipFile.parentFile?.mkdirs()

            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                if (sourceFile.isDirectory) {
                    zipDirectory(sourceFile, sourceFile, zos)
                } else {
                    zipFile(sourceFile, sourceFile.parentFile, zos)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun zipDirectory(rootDirectory: File, currentDirectory: File, zos: java.util.zip.ZipOutputStream) {
        val files = currentDirectory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                val entryName = file.relativeTo(rootDirectory).path.replace('\\', '/') + "/"
                zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                zos.closeEntry()
                zipDirectory(rootDirectory, file, zos)
            } else {
                zipFile(file, rootDirectory, zos)
            }
        }
    }

    private fun zipFile(file: File, baseDir: File?, zos: java.util.zip.ZipOutputStream) {
        val entryName = if (baseDir != null) {
            file.relativeTo(baseDir).path.replace('\\', '/')
        } else {
            file.name
        }
        val entry = java.util.zip.ZipEntry(entryName)
        zos.putNextEntry(entry)
        java.io.FileInputStream(file).use { input ->
            input.copyTo(zos)
        }
        zos.closeEntry()
    }
}
