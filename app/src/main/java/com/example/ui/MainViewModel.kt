package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.data.model.CompressionLevel
import com.example.data.model.CompressionResult
import com.example.data.model.ImageFilterType
import com.example.data.model.NormPoint
import com.example.data.model.PageEditParams
import com.example.data.model.PdfBuildConfig
import com.example.data.model.WorkspacePage
import com.example.data.repository.PdfRepository
import com.example.engine.DocumentCornerDetector
import com.example.engine.ExtractedPdfPage
import com.example.engine.ImageFilterProcessor
import com.example.engine.PdfCompressor
import com.example.engine.PdfGenerator
import com.example.engine.PdfPageExtractor
import com.example.util.BengaliFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface ProgressUiState {
    data object Idle : ProgressUiState
    data class Running(val current: Int, val total: Int, val messageBn: String) : ProgressUiState
    data class Completed(val documentId: Long, val documentName: String) : ProgressUiState
    data class Failed(val errorBn: String) : ProgressUiState
}

data class CompressionSelection(
    val file: File,
    val displayName: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val sourceDocumentId: Long? = null
)

data class CompressionUiState(
    val selectedPdf: CompressionSelection? = null,
    val level: CompressionLevel = CompressionLevel.MEDIUM,
    val isCompressing: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val result: CompressionResult? = null,
    val errorBn: String? = null
)

data class PdfToImageUiState(
    val sourceName: String = "",
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val pages: List<ExtractedPdfPage> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val errorBn: String? = null
)

