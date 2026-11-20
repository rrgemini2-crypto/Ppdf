package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.NormPoint
import com.example.data.model.PageEditParams
import com.example.data.model.WorkspacePage
import com.example.engine.DocumentCornerDetector
import com.example.engine.DocumentDetectionResult
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueHeader
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyBlueAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import java.io.File
import java.util.concurrent.Executors

@Composable
fun DocumentScannerScreen(
    isDocumentScanMode: Boolean,
    capturedPages: List<WorkspacePage>,
    onCaptureSaved: (file: File, corners: List<NormPoint>?, autoCrop: Boolean, isScan: Boolean, openEditor: Boolean) -> Unit,
    onRetakeLast: () -> Unit,
    onPickFromGallery: (List<android.net.Uri>) -> Unit,
    onEditLastPage: (String) -> Unit,
    onFinishScanning: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) onPickFromGallery(uris)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhiteBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = LightBlueHeader,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "ক্যামেরার অনুমতি প্রয়োজন",
                style = MaterialTheme.typography.headlineSmall,
                color = DeepInk,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "দলিল স্ক্যান বা ছবি তোলার জন্য ক্যামেরা ব্যবহারের অনুমতি দিন। আপনার কোনো ছবি বাইরে পাঠানো হয় না।",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_camera_permission_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("ক্যামেরার অনুমতি দিন")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("গ্যালারি থেকে ছবি বাছুন")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("ফিরে যান", color = MutedInk)
            }
        }
        return
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var autoScanEnabled by remember { mutableStateOf(isDocumentScanMode) }
    var isCapturing by remember { mutableStateOf(false) }
    var detection by remember {
        mutableStateOf(
            DocumentDetectionResult(
                corners = PageEditParams.defaultCorners(),
                confidence = 0f,
                isDetected = false
            )
        )
    }
    var stableFrameCount by remember { mutableIntStateOf(0) }
    var lastAutoCaptureMs by remember { mutableLongStateOf(0L) }
    var cameraErrorBn by remember { mutableStateOf<String?>(null) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    fun triggerCapture(openEditorAfter: Boolean) {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true

        val scanDir = File(context.filesDir, "workspace").apply { mkdirs() }
        val photoFile = File(scanDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputOpts = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val snappedCorners = if (detection.isDetected) detection.corners else null

        capture.takePicture(
            outputOpts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onCaptureSaved(
                        photoFile,
                        snappedCorners,
                        isDocumentScanMode && snappedCorners != null,
                        isDocumentScanMode,
                        openEditorAfter
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(
                        context,
                        "ছবি তুলতে সমস্যা হয়েছে। আবার চেষ্টা করুন।",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    LaunchedEffect(flashEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(flashEnabled)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Live CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = capture

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imgAnalysis ->
                                imgAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    try {
                                        val res = DocumentCornerDetector.detectFromImageProxy(imageProxy)
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            detection = res
                                            if (autoScanEnabled && res.isDetected && res.confidence >= 0.52f) {
                                                stableFrameCount++
                                                val now = System.currentTimeMillis()
                                                if (stableFrameCount >= 12 && now - lastAutoCaptureMs > 2800L && !isCapturing) {
                                                    stableFrameCount = 0
                                                    lastAutoCaptureMs = now
                                                    triggerCapture(openEditorAfter = false)
                                                }
                                            } else {
                                                stableFrameCount = 0
                                            }
                                        }
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                        cameraProvider.unbindAll()
                        boundCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                            analysis
                        )
                    } catch (e: Exception) {
                        cameraErrorBn = "ক্যামেরা চালু করা যায়নি। ডিভাইসটি পুনরায় পরীক্ষা করুন।"
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Document Quadrilateral Overlay
        if (isDocumentScanMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pts = detection.corners
                if (pts.size == 4) {
                    val p0 = Offset(pts[0].x * size.width, pts[0].y * size.height)
                    val p1 = Offset(pts[1].x * size.width, pts[1].y * size.height)
                    val p2 = Offset(pts[2].x * size.width, pts[2].y * size.height)
                    val p3 = Offset(pts[3].x * size.width, pts[3].y * size.height)

                    val quadColor = if (detection.isDetected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.55f)
                    val path = Path().apply {
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = quadColor.copy(alpha = if (detection.isDetected) 0.18f else 0.06f)
                    )
                    drawPath(
                        path = path,
                        color = quadColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    listOf(p0, p1, p2, p3).forEach { corner ->
                        drawCircle(color = quadColor, radius = 14f, center = corner)
                        drawCircle(color = Color.White, radius = 7f, center = corner)
                    }
                }
            }
        }

        // Top Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "পিছনে যান",
                        tint = Color.White
                    )
                }

                Text(
                    text = if (isDocumentScanMode) "স্বয়ংক্রিয় দলিল স্ক্যান" else "ক্যামেরা দিয়ে ছবি তুলুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "ফ্ল্যাশ",
                        tint = if (flashEnabled) SkyBlueAccent else Color.White
                    )
                }
            }

            if (isDocumentScanMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = autoScanEnabled,
                        onClick = { autoScanEnabled = !autoScanEnabled },
                        leadingIcon = {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(if (autoScanEnabled) "স্বয়ংক্রিয় স্ক্যান চালু" else "নিজে ছবি তুলুন")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            labelColor = Color.White,
                            iconColor = Color.White
                        )
                    )

                    Text(
                        text = when {
                            cameraErrorBn != null -> cameraErrorBn!!
                            detection.isDetected -> "কাগজ শনাক্ত হয়েছে • স্থির রাখুন"
                            else -> "কাগজের চারটি কোণা ফ্রেমে রাখুন"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (detection.isDetected) SkyBlueAccent else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Bottom Controls & Multi-page Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.62f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (capturedPages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable {
                            onEditLastPage(capturedPages.last().id)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, SkyBlueAccent, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(capturedPages.last().previewPath)),
                                contentDescription = "সর্বশেষ তোলা পৃষ্ঠা",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Text(
                                text = BengaliFormatters.formatOrdinalPage(capturedPages.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ট্যাপ করে সম্পাদনা করুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyBlueAccent
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onRetakeLast,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "পুনরায় তুলুন",
                                tint = Color.White
                            )
                        }
                        Button(
                            onClick = onFinishScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("সম্পন্ন (${BengaliFormatters.toBengaliDigits(capturedPages.size)})")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = "গ্যালারি থেকে নিন",
                        tint = Color.White
                    )
                }

                // Main Shutter Button
                Surface(
                    onClick = { triggerCapture(openEditorAfter = capturedPages.isEmpty() && isDocumentScanMode) },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(72.dp)
                        .border(4.dp, RoyalBlue, CircleShape)
                        .testTag("camera_shutter_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "ছবি তুলুন",
                            tint = RoyalBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                if (capturedPages.isNotEmpty()) {
                    IconButton(
                        onClick = onFinishScanning,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(RoyalBlue)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "পরবর্তী ধাপ",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}
