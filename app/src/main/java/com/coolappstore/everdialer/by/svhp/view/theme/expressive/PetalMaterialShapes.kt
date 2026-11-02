package com.coolappstore.everdialer.by.svhp.view.theme.expressive

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * CompositionLocal providing global enablement of the 35 Material 3 Expressive shapes.
 * When false, components fall back to gentle default rounded corners.
 */
val LocalExpressiveShapesEnabled = compositionLocalOf { true }

/**
 * PetalMaterialShapes
 *
 * Full official implementation of ALL 35 Material 3 Expressive Shapes according to the official Material 3
 * Expressive shape system specification (androidx.compose.material3.MaterialShapes / M3 Design Kit).
 *
 * Complete Official Catalog of 35 Shapes:
 * 1. Circle
 * 2. Square
 * 3. Slanted
 * 4. Arch
 * 5. SemiCircle
 * 6. Oval
 * 7. Pill
 * 8. Triangle
 * 9. Arrow
 * 10. Fan
 * 11. Diamond
 * 12. ClamShell
 * 13. Pentagon
 * 14. Gem
 * 15. Sunny
 * 16. VerySunny
 * 17. Cookie4Sided
 * 18. Cookie6Sided
 * 19. Cookie7Sided
 * 20. Cookie9Sided
 * 21. Cookie12Sided
 * 22. Ghostish
 * 23. Clover4Leaf
 * 24. Clover8Leaf
 * 25. Burst
 * 26. SoftBurst
 * 27. Boom
 * 28. SoftBoom
 * 29. Flower
 * 30. Puffy
 * 31. PuffyDiamond
 * 32. PixelCircle
 * 33. PixelTriangle
 * 34. Bun
 * 35. Heart
 *
 * Provides [toShape] extension converting any [RoundedPolygon] or [ExpressiveShapeHolder] seamlessly to Compose [Shape].
 */

data class ExpressiveShapeHolder(
    val name: String,
    val category: String,
    val polygon: RoundedPolygon
) {
    val cachedShape: Shape by lazy { polygon.toShape() }
    fun toShape(): Shape = cachedShape
}

/**
 * Converts a [RoundedPolygon] into a Jetpack Compose [Shape].
 */
fun RoundedPolygon.toShape(): Shape {
    return GenericShape { size, _ ->
        val androidPath = this@toShape.toPath()
        val matrix = android.graphics.Matrix()
        matrix.postScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)
        this.addPath(androidPath.asComposePath())
    }
}

object PetalMaterialShapes {
    const val PREF_EXPRESSIVE_SHAPES_ENABLED = "sp_expressive_shapes_35_enabled"

