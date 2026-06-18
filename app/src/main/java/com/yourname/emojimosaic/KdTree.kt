package com.yourname.emojimosaic

// Represents one emoji with its average RGB color
data class EmojiPoint(
    val src: String,       // e.g. "sprite/emoji_u1f600.svg"
    val avg: List<Double>  // [R, G, B]
)

// One node in the KD-Tree
data class KdNode(
    val point: EmojiPoint,
    val left: KdNode?,
    val right: KdNode?,
    val axis: Int  // 0 = R, 1 = G, 2 = B
)

// Converts unicode name to actual emoji character
// e.g. "sprite/emoji_u1f600.svg" -> "😀"
fun srcToEmoji(src: String): String {
    return try {
        // Extract the hex part: "sprite/emoji_u1f600.svg" -> "1f600"
        val hexPart = src
            .removePrefix("sprite/emoji_u")
            .removeSuffix(".svg")

        // Split by "_" to handle multi-codepoint emojis (ZWJ sequences)
        val codepoints = hexPart.split("_").map { it.toInt(16) }

        // Build the emoji string from codepoints
        codepoints.joinToString("") { cp ->
            String(Character.toChars(cp))
        }
    } catch (e: Exception) {
        "❓" // fallback if something goes wrong
    }
}

// KD-Tree nearest neighbor search
class KdTree(private val root: KdNode?) {

    // Find the closest emoji to a given RGB color
    fun findNearest(r: Double, g: Double, b: Double): EmojiPoint? {
        val target = listOf(r, g, b)
        return search(root, target)?.point
    }

    private fun search(node: KdNode?, target: List<Double>): KdNode? {
        if (node == null) return null

        val axis = node.axis
        val diff = target[axis] - node.point.avg[axis]

        // Decide which side to search first
        val (first, second) = if (diff <= 0) {
            Pair(node.left, node.right)
        } else {
            Pair(node.right, node.left)
        }

        // Search the closer side first
        var best = closerNode(target, search(first, target), node)

        // Check if the other side could have a closer point
        if (diff * diff < squaredDistance(target, best!!.point.avg)) {
            best = closerNode(target, search(second, target), best)
        }

        return best
    }

    // Returns whichever node is closer to the target
    private fun closerNode(
        target: List<Double>,
        a: KdNode?,
        b: KdNode?
    ): KdNode? {
        if (a == null) return b
        if (b == null) return a
        return if (squaredDistance(target, a.point.avg) <= squaredDistance(target, b.point.avg)) a else b
    }

    // Euclidean distance squared in RGB space
    private fun squaredDistance(a: List<Double>, b: List<Double>): Double {
        return (a[0] - b[0]).let { it * it } +
               (a[1] - b[1]).let { it * it } +
               (a[2] - b[2]).let { it * it }
    }
}
