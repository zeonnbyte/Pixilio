package com.pixilio.app

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ikon bergaya Lucide (https://lucide.dev, lisensi ISC): grid 24x24, stroke 2, ujung & sambungan bulat.
 * Data path ditanam langsung supaya tidak butuh dependency tambahan.
 */
object Lu {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (p in paths) {
            b.addPath(
                pathData = PathParser().parsePathString(p).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return b.build()
    }

    private const val FOLDER =
        "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"

    val Pencil by lazy {
        icon(
            "Pencil",
            "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
            "m15 5 4 4"
        )
    }
    val Eraser by lazy {
        icon(
            "Eraser",
            "m7 21-4.3-4.3c-1-1-1-2.5 0-3.4l9.6-9.6c1-1 2.5-1 3.4 0l5.6 5.6c1 1 1 2.5 0 3.4L13 21",
            "M22 21H7",
            "m5 11 9 9"
        )
    }
    val PaintBucket by lazy {
        icon(
            "PaintBucket",
            "m19 11-8-8-8.6 8.6a2 2 0 0 0 0 2.8l5.2 5.2c.8.8 2 .8 2.8 0L19 11Z",
            "m5 2 5 5",
            "M2 13h15",
            "M22 20a2 2 0 1 1-4 0c0-1.6 1.7-2.4 2-4 .3 1.6 2 2.4 2 4Z"
        )
    }
    val Pipette by lazy {
        icon(
            "Pipette",
            "m2 22 1-1h3l9-9",
            "M3 21v-3l9-9",
            "m15 6 3.4-3.4a2.1 2.1 0 1 1 3 3L18 9l.4.4a2.1 2.1 0 1 1-3 3l-3.8-3.8a2.1 2.1 0 1 1 3-3l.4.4Z"
        )
    }
    val Slash by lazy { icon("Slash", "M22 2 2 22") }
    val Square by lazy {
        icon("Square", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
    }
    val Circle by lazy { icon("Circle", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0") }
    val BoxSelect by lazy {
        icon(
            "BoxSelect",
            "M5 3a2 2 0 0 0-2 2", "M19 3a2 2 0 0 1 2 2", "M21 19a2 2 0 0 1-2 2", "M5 21a2 2 0 0 1-2-2",
            "M9 3h1", "M9 21h1", "M14 3h1", "M14 21h1", "M3 9v1", "M21 9v1", "M3 14v1", "M21 14v1"
        )
    }
    val Hand by lazy {
        icon(
            "Hand",
            "M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2",
            "M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2",
            "M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8",
            "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15"
        )
    }
    val ChevronLeft by lazy { icon("ChevronLeft", "m15 18-6-6 6-6") }
    val Undo2 by lazy {
        icon("Undo2", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11")
    }
    val Redo2 by lazy {
        icon("Redo2", "m15 14 5-5-5-5", "M20 9H9.5A5.5 5.5 0 0 0 4 14.5A5.5 5.5 0 0 0 9.5 20H13")
    }
    val EllipsisVertical by lazy { icon("EllipsisVertical", "M12 5h.01", "M12 12h.01", "M12 19h.01") }
    val Plus by lazy { icon("Plus", "M5 12h14", "M12 5v14") }
    val Folder by lazy { icon("Folder", FOLDER) }
    val FolderPlus by lazy { icon("FolderPlus", "M12 10v6", "M9 13h6", FOLDER) }
    val Palette by lazy {
        icon(
            "Palette",
            "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
            "M13.5 6.5h.01", "M17.5 10.5h.01", "M6.5 12.5h.01", "M8.5 7.5h.01"
        )
    }
    val Languages by lazy {
        icon("Languages", "m5 8 6 6", "m4 14 6-6 2-3", "M2 5h12", "M7 2h1", "m22 22-5-10-5 10", "M14 18h6")
    }
    val Layers by lazy {
        icon(
            "Layers",
            "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z",
            "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12",
            "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17"
        )
    }
    val Clipboard by lazy {
        icon(
            "Clipboard",
            "M8 2h8a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z",
            "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"
        )
    }
    val Grid3x3 by lazy {
        icon(
            "Grid3x3",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18"
        )
    }
    val FlipHorizontal2 by lazy {
        icon(
            "FlipHorizontal2",
            "m3 7 5 5-5 5V7", "m21 7-5 5 5 5V7", "M12 20v2", "M12 14v2", "M12 8v2", "M12 2v2"
        )
    }
    val FlipVertical2 by lazy {
        icon(
            "FlipVertical2",
            "m17 3-5 5-5-5h10", "m17 21-5-5-5 5h10", "M4 12H2", "M10 12H8", "M16 12h-2", "M22 12h-2"
        )
    }
    val Check by lazy { icon("Check", "M20 6 9 17l-5-5") }
    val X by lazy { icon("X", "M18 6 6 18", "m6 6 12 12") }
    val Copy by lazy {
        icon(
            "Copy",
            "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z",
            "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
        )
    }
    val Scissors by lazy {
        icon(
            "Scissors",
            "M9 6a3 3 0 1 1-6 0a3 3 0 0 1 6 0", "M9 18a3 3 0 1 1-6 0a3 3 0 0 1 6 0",
            "M20 4 8.12 15.88", "M14.47 14.48 20 20", "M8.12 8.12 12 12"
        )
    }
    val Trash2 by lazy {
        icon(
            "Trash2",
            "M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
            "M10 11v6", "M14 11v6"
        )
    }
    val Eye by lazy {
        icon(
            "Eye",
            "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0",
            "M15 12a3 3 0 1 1-6 0a3 3 0 0 1 6 0"
        )
    }
    val EyeOff by lazy {
        icon(
            "EyeOff",
            "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49",
            "M14.084 14.158a3 3 0 0 1-4.242-4.242",
            "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143",
            "m2 2 20 20"
        )
    }
}

fun toolIcon(t: Tool): ImageVector = when (t) {
    Tool.PENCIL -> Lu.Pencil
    Tool.ERASER -> Lu.Eraser
    Tool.FILL -> Lu.PaintBucket
    Tool.PICKER -> Lu.Pipette
    Tool.LINE -> Lu.Slash
    Tool.RECT -> Lu.Square
    Tool.ELLIPSE -> Lu.Circle
    Tool.SELECT -> Lu.BoxSelect
    Tool.PAN -> Lu.Hand
}

fun chipIcon(v: ImageVector): @Composable () -> Unit = {
    Icon(v, contentDescription = null, modifier = Modifier.size(18.dp))
}
