package com.example.data.model

data class NormPoint(
    val x: Float,
    val y: Float
)

data class NormRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFull: Boolean
        get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class PageEditParams(
    val filterType: ImageFilterType = ImageFilterType.ORIGINAL,
    val rotationDegrees: Int = 0,
    val brightness: Float = 0f, // -100f..100f
    val contrast: Float = 1f,   // 0.5f..2.0f
    val corners: List<NormPoint> = defaultCorners(),
    val usePerspectiveWarp: Boolean = false,
    val cropRect: NormRect = NormRect()
) {
    companion object {
        fun defaultCorners(): List<NormPoint> = listOf(
            NormPoint(0f, 0f),
            NormPoint(1f, 0f),
            NormPoint(1f, 1f),
            NormPoint(0f, 1f)
        )
    }
}

data class WorkspacePage(
    val id: String,
    val originalPath: String,
    val previewPath: String,
    val editParams: PageEditParams = PageEditParams(),
    val isFromScan: Boolean = false
)
