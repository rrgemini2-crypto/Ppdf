package com.example.data.model

enum class PdfPageSize(
    val titleBn: String,
    val subtitleBn: String,
    val widthPt: Int,
    val heightPt: Int
) {
    A4("এ৪", "২১০ × ২৯৭ মিমি", 595, 842),
    A3("এ৩", "২৯৭ × ৪২০ মিমি", 842, 1191),
    A5("এ৫", "১৪৮ × ২১০ মিমি", 420, 595),
    LETTER("লেটার", "৮.৫ × ১১ ইঞ্চি", 612, 792),
    LEGAL("লিগ্যাল", "৮.৫ × ১৪ ইঞ্চি", 612, 1008),
    CUSTOM("নিজের পছন্দমতো আকার", "ইচ্ছামতো প্রস্থ ও উচ্চতা", 595, 842)
}

enum class PageOrientation(val titleBn: String) {
    PORTRAIT("লম্বালম্বি পৃষ্ঠা"),
    LANDSCAPE("আড়াআড়ি পৃষ্ঠা")
}

enum class PageMargin(val titleBn: String, val marginPt: Int) {
    NONE("মার্জিন ছাড়া", 0),
    SMALL("ছোট মার্জিন", 18),
    MEDIUM("মাঝারি মার্জিন", 36),
    LARGE("বড় মার্জিন", 54)
}

enum class ImagePlacement(val titleBn: String) {
    FIT_ENTIRE("পৃষ্ঠার মধ্যে ছবি সম্পূর্ণ দৃশ্যমান রাখা"),
    FILL_PAGE("পুরো পৃষ্ঠায় ছবি বসানো"),
    CENTER_MEDIUM("কেন্দ্রে সুষম আকারে বসানো")
}

enum class ImageQuality(val titleBn: String, val jpegQuality: Int, val targetMaxDimension: Int) {
    HIGH("উচ্চ মান (সবচেয়ে স্পষ্ট)", 92, 2200),
    MEDIUM("মাঝারি মান (ভারসাম্যপূর্ণ)", 76, 1650),
    LOW("কম মান (ছোট ফাইল)", 56, 1200)
}

data class PdfBuildConfig(
    val fileName: String,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val customWidthMm: Int = 210,
    val customHeightMm: Int = 297,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val margin: PageMargin = PageMargin.SMALL,
    val placement: ImagePlacement = ImagePlacement.FIT_ENTIRE,
    val quality: ImageQuality = ImageQuality.HIGH,
    val compressOutput: Boolean = false,
    val folderId: Long? = null
) {
    fun resolvePageDimensionsPt(): Pair<Int, Int> {
        val (baseW, baseH) = if (pageSize == PdfPageSize.CUSTOM) {
            val wPt = (customWidthMm.coerceIn(50, 1200) * 72f / 25.4f).toInt()
            val hPt = (customHeightMm.coerceIn(50, 1600) * 72f / 25.4f).toInt()
            wPt to hPt
        } else {
            pageSize.widthPt to pageSize.heightPt
        }
        return if (orientation == PageOrientation.LANDSCAPE) {
            maxOf(baseW, baseH) to minOf(baseW, baseH)
        } else {
            minOf(baseW, baseH) to maxOf(baseW, baseH)
        }
    }
}
