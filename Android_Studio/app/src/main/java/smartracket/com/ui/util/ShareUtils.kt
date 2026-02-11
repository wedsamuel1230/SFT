package smartracket.com.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ShareUtils {
    fun shareSnapshot(
        context: Context,
        view: View,
        shareText: String,
        chooserTitle: String,
        cropRect: android.graphics.Rect? = null,
        cropPaddingPx: Int = 0,
        appName: String? = null,
        tagline: String? = null
    ) {
        val imageUri = saveViewSnapshot(context, view, cropRect, cropPaddingPx, appName, tagline) ?: run {
            shareTextOnly(context, shareText, chooserTitle)
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun shareTextOnly(context: Context, shareText: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun saveViewSnapshot(
        context: Context,
        view: View,
        cropRect: android.graphics.Rect?,
        cropPaddingPx: Int,
        appName: String?,
        tagline: String?
    ): Uri? {
        return try {
            val bitmap = view.drawToBitmap()
            val croppedBitmap = cropRect?.let { rect ->
                val paddedRect = padCropRect(rect, cropPaddingPx, bitmap.width, bitmap.height)
                cropBitmap(bitmap, paddedRect)
            } ?: bitmap

            if (croppedBitmap == null) {
                return null
            }

            val shareBitmap = if (!appName.isNullOrBlank() || !tagline.isNullOrBlank()) {
                drawBrandingOverlay(croppedBitmap, appName.orEmpty(), tagline.orEmpty(), context)
            } else {
                croppedBitmap
            }

            val imagesDir = File(context.cacheDir, "share_images").apply { mkdirs() }
            val imageFile = File(imagesDir, "analytics-${UUID.randomUUID()}.png")

            FileOutputStream(imageFile).use { output ->
                shareBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Draws a header bar with the app name and tagline above the card content,
     * producing a polished branded share image.
     */
    private fun drawBrandingOverlay(
        cardBitmap: Bitmap,
        appName: String,
        tagline: String,
        context: Context
    ): Bitmap {
        val density = context.resources.displayMetrics.density

        // Sizing
        val horizontalPadding = (20 * density).toInt()
        val topPadding = (24 * density).toInt()
        val appNameSize = 22 * density
        val taglineSize = 12 * density
        val spacingAfterName = (6 * density).toInt()
        val spacingAfterTagline = (18 * density).toInt()

        // Paint for app name
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A2E")
            textSize = appNameSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Paint for tagline
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = taglineSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        // Split tagline at comma or Chinese semicolon into multiple lines
        val taglineLines = if (tagline.isNotBlank()) {
            tagline.split(Regex("[,，;；]"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else emptyList()

        // Measure text heights
        val appNameHeight = if (appName.isNotBlank()) {
            val fm = appNamePaint.fontMetrics
            (fm.descent - fm.ascent + fm.leading).toInt()
        } else 0

        val singleTaglineHeight = if (taglineLines.isNotEmpty()) {
            val fm = taglinePaint.fontMetrics
            (fm.descent - fm.ascent + fm.leading).toInt()
        } else 0
        val taglineLineSpacing = (2 * density).toInt()
        val totalTaglineHeight = if (taglineLines.isNotEmpty()) {
            singleTaglineHeight * taglineLines.size + taglineLineSpacing * (taglineLines.size - 1)
        } else 0

        // Calculate header height
        val headerHeight = topPadding +
                (if (appName.isNotBlank()) appNameHeight + spacingAfterName else 0) +
                totalTaglineHeight +
                spacingAfterTagline

        // Create new bitmap with header + card content
        val totalWidth = cardBitmap.width
        val totalHeight = headerHeight + cardBitmap.height
        val result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fill header background (light grey matching card style)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F7")
        }
        canvas.drawRect(0f, 0f, totalWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        // Draw app name
        var yOffset = topPadding.toFloat()
        if (appName.isNotBlank()) {
            val appNameFm = appNamePaint.fontMetrics
            yOffset -= appNameFm.ascent
            canvas.drawText(appName, horizontalPadding.toFloat(), yOffset, appNamePaint)
            yOffset += appNameFm.descent + spacingAfterName
        }

        // Draw tagline lines
        if (taglineLines.isNotEmpty()) {
            val taglineFm = taglinePaint.fontMetrics
            for ((index, line) in taglineLines.withIndex()) {
                yOffset -= taglineFm.ascent
                canvas.drawText(line, horizontalPadding.toFloat(), yOffset, taglinePaint)
                yOffset += taglineFm.descent
                if (index < taglineLines.size - 1) {
                    yOffset += taglineLineSpacing
                }
            }
        }

        // Draw the card bitmap below header
        canvas.drawBitmap(cardBitmap, 0f, headerHeight.toFloat(), null)

        return result
    }

    private fun cropBitmap(bitmap: Bitmap, cropRect: android.graphics.Rect): Bitmap? {
        val safeLeft = cropRect.left.coerceIn(0, bitmap.width - 1)
        val safeTop = cropRect.top.coerceIn(0, bitmap.height - 1)
        val safeRight = cropRect.right.coerceIn(safeLeft + 1, bitmap.width)
        val safeBottom = cropRect.bottom.coerceIn(safeTop + 1, bitmap.height)

        val width = safeRight - safeLeft
        val height = safeBottom - safeTop

        if (width <= 1 || height <= 1) {
            return null
        }

        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, width, height)
    }

    private fun padCropRect(
        rect: android.graphics.Rect,
        paddingPx: Int,
        maxWidth: Int,
        maxHeight: Int
    ): android.graphics.Rect {
        if (paddingPx <= 0) {
            return rect
        }

        val left = (rect.left - paddingPx).coerceAtLeast(0)
        val top = (rect.top - paddingPx).coerceAtLeast(0)
        val right = (rect.right + paddingPx).coerceAtMost(maxWidth)
        val bottom = (rect.bottom + paddingPx).coerceAtMost(maxHeight)

        return android.graphics.Rect(left, top, right, bottom)
    }
}