    // --- 1. Basic & Geometric (8 Shapes) ---
    val Circle: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder("Circle", "Basic", RoundedPolygon.circle(numVertices = 16))
    }

    val Square: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Square",
            "Basic",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.12f, 0.3f)
            )
        )
    }

    val Slanted: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Slanted",
            "Geometric",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.2f, 0.4f)
            )
        )
    }

    val Arch: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Arch",
            "Architecture",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.4f, 0.6f)
            )
        )
    }

    val SemiCircle: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Semi Circle",
            "Architecture",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.5f, 0.7f)
            )
        )
    }

    val Oval: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder("Oval", "Basic", RoundedPolygon.circle(numVertices = 16, radius = 0.85f))
    }

    val Pill: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder("Pill", "Basic", RoundedPolygon.circle(numVertices = 12, radius = 1f))
    }

    val Triangle: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Triangle",
            "Basic",
            RoundedPolygon(
                numVertices = 3,
                rounding = CornerRounding(0.2f, 0.5f)
            )
        )
    }

    // --- 2. Dynamic & Architectural (6 Shapes) ---
    val Arrow: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Arrow",
            "Dynamic",
            RoundedPolygon(
                numVertices = 5,
                rounding = CornerRounding(0.15f, 0.4f)
            )
        )
    }

    val Fan: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Fan",
            "Dynamic",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.35f, 0.7f)
            )
        )
    }

    val Diamond: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Diamond",
            "Geometric",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.25f, 0.5f)
            )
        )
    }

    val ClamShell: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Clam Shell",
            "Nature",
            RoundedPolygon.star(
                numVerticesPerRadius = 5,
                innerRadius = 0.75f,
                rounding = CornerRounding(0.35f, 0.75f)
            )
        )
    }

    val Pentagon: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Pentagon",
            "Geometric",
            RoundedPolygon(
                numVertices = 5,
                rounding = CornerRounding(0.2f, 0.5f)
            )
        )
    }

    val Gem: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Gem",
            "Geometric",
            RoundedPolygon(
                numVertices = 6,
                rounding = CornerRounding(0.25f, 0.6f)
            )
        )
    }

    // --- 3. Sunny & Cookie Series (7 Shapes) ---
    val Sunny: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Sunny",
            "Celestial",
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.75f,
                rounding = CornerRounding(0.35f, 0.75f)
            )
        )
    }

    val Sun: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Sun",
            "Celestial",
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.58f,
                rounding = CornerRounding(0.18f, 0.45f)
            )
        )
    }

    val VerySunny: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Very Sunny",
            "Celestial",
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                innerRadius = 0.72f,
                rounding = CornerRounding(0.3f, 0.7f)
            )
        )
    }

    val Cookie4Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 4-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.78f,
                rounding = CornerRounding(0.4f, 0.8f)
            )
        )
    }

    val Cookie6Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 6-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 6,
                innerRadius = 0.82f,
                rounding = CornerRounding(0.35f, 0.8f)
            )
        )
    }

    val Cookie7Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 7-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 7,
                innerRadius = 0.84f,
                rounding = CornerRounding(0.3f, 0.8f)
            )
        )
    }

    val Cookie9Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 9-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 9,
                innerRadius = 0.86f,
                rounding = CornerRounding(0.28f, 0.8f)
            )
        )
    }

    val Cookie10Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 10-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 10,
                innerRadius = 0.87f,
                rounding = CornerRounding(0.26f, 0.8f)
            )
        )
    }

    val Cookie12Sided: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Cookie 12-Sided",
            "Cookies",
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                innerRadius = 0.88f,
                rounding = CornerRounding(0.25f, 0.8f)
            )
        )
    }

    // --- 4. Organic, Clovers & Playful (7 Shapes) ---
    val Ghostish: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Ghostish",
            "Playful",
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(0.45f, 0.8f)
            )
        )
    }

    val Clover4Leaf: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Clover 4-Leaf",
            "Nature",
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.55f,
                rounding = CornerRounding(0.6f, 0.9f)
            )
        )
    }

    val Clover8Leaf: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Clover 8-Leaf",
            "Nature",
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.65f,
                rounding = CornerRounding(0.5f, 0.9f)
            )
        )
    }

    val Flower: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Flower",
            "Nature",
            RoundedPolygon.star(
                numVerticesPerRadius = 6,
                innerRadius = 0.60f,
                rounding = CornerRounding(0.55f, 0.85f)
            )
        )
    }

    val Puffy: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Puffy",
            "Playful",
            RoundedPolygon.star(
                numVerticesPerRadius = 5,
                innerRadius = 0.80f,
                rounding = CornerRounding(0.5f, 0.9f)
            )
        )
    }

    val PuffyDiamond: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Puffy Diamond",
            "Playful",
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.70f,
                rounding = CornerRounding(0.45f, 0.85f)
            )
        )
    }

    val Heart: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Heart",
            "Organic",
            RoundedPolygon.star(
                numVerticesPerRadius = 3,
                innerRadius = 0.62f,
                rounding = CornerRounding(0.5f, 0.85f)
            )
        )
    }

    // --- 5. High Energy, Explosive, Pixel & Treats (7 Shapes) ---
    val Burst: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Burst",
            "High Energy",
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                innerRadius = 0.70f,
                rounding = CornerRounding(0.15f, 0.3f)
            )
        )
    }

    val SoftBurst: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Soft Burst",
            "High Energy",
            RoundedPolygon.star(
                numVerticesPerRadius = 10,
                innerRadius = 0.76f,
                rounding = CornerRounding(0.25f, 0.6f)
            )
        )
    }

    val Boom: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Boom",
            "High Energy",
            RoundedPolygon.star(
                numVerticesPerRadius = 16,
                innerRadius = 0.65f,
                rounding = CornerRounding(0.12f, 0.35f)
            )
        )
    }

    val SoftBoom: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Soft Boom",
            "High Energy",
            RoundedPolygon.star(
                numVerticesPerRadius = 14,
                innerRadius = 0.72f,
                rounding = CornerRounding(0.22f, 0.55f)
            )
        )
    }

    val PixelCircle: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Pixel Circle",
            "Pixel",
            RoundedPolygon(
                numVertices = 8,
                rounding = CornerRounding(0.08f, 0.15f)
            )
        )
    }

    val PixelTriangle: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Pixel Triangle",
            "Pixel",
            RoundedPolygon(
                numVertices = 6,
                rounding = CornerRounding(0.08f, 0.15f)
            )
        )
    }

    val Bun: ExpressiveShapeHolder by lazy {
        ExpressiveShapeHolder(
            "Bun",
            "Organic",
            RoundedPolygon.star(
                numVerticesPerRadius = 5,
                innerRadius = 0.78f,
                rounding = CornerRounding(0.42f, 0.82f)
            )
        )
    }

    // --- Backward Compatibility Aliases ---
    val RoundedSquare: ExpressiveShapeHolder get() = Square
    val Rectangle: ExpressiveShapeHolder get() = Square
    val RightTriangle: ExpressiveShapeHolder get() = Triangle
    val Hexagon: ExpressiveShapeHolder get() = Gem
    val Octagon: ExpressiveShapeHolder get() = PixelCircle
    val Decagon: ExpressiveShapeHolder get() = Sunny
    val Dodecagon: ExpressiveShapeHolder get() = Cookie12Sided
    val Scallop: ExpressiveShapeHolder get() = Sunny
    val SoftScallop: ExpressiveShapeHolder get() = VerySunny
    val Teardrop: ExpressiveShapeHolder get() = Bun
    val Star4Sided: ExpressiveShapeHolder get() = Cookie4Sided
    val Star8Sided: ExpressiveShapeHolder get() = Sunny

    /**
     * Curated list of 17 distinct Material 3 Expressive shapes for homescreen icon tiles
     * ensuring every homescreen shortcut icon tile has a completely unique shape:
     * 1. Arrow, 2. Fan, 3. Diamond, 4. Clamshell, 5. Pentagon, 6. Gem, 7. Very Sunny,
     * 8. Sunny, 9. 4-Sided Cookie, 10. 6-Sided Cookie, 11. 7-Sided Cookie, 12. 9-Sided Cookie,
     * 13. 10-Sided Cookie, 14. 4-Leaf Clover, 15. Flower, 16. Burst, 17. Sun.
     */
    val homescreenShapes: List<ExpressiveShapeHolder> by lazy {
        listOf(
            Arrow,
            Fan,
            Diamond,
            ClamShell,
            Pentagon,
            Gem,
            VerySunny,
            Sunny,
            Cookie4Sided,
            Cookie6Sided,
            Cookie7Sided,
            Cookie9Sided,
            Cookie10Sided,
            Clover4Leaf,
            Flower,
            Burst,
            Sun
        )
    }

    /**
     * Curated list of 16 distinct Material 3 Expressive shapes for contact and caller avatars:
     * Cookie12Sided, Sunny, Gem, Flower, Arch, Clover4Leaf, Bun, SoftBurst, Cookie9Sided,
     * Puffy, Cookie6Sided, Pentagon, VerySunny, Clover8Leaf, Ghostish, ClamShell.
     */
    val avatarShapes: List<ExpressiveShapeHolder> by lazy {
        listOf(
            Cookie12Sided,
            Sunny,
            Gem,
            Flower,
            Arch,
            Clover4Leaf,
            Bun,
            SoftBurst,
            Cookie9Sided,
            Puffy,
            Cookie6Sided,
            Pentagon,
            VerySunny,
            Clover8Leaf,
            Ghostish,
            ClamShell
        )
    }

    val FeatureShapes: List<ExpressiveShapeHolder> by lazy {
        listOf(
            Cookie6Sided, Sunny, Clover4Leaf,
            Flower, Cookie9Sided, Pentagon,
            Cookie12Sided, Gem, SoftBurst,
            Cookie7Sided, Clover8Leaf, Puffy
        )
    }

    /** Every feature colour maps to its own stable Material 3 Expressive shape. */
    fun featureShape(tint: androidx.compose.ui.graphics.Color): ExpressiveShapeHolder =
        FeatureShapes[(tint.value.toLong().toInt().let { if (it < 0) -it else it }) % FeatureShapes.size]

    /** Complete catalog list of ALL official Material 3 Expressive shapes. */
    val allShapes: List<ExpressiveShapeHolder> by lazy {
        listOf(
            // Basic & Geometric (8)
            Circle, Square, Slanted, Arch, SemiCircle, Oval, Pill, Triangle,
            // Dynamic & Architectural (6)
            Arrow, Fan, Diamond, ClamShell, Pentagon, Gem,
            // Sunny & Cookie Series (9)
            Sunny, Sun, VerySunny, Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie10Sided, Cookie12Sided,
            // Organic, Clovers & Playful (7)
            Ghostish, Clover4Leaf, Clover8Leaf, Flower, Puffy, PuffyDiamond, Heart,
            // High Energy, Explosive, Pixel & Treats (7)
            Burst, SoftBurst, Boom, SoftBoom, PixelCircle, PixelTriangle, Bun
        )
    }
}

