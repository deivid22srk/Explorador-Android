package com.example.model

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ZipFileSystem {

    // Lists the files and subfolders within a specific archive given an inner path prefix
    fun listZipContents(zipFilePath: String, innerPath: String): List<VirtualItem> {
        val zipFile = File(zipFilePath)
        if (!zipFile.exists() || !zipFile.isFile) return emptyList()

        val results = mutableListOf<VirtualItem>()
        val seenDirectories = mutableSetOf<String>()

        // Normalize innerPath so we don't have double slashes
        val normalizedInner = innerPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }

        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name

                    // We are looking for entries that start with our innerPath but are not the path itself
                    if (entryName.startsWith(normalizedInner) && entryName != normalizedInner) {
                        // Extract suffix/relative path
                        val relativePath = entryName.substring(normalizedInner.length)
                        if (relativePath.isEmpty()) continue

                        val firstSlash = relativePath.indexOf('/')
                        if (firstSlash == -1) {
                            // Immediate child file
                            val isDir = entry.isDirectory
                            val name = relativePath
                            val details = if (isDir) "Diretório" else RealFileSystem.formatFileSize(entry.size)
                            val formattedDate = try {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.time))
                            } catch (e: Exception) {
                                "---"
                            }
                            // Form active path within ZIP so navigation works seamlessly: /path/to/file.zip::/inner/path
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
                            // Immediate child directory
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort directories first, then files
        return results.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
    }

    // Extracts a single file entry from the archive to the target folder
    fun extractEntry(zipFilePath: String, entryPathInZip: String, targetFolder: String): Boolean {
        try {
            val archive = File(zipFilePath)
            val outputDir = File(targetFolder)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            ZipFile(archive).use { zip ->
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
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Extracts all zip entries to the destination directory
    fun extractAll(zipFilePath: String, targetFolderPath: String): Boolean {
        try {
            val archive = File(zipFilePath)
            val outputDir = File(targetFolderPath)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
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
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
