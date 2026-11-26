package com.coolappstore.evercallrecorder.by.svhp.ui.common

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Converts a [RoundedPolygon] into a Compose [Shape] with optional pre-rotation and scaling.
 */
fun RoundedPolygon.toPfpShape(
    rotationDegrees: Float = 0f,
    scaleX: Float = 1f,
    scaleY: Float = 1f
): Shape {
    return GenericShape { size, _ ->
        val androidPath = this@toPfpShape.toPath()
        val matrix = Matrix()
        if (scaleX != 1f || scaleY != 1f) {
            matrix.postScale(scaleX, scaleY)
        }
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees)
        }
        matrix.postScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)
        this.addPath(androidPath.asComposePath())
    }
}

/**
 * Metadata and Shape holder for PFP Shapes in Recorder.
 */
data class RecorderPfpShapeItem(
    val key: String,
    val label: String,
    val isExpressive: Boolean = true,
    private val shapeBuilder: () -> Shape
) {
    val shape: Shape by lazy { shapeBuilder() }
}

/**
 * Complete library of 32 PFP Shapes for Recorder:
 * - 2 Standard: Circle (Default), Square
 * - 30 Material 3 Expressive Shapes
 */
object RecorderPfpShapeRegistry {

    // 1. Diamond (rounded)
    val Diamond = RecorderPfpShapeItem("diamond", "Diamond") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.28f, 0.55f))
            .toPfpShape(rotationDegrees = 45f)
    }

    // 2. Soft Star (8-Point)
    val SoftStar8 = RecorderPfpShapeItem("soft_star_8", "Soft Star (8-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.35f, 0.70f)
        ).toPfpShape()
    }

    // 3. Rounded Hexagon
    val RoundedHexagon = RecorderPfpShapeItem("rounded_hexagon", "Rounded Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.28f, 0.60f))
            .toPfpShape()
    }

    // 4. Arch (flat bottom, curved top)
    val Arch = RecorderPfpShapeItem("arch", "Arch") {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val r = w / 2f
            val botR = w * 0.12f
            reset()
            moveTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, w, r * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w, h - botR)
            arcTo(
                rect = Rect(w - botR * 2f, h - botR * 2f, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(botR, h)
            arcTo(
                rect = Rect(0f, h - botR * 2f, botR * 2f, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
    }

    // 5. Wide Hexagon
    val WideHexagon = RecorderPfpShapeItem("wide_hexagon", "Wide Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.24f, 0.55f))
            .toPfpShape(scaleX = 1.05f, scaleY = 0.82f)
    }

    // 6. Cloud (10-Lobe)
    val Cloud10 = RecorderPfpShapeItem("cloud_10", "Cloud (10-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.84f,
            rounding = CornerRounding(0.42f, 0.85f)
        ).toPfpShape()
    }

    // 7. Clover (4-Leaf)
    val Clover4 = RecorderPfpShapeItem("clover_4", "Clover (4-Leaf)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.52f,
            rounding = CornerRounding(0.60f, 0.95f)
        ).toPfpShape(rotationDegrees = 45f)
    }

    // 8. Tilted Squircle
    val TiltedSquircle = RecorderPfpShapeItem("tilted_squircle", "Tilted Squircle") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.42f, 0.85f))
            .toPfpShape(rotationDegrees = 15f)
    }

    // 9. Sunburst (16-Ray)
    val Sunburst16 = RecorderPfpShapeItem("sunburst_16", "Sunburst (16-Ray)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 16,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.18f, 0.45f)
        ).toPfpShape()
    }

    // 10. Star (12-Point)
    val Star12 = RecorderPfpShapeItem("star_12", "Star (12-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.65f,
            rounding = CornerRounding(0.22f, 0.55f)
        ).toPfpShape()
    }

    // 11. Squircle
    val Squircle = RecorderPfpShapeItem("squircle", "Squircle") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.45f, 0.88f))
            .toPfpShape()
    }

    // 12. Scallop (8-Wave)
    val Scallop8 = RecorderPfpShapeItem("scallop_8", "Scallop (8-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.82f,
            rounding = CornerRounding(0.38f, 0.85f)
        ).toPfpShape()
    }

    // 13. Rounded Triangle
    val RoundedTriangle = RecorderPfpShapeItem("rounded_triangle", "Rounded Triangle") {
        RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.32f, 0.68f))
            .toPfpShape()
    }

    // 14. Flower (8-Petal)
    val Flower8 = RecorderPfpShapeItem("flower_8", "Flower (8-Petal)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.60f,
            rounding = CornerRounding(0.55f, 0.92f)
        ).toPfpShape()
    }

    // 15. Rounded Pentagon
    val RoundedPentagon = RecorderPfpShapeItem("rounded_pentagon", "Rounded Pentagon") {
        RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.28f, 0.60f))
            .toPfpShape()
    }

    // 16. Tilted Oval
    val TiltedOval = RecorderPfpShapeItem("tilted_oval", "Tilted Oval") {
        GenericShape { size, _ ->
            val matrix = Matrix()
            val path = AndroidPath()
            val rect = RectF(-size.width * 0.38f, -size.height * 0.52f, size.width * 0.38f, size.height * 0.52f)
            path.addOval(rect, AndroidPath.Direction.CW)
            matrix.postRotate(25f)
            matrix.postTranslate(size.width / 2f, size.height / 2f)
            path.transform(matrix)
            this.addPath(path.asComposePath())
        }
    }

    // 17. Diagonal Pill
    val DiagonalPill = RecorderPfpShapeItem("diagonal_pill", "Diagonal Pill") {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val matrix = Matrix()
            val path = AndroidPath()
            val r = w * 0.22f
            val rect = RectF(-w * 0.22f, -h * 0.48f, w * 0.22f, h * 0.48f)
            path.addRoundRect(rect, r, r, AndroidPath.Direction.CW)
            matrix.postRotate(45f)
            matrix.postTranslate(w / 2f, h / 2f)
            path.transform(matrix)
            this.addPath(path.asComposePath())
        }
    }

    // 18. Heart
    val Heart = RecorderPfpShapeItem("heart", "Heart") {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            reset()
            moveTo(w / 2f, h * 0.86f)
            cubicTo(w * 0.10f, h * 0.60f, 0f, h * 0.35f, 0f, h * 0.22f)
            cubicTo(0f, h * 0.08f, w * 0.15f, 0f, w * 0.32f, 0f)
            cubicTo(w * 0.42f, 0f, w * 0.48f, h * 0.08f, w / 2f, h * 0.20f)
            cubicTo(w * 0.52f, h * 0.08f, w * 0.58f, 0f, w * 0.68f, 0f)
            cubicTo(w * 0.85f, 0f, w, h * 0.08f, w, h * 0.22f)
            cubicTo(w, h * 0.35f, w * 0.90f, h * 0.60f, w / 2f, h * 0.86f)
            close()
        }
    }

    // 19. Pixel Square (stepped corners)
    val PixelSquare = RecorderPfpShapeItem("pixel_square", "Pixel Square") {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val step = w * 0.14f
            reset()
            moveTo(step, 0f)
            lineTo(w - step, 0f)
            lineTo(w - step, step)
            lineTo(w, step)
            lineTo(w, h - step)
            lineTo(w - step, h - step)
            lineTo(w - step, h)
            lineTo(step, h)
            lineTo(step, h - step)
            lineTo(0f, h - step)
            lineTo(0f, step)
            lineTo(step, step)
            close()
        }
    }

    // 20. Sharp Star (12-Point)
    val SharpStar12 = RecorderPfpShapeItem("sharp_star_12", "Sharp Star (12-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.52f,
            rounding = CornerRounding(0.06f, 0.15f)
        ).toPfpShape()
    }

    // 21. Double Bun
    val DoubleBun = RecorderPfpShapeItem("double_bun", "Double Bun") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.70f,
            rounding = CornerRounding(0.45f, 0.85f)
        ).toPfpShape(scaleX = 0.85f, scaleY = 1.05f)
    }

    // 22. Soft Triangle
    val SoftTriangle = RecorderPfpShapeItem("soft_triangle", "Soft Triangle") {
        RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.48f, 0.90f))
            .toPfpShape()
    }

    // 23. Dome (tombstone arch)
    val Dome = RecorderPfpShapeItem("dome", "Dome") {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val r = w / 2f
            val botR = w * 0.14f
            reset()
            moveTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, w, r * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w, h - botR)
            arcTo(
                rect = Rect(w - botR * 2f, h - botR * 2f, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(botR, h)
            arcTo(
                rect = Rect(0f, h - botR * 2f, botR * 2f, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
    }

    // 24. Scallop (10-Wave)
    val Scallop10 = RecorderPfpShapeItem("scallop_10", "Scallop (10-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.84f,
            rounding = CornerRounding(0.32f, 0.82f)
        ).toPfpShape()
    }

    // 25. Scallop (12-Wave)
    val Scallop12 = RecorderPfpShapeItem("scallop_12", "Scallop (12-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.87f,
            rounding = CornerRounding(0.28f, 0.80f)
        ).toPfpShape()
    }

    // 26. Wavy Circle (10-Lobe)
    val WavyCircle10 = RecorderPfpShapeItem("wavy_circle_10", "Wavy Circle (10-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.90f,
            rounding = CornerRounding(0.45f, 0.90f)
        ).toPfpShape()
    }

    // 27. Soft Hexagon
    val SoftHexagon = RecorderPfpShapeItem("soft_hexagon", "Soft Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.42f, 0.85f))
            .toPfpShape()
    }

    // 28. Cushion (4-Corner)
    val Cushion4 = RecorderPfpShapeItem("cushion_4", "Cushion (4-Corner)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.88f,
            rounding = CornerRounding(0.45f, 0.85f)
        ).toPfpShape(rotationDegrees = 45f)
    }

    // 29. Soft Clover (4-Lobe)
    val SoftClover4 = RecorderPfpShapeItem("soft_clover_4", "Soft Clover (4-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.65f,
            rounding = CornerRounding(0.55f, 0.90f)
        ).toPfpShape()
    }

    // 30. Cookie (16-Wave)
    val Cookie16 = RecorderPfpShapeItem("cookie_16", "Cookie (16-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 16,
            innerRadius = 0.88f,
            rounding = CornerRounding(0.30f, 0.80f)
        ).toPfpShape()
    }

    // Standard Shapes
    val Circle = RecorderPfpShapeItem("circle", "Circle (Default)", isExpressive = false) { CircleShape }
    val Square = RecorderPfpShapeItem("square", "Square", isExpressive = false) { RoundedCornerShape(12.dp) }

    val allShapes: List<RecorderPfpShapeItem> by lazy {
        listOf(
            Circle,
            Square,
            Diamond,
            SoftStar8,
            RoundedHexagon,
            Arch,
            WideHexagon,
            Cloud10,
            Clover4,
            TiltedSquircle,
            Sunburst16,
            Star12,
            Squircle,
            Scallop8,
            RoundedTriangle,
            Flower8,
            RoundedPentagon,
            TiltedOval,
            DiagonalPill,
            Heart,
            PixelSquare,
            SharpStar12,
            DoubleBun,
            SoftTriangle,
            Dome,
            Scallop10,
            Scallop12,
            WavyCircle10,
            SoftHexagon,
            Cushion4,
            SoftClover4,
            Cookie16
        )
    }

    private val shapesMap: Map<String, RecorderPfpShapeItem> by lazy {
        allShapes.associateBy { it.key }
    }

    fun getShapeItem(key: String): RecorderPfpShapeItem {
        return shapesMap[key] ?: Circle
    }

    fun getShape(key: String): Shape {
        return (shapesMap[key] ?: Circle).shape
    }

    fun isExpressive(key: String): Boolean {
        return shapesMap[key]?.isExpressive ?: false
    }
}

val LocalRecorderPfpRotationAngle = androidx.compose.runtime.compositionLocalOf { 0f }
val LocalRecorderPfpShapeKey = androidx.compose.runtime.compositionLocalOf { "circle" }
val LocalRecorderPfpAnimate = androidx.compose.runtime.compositionLocalOf { true }