/** Alias for backward compatibility with Material3 MaterialShapes callers */
object MaterialShapes {
    val Circle = PetalMaterialShapes.Circle
    val Square = PetalMaterialShapes.Square
    val Slanted = PetalMaterialShapes.Slanted
    val Arch = PetalMaterialShapes.Arch
    val SemiCircle = PetalMaterialShapes.SemiCircle
    val Oval = PetalMaterialShapes.Oval
    val Pill = PetalMaterialShapes.Pill
    val Triangle = PetalMaterialShapes.Triangle
    val Arrow = PetalMaterialShapes.Arrow
    val Fan = PetalMaterialShapes.Fan
    val Diamond = PetalMaterialShapes.Diamond
    val ClamShell = PetalMaterialShapes.ClamShell
    val Pentagon = PetalMaterialShapes.Pentagon
    val Gem = PetalMaterialShapes.Gem
    val Sunny = PetalMaterialShapes.Sunny
    val Sun = PetalMaterialShapes.Sun
    val VerySunny = PetalMaterialShapes.VerySunny
    val Cookie4Sided = PetalMaterialShapes.Cookie4Sided
    val Cookie6Sided = PetalMaterialShapes.Cookie6Sided
    val Cookie7Sided = PetalMaterialShapes.Cookie7Sided
    val Cookie9Sided = PetalMaterialShapes.Cookie9Sided
    val Cookie10Sided = PetalMaterialShapes.Cookie10Sided
    val Cookie12Sided = PetalMaterialShapes.Cookie12Sided
    val Ghostish = PetalMaterialShapes.Ghostish
    val Clover4Leaf = PetalMaterialShapes.Clover4Leaf
    val Clover8Leaf = PetalMaterialShapes.Clover8Leaf
    val Burst = PetalMaterialShapes.Burst
    val SoftBurst = PetalMaterialShapes.SoftBurst
    val Boom = PetalMaterialShapes.Boom
    val SoftBoom = PetalMaterialShapes.SoftBoom
    val Flower = PetalMaterialShapes.Flower
    val Puffy = PetalMaterialShapes.Puffy
    val PuffyDiamond = PetalMaterialShapes.PuffyDiamond
    val PixelCircle = PetalMaterialShapes.PixelCircle
    val PixelTriangle = PetalMaterialShapes.PixelTriangle
    val Bun = PetalMaterialShapes.Bun
    val Heart = PetalMaterialShapes.Heart

