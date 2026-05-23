package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ExtractionManager {
    sealed class ProgressState {
        object Idle : ProgressState()
        data class Running(
            val archiveName: String,
            val extractedCount: Int,
            val totalCount: Int,
            val currentFileName: String,
            val progress: Float // 0.0 to 1.0
        ) : ProgressState()
        data class Success(val archiveName: String) : ProgressState()
        data class Error(val archiveName: String, val message: String) : ProgressState()
    }

    private val _progressState = MutableStateFlow<ProgressState>(ProgressState.Idle)
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()

    fun updateProgress(archiveName: String, extractedCount: Int, totalCount: Int, currentFileName: String) {
        val progressVal = if (totalCount > 0) extractedCount.toFloat() / totalCount.toFloat() else 0f
        _progressState.value = ProgressState.Running(
            archiveName = archiveName,
            extractedCount = extractedCount,
            totalCount = totalCount,
            currentFileName = currentFileName,
            progress = progressVal.coerceIn(0f, 1f)
        )
    }

    fun setSuccess(archiveName: String) {
        _progressState.value = ProgressState.Success(archiveName)
    }

    fun setError(archiveName: String, message: String) {
        _progressState.value = ProgressState.Error(archiveName, message)
    }

    fun reset() {
        _progressState.value = ProgressState.Idle
    }
}
