package com.coolappstore.everdialer.by.svhp.view.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.coolappstore.everdialer.by.svhp.R
import java.io.File

object AppFontConstants {
    const val FONT_ID_SYSTEM = "system"
    const val FONT_ID_PLAYWRITE = "playwrite_us_modern"
    const val FONT_ID_COMFORTAA = "comfortaa"
    const val FONT_ID_PATRICK_HAND = "patrick_hand"
    const val FONT_ID_GOCHI_HAND = "gochi_hand"

    val OFL_1_1_LICENSE_TEXT = """
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide development of collaborative font projects, to support the font creation efforts of academic and linguistic communities, and to provide a free and open framework in which fonts may be shared and improved in partnership with others.

The OFL allows the licensed fonts to be used, studied, modified and redistributed freely as long as they are not sold by themselves. The fonts, including any derivative works, can be bundled, embedded, redistributed and/or sold with any software provided that any reserved names are not used by derivative works. The fonts and derivatives, however, cannot be released under any other type of license. The requirement for fonts to remain under this license does not apply to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright Holder(s) under this license and clearly marked as such. This may include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the copyright statement(s).

"Original Version" refers to the collection of Font Software components as distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting, or substituting -- in part or in whole -- any of the components of the Original Version, by changing formats or by porting the Font Software to a new environment.

"Author" refers to any designer, engineer, programmer, technical writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining a copy of the Font Software, to use, study, copy, merge, embed, modify, redistribute, and sell modified and unmodified copies of the Font Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components, in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled, redistributed and/or sold with any software, provided that each copy contains the above copyright notice and this license. These can be included either as stand-alone text files, human-readable headers or in the appropriate machine-readable metadata fields within text or binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font Name(s) unless explicit written permission is granted by the corresponding Copyright Holder. This restriction only applies to the primary font name as presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font Software shall not be used to promote, endorse or advertise any Modified Version, except to acknowledge the contribution(s) of the Copyright Holder(s) and the Author(s) or with their explicit written permission.

5) The Font Software, modified or unmodified, in part or in whole, must be distributed entirely under this license, and must not be distributed under any other license. The requirement for fonts to remain under this license does not apply to any document created using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM OTHER DEALINGS IN THE FONT SOFTWARE.
""".trimIndent()
}

val PlaywriteUsModernFontFamily = FontFamily(
    Font(R.font.playwrite_us_modern_thin, FontWeight.Thin),
    Font(R.font.playwrite_us_modern_extralight, FontWeight.ExtraLight),
    Font(R.font.playwrite_us_modern_light, FontWeight.Light),
    Font(R.font.playwrite_us_modern_regular, FontWeight.Normal),
    Font(R.font.playwrite_us_modern_regular, FontWeight.Medium),
    Font(R.font.playwrite_us_modern_regular, FontWeight.SemiBold),
    Font(R.font.playwrite_us_modern_regular, FontWeight.Bold)
)

val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa_light, FontWeight.Light),
    Font(R.font.comfortaa_regular, FontWeight.Normal),
    Font(R.font.comfortaa_medium, FontWeight.Medium),
    Font(R.font.comfortaa_semibold, FontWeight.SemiBold),
    Font(R.font.comfortaa_bold, FontWeight.Bold)
)

val PatrickHandFontFamily = FontFamily(
    Font(R.font.patrick_hand_regular, FontWeight.Normal)
)

val GochiHandFontFamily = FontFamily(
    Font(R.font.gochi_hand_regular, FontWeight.Normal)
)

data class AppFontItem(
    val id: String,
    val name: String,
    val styleTag: String,
    val designer: String,
    val about: String,
    val upstreamUrl: String,
    val copyrightNotice: String,
    val licenseName: String,
    val licenseText: String,
    val fontFamily: FontFamily
)

