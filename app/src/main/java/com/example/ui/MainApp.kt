package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.CreateActionBottomSheet
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.CreateSheetAction
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.components.RenameFolderDialog
import com.example.ui.components.SelectPdfDocumentDialog
import com.example.ui.screens.DocumentScannerScreen
import com.example.ui.screens.FavoritesTab
import com.example.ui.screens.HomeTab
import com.example.ui.screens.ImageEditorScreen
import com.example.ui.screens.PageOrganizerScreen
import com.example.ui.screens.PdfCompressScreen
import com.example.ui.screens.PdfSettingsScreen
import com.example.ui.screens.PdfToImageScreen
import com.example.ui.screens.PdfToLongImageScreen
import com.example.ui.screens.PdfToWordScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.RecentTab
import com.example.ui.screens.TextToPdfScreen
import com.example.ui.screens.ToolAction
import com.example.ui.screens.ToolsTab
import com.example.ui.screens.WordToPdfScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.LightBlueHeader
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.WhiteBackground
import com.example.util.FileActionsHelper
import java.io.File

private object AppRoutes {
    const val MAIN_TABS = "main_tabs"
    const val WORKSPACE = "workspace"
    const val SCANNER = "scanner/{scanMode}"
    const val IMAGE_EDITOR = "image_editor/{pageId}/{initialTab}/{standalone}"
    const val PDF_SETTINGS = "pdf_settings"
    const val PDF_VIEWER = "pdf_viewer/{docId}"
    const val PDF_COMPRESS = "pdf_compress"
    const val PDF_TO_IMAGE = "pdf_to_image"
    const val PAGE_ORGANIZER = "page_organizer"
    const val TEXT_TO_PDF = "text_to_pdf"
    const val WORD_TO_PDF = "word_to_pdf"
    const val PDF_TO_WORD = "pdf_to_word"
    const val PDF_TO_LONG_IMAGE = "pdf_to_long_image"

    fun scanner(scanMode: Boolean) = "scanner/$scanMode"
    fun imageEditor(pageId: String, initialTab: Int = 0, standalone: Boolean = false) =
        "image_editor/$pageId/$initialTab/$standalone"
    fun pdfViewer(docId: Long) = "pdf_viewer/$docId"
}

private enum class MainTab(
    val titleBn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("হোম", Icons.Filled.Home, Icons.Outlined.Home),
    RECENT("সাম্প্রতিক", Icons.Filled.History, Icons.Outlined.History),
    FAVORITES("পছন্দের তালিকা", Icons.Filled.Star, Icons.Outlined.StarOutline),
    TOOLS("অন্যান্য সুবিধা", Icons.Filled.GridView, Icons.Outlined.GridView)
}

private enum class PendingPdfPickPurpose {
    COMPRESS,
    PDF_TO_IMAGE,
    ORGANIZE,
    RENAME,
    SHARE,
    DELETE,
    PRINT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val filteredHomeDocs by viewModel.filteredHomeDocuments.collectAsStateWithLifecycle()
    val recentDocs by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val favoriteDocs by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val workspacePages by viewModel.workspacePages.collectAsStateWithLifecycle()
    val pdfBuildConfig by viewModel.pdfBuildConfig.collectAsStateWithLifecycle()
    val buildProgress by viewModel.buildProgress.collectAsStateWithLifecycle()
    val compressionState by viewModel.compressionState.collectAsStateWithLifecycle()
    val pdfToImageState by viewModel.pdfToImageState.collectAsStateWithLifecycle()
    val organizerState by viewModel.organizerState.collectAsStateWithLifecycle()
    val textToPdfState by viewModel.textToPdfState.collectAsStateWithLifecycle()
    val wordToPdfState by viewModel.wordToPdfState.collectAsStateWithLifecycle()
    val pdfToWordState by viewModel.pdfToWordState.collectAsStateWithLifecycle()
    val pdfToLongImageState by viewModel.pdfToLongImageState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var showCreateSheet by remember { mutableStateOf(false) }

    // Global Dialog States
    var renameTarget by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var infoTarget by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameFolderTarget by remember { mutableStateOf<FolderEntity?>(null) }
    var pendingPickPurpose by remember { mutableStateOf<PendingPdfPickPurpose?>(null) }

