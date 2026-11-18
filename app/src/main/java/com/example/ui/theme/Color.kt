package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFF9BF063)
val PurpleGrey80 = Color(0xFF94A3B8)
val Pink80 = Color(0xFFFCEB4E)
val Purple40 = Color(0xFF6741FF)
val PurpleGrey40 = Color(0xFF475569)
val Pink40 = Color(0xFFDF574C)

// Signature punchy color-block tokens inspired by the reference fintech & bento designs
val ElectricYellow = Color(0xFFFCEB4E)
val AcidLime = Color(0xFF9BF063)
val SoftPistachio = Color(0xFFB8CE68)
val ElectricViolet = Color(0xFF6741FF)
val CoralCrimson = Color(0xFFDF574C)
val WarmMarigold = Color(0xFFF8C957)
val ObsidianInk = Color(0xFF111216)
val CharcoalSurface = Color(0xFF1C1D22)
val CreamCanvas = Color(0xFFF4F1EA)

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val auraCyan: Color,
    val auraEmerald: Color,
    val auraAmber: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val glassModal: Color,
    val glassChip: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val ink: Color,
    val subInk: Color,
    val mutedInk: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val income: Color,
    val onIncome: Color,
    val expense: Color,
    val onExpense: Color,
    val warning: Color,
    val transfer: Color,
    val gold: Color,
    val navBarBg: Color,
    val navPill: Color,
    // Bold Color-Block Bento tokens
    val heroYellow: Color = ElectricYellow,
    val acidLime: Color = AcidLime,
    val electricViolet: Color = ElectricViolet,
    val coralRed: Color = CoralCrimson,
    val marigold: Color = WarmMarigold,
    val obsidian: Color = ObsidianInk,
    val pureWhite: Color = Color.White,
)

val DarkGlassPalette = GlassPalette(
    isDark = true,
    bgTop = Color(0xFF0A0A0D),
    bgMid = Color(0xFF0E0F13),
    bgBottom = Color(0xFF08080A),
    auraCyan = Color(0x186741FF),
    auraEmerald = Color(0x149BF063),
    auraAmber = Color(0x14FCEB4E),
    glassSurface = Color(0xFF18191E),
    glassElevated = Color(0xFF22232A),
    glassModal = Color(0xFF14151A),
    glassChip = Color(0xFF23242C),
    glassBorderTop = Color(0x2EFFFFFF),
    glassBorderBottom = Color(0x12FFFFFF),
    ink = Color(0xFFFAFAFA),
    subInk = Color(0xFFA1A1AA),
    mutedInk = Color(0xFF71717A),
    primary = ElectricYellow,
    onPrimary = ObsidianInk,
    secondary = ElectricViolet,
    income = AcidLime,
    onIncome = ObsidianInk,
    expense = CoralCrimson,
    onExpense = Color.White,
    warning = WarmMarigold,
    transfer = Color(0xFF7C8BFF),
    gold = ElectricYellow,
    navBarBg = Color(0xF516171C),
    navPill = ElectricYellow,
)

val LightGlassPalette = GlassPalette(
    isDark = false,
    bgTop = CreamCanvas,
    bgMid = Color(0xFFF7F5EF),
    bgBottom = Color(0xFFEFECE2),
    auraCyan = Color(0x166741FF),
    auraEmerald = Color(0x169BF063),
    auraAmber = Color(0x1AFCEB4E),
    glassSurface = Color(0xFFFFFFFF),
    glassElevated = Color(0xFFFFFFFF),
    glassModal = Color(0xFFFCFBF8),
    glassChip = Color(0xFFEBE7DC),
    glassBorderTop = Color(0x22000000),
    glassBorderBottom = Color(0x14000000),
    ink = ObsidianInk,
    subInk = Color(0xFF52525B),
    mutedInk = Color(0xFF71717A),
    primary = ObsidianInk,
    onPrimary = Color.White,
    secondary = ElectricViolet,
    income = Color(0xFF16A34A),
    onIncome = Color.White,
    expense = CoralCrimson,
    onExpense = Color.White,
    warning = Color(0xFFD97706),
    transfer = ElectricViolet,
    gold = Color(0xFFCA8A04),
    navBarBg = Color(0xF8FFFFFF),
    navPill = ObsidianInk,
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }
