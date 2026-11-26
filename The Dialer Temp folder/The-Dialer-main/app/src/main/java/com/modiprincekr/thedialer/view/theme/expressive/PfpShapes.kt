package com.modiprincekr.thedialer.view.theme.expressive

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
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
 * Metadata and Shape holder for PFP Shapes.
 */
data class PfpShapeItem(
    val key: String,
    val label: String,
    val isExpressive: Boolean = true,
    private val shapeBuilder: () -> Shape
) {
    val shape: Shape by lazy { shapeBuilder() }
}

/**
 * Complete library of 32 PFP Shapes:
 * - 2 Standard: Circle (Default), Square
 * - 30 Material 3 Expressive Shapes
 */
object PfpShapeRegistry {

    // 1. Diamond (rounded)
    val Diamond = PfpShapeItem("diamond", "Diamond") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.28f, 0.55f))
            .toPfpShape(rotationDegrees = 45f)
    }

    // 2. Soft Star (8-Point)
    val SoftStar8 = PfpShapeItem("soft_star_8", "Soft Star (8-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.35f, 0.70f)
        ).toPfpShape()
    }

    // 3. Rounded Hexagon
    val RoundedHexagon = PfpShapeItem("rounded_hexagon", "Rounded Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.28f, 0.60f))
            .toPfpShape()
    }

    // 4. Arch (flat bottom, curved top)
    val Arch = PfpShapeItem("arch", "Arch") {
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
    val WideHexagon = PfpShapeItem("wide_hexagon", "Wide Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.24f, 0.55f))
            .toPfpShape(scaleX = 1.05f, scaleY = 0.82f)
    }

    // 6. Cloud (10-Lobe)
    val Cloud10 = PfpShapeItem("cloud_10", "Cloud (10-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.84f,
            rounding = CornerRounding(0.42f, 0.85f)
        ).toPfpShape()
    }

    // 7. Clover (4-Leaf)
    val Clover4 = PfpShapeItem("clover_4", "Clover (4-Leaf)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.52f,
            rounding = CornerRounding(0.60f, 0.95f)
        ).toPfpShape(rotationDegrees = 45f)
    }

    // 8. Tilted Squircle
    val TiltedSquircle = PfpShapeItem("tilted_squircle", "Tilted Squircle") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.42f, 0.85f))
            .toPfpShape(rotationDegrees = 15f)
    }

    // 9. Sunburst (16-Ray)
    val Sunburst16 = PfpShapeItem("sunburst_16", "Sunburst (16-Ray)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 16,
            innerRadius = 0.72f,
            rounding = CornerRounding(0.18f, 0.45f)
        ).toPfpShape()
    }

    // 10. Star (12-Point)
    val Star12 = PfpShapeItem("star_12", "Star (12-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.65f,
            rounding = CornerRounding(0.22f, 0.55f)
        ).toPfpShape()
    }

    // 11. Squircle
    val Squircle = PfpShapeItem("squircle", "Squircle") {
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.45f, 0.88f))
            .toPfpShape()
    }

    // 12. Scallop (8-Wave)
    val Scallop8 = PfpShapeItem("scallop_8", "Scallop (8-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.82f,
            rounding = CornerRounding(0.38f, 0.85f)
        ).toPfpShape()
    }

    // 13. Rounded Triangle
    val RoundedTriangle = PfpShapeItem("rounded_triangle", "Rounded Triangle") {
        RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.32f, 0.68f))
            .toPfpShape()
    }

    // 14. Flower (8-Petal)
    val Flower8 = PfpShapeItem("flower_8", "Flower (8-Petal)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.60f,
            rounding = CornerRounding(0.55f, 0.92f)
        ).toPfpShape()
    }

    // 15. Rounded Pentagon
    val RoundedPentagon = PfpShapeItem("rounded_pentagon", "Rounded Pentagon") {
        RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.28f, 0.60f))
            .toPfpShape()
    }

    // 16. Tilted Oval
    val TiltedOval = PfpShapeItem("tilted_oval", "Tilted Oval") {
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
    val DiagonalPill = PfpShapeItem("diagonal_pill", "Diagonal Pill") {
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
    val Heart = PfpShapeItem("heart", "Heart") {
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
    val PixelSquare = PfpShapeItem("pixel_square", "Pixel Square") {
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
    val SharpStar12 = PfpShapeItem("sharp_star_12", "Sharp Star (12-Point)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.52f,
            rounding = CornerRounding(0.06f, 0.15f)
        ).toPfpShape()
    }

    // 21. Double Bun
    val DoubleBun = PfpShapeItem("double_bun", "Double Bun") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.70f,
            rounding = CornerRounding(0.45f, 0.85f)
        ).toPfpShape(scaleX = 0.85f, scaleY = 1.05f)
    }

    // 22. Soft Triangle
    val SoftTriangle = PfpShapeItem("soft_triangle", "Soft Triangle") {
        RoundedPolygon(numVertices = 3, rounding = CornerRounding(0.48f, 0.90f))
            .toPfpShape()
    }

    // 23. Dome (tombstone arch)
    val Dome = PfpShapeItem("dome", "Dome") {
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
    val Scallop10 = PfpShapeItem("scallop_10", "Scallop (10-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.84f,
            rounding = CornerRounding(0.32f, 0.82f)
        ).toPfpShape()
    }

    // 25. Scallop (12-Wave)
    val Scallop12 = PfpShapeItem("scallop_12", "Scallop (12-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.87f,
            rounding = CornerRounding(0.28f, 0.80f)
        ).toPfpShape()
    }

    // 26. Wavy Circle (10-Lobe)
    val WavyCircle10 = PfpShapeItem("wavy_circle_10", "Wavy Circle (10-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.90f,
            rounding = CornerRounding(0.45f, 0.90f)
        ).toPfpShape()
    }

    // 27. Soft Hexagon
    val SoftHexagon = PfpShapeItem("soft_hexagon", "Soft Hexagon") {
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.42f, 0.85f))
            .toPfpShape()
    }

    // 28. Cushion (4-Corner)
    val Cushion4 = PfpShapeItem("cushion_4", "Cushion (4-Corner)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.88f,
            rounding = CornerRounding(0.45f, 0.85f)
        ).toPfpShape(rotationDegrees = 45f)
    }

    // 29. Soft Clover (4-Lobe)
    val SoftClover4 = PfpShapeItem("soft_clover_4", "Soft Clover (4-Lobe)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            innerRadius = 0.65f,
            rounding = CornerRounding(0.55f, 0.90f)
        ).toPfpShape()
    }

    // 30. Cookie (16-Wave)
    val Cookie16 = PfpShapeItem("cookie_16", "Cookie (16-Wave)") {
        RoundedPolygon.star(
            numVerticesPerRadius = 16,
            innerRadius = 0.88f,
            rounding = CornerRounding(0.30f, 0.80f)
        ).toPfpShape()
    }

    // Standard Shapes
    val Circle = PfpShapeItem("circle", "Circle (Default)", isExpressive = false) { CircleShape }
    val Square = PfpShapeItem("square", "Square", isExpressive = false) { RoundedCornerShape(18.dp) }

    /**
     * All 32 shapes in organized order.
     */
    val allShapes: List<PfpShapeItem> by lazy {
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

    private val shapesMap: Map<String, PfpShapeItem> by lazy {
        allShapes.associateBy { it.key }
    }

    fun getShapeItem(key: String): PfpShapeItem {
        return shapesMap[key] ?: Circle
    }

    fun getShape(key: String): Shape {
        return (shapesMap[key] ?: Circle).shape
    }

    fun isExpressive(key: String): Boolean {
        return shapesMap[key]?.isExpressive ?: false
    }
}

/**
 * Animated rotating outer shape container with counter-rotated inner photo.
 * Ensures the outer shape mask smoothly rotates 360° over ~14s while the inner contact photo
 * stays upright, stationary, and scaled by 1.18f to avoid unmasked edge gaps.
 */
@Composable
fun RotatingPfpShapeContainer(
    shapeKey: String,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    animateRotation: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isExpressive = PfpShapeRegistry.isExpressive(shapeKey) || (shape != null && shape != CircleShape && shapeKey != "square" && shapeKey != "circle")
    val effectiveShape = shape ?: PfpShapeRegistry.getShape(shapeKey)
    val shouldAnimate = isExpressive && animateRotation

    val angle = if (shouldAnimate) {
        val transition = rememberInfiniteTransition(label = "pfpShapeRotation")
        val rot by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pfpRotationAngle"
        )
        rot
    } else 0f

    Box(
        modifier = modifier
            .then(if (shouldAnimate) Modifier.graphicsLayer { rotationZ = angle } else Modifier)
            .clip(effectiveShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (shouldAnimate) Modifier.graphicsLayer {
                        rotationZ = -angle
                        scaleX = 1.18f
                        scaleY = 1.18f
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