    // Pending single-image editor target tab
    var pendingSingleEditTab by remember { mutableIntStateOf(0) }
    var pendingFileToExport by remember { mutableStateOf<File?>(null) }
    var pendingDocxToExport by remember { mutableStateOf<File?>(null) }

    // Activity Result Launchers (Zero Broad Storage Permissions!)
    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isEmpty()) {
            viewModel.showToast("কোনো ছবি নির্বাচন করা হয়নি।")
        } else {
            viewModel.addPickedImagesToWorkspace(uris) {
                if (navController.currentDestination?.route != AppRoutes.WORKSPACE) {
                    navController.navigate(AppRoutes.WORKSPACE)
                }
            }
        }
    }

    val singleImageForEditPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            viewModel.showToast("কোনো ছবি নির্বাচন করা হয়নি।")
        } else {
            viewModel.clearWorkspace()
            viewModel.addPickedImagesToWorkspace(listOf(uri)) { firstId ->
                if (firstId != null) {
                    navController.navigate(
                        AppRoutes.imageEditor(
                            pageId = firstId,
                            initialTab = pendingSingleEditTab,
                            standalone = true
                        )
                    )
                }
            }
        }
    }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPickedFiles(
                uris = uris,
                onOpenWorkspace = { navController.navigate(AppRoutes.WORKSPACE) },
                onOpenWordToPdf = { navController.navigate(AppRoutes.WORD_TO_PDF) },
                onOpenTextToPdf = { navController.navigate(AppRoutes.TEXT_TO_PDF) }
            )
        }
    }

    val importTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importTextFileToEditor(uri)
    }

    val pickWordFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.loadWordUri(uri)
    }

    val externalPdfForWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.selectExternalPdfForWord(uri)
    }

    val externalPdfForLongImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.loadExternalPdfForLongImage(uri)
    }

    val externalPdfForOrganizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadExternalPdfForOrganizer(uri)
            if (navController.currentDestination?.route != AppRoutes.PAGE_ORGANIZER) {
                navController.navigate(AppRoutes.PAGE_ORGANIZER)
            }
        }
    }

    val externalPdfForCompressLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectExternalPdfForCompression(uri)
        }
    }

    val externalPdfForImageExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadExternalPdfForImageExport(uri)
        }
    }

    val savePdfCopyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        val src = pendingFileToExport
        pendingFileToExport = null
        if (destUri != null && src != null && src.exists()) {
            val ok = FileActionsHelper.copyFileToUri(context, src, destUri)
            viewModel.showToast(
                if (ok) "নির্বাচিত ফোল্ডারে সংরক্ষণ করা হয়েছে।" else "ফাইল সংরক্ষণ করতে সমস্যা হয়েছে।"
            )
        }
    }

    val saveDocxCopyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { destUri: Uri? ->
        val src = pendingDocxToExport
        pendingDocxToExport = null
        if (destUri != null && src != null && src.exists()) {
            val ok = FileActionsHelper.copyFileToUri(context, src, destUri)
            viewModel.showToast(
                if (ok) "ওয়ার্ড ফাইলটি ফোল্ডারে সংরক্ষণ করা হয়েছে।" else "ফাইল সংরক্ষণ করতে সমস্যা হয়েছে।"
            )
        }
    }

    fun openDocumentViewer(doc: PdfDocumentEntity) {
        viewModel.markOpened(doc.id)
        navController.navigate(AppRoutes.pdfViewer(doc.id))
    }

    fun shareDocument(doc: PdfDocumentEntity) {
        FileActionsHelper.sharePdf(context, File(doc.filePath), doc.name).onFailure {
            viewModel.showToast("ফাইলটি শেয়ার করা যায়নি।")
        }
    }

    fun printDocument(doc: PdfDocumentEntity) {
        FileActionsHelper.printPdf(context, File(doc.filePath), doc.name).onFailure {
            viewModel.showToast("প্রিন্ট সেবা চালু করা যায়নি।")
        }
    }

    fun startFreshGalleryToPdf() {
        viewModel.clearWorkspace()
        multiImagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun startFreshScanner(scanMode: Boolean) {
        viewModel.clearWorkspace()
        navController.navigate(AppRoutes.scanner(scanMode))
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.MAIN_TABS
    ) {
        composable(AppRoutes.MAIN_TABS) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = WhiteBackground,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    floatingActionButton = {
                        if (selectedTab != MainTab.TOOLS) {
                            ExtendedFloatingActionButton(
                                onClick = { showCreateSheet = true },
                                containerColor = RoyalBlue,
                                contentColor = WhiteBackground,
                                shape = RoundedCornerShape(16.dp),
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text("তৈরি করুন", fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .then(if (useRail) Modifier.navigationBarsPadding() else Modifier)
                                    .testTag("main_create_fab")
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = WhiteBackground,
                                tonalElevation = 4.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.titleBn
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.titleBn,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = RoyalBlue,
                                            selectedTextColor = RoyalBlue,
                                            indicatorColor = LightBlueHeader,
                                            unselectedIconColor = MutedInk,
                                            unselectedTextColor = MutedInk
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = LightBlueTint,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(Modifier.height(16.dp))
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.titleBn
                                            )
                                        },
                                        label = { Text(tab.titleBn) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = RoyalBlue,
                                            selectedTextColor = RoyalBlue,
                                            indicatorColor = LightBlueHeader
                                        )
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (selectedTab) {
                                MainTab.HOME -> HomeTab(
                                    documents = filteredHomeDocs,
                                    folders = folders,
                                    selectedFolderId = selectedFolderId,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSelectFolder = viewModel::setSelectedFolder,
                                    onCreateFolderClick = { showCreateFolderDialog = true },
                                    onRenameFolderClick = { folder -> renameFolderTarget = folder },
                                    onDeleteFolderClick = { folder -> viewModel.deleteFolder(folder.id) },
                                    onImageToPdfClick = { startFreshGalleryToPdf() },
                                    onScanDocumentClick = { startFreshScanner(scanMode = true) },
                                    onOpenPdf = ::openDocumentViewer,
                                    onRenamePdf = { renameTarget = it },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSharePdf = ::shareDocument,
                                    onViewInfo = { infoTarget = it },
                                    onDeletePdf = { deleteTarget = it }
                                )

                                MainTab.RECENT -> RecentTab(
                                    documents = recentDocs,
                                    onOpenPdf = ::openDocumentViewer,
                                    onRenamePdf = { renameTarget = it },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSharePdf = ::shareDocument,
                                    onViewInfo = { infoTarget = it },
                                    onDeletePdf = { deleteTarget = it },
                                    onCreateNewClick = { showCreateSheet = true }
                                )

                                MainTab.FAVORITES -> FavoritesTab(
                                    documents = favoriteDocs,
                                    onOpenPdf = ::openDocumentViewer,
                                    onRenamePdf = { renameTarget = it },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSharePdf = ::shareDocument,
                                    onViewInfo = { infoTarget = it },
                                    onDeletePdf = { deleteTarget = it }
                                )

                                MainTab.TOOLS -> ToolsTab(
                                    onToolClick = { action ->
                                        when (action) {
                                            ToolAction.IMAGE_TO_PDF,
                                            ToolAction.MULTI_IMAGE_TO_PDF -> startFreshGalleryToPdf()
                                            ToolAction.CAMERA_TO_PDF -> startFreshScanner(scanMode = false)
                                            ToolAction.DOCUMENT_SCAN -> startFreshScanner(scanMode = true)
                                            ToolAction.TEXT_TO_PDF -> {
                                                viewModel.resetTextToPdf()
                                                navController.navigate(AppRoutes.TEXT_TO_PDF)
                                            }
                                            ToolAction.WORD_TO_PDF -> {
                                                viewModel.resetWordToPdf()
                                                navController.navigate(AppRoutes.WORD_TO_PDF)
                                            }
                                            ToolAction.PDF_TO_WORD -> {
                                                viewModel.resetPdfToWord()
                                                navController.navigate(AppRoutes.PDF_TO_WORD)
                                            }
                                            ToolAction.PDF_TO_IMAGE -> navController.navigate(AppRoutes.PDF_TO_IMAGE)
                                            ToolAction.PDF_TO_LONG_IMAGE -> {
                                                viewModel.resetPdfToLongImage()
                                                navController.navigate(AppRoutes.PDF_TO_LONG_IMAGE)
                                            }
                                            ToolAction.COMPRESS_PDF -> {
                                                viewModel.resetCompression()
                                                navController.navigate(AppRoutes.PDF_COMPRESS)
                                            }
                                            ToolAction.EDIT_IMAGE -> {
                                                pendingSingleEditTab = 0
                                                singleImageForEditPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolAction.FILTER_IMAGE -> {
                                                pendingSingleEditTab = 0
                                                singleImageForEditPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolAction.CROP_IMAGE -> {
                                                pendingSingleEditTab = 1
                                                singleImageForEditPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolAction.ORGANIZE_PAGES -> {
                                                viewModel.resetOrganizer()
                                                pendingPickPurpose = PendingPdfPickPurpose.ORGANIZE
                                            }
                                            ToolAction.IMPORT_FILES -> {
                                                importFilesLauncher.launch(
                                                    arrayOf(
                                                        "application/pdf",
                                                        "image/*",
                                                        "text/plain",
                                                        "application/msword",
                                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                                    )
                                                )
                                            }
                                            ToolAction.RENAME_PDF -> pendingPickPurpose = PendingPdfPickPurpose.RENAME
                                            ToolAction.SHARE_PDF -> pendingPickPurpose = PendingPdfPickPurpose.SHARE
                                            ToolAction.DELETE_PDF -> pendingPickPurpose = PendingPdfPickPurpose.DELETE
                                            ToolAction.CREATE_FOLDER -> showCreateFolderDialog = true
                                            ToolAction.PRINT_PDF -> pendingPickPurpose = PendingPdfPickPurpose.PRINT
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        composable(AppRoutes.WORKSPACE) {
            WorkspaceScreen(
                pages = workspacePages,
                onBack = { navController.popBackStack() },
                onAddFromGallery = {
                    multiImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onAddFromCamera = { navController.navigate(AppRoutes.scanner(false)) },
                onAddFromScanner = { navController.navigate(AppRoutes.scanner(true)) },
                onEditPage = { pageId ->
                    navController.navigate(AppRoutes.imageEditor(pageId = pageId, initialTab = 0, standalone = false))
                },
                onRotatePage = viewModel::rotateWorkspacePageQuick,
                onDeletePage = viewModel::removeWorkspacePage,
                onMovePage = viewModel::moveWorkspacePage,
                onProceedToSettings = {
                    viewModel.resetBuildProgress()
                    navController.navigate(AppRoutes.PDF_SETTINGS)
                }
            )
        }

        composable(
            route = AppRoutes.SCANNER,
            arguments = listOf(navArgument("scanMode") { type = NavType.BoolType })
        ) { backStackEntry ->
            val scanMode = backStackEntry.arguments?.getBoolean("scanMode") ?: true
            DocumentScannerScreen(
                isDocumentScanMode = scanMode,
                capturedPages = workspacePages,
                onCaptureSaved = { file, corners, autoCrop, isScan, openEditorAfter ->
                    viewModel.addCapturedScanFile(
                        imageFile = file,
                        detectedCorners = corners,
                        autoPerspectiveCrop = autoCrop,
                        isFromScan = isScan
                    ) { added ->
                        if (openEditorAfter) {
                            navController.navigate(
                                AppRoutes.imageEditor(
                                    pageId = added.id,
                                    initialTab = 1,
                                    standalone = false
                                )
                            )
                        }
                    }
                },
                onRetakeLast = {
                    workspacePages.lastOrNull()?.let { viewModel.removeWorkspacePage(it.id) }
                },
                onPickFromGallery = { uris ->
                    viewModel.addPickedImagesToWorkspace(uris) {
                        navController.navigate(AppRoutes.WORKSPACE) {
                            popUpTo(AppRoutes.MAIN_TABS)
                        }
                    }
                },
                onEditLastPage = { pageId ->
                    navController.navigate(AppRoutes.imageEditor(pageId = pageId, initialTab = 1, standalone = false))
                },
                onFinishScanning = {
                    navController.navigate(AppRoutes.WORKSPACE) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.IMAGE_EDITOR,
            arguments = listOf(
                navArgument("pageId") { type = NavType.StringType },
                navArgument("initialTab") { type = NavType.IntType },
                navArgument("standalone") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            val standalone = backStackEntry.arguments?.getBoolean("standalone") ?: false
            val page = workspacePages.find { it.id == pageId }

            if (page == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ImageEditorScreen(
                    page = page,
                    initialTabIndex = initialTab,
                    showSaveImageToGalleryButton = standalone,
                    onSaveParams = { newParams ->
                        viewModel.updateWorkspacePageParams(pageId, newParams) {
                            if (standalone) {
                                navController.navigate(AppRoutes.WORKSPACE) {
                                    popUpTo(AppRoutes.MAIN_TABS)
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(AppRoutes.PDF_SETTINGS) {
            PdfSettingsScreen(
                pageCount = workspacePages.size,
                config = pdfBuildConfig,
                folders = folders,
                progressState = buildProgress,
                onUpdateConfig = viewModel::updatePdfBuildConfig,
                onBuildPdf = viewModel::buildPdfFromWorkspace,
                onCancelBuild = viewModel::cancelActivePdfBuild,
                onResetProgress = viewModel::resetBuildProgress,
                onOpenGeneratedPdf = { docId ->
                    viewModel.resetBuildProgress()
                    viewModel.markOpened(docId)
                    navController.navigate(AppRoutes.pdfViewer(docId)) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onShareGeneratedPdf = { docId ->
                    allDocs.find { it.id == docId }?.let(::shareDocument)
                },
                onBackToHome = {
                    viewModel.resetBuildProgress()
                    viewModel.clearWorkspace()
                    selectedTab = MainTab.HOME
                    navController.popBackStack(AppRoutes.MAIN_TABS, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.PDF_VIEWER,
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: -1L
            val doc = allDocs.find { it.id == docId }

            if (doc != null) {
                PdfViewerScreen(
                    document = doc,
                    onBack = { navController.popBackStack() },
                    onShare = { shareDocument(doc) },
                    onPrint = { printDocument(doc) },
                    onViewInfo = { infoTarget = doc },
                    onOrganizePages = {
                        viewModel.loadDocumentForOrganizer(doc)
                        navController.navigate(AppRoutes.PAGE_ORGANIZER)
                    }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(AppRoutes.PDF_COMPRESS) {
            PdfCompressScreen(
                uiState = compressionState,
                appDocuments = allDocs,
                onPickFromMobile = { externalPdfForCompressLauncher.launch(arrayOf("application/pdf")) },
                onSelectAppDocument = viewModel::selectDocumentForCompression,
                onSelectLevel = viewModel::setCompressionLevel,
                onStartCompression = viewModel::startCompression,
                onCancelCompression = viewModel::cancelCompression,
                onOpenCompressedPdf = { docId ->
                    viewModel.markOpened(docId)
                    navController.navigate(AppRoutes.pdfViewer(docId)) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onSaveCompressedCopy = { file, name ->
                    pendingFileToExport = file
                    savePdfCopyLauncher.launch("$name.pdf")
                },
                onShareCompressedFile = { file, name ->
                    FileActionsHelper.sharePdf(context, file, name)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_TO_IMAGE) {
            PdfToImageScreen(
                uiState = pdfToImageState,
                appDocuments = allDocs,
                onPickFromMobile = { externalPdfForImageExportLauncher.launch(arrayOf("application/pdf")) },
                onSelectAppDocument = viewModel::loadPdfForImageExport,
                onToggleSelect = viewModel::togglePdfToImageSelection,
                onSelectAll = viewModel::selectAllPdfToImage,
                onSelectFormat = viewModel::setPdfToImageFormat,
                onSelectQuality = viewModel::setPdfToImageQuality,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PAGE_ORGANIZER) {
            PageOrganizerScreen(
                uiState = organizerState,
                appDocuments = allDocs,
                onPickFromMobile = { externalPdfForOrganizerLauncher.launch(arrayOf("application/pdf")) },
                onSelectAppDocument = viewModel::loadDocumentForOrganizer,
                onMovePage = viewModel::moveOrganizerPage,
                onRotatePage = viewModel::rotateOrganizerPage,
                onDeletePage = viewModel::deleteOrganizerPage,
                onSaveOrganized = viewModel::saveOrganizedPdf,
                onOpenSavedPdf = { docId ->
                    viewModel.resetOrganizer()
                    navController.navigate(AppRoutes.pdfViewer(docId)) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.TEXT_TO_PDF) {
            TextToPdfScreen(
                uiState = textToPdfState,
                onUpdateState = viewModel::updateTextToPdf,
                onImportTxtFile = { importTxtLauncher.launch(arrayOf("text/plain", "text/*")) },
                onCreatePdf = viewModel::createPdfFromText,
                onOpenCreatedPdf = { docId ->
                    viewModel.markOpened(docId)
                    navController.navigate(AppRoutes.pdfViewer(docId)) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onShareCreatedPdf = { docId ->
                    allDocs.find { it.id == docId }?.let(::shareDocument)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.WORD_TO_PDF) {
            WordToPdfScreen(
                uiState = wordToPdfState,
                onPickWordFile = {
                    pickWordFileLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword"
                        )
                    )
                },
                onConvertToPdf = viewModel::convertWordToPdf,
                onOpenCreatedPdf = { docId ->
                    viewModel.markOpened(docId)
                    navController.navigate(AppRoutes.pdfViewer(docId)) {
                        popUpTo(AppRoutes.MAIN_TABS)
                    }
                },
                onShareCreatedPdf = { docId ->
                    allDocs.find { it.id == docId }?.let(::shareDocument)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_TO_WORD) {
            PdfToWordScreen(
                uiState = pdfToWordState,
                appDocuments = allDocs,
                onPickFromMobile = { externalPdfForWordLauncher.launch(arrayOf("application/pdf")) },
                onSelectAppDocument = viewModel::selectAppPdfForWord,
                onDownloadOcrPack = viewModel::downloadBengaliOcrPack,
                onConvertToWord = viewModel::convertPdfToWord,
                onSaveDocxCopy = { file, name ->
                    pendingDocxToExport = file
                    saveDocxCopyLauncher.launch("$name.docx")
                },
                onShareDocx = { file, name ->
                    FileActionsHelper.shareDocumentFile(
                        context = context,
                        file = file,
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        displayTitle = name,
                        chooserTitle = "ওয়ার্ড ফাইল শেয়ার করুন"
                    ).onFailure {
                        viewModel.showToast("ওয়ার্ড ফাইলটি শেয়ার করা যায়নি।")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_TO_LONG_IMAGE) {
            PdfToLongImageScreen(
                uiState = pdfToLongImageState,
                appDocuments = allDocs,
                onPickFromMobile = { externalPdfForLongImageLauncher.launch(arrayOf("application/pdf")) },
                onSelectAppDocument = viewModel::loadAppPdfForLongImage,
                onTogglePage = viewModel::toggleLongImagePage,
                onSelectAll = viewModel::selectAllLongImagePages,
                onSelectFormat = viewModel::setLongImageFormat,
                onSelectQuality = viewModel::setLongImageQuality,
                onGenerateLongImage = viewModel::generateLongImage,
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Modern Reference-Matched Create Bottom Sheet
    if (showCreateSheet) {
        CreateActionBottomSheet(
            onDismiss = { showCreateSheet = false },
            onActionSelected = { action ->
                when (action) {
                    CreateSheetAction.IMPORT_FILES -> {
                        importFilesLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "image/*",
                                "text/plain",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    }
                    CreateSheetAction.CREATE_FOLDER -> showCreateFolderDialog = true
                    CreateSheetAction.IMAGE_TO_PDF -> startFreshGalleryToPdf()
                    CreateSheetAction.SCAN_TO_PDF -> startFreshScanner(scanMode = true)
                    CreateSheetAction.WORD_TO_PDF -> {
                        viewModel.resetWordToPdf()
                        navController.navigate(AppRoutes.WORD_TO_PDF)
                    }
                    CreateSheetAction.PDF_TO_WORD -> {
                        viewModel.resetPdfToWord()
                        navController.navigate(AppRoutes.PDF_TO_WORD)
                    }
                    CreateSheetAction.TEXT_TO_PDF -> {
                        viewModel.resetTextToPdf()
                        navController.navigate(AppRoutes.TEXT_TO_PDF)
                    }
                    CreateSheetAction.PDF_TO_IMAGE -> navController.navigate(AppRoutes.PDF_TO_IMAGE)
                    CreateSheetAction.PDF_TO_LONG_IMAGE -> {
                        viewModel.resetPdfToLongImage()
                        navController.navigate(AppRoutes.PDF_TO_LONG_IMAGE)
                    }
                    CreateSheetAction.COMPRESS_PDF -> {
                        viewModel.resetCompression()
                        navController.navigate(AppRoutes.PDF_COMPRESS)
                    }
                    CreateSheetAction.ORGANIZE_PDF_PAGES -> {
                        viewModel.resetOrganizer()
                        pendingPickPurpose = PendingPdfPickPurpose.ORGANIZE
                    }
                }
            }
        )
    }

    // Rename Dialog
    renameTarget?.let { target ->
        RenameDocumentDialog(
            initialName = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameDocument(target.id, newName)
                renameTarget = null
            }
        )
    }

    // Rename Folder Dialog
    renameFolderTarget?.let { folder ->
        RenameFolderDialog(
            initialName = folder.name,
            onDismiss = { renameFolderTarget = null },
            onConfirm = { newName ->
                viewModel.renameFolder(folder.id, newName)
                renameFolderTarget = null
            }
        )
    }

    // Delete Confirm Dialog
    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            documentName = target.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteDocument(target)
                deleteTarget = null
            }
        )
    }

    // File Info Dialog
    infoTarget?.let { target ->
        val latestDoc = allDocs.find { it.id == target.id } ?: target
        DocumentInfoDialog(
            document = latestDoc,
            folders = folders,
            onMoveToFolder = { folderId ->
                viewModel.moveDocumentToFolder(latestDoc.id, folderId)
            },
            onSaveToDevice = {
                pendingFileToExport = File(latestDoc.filePath)
                savePdfCopyLauncher.launch("${latestDoc.name}.pdf")
            },
            onShare = {
                infoTarget = null
                shareDocument(latestDoc)
            },
            onPrint = {
                infoTarget = null
                printDocument(latestDoc)
            },
            onDismiss = { infoTarget = null }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    // Quick PDF Picker for Manage / Organize tools
    pendingPickPurpose?.let { purpose ->
        val dialogTitle = when (purpose) {
            PendingPdfPickPurpose.COMPRESS -> "সংকোচনের জন্য পিডিএফ বাছুন"
            PendingPdfPickPurpose.PDF_TO_IMAGE -> "ছবিতে রূপান্তরের পিডিএফ বাছুন"
            PendingPdfPickPurpose.ORGANIZE -> "পৃষ্ঠা সাজানোর পিডিএফ বাছুন"
            PendingPdfPickPurpose.RENAME -> "নাম পরিবর্তনের পিডিএফ বাছুন"
            PendingPdfPickPurpose.SHARE -> "শেয়ার করার পিডিএফ বাছুন"
            PendingPdfPickPurpose.DELETE -> "মুছে ফেলার পিডিএফ বাছুন"
            PendingPdfPickPurpose.PRINT -> "প্রিন্ট করার পিডিএফ বাছুন"
        }
        SelectPdfDocumentDialog(
            title = dialogTitle,
            documents = allDocs,
            allowExternalPick = purpose == PendingPdfPickPurpose.ORGANIZE,
            onPickExternal = {
                if (purpose == PendingPdfPickPurpose.ORGANIZE) {
                    externalPdfForOrganizerLauncher.launch(arrayOf("application/pdf"))
                }
            },
            onSelectDocument = { doc ->
                when (purpose) {
                    PendingPdfPickPurpose.COMPRESS -> {
                        viewModel.selectDocumentForCompression(doc)
                        navController.navigate(AppRoutes.PDF_COMPRESS)
                    }
                    PendingPdfPickPurpose.PDF_TO_IMAGE -> {
                        viewModel.loadPdfForImageExport(doc)
                        navController.navigate(AppRoutes.PDF_TO_IMAGE)
                    }
                    PendingPdfPickPurpose.ORGANIZE -> {
                        viewModel.loadDocumentForOrganizer(doc)
                        navController.navigate(AppRoutes.PAGE_ORGANIZER)
                    }
                    PendingPdfPickPurpose.RENAME -> renameTarget = doc
                    PendingPdfPickPurpose.SHARE -> shareDocument(doc)
                    PendingPdfPickPurpose.DELETE -> deleteTarget = doc
                    PendingPdfPickPurpose.PRINT -> printDocument(doc)
                }
            },
            onDismiss = { pendingPickPurpose = null }
        )
    }
}