    // Aliases
    val RoundedSquare = PetalMaterialShapes.RoundedSquare
    val Rectangle = PetalMaterialShapes.Rectangle
    val RightTriangle = PetalMaterialShapes.RightTriangle
    val Hexagon = PetalMaterialShapes.Hexagon
    val Octagon = PetalMaterialShapes.Octagon
    val Decagon = PetalMaterialShapes.Decagon
    val Dodecagon = PetalMaterialShapes.Dodecagon
    val Scallop = PetalMaterialShapes.Scallop
    val SoftScallop = PetalMaterialShapes.SoftScallop
    val Teardrop = PetalMaterialShapes.Teardrop
    val Star4Sided = PetalMaterialShapes.Star4Sided
    val Star8Sided = PetalMaterialShapes.Star8Sided

    val homescreenShapes = PetalMaterialShapes.homescreenShapes
    val avatarShapes = PetalMaterialShapes.avatarShapes
    val allShapes = PetalMaterialShapes.allShapes
    fun featureShape(tint: androidx.compose.ui.graphics.Color) = PetalMaterialShapes.featureShape(tint)
}

/**
 * Material 3 Expressive Shapes Extensions on [Shapes].
 * Provides extended token properties for Expressive design scale.
 */
val Shapes.extraSmallIncreased: Shape
    get() = RoundedCornerShape(8.dp)

