package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class PastelTheme(
    val id: String,
    val displayName: String,
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val gradientColors: List<Color>,
    val bgHex: String,
    val textHex: String,
    val accentHex: String
) {
    LAVENDER_MIST(
        id = "pastel_lavender",
        displayName = "Lavender Mist",
        backgroundColor = PastelLavenderBg,
        borderColor = PastelLavenderBorder,
        textColor = PastelLavenderText,
        accentColor = PastelLavenderAccent,
        gradientColors = listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB)),
        bgHex = "#EDE7F6",
        textHex = "#4A148C",
        accentHex = "#7C4DFF"
    ),
    ROSE_QUARTZ(
        id = "pastel_rose",
        displayName = "Rose Quartz",
        backgroundColor = PastelRoseBg,
        borderColor = PastelRoseBorder,
        textColor = PastelRoseText,
        accentColor = PastelRoseAccent,
        gradientColors = listOf(Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1)),
        bgHex = "#FCE4EC",
        textHex = "#880E4F",
        accentHex = "#FF4081"
    ),
    SKY_CYAN(
        id = "pastel_sky",
        displayName = "Sky Cyan",
        backgroundColor = PastelSkyBg,
        borderColor = PastelSkyBorder,
        textColor = PastelSkyText,
        accentColor = PastelSkyAccent,
        gradientColors = listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF81D4FA)),
        bgHex = "#E1F5FE",
        textHex = "#01579B",
        accentHex = "#00B0FF"
    ),
    MINT_BREEZE(
        id = "pastel_mint",
        displayName = "Mint Breeze",
        backgroundColor = PastelMintBg,
        borderColor = PastelMintBorder,
        textColor = PastelMintText,
        accentColor = PastelMintAccent,
        gradientColors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7)),
        bgHex = "#E8F5E9",
        textHex = "#1B5E20",
        accentHex = "#00E676"
    ),
    PEACH_SUNSET(
        id = "pastel_peach",
        displayName = "Peach Sunset",
        backgroundColor = PastelPeachBg,
        borderColor = PastelPeachBorder,
        textColor = PastelPeachText,
        accentColor = PastelPeachAccent,
        gradientColors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80)),
        bgHex = "#FFF3E0",
        textHex = "#E65100",
        accentHex = "#FF9100"
    ),
    BUTTERCUP_LEMON(
        id = "pastel_lemon",
        displayName = "Buttercup",
        backgroundColor = PastelLemonBg,
        borderColor = PastelLemonBorder,
        textColor = PastelLemonText,
        accentColor = PastelLemonAccent,
        gradientColors = listOf(Color(0xFFFFFDE7), Color(0xFFFFF9C4), Color(0xFFFFF59D)),
        bgHex = "#FFFDE7",
        textHex = "#F57F17",
        accentHex = "#FFD600"
    ),
    MATCHA_EMERALD(
        id = "pastel_matcha",
        displayName = "Matcha Sage",
        backgroundColor = PastelMatchaBg,
        borderColor = PastelMatchaBorder,
        textColor = PastelMatchaText,
        accentColor = PastelMatchaAccent,
        gradientColors = listOf(Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFC5E1A5)),
        bgHex = "#F1F8E9",
        textHex = "#33691E",
        accentHex = "#7CB342"
    ),
    MIDNIGHT_VELVET(
        id = "pastel_midnight",
        displayName = "Midnight Velvet",
        backgroundColor = PastelMidnightBg,
        borderColor = PastelMidnightBorder,
        textColor = PastelMidnightText,
        accentColor = PastelMidnightAccent,
        gradientColors = listOf(Color(0xFF1E1B2E), Color(0xFF2B2544), Color(0xFF38315B)),
        bgHex = "#1E1B2E",
        textHex = "#E4DFFF",
        accentHex = "#9D7BFF"
    );

    companion object {
        fun fromId(id: String?): PastelTheme {
            return entries.find { it.id == id } ?: LAVENDER_MIST
        }
    }
}
