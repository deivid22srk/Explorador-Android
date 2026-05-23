package com.example.model

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// VirtualItem represents a file or directory item with essential details
data class VirtualItem(
    val path: String,          // e.g. "/storage/emulated/0/Download"
    val name: String,          // e.g. "Download"
    val isDirectory: Boolean,
    val itemDetails: String,   // e.g. "7 itens", "Vazio", "2.4 MB" (displays count for folders, size for files)
    val lastModified: String,  // e.g. "22/04/2026" or "26-05-21 08:18"
    val isHidden: Boolean = false
)

object RealFileSystem {

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val index = if (digitGroups < units.size) digitGroups else units.size - 1
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun listRealFiles(directoryPath: String): List<VirtualItem> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }
        val files = dir.listFiles() ?: return emptyList()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return files.map { file ->
            val formattedDate = try {
                sdf.format(Date(file.lastModified()))
            } catch (e: Exception) {
                "---"
            }
            val details = if (file.isDirectory) {
                val subFiles = file.listFiles()
                val count = subFiles?.size ?: 0
                if (count == 0) "Vazio" else if (count == 1) "1 item" else "$count itens"
            } else {
                formatFileSize(file.length())
            }
            VirtualItem(
                path = file.absolutePath,
                name = file.name,
                isDirectory = file.isDirectory,
                itemDetails = details,
                lastModified = formattedDate,
                isHidden = file.name.startsWith(".")
            )
        }
    }

    fun createFolder(parentPath: String, name: String): Boolean {
        val folder = File(parentPath, name)
        return folder.mkdirs()
    }

    fun createFile(parentPath: String, name: String): Boolean {
        val file = File(parentPath, name)
        return try {
            file.createNewFile()
        } catch (e: Exception) {
            false
        }
    }

    fun deleteRecursively(path: String): Boolean {
        val file = File(path)
        return file.deleteRecursively()
    }

    fun rename(oldPath: String, newName: String): Boolean {
        val sourceFile = File(oldPath)
        if (!sourceFile.exists()) return false
        val destFile = File(sourceFile.parent, newName)
        return sourceFile.renameTo(destFile)
    }

    fun copy(sourcePath: String, destParentPath: String): Boolean {
        val source = File(sourcePath)
        val dest = File(destParentPath, source.name)
        return copyRecursively(source, dest)
    }

    fun move(sourcePath: String, destParentPath: String): Boolean {
        val source = File(sourcePath)
        val dest = File(destParentPath, source.name)
        if (source.renameTo(dest)) return true
        // Fallback copy then delete if renaming across file systems
        if (copyRecursively(source, dest)) {
            source.deleteRecursively()
            return true
        }
        return false
    }

    private fun copyRecursively(source: File, dest: File): Boolean {
        return try {
            if (source.isDirectory) {
                if (!dest.exists()) dest.mkdirs()
                val children = source.list() ?: return true
                for (child in children) {
                    copyRecursively(File(source, child), File(dest, child))
                }
                true
            } else {
                val destParent = dest.parentFile
                if (destParent != null && !destParent.exists()) {
                    destParent.mkdirs()
                }
                FileInputStream(source).use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
