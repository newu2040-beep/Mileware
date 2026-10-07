package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.ConfessionPost
import com.example.data.model.PastelTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CardExportHelper {

    enum class ExportResolution(
        val label: String,
        val width: Int,
        val height: Int,
        val badge: String
    ) {
        FHD_PORTRAIT("1080p FHD (Story 9:16)", 1080, 1920, "1080P"),
        FHD_SQUARE("1080p Post (Square 1:1)", 1080, 1080, "1080P"),
        UHD_4K_PORTRAIT("4K UHD (Story 9:16)", 2160, 3840, "4K UHD"),
        UHD_4K_SQUARE("4K UHD (Square 1:1)", 2160, 2160, "4K UHD"),
        UHD_8K_PORTRAIT("8K Ultra (Story 9:16)", 4320, 7680, "8K MASTER"),
        UHD_8K_SQUARE("8K Ultra (Square 1:1)", 4320, 4320, "8K MASTER")
    }

    suspend fun renderConfessionCard(
        post: ConfessionPost,
        theme: PastelTheme,
        resolution: ExportResolution
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = resolution.width
        val height = resolution.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scale = width / 1080f

        // 1. Draw outer deep canvas background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            val startColor = android.graphics.Color.parseColor("#0F0E17")
            val endColor = android.graphics.Color.parseColor("#1B1A28")
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Pastel Card in center
        val cardMarginX = 64f * scale
        val cardMarginY = if (resolution.height > resolution.width) 140f * scale else 80f * scale
        val cardRect = RectF(
            cardMarginX,
            cardMarginY,
            width - cardMarginX,
            height - cardMarginY
        )
        val cornerRadius = 48f * scale

        // Card Pastel Gradient
        val cardBgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            val color1 = android.graphics.Color.parseColor(theme.bgHex)
            val color2 = android.graphics.Color.parseColor(theme.accentHex)
            // Blend with soft opacity
            shader = LinearGradient(
                cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                intArrayOf(color1, color1, color2),
                floatArrayOf(0f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBgPaint)

        // Card Border
        val cardBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
            color = android.graphics.Color.parseColor(theme.accentHex)
            alpha = 180
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBorderPaint)

        // 3. Top Header: MilesAre watermark badge
        val headerY = cardRect.top + (60f * scale)
        val appTitlePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 34f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
        }
        canvas.drawText("MilesAre", cardRect.left + (50f * scale), headerY, appTitlePaint)

        val resolutionBadgePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 20f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
            alpha = 200
        }
        val badgeText = "${resolution.badge} • ANONYMOUS"
        val badgeWidth = resolutionBadgePaint.measureText(badgeText)
        canvas.drawText(badgeText, cardRect.right - (50f * scale) - badgeWidth, headerY - (4f * scale), resolutionBadgePaint)

        // 4. Persona & Meta info row
        val personaY = headerY + (70f * scale)
        val avatarPaint = Paint().apply {
            isAntiAlias = true
            textSize = 48f * scale
        }
        canvas.drawText(post.anonymousAvatar, cardRect.left + (50f * scale), personaY + (10f * scale), avatarPaint)

        val personaNamePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 30f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
        }
        canvas.drawText(post.anonymousId, cardRect.left + (120f * scale), personaY - (8f * scale), personaNamePaint)

        val tagPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 22f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
            alpha = 180
        }
        val categoryText = "${post.postType} • #${post.category}"
        canvas.drawText(categoryText, cardRect.left + (120f * scale), personaY + (24f * scale), tagPaint)

        // Content warning banner if present
        var contentStartY = personaY + (80f * scale)
        if (post.contentWarning.isNotEmpty()) {
            val warningRect = RectF(
                cardRect.left + (50f * scale),
                contentStartY,
                cardRect.right - (50f * scale),
                contentStartY + (50f * scale)
            )
            val warningBgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = android.graphics.Color.parseColor("#E53935")
                alpha = 40
            }
            canvas.drawRoundRect(warningRect, 16f * scale, 16f * scale, warningBgPaint)

            val warningTextPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f * scale
                color = android.graphics.Color.parseColor("#C62828")
            }
            canvas.drawText("⚠️ Content Warning: ${post.contentWarning}", warningRect.left + (24f * scale), warningRect.centerY() + (8f * scale), warningTextPaint)
            contentStartY += (70f * scale)
        }

        // 5. Decorative quote symbol
        val quotePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 120f * scale
            color = android.graphics.Color.parseColor(theme.accentHex)
            alpha = 60
        }
        canvas.drawText("“", cardRect.left + (40f * scale), contentStartY + (40f * scale), quotePaint)

        // 6. Confession Content (Text wrapping with dynamic size calculation)
        val availableWidth = (cardRect.width() - (100f * scale)).toInt()
        val bottomReserved = 160f * scale
        val availableHeight = (cardRect.bottom - contentStartY - bottomReserved).toInt()

        var calculatedTextSize = when {
            post.content.length < 100 -> 44f * scale
            post.content.length < 250 -> 36f * scale
            post.content.length < 500 -> 30f * scale
            else -> 26f * scale
        }

        val contentPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            color = android.graphics.Color.parseColor(theme.textHex)
            textSize = calculatedTextSize
        }

        val lines = wrapText(post.content, contentPaint, availableWidth.toFloat())
        val lineHeight = contentPaint.fontSpacing * 1.25f

        // Draw lines
        var lineY = contentStartY + (70f * scale)
        for (line in lines) {
            if (lineY > cardRect.bottom - bottomReserved - (40f * scale)) {
                canvas.drawText("...", cardRect.left + (50f * scale), lineY, contentPaint)
                break
            }
            canvas.drawText(line, cardRect.left + (50f * scale), lineY, contentPaint)
            lineY += lineHeight
        }

        // 7. Footer: Reaction pills, Comments, Timestamp & Privacy Seal
        val footerY = cardRect.bottom - (50f * scale)
        val footerDividerPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
            color = android.graphics.Color.parseColor(theme.accentHex)
            alpha = 150
        }
        canvas.drawLine(
            cardRect.left + (50f * scale),
            footerY - (40f * scale),
            cardRect.right - (50f * scale),
            footerY - (40f * scale),
            footerDividerPaint
        )

        val statsPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
        }
        val statsText = "❤️ ${post.reactionCount}  💬 ${post.commentCount}  🔒 100% Anonymous"
        canvas.drawText(statsText, cardRect.left + (50f * scale), footerY, statsPaint)

        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textSize = 20f * scale
            color = android.graphics.Color.parseColor(theme.textHex)
            alpha = 180
        }
        val dateWidth = datePaint.measureText(dateStr)
        canvas.drawText(dateStr, cardRect.right - (50f * scale) - dateWidth, footerY, datePaint)

        bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        resolutionLabel: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val filename = "MilesAre_${resolutionLabel.replace(" ", "_")}_${System.currentTimeMillis()}.png"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MilesAre")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        caption: String
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "shared_cards")
            cacheDir.mkdirs()
            val imageFile = File(cacheDir, "confession_share_${System.currentTimeMillis()}.png")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "✨ $caption\n\nShared via MilesAre — Anonymous Confessions")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Result.success(Intent.createChooser(shareIntent, "Share MilesAre Confession Card"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