val BuiltInAppFonts: List<AppFontItem> = listOf(
    AppFontItem(
        id = AppFontConstants.FONT_ID_SYSTEM,
        name = "System (Default)",
        styleTag = "Default Android Typeface",
        designer = "Google / Device Manufacturer",
        about = "The default typography provided by the Android OS (such as Roboto or Google Sans, matching your system settings and device theme).",
        upstreamUrl = "https://source.android.com",
        copyrightNotice = "Android Open Source Project / Device System",
        licenseName = "Apache License 2.0 / System Default",
        licenseText = "Standard Android open source system typeface provided by your operating system.",
        fontFamily = FontFamily.Default
    ),
    AppFontItem(
        id = AppFontConstants.FONT_ID_PLAYWRITE,
        name = "Playwrite USA Modern",
        styleTag = "Elegant Cursive Script",
        designer = "TypeTogether, Veronika Burian, José Scaglione",
        about = """
In the absence of a unified national approach, elementary education in the United States is supported by several private companies that espouse different approaches to teaching handwriting. According to American handwriting expert Kate Gladstone, it is difficult to confidently determine which handwriting models are most commonly taught in the country, but among the most widespread are Zaner-Bloser and D’Nealian, both following a traditional style, and Handwriting Without Tears. Two other programs featuring modern cursive models — Getty-Dubay and Barchowsky Fluent Hand — are rapidly gaining interest as writing models not only for children but also for adults seeking to improve their handwriting.

Playwrite USA Modern is a variable font with a weight range from Thin (100) to Regular (400), and supports over 150 Latin-based languages. It has a single-weight sibling with Guides, Playwrite US Modern Guides, designed to harmonize seamlessly with this main font while providing a visual aid for primary school children.

To contribute, see github.com/TypeTogether/Playwrite.
        """.trimIndent(),
        upstreamUrl = "https://fonts.google.com/specimen/Playwrite+US+Modern",
        copyrightNotice = "Copyright 2023 The Playwrite Project Authors (https://github.com/TypeTogether/Playwrite)",
        licenseName = "SIL Open Font License, Version 1.1",
        licenseText = AppFontConstants.OFL_1_1_LICENSE_TEXT,
        fontFamily = PlaywriteUsModernFontFamily
    ),
    AppFontItem(
        id = AppFontConstants.FONT_ID_COMFORTAA,
        name = "Comfortaa",
        styleTag = "Rounded Geometric Sans",
        designer = "Johan Aakerlund",
        about = """
Comfortaa is a rounded geometric sans-serif type design intended for large sizes. It is absolutely free, both for personal and commercial use.

If you like it please visit my DeviantArt page and fav it (but obviously only if you like it.) You are also more than welcome to comment about anything you want (I'm open to critique). I obviously would love to see how my font is being used, so feel free to comment with a link to your work, or send me a message.

I hope you will enjoy using my font!
        """.trimIndent(),
        upstreamUrl = "https://fonts.google.com/specimen/Comfortaa",
        copyrightNotice = "Copyright 2011 The Comfortaa Project Authors (https://github.com/alexeiva/comfortaa), with Reserved Font Name \"Comfortaa\".",
        licenseName = "SIL Open Font License, Version 1.1",
        licenseText = AppFontConstants.OFL_1_1_LICENSE_TEXT,
        fontFamily = ComfortaaFontFamily
    ),
    AppFontItem(
        id = AppFontConstants.FONT_ID_PATRICK_HAND,
        name = "Patrick Hand",
        styleTag = "Warm Handwriting Script",
        designer = "Patrick Wagesreiter",
        about = """
Patrick Hand is a font based on the designer's own handwriting. It is developed to bring an impressive and useful handwriting effect to your texts.

It has all the basic latin characters as well as most of the latin extended ones. It also includes some fancy glyphs like heavy quotation marks and the floral heart! Ligatures, small caps and old style numbers are available as OpenType features in the downloaded version of this font.
Updated with support for more European languages and Vietnamese, and a Small Caps sister family.
        """.trimIndent(),
        upstreamUrl = "https://fonts.google.com/specimen/Patrick+Hand",
        copyrightNotice = "Copyright (c) 2010-2012 Patrick Wagesreiter (mail@patrickwagesreiter.at)",
        licenseName = "SIL Open Font License, Version 1.1",
        licenseText = AppFontConstants.OFL_1_1_LICENSE_TEXT,
        fontFamily = PatrickHandFontFamily
    ),
    AppFontItem(
        id = AppFontConstants.FONT_ID_GOCHI_HAND,
        name = "Gochi Hand",
        styleTag = "Spontaneous Casual Script",
        designer = "HT Fonts, Juan Pablo del Peral",
        about = """
Gochi Hand is a typographic interpretation of the handwriting of a teenager. The style is fresh, not like the letters made by a calligrapher, but those of an ordinary person. The text line is spontaneous but solid and consistent, expressive and works well on screen, even in small sizes. The glyphs were carefully designed with a good curve quality that makes it able to look good when printed too.

Designed by Juan Pablo del Peral for HT Fonts. To contribute, see github.com/huertatipografica/gochi-hand.
        """.trimIndent(),
        upstreamUrl = "https://fonts.google.com/specimen/Gochi+Hand",
        copyrightNotice = "Copyright (c) 2011, Juan Pablo del Peral (juandelperal@htfonts.com), with Reserved Font Names \"Gochi\" and \"Gochi Hand\"",
        licenseName = "SIL Open Font License, Version 1.1",
        licenseText = AppFontConstants.OFL_1_1_LICENSE_TEXT,
        fontFamily = GochiHandFontFamily
    )
)

object AppFontHelper {
    fun resolveFontFamily(fontKeyOrPath: String?): FontFamily {
        if (fontKeyOrPath.isNullOrEmpty() || fontKeyOrPath == AppFontConstants.FONT_ID_SYSTEM) {
            return FontFamily.Default
        }
        return when (fontKeyOrPath) {
            AppFontConstants.FONT_ID_PLAYWRITE -> PlaywriteUsModernFontFamily
            AppFontConstants.FONT_ID_COMFORTAA -> ComfortaaFontFamily
            AppFontConstants.FONT_ID_PATRICK_HAND -> PatrickHandFontFamily
            AppFontConstants.FONT_ID_GOCHI_HAND -> GochiHandFontFamily
            else -> {
                val file = File(fontKeyOrPath)
                if (file.exists()) {
                    try {
                        FontFamily(Typeface.createFromFile(file))
                    } catch (_: Exception) {
                        FontFamily.Default
                    }
                } else {
                    FontFamily.Default
                }
            }
        }
    }

    fun getFontDisplayName(fontKeyOrPath: String?): String {
        if (fontKeyOrPath.isNullOrEmpty() || fontKeyOrPath == AppFontConstants.FONT_ID_SYSTEM) {
            return "System (Default)"
        }
        return when (fontKeyOrPath) {
            AppFontConstants.FONT_ID_PLAYWRITE -> "Playwrite USA Modern"
            AppFontConstants.FONT_ID_COMFORTAA -> "Comfortaa"
            AppFontConstants.FONT_ID_PATRICK_HAND -> "Patrick Hand"
            AppFontConstants.FONT_ID_GOCHI_HAND -> "Gochi Hand"
            else -> {
                val file = File(fontKeyOrPath)
                if (file.exists()) {
                    "Custom (${file.name})"
                } else {
                    "System (Default)"
                }
            }
        }
    }
}
