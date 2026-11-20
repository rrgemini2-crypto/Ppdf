package com.example.data.model

enum class CompressionLevel(
    val titleBn: String,
    val descriptionBn: String,
    val scaleFactor: Float,
    val jpegQuality: Int
) {
    LOW(
        titleBn = "কম সংকোচন",
        descriptionBn = "ছবির মান ভালো রাখবে ও ফাইলের আকার সামান্য কমাবে। গুরুত্বপূর্ণ দলিলের জন্য উপযোগী।",
        scaleFactor = 1.65f,
        jpegQuality = 82
    ),
    MEDIUM(
        titleBn = "মাঝারি সংকোচন",
        descriptionBn = "ছবির মান ও ফাইলের আকারের মধ্যে ভারসাম্য রাখবে। সাধারণ ব্যবহারের জন্য উপযোগী।",
        scaleFactor = 1.25f,
        jpegQuality = 62
    ),
    HIGH(
        titleBn = "বেশি সংকোচন",
        descriptionBn = "ফাইলের আকার যতটা সম্ভব কমাবে। ছবির রেজল্যুশন ও মান কিছুটা কমতে পারে।",
        scaleFactor = 0.90f,
        jpegQuality = 38
    )
}

sealed interface CompressionResult {
    data class Success(
        val outputFilePath: String,
        val outputFileName: String,
        val originalSizeBytes: Long,
        val compressedSizeBytes: Long,
        val savedBytes: Long,
        val savedPercent: Int,
        val pageCount: Int,
        val savedDocumentId: Long? = null
    ) : CompressionResult

    data class AlreadyCompressed(
        val originalSizeBytes: Long,
        val pageCount: Int
    ) : CompressionResult

    data class Error(val messageBn: String) : CompressionResult
}