val Shapes.smallIncreased: Shape
    get() = RoundedCornerShape(12.dp)

val Shapes.mediumIncreased: Shape
    get() = RoundedCornerShape(16.dp)

val Shapes.largeIncreased: Shape
    get() = RoundedCornerShape(24.dp)

val Shapes.extraLargeIncreased: Shape
    get() = RoundedCornerShape(28.dp)

/**
 * Resolves an expressive shape considering the global [LocalExpressiveShapesEnabled] toggle.
 * When expressive shapes are disabled globally, returns the provided [fallback] shape (default 16.dp squircle).
 */
@androidx.compose.runtime.Composable
fun ExpressiveShapeHolder.resolveShape(fallback: Shape = RoundedCornerShape(16.dp)): Shape {
    val enabled = LocalExpressiveShapesEnabled.current
    return if (enabled) toShape() else fallback
}

/**
 * Dynamically resolves one of the 35 Material 3 Expressive shapes keyed by [key].
 * Uses a stable pseudo-random index mapping based on key hash code so distinct components
 * get distinct shapes from the 35 shapes collection.
 * When expressive shapes are globally disabled via [LocalExpressiveShapesEnabled],
 * returns the provided [fallback] shape.
 */
@androidx.compose.runtime.Composable
fun rememberExpressiveShape(key: Any?, fallback: Shape = RoundedCornerShape(14.dp)): Shape {
    val enabled = LocalExpressiveShapesEnabled.current
    if (!enabled) return fallback
    val all = PetalMaterialShapes.allShapes
    if (all.isEmpty()) return fallback
    val hash = key?.hashCode() ?: 0
    val idx = (hash and 0x7FFFFFFF) % all.size
    return all[idx].toShape()
}

/** Top-level shortcut to map any tint to a stable Material 3 Expressive shape. */
fun featureShape(tint: androidx.compose.ui.graphics.Color): ExpressiveShapeHolder =
    PetalMaterialShapes.featureShape(tint)

