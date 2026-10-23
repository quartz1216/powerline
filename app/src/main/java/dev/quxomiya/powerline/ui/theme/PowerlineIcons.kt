package dev.quxomiya.powerline.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// The Material icons Powerline uses beyond the core set that comes with material3 (Filled style,
// path data as in material-icons-extended), so the large extended artifact is not needed
object PowerlineIcons {
    val Apps: ImageVector by lazy {
        icon(
            "Apps",
            "M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4z" +
                "m6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"
        )
    }

    val Calculate: ImageVector by lazy {
        icon(
            "Calculate",
            "M19 3H5C3.9 3 3 3.9 3 5v14c0 1.1 0.9 2 2 2h14c1.1 0 2-0.9 2-2V5C21 3.9 20.1 3 19 3z" +
                "M13.03 7.06L14.09 6l1.41 1.41L16.91 6l1.06 1.06l-1.41 1.41l1.41 1.41l-1.06 1.06L15.5 9.54" +
                "l-1.41 1.41l-1.06-1.06l1.41-1.41L13.03 7.06z" +
                "M6.25 7.72h5v1.5h-5V7.72z" +
                "M11.5 16h-2v2H8v-2H6v-1.5h2v-2h1.5v2h2V16z" +
                "M18 17.25h-5v-1.5h5V17.25z" +
                "M18 14.75h-5v-1.5h5V14.75z"
        )
    }

    val Public: ImageVector by lazy {
        icon(
            "Public",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" +
                "M11 19.93c-3.95-0.49-7-3.85-7-7.93c0-0.62 0.08-1.21 0.21-1.79L9 15v1c0 1.1 0.9 2 2 2v1.93z" +
                "M17.9 17.39c-0.26-0.81-1-1.39-1.9-1.39h-1v-3c0-0.55-0.45-1-1-1H8v-2h2c0.55 0 1-0.45 1-1V7h2" +
                "c1.1 0 2-0.9 2-2v-0.41c2.93 1.19 5 4.06 5 7.41c0 2.08-0.8 3.97-2.1 5.39z"
        )
    }

    val Shortcut: ImageVector by lazy {
        icon("Shortcut", "M21 11l-6-6v5H8c-2.76 0-5 2.24-5 5v4h2v-4c0-1.65 1.35-3 3-3h7v5l6-6z")
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = "Filled.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(addPathNodes(pathData), fill = SolidColor(Color.Black)).build()
}