data class OrganizerUiState(
    val sourceDocumentId: Long? = null,
    val sourceName: String = "",
    val sourceFile: File? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val pages: List<ExtractedPdfPage> = emptyList(),
    val savedDocumentId: Long? = null,
    val errorBn: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val repository = PdfRepository(appContext)

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<PdfDocumentEntity>> = repository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<PdfDocumentEntity>> = repository.favoriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    val filteredHomeDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        allDocuments,
        _searchQuery,
        _selectedFolderId
    ) { docs, query, folderId ->
        docs.filter { doc ->
            val matchesFolder = folderId == null || doc.folderId == folderId
            val matchesQuery = query.isBlank() || doc.name.contains(query.trim(), ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _workspacePages = MutableStateFlow<List<WorkspacePage>>(emptyList())
    val workspacePages: StateFlow<List<WorkspacePage>> = _workspacePages.asStateFlow()

    private val _pdfBuildConfig = MutableStateFlow(
        PdfBuildConfig(fileName = BengaliFormatters.defaultPdfTitle())
    )
    val pdfBuildConfig: StateFlow<PdfBuildConfig> = _pdfBuildConfig.asStateFlow()

    private val _buildProgress = MutableStateFlow<ProgressUiState>(ProgressUiState.Idle)
    val buildProgress: StateFlow<ProgressUiState> = _buildProgress.asStateFlow()

    private val _compressionState = MutableStateFlow(CompressionUiState())
    val compressionState: StateFlow<CompressionUiState> = _compressionState.asStateFlow()

    private val _pdfToImageState = MutableStateFlow(PdfToImageUiState())
    val pdfToImageState: StateFlow<PdfToImageUiState> = _pdfToImageState.asStateFlow()

    private val _organizerState = MutableStateFlow(OrganizerUiState())
    val organizerState: StateFlow<OrganizerUiState> = _organizerState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var activePdfBuildJob: Job? = null
    private var activeCompressionJob: Job? = null

    fun showToast(messageBn: String) {
        _toastEvents.tryEmit(messageBn)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    // --- Document CRUD Actions ---

    fun markOpened(docId: Long) {
        viewModelScope.launch { repository.markDocumentOpened(docId) }
    }

    fun renameDocument(docId: Long, newName: String) {
        if (newName.isBlank()) {
            showToast("অনুগ্রহ করে একটি বৈধ নাম লিখুন।")
            return
        }
        viewModelScope.launch {
            repository.renameDocument(docId, newName)
            showToast("ফাইলের নাম পরিবর্তন করা হয়েছে।")
        }
    }

    fun toggleFavorite(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            val next = !doc.isFavorite
            repository.setFavorite(doc.id, next)
            showToast(
                if (next) "পছন্দের তালিকায় যুক্ত করা হয়েছে।" else "পছন্দের তালিকা থেকে সরানো হয়েছে।"
            )
        }
    }

    fun moveDocumentToFolder(docId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveToFolder(docId, folderId)
            showToast("ফোল্ডার হালনাগাদ হয়েছে।")
        }
    }

    fun deleteDocument(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            showToast("ফাইলটি স্থায়ীভাবে মুছে ফেলা হয়েছে।")
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) {
            showToast("অনুগ্রহ করে ফোল্ডারের নাম লিখুন।")
            return
        }
        viewModelScope.launch {
            repository.createFolder(name)
            showToast("নতুন ফোল্ডার তৈরি হয়েছে।")
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folderId) _selectedFolderId.value = null
            repository.deleteFolder(folderId)
            showToast("ফোল্ডারটি মুছে ফেলা হয়েছে।")
        }
    }

    // --- Workspace & Scanning ---

    fun clearWorkspace() {
        _workspacePages.value = emptyList()
        _pdfBuildConfig.value = PdfBuildConfig(fileName = BengaliFormatters.defaultPdfTitle())
        _buildProgress.value = ProgressUiState.Idle
    }

    fun addPickedImagesToWorkspace(uris: List<Uri>, onAdded: (firstPageId: String?) -> Unit = {}) {
        if (uris.isEmpty()) {
            showToast("কোনো ছবি নির্বাচন করা হয়নি।")
            onAdded(null)
            return
        }
        viewModelScope.launch {
            val added = mutableListOf<WorkspacePage>()
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    val file = repository.copyPickedImageToWorkspace(uri) ?: continue
                    val id = UUID.randomUUID().toString().take(8)
                    added.add(
                        WorkspacePage(
                            id = id,
                            originalPath = file.absolutePath,
                            previewPath = file.absolutePath
                        )
                    )
                }
            }
            if (added.isEmpty()) {
                showToast("নির্বাচিত ছবিগুলো লোড করা যায়নি।")
                onAdded(null)
            } else {
                _workspacePages.update { it + added }
                onAdded(added.first().id)
            }
        }
    }

    fun addCapturedScanFile(
        imageFile: File,
        detectedCorners: List<NormPoint>?,
        autoPerspectiveCrop: Boolean,
        isFromScan: Boolean,
        onAdded: (WorkspacePage) -> Unit = {}
    ) {
        viewModelScope.launch {
            val page = withContext(Dispatchers.IO) {
                val id = UUID.randomUUID().toString().take(8)
                val corners = if (detectedCorners != null && detectedCorners.size == 4) {
                    detectedCorners
                } else {
                    val bmp = ImageFilterProcessor.decodeOrientedBitmap(imageFile.absolutePath, 640)
                    val res = bmp?.let { DocumentCornerDetector.detectFromBitmap(it) }
                    bmp?.recycle()
                    res?.corners ?: PageEditParams.defaultCorners()
                }
                val params = PageEditParams(
                    filterType = if (isFromScan) ImageFilterType.DOCUMENT_SCAN else ImageFilterType.ORIGINAL,
                    corners = corners,
                    usePerspectiveWarp = autoPerspectiveCrop
                )
                val previewPath = ImageFilterProcessor.saveWorkspacePreview(
                    context = appContext,
                    pageId = id,
                    originalPath = imageFile.absolutePath,
                    params = params
                ) ?: imageFile.absolutePath

                WorkspacePage(
                    id = id,
                    originalPath = imageFile.absolutePath,
                    previewPath = previewPath,
                    editParams = params,
                    isFromScan = isFromScan
                )
            }
            _workspacePages.update { it + page }
            onAdded(page)
        }
    }

    fun removeWorkspacePage(pageId: String) {
        _workspacePages.update { list -> list.filterNot { it.id == pageId } }
    }

    fun moveWorkspacePage(fromIndex: Int, toIndex: Int) {
        _workspacePages.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                list
            } else {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            }
        }
    }

    fun rotateWorkspacePageQuick(pageId: String) {
        val target = _workspacePages.value.find { it.id == pageId } ?: return
        val updatedParams = target.editParams.copy(
            rotationDegrees = (target.editParams.rotationDegrees + 90) % 360
        )
        updateWorkspacePageParams(pageId, updatedParams)
    }

    fun updateWorkspacePageParams(pageId: String, newParams: PageEditParams, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val target = _workspacePages.value.find { it.id == pageId } ?: return@launch
            val previewPath = withContext(Dispatchers.IO) {
                ImageFilterProcessor.saveWorkspacePreview(
                    context = appContext,
                    pageId = pageId,
                    originalPath = target.originalPath,
                    params = newParams
                ) ?: target.previewPath
            }
            _workspacePages.update { list ->
                list.map {
                    if (it.id == pageId) it.copy(editParams = newParams, previewPath = previewPath)
                    else it
                }
            }
            onDone()
        }
    }

    fun updatePdfBuildConfig(transform: (PdfBuildConfig) -> PdfBuildConfig) {
        _pdfBuildConfig.update(transform)
    }

    fun buildPdfFromWorkspace() {
        val pages = _workspacePages.value
        if (pages.isEmpty()) {
            _buildProgress.value = ProgressUiState.Failed("পিডিএফ তৈরির জন্য কমপক্ষে একটি ছবি যোগ করুন।")
            return
        }
        activePdfBuildJob?.cancel()
        activePdfBuildJob = viewModelScope.launch {
            val config = _pdfBuildConfig.value
            val (pdfFile, thumbFile) = repository.createNewPdfTargetPair()
            _buildProgress.value = ProgressUiState.Running(0, pages.size, "পিডিএফ তৈরি হচ্ছে...")

            val result = PdfGenerator.createPdfFromPages(
                context = appContext,
                pages = pages,
                config = config,
                outputPdfFile = pdfFile,
                outputThumbFile = thumbFile,
                onProgress = { cur, tot ->
                    _buildProgress.value = ProgressUiState.Running(
                        cur,
                        tot,
                        "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(cur)} / ${BengaliFormatters.toBengaliDigits(tot)} প্রক্রিয়াকরণ হচ্ছে..."
                    )
                }
            )

            result.fold(
                onSuccess = { info ->
                    val saved = repository.registerPdfDocument(
                        name = config.fileName,
                        info = info,
                        folderId = config.folderId,
                        isCompressed = config.compressOutput
                    )
                    _buildProgress.value = ProgressUiState.Completed(saved.id, saved.name)
                },
                onFailure = { err ->
                    _buildProgress.value = ProgressUiState.Failed(
                        err.message ?: "পিডিএফ তৈরি করতে সমস্যা হয়েছে। আবার চেষ্টা করুন।"
                    )
                }
            )
        }
    }

    fun cancelActivePdfBuild() {
        activePdfBuildJob?.cancel()
        _buildProgress.value = ProgressUiState.Idle
        showToast("পিডিএফ তৈরি বাতিল করা হয়েছে।")
    }

    fun resetBuildProgress() {
        _buildProgress.value = ProgressUiState.Idle
    }

    // --- Compression Flow ---

    fun selectDocumentForCompression(doc: PdfDocumentEntity) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            showToast("ফাইলটি আর ডিভাইসে পাওয়া যাচ্ছে না।")
            return
        }
        _compressionState.value = CompressionUiState(
            selectedPdf = CompressionSelection(
                file = file,
                displayName = doc.name,
                sizeBytes = file.length(),
                pageCount = doc.pageCount,
                sourceDocumentId = doc.id
            )
        )
    }

    fun selectExternalPdfForCompression(uri: Uri) {
        viewModelScope.launch {
            repository.copyExternalPdfToTemp(uri).fold(
                onSuccess = { (file, name) ->
                    val pages = PdfGenerator.inspectPdfPageCount(file)
                    _compressionState.value = CompressionUiState(
                        selectedPdf = CompressionSelection(
                            file = file,
                            displayName = name,
                            sizeBytes = file.length(),
                            pageCount = pages,
                            sourceDocumentId = null
                        )
                    )
                },
                onFailure = { err ->
                    _compressionState.update {
                        it.copy(errorBn = err.message ?: "পিডিএফ ফাইলটি নির্বাচন করা যায়নি।")
                    }
                }
            )
        }
    }

    fun setCompressionLevel(level: CompressionLevel) {
        _compressionState.update { it.copy(level = level, result = null, errorBn = null) }
    }

    fun startCompression() {
        val sel = _compressionState.value.selectedPdf ?: return
        activeCompressionJob?.cancel()
        activeCompressionJob = viewModelScope.launch {
            _compressionState.update {
                it.copy(isCompressing = true, currentPage = 0, totalPages = sel.pageCount, result = null, errorBn = null)
            }
            val (outFile, _) = repository.createNewPdfTargetPair()
            val compResult = PdfCompressor.compressPdf(
                context = appContext,
                sourceFile = sel.file,
                outputFile = outFile,
                level = _compressionState.value.level,
                onProgress = { cur, tot ->
                    _compressionState.update { it.copy(currentPage = cur, totalPages = tot) }
                }
            )
            val finalResult = if (compResult is CompressionResult.Success) {
                val compressedName = "${sel.displayName}_সংকুচিত"
                val savedEntity = repository.registerCompressedPdf(
                    name = compressedName,
                    outputPdfFile = outFile,
                    pageCount = compResult.pageCount
                )
                compResult.copy(
                    outputFileName = savedEntity.name,
                    savedDocumentId = savedEntity.id
                )
            } else {
                compResult
            }
            _compressionState.update { it.copy(isCompressing = false, result = finalResult) }
        }
    }

    fun cancelCompression() {
        activeCompressionJob?.cancel()
        _compressionState.update { it.copy(isCompressing = false) }
        showToast("সংকোচন কাজ বাতিল করা হয়েছে।")
    }

    fun resetCompression() {
        _compressionState.value = CompressionUiState()
    }

    // --- PDF to Image Flow ---

    fun loadPdfForImageExport(doc: PdfDocumentEntity) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            showToast("পিডিএফ ফাইলটি পাওয়া যায়নি।")
            return
        }
        extractPdfForImages(file, doc.name)
    }

    fun loadExternalPdfForImageExport(uri: Uri) {
        viewModelScope.launch {
            repository.copyExternalPdfToTemp(uri).fold(
                onSuccess = { (file, name) -> extractPdfForImages(file, name) },
                onFailure = { err ->
                    _pdfToImageState.value = PdfToImageUiState(
                        errorBn = err.message ?: "পিডিএফ ফাইলটি লোড করা যায়নি।"
                    )
                }
            )
        }
    }

    private fun extractPdfForImages(file: File, name: String) {
        viewModelScope.launch {
            _pdfToImageState.value = PdfToImageUiState(sourceName = name, isLoading = true)
            PdfPageExtractor.extractPagesToTempFiles(
                context = appContext,
                pdfFile = file,
                scale = 2.0f,
                onProgress = { cur, tot ->
                    _pdfToImageState.update { it.copy(currentPage = cur, totalPages = tot) }
                }
            ).fold(
                onSuccess = { pages ->
                    _pdfToImageState.value = PdfToImageUiState(
                        sourceName = name,
                        isLoading = false,
                        pages = pages,
                        selectedIndices = pages.indices.toSet()
                    )
                },
                onFailure = { err ->
                    _pdfToImageState.value = PdfToImageUiState(
                        sourceName = name,
                        isLoading = false,
                        errorBn = err.message ?: "পিডিএফ থেকে ছবি তৈরি করা যায়নি।"
                    )
                }
            )
        }
    }

    fun togglePdfToImageSelection(index: Int) {
        _pdfToImageState.update { state ->
            val next = if (index in state.selectedIndices) {
                state.selectedIndices - index
            } else {
                state.selectedIndices + index
            }
            state.copy(selectedIndices = next)
        }
    }

    fun selectAllPdfToImage(selectAll: Boolean) {
        _pdfToImageState.update { state ->
            state.copy(selectedIndices = if (selectAll) state.pages.indices.toSet() else emptySet())
        }
    }

    // --- Page Organizer Flow ---

    fun loadDocumentForOrganizer(doc: PdfDocumentEntity) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            showToast("পিডিএফ ফাইলটি পাওয়া যায়নি।")
            return
        }
        viewModelScope.launch {
            _organizerState.value = OrganizerUiState(
                sourceDocumentId = doc.id,
                sourceName = doc.name,
                sourceFile = file,
                isLoading = true
            )
            PdfPageExtractor.extractPagesToTempFiles(
                context = appContext,
                pdfFile = file,
                scale = 2.0f,
                onProgress = { cur, tot ->
                    _organizerState.update { it.copy(progressCurrent = cur, progressTotal = tot) }
                }
            ).fold(
                onSuccess = { pages ->
                    _organizerState.update { it.copy(isLoading = false, pages = pages) }
                },
                onFailure = { err ->
                    _organizerState.update {
                        it.copy(isLoading = false, errorBn = err.message ?: "পৃষ্ঠাগুলো পড়া যায়নি।")
                    }
                }
            )
        }
    }

    fun moveOrganizerPage(fromIndex: Int, toIndex: Int) {
        _organizerState.update { state ->
            if (fromIndex !in state.pages.indices || toIndex !in state.pages.indices || fromIndex == toIndex) {
                state
            } else {
                val mutable = state.pages.toMutableList()
                val moved = mutable.removeAt(fromIndex)
                mutable.add(toIndex, moved)
                state.copy(pages = mutable)
            }
        }
    }

    fun rotateOrganizerPage(index: Int) {
        _organizerState.update { state ->
            val mutable = state.pages.toMutableList()
            if (index in mutable.indices) {
                val item = mutable[index]
                mutable[index] = item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
            }
            state.copy(pages = mutable)
        }
    }

    fun deleteOrganizerPage(index: Int) {
        _organizerState.update { state ->
            if (state.pages.size <= 1) {
                showToast("কমপক্ষে একটি পৃষ্ঠা রাখতে হবে।")
                return@update state
            }
            val mutable = state.pages.toMutableList()
            if (index in mutable.indices) mutable.removeAt(index)
            state.copy(pages = mutable)
        }
    }

    fun saveOrganizedPdf(saveAsCopy: Boolean) {
        val state = _organizerState.value
        if (state.pages.isEmpty()) return
        viewModelScope.launch {
            _organizerState.update { it.copy(isSaving = true, errorBn = null) }
            val (outPdf, outThumb) = repository.createNewPdfTargetPair()
            PdfPageExtractor.rebuildPdfFromExtractedPages(
                pages = state.pages,
                outputPdfFile = outPdf,
                outputThumbFile = outThumb,
                onProgress = { cur, tot ->
                    _organizerState.update { it.copy(progressCurrent = cur, progressTotal = tot) }
                }
            ).fold(
                onSuccess = { info ->
                    val targetId = if (!saveAsCopy && state.sourceDocumentId != null) {
                        repository.updateExistingPdfMetadata(state.sourceDocumentId, info)
                        state.sourceDocumentId
                    } else {
                        val saved = repository.registerPdfDocument(
                            name = "${state.sourceName}_সাজানো",
                            info = info
                        )
                        saved.id
                    }
                    _organizerState.update { it.copy(isSaving = false, savedDocumentId = targetId) }
                    showToast("সাজানো পিডিএফ সংরক্ষণ করা হয়েছে।")
                },
                onFailure = { err ->
                    _organizerState.update {
                        it.copy(isSaving = false, errorBn = err.message ?: "পিডিএফ সংরক্ষণ করতে সমস্যা হয়েছে।")
                    }
                }
            )
        }
    }

    fun resetOrganizer() {
        _organizerState.value = OrganizerUiState()
    }
}
