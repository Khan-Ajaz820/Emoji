package com.yourname.emojimosaic

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.JsonObject

class MosaicGenerator(private val context: Context) {

    private var kdTree: KdTree? = null

    // Load and parse the KD-Tree from assets/kd_tree.json
    fun loadKdTree() {
        val json = context.assets.open("kd_tree.json")
            .bufferedReader()
            .use { it.readText() }

        val gson = Gson()
        val rootJson = gson.fromJson(json, JsonObject::class.java)
        kdTree = KdTree(parseNode(rootJson))
    }

    // Recursively parse JSON into KdNode tree
    private fun parseNode(obj: JsonObject?): KdNode? {
        if (obj == null || obj.isJsonNull) return null

        val pointObj = obj.getAsJsonObject("point")
        val src = pointObj.get("src").asString
        val avgArray = pointObj.getAsJsonArray("avg")
        val avg = listOf(
            avgArray[0].asDouble,
            avgArray[1].asDouble,
            avgArray[2].asDouble
        )

        val point = EmojiPoint(src, avg)
        val axis = obj.get("axis").asInt

        val left = if (obj.has("left") && !obj.get("left").isJsonNull)
            parseNode(obj.getAsJsonObject("left")) else null

        val right = if (obj.has("right") && !obj.get("right").isJsonNull)
            parseNode(obj.getAsJsonObject("right")) else null

        return KdNode(point, left, right, axis)
    }

    // Main function: converts bitmap into emoji mosaic string
    fun generateMosaic(
        bitmap: Bitmap,
        tileSize: Int = 16,        // pixels per tile
        maxCols: Int = 48          // max columns of emojis
    ): String {
        val tree = kdTree ?: return "KD-Tree not loaded!"

        // Scale down bitmap so mosaic fits in maxCols
        val scale = maxCols.toFloat() / (bitmap.width.toFloat() / tileSize)
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val cols = scaledWidth / tileSize
        val rows = scaledHeight / tileSize

        val sb = StringBuilder()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Get average RGB of this tile
                val (r, g, b) = getAverageRgb(scaled, col * tileSize, row * tileSize, tileSize)

                // Find nearest emoji
                val emojiPoint = tree.findNearest(r, g, b)
                val emoji = if (emojiPoint != null) srcToEmoji(emojiPoint.src) else "❓"

                sb.append(emoji)
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    // Calculate average RGB color of a tile region
    private fun getAverageRgb(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        tileSize: Int
    ): Triple<Double, Double, Double> {
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0

        val endX = (startX + tileSize).coerceAtMost(bitmap.width)
        val endY = (startY + tileSize).coerceAtMost(bitmap.height)

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = bitmap.getPixel(x, y)
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
                count++
            }
        }

        return if (count == 0) Triple(0.0, 0.0, 0.0)
        else Triple(r.toDouble() / count, g.toDouble() / count, b.toDouble() / count)
    }
}
