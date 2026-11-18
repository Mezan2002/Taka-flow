package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BadgeDefinition
import com.example.ui.theme.LocalGlassPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LiquidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val P = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(P.bgTop),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(P.bgTop, P.bgMid, P.bgBottom)
                )
            )
            // Subtle architectural corner glow without washing out punchy bento blocks
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(P.auraCyan, Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.08f),
                    radius = size.width * 0.65f,
                ),
                center = Offset(size.width * 0.9f, size.height * 0.08f),
                radius = size.width * 0.65f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(P.auraAmber, Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.85f),
                    radius = size.width * 0.6f,
                ),
                center = Offset(size.width * 0.1f, size.height * 0.85f),
                radius = size.width * 0.6f,
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    tint: Color? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val P = LocalGlassPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val fill = tint ?: P.glassSurface
    val borderColor = if (tint != null) {
        Color.Transparent
    } else if (P.isDark) {
        Color(0x1EFFFFFF)
    } else {
        Color(0x14000000)
    }

    val cardModifier = modifier
        .shadow(
            elevation = if (P.isDark) 6.dp else 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (P.isDark) 0.45f else 0.12f),
            spotColor = Color.Black.copy(alpha = if (P.isDark) 0.45f else 0.12f),
        )
        .clip(shape)
        .background(fill)
        .let { if (borderColor != Color.Transparent) it.border(1.dp, borderColor, shape) else it }
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(padding)

    Column(modifier = cardModifier, content = content)
}

@Composable
fun CircleActionBadge(
    icon: ImageVector = Icons.Filled.NorthEast,
    contentDescription: String? = null,
    containerColor: Color,
    iconTint: Color,
    size: Dp = 42.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
fun PillTag(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/**
 * Bold Acid-Lime Semi-Circular Speedometer Budget Gauge inspired by Image 1's Monthly Limit card.
 */
@Composable
fun SemiCircularBudgetGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF111216),
    tickColor: Color = Color(0x66111216),
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val strokeWidth = 22.dp.toPx()
        val pad = strokeWidth / 2f + 6.dp.toPx()
        val arcWidth = size.width - pad * 2f
        val arcHeight = arcWidth
        val topLeft = Offset(pad, size.height - arcHeight / 2f - 10.dp.toPx())
        val arcSize = Size(arcWidth, arcHeight)
        val center = Offset(topLeft.x + arcWidth / 2f, topLeft.y + arcHeight / 2f)
        val radius = arcWidth / 2f

        // Radial hatched ticks along the full 180-degree semi-circle
        val tickCount = 34
        for (i in 0..tickCount) {
            val angleDeg = 180.0 + (180.0 * i / tickCount)
            val angleRad = angleDeg * PI / 180.0
            val innerR = radius - strokeWidth * 0.46f
            val outerR = radius + strokeWidth * 0.46f
            val p1 = Offset(
                center.x + (innerR * cos(angleRad)).toFloat(),
                center.y + (innerR * sin(angleRad)).toFloat(),
            )
            val p2 = Offset(
                center.x + (outerR * cos(angleRad)).toFloat(),
                center.y + (outerR * sin(angleRad)).toFloat(),
            )
            drawLine(color = tickColor, start = p1, end = p2, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        // Inner soft gradient fan under the active arc
        if (clamped > 0.01f) {
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, trackColor.copy(alpha = 0.22f)),
                    center = center,
                    radius = radius,
                ),
                startAngle = 180f,
                sweepAngle = 180f * clamped,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
            )
        }

        // Thick solid dark progress arc
        if (clamped > 0.005f) {
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        // Bold central pointer arrow pointing toward the current progress angle
        val pointerAngleRad = (180.0 + 180.0 * clamped) * PI / 180.0
        val tipR = radius * 0.44f
        val baseR = radius * 0.14f
        val sideAngle = 0.34
        val tip = Offset(
            center.x + (tipR * cos(pointerAngleRad)).toFloat(),
            center.y + (tipR * sin(pointerAngleRad)).toFloat(),
        )
        val left = Offset(
            center.x + (baseR * cos(pointerAngleRad - sideAngle)).toFloat(),
            center.y + (baseR * sin(pointerAngleRad - sideAngle)).toFloat(),
        )
        val right = Offset(
            center.x + (baseR * cos(pointerAngleRad + sideAngle)).toFloat(),
            center.y + (baseR * sin(pointerAngleRad + sideAngle)).toFloat(),
        )
        val needle = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(center.x, center.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(needle, color = trackColor)
    }
}

@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val P = LocalGlassPalette.current
    val activeFill = accent ?: P.primary
    val bg = if (selected) activeFill else P.glassChip
    val textColor = if (selected) contrastInkFor(activeFill) else P.subInk

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    icon: ImageVector? = null,
) {
    val P = LocalGlassPalette.current
    val fill = containerColor ?: if (P.isDark) P.heroYellow else P.obsidian
    val contentCol = contrastInkFor(fill)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = contentCol,
            disabledContainerColor = fill.copy(alpha = 0.35f),
            disabledContentColor = contentCol.copy(alpha = 0.6f),
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
) {
    val P = LocalGlassPalette.current
    val col = tint ?: P.ink
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(50),
        color = P.glassChip,
        border = BorderStroke(1.dp, if (P.isDark) Color(0x22FFFFFF) else Color(0x18000000)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = col, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = col,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    val P = LocalGlassPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = P.mutedInk) }
        } else null,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = P.glassChip,
            unfocusedContainerColor = P.glassChip.copy(alpha = 0.75f),
            focusedBorderColor = P.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = P.ink,
            unfocusedTextColor = P.ink,
            focusedLabelColor = P.primary,
            unfocusedLabelColor = P.subInk,
            cursorColor = P.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

enum class BottomNavTab(val route: String, val label: String) {
    HOME("home", "HOME"),
    HISTORY("history", "HISTORY"),
    PLANS("plans", "PLANS"),
    SETTINGS("settings", "SETTINGS"),
}

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onSelectTab: (BottomNavTab) -> Unit,
    onOpenQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(36.dp))
                .clip(RoundedCornerShape(36.dp))
                .background(P.navBarBg)
                .border(
                    1.dp,
                    if (P.isDark) Color(0x26FFFFFF) else Color(0x16000000),
                    RoundedCornerShape(36.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTabItem(
                tab = BottomNavTab.HOME,
                selected = currentRoute == BottomNavTab.HOME.route,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onSelectTab(BottomNavTab.HOME) },
                modifier = Modifier.weight(1f).testTag("nav_home"),
            )
            NavTabItem(
                tab = BottomNavTab.HISTORY,
                selected = currentRoute == BottomNavTab.HISTORY.route,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                onClick = { onSelectTab(BottomNavTab.HISTORY) },
                modifier = Modifier.weight(1f).testTag("nav_history"),
            )
            // Punchy Acid-Lime + Electric-Yellow center '+' orb
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(52.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(P.heroYellow, P.acidLime))
                    )
                    .clickable(onClick = onOpenQuickAdd)
                    .testTag("nav_add_fab"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add new entry",
                    tint = P.obsidian,
                    modifier = Modifier.size(28.dp),
                )
            }
            NavTabItem(
                tab = BottomNavTab.PLANS,
                selected = currentRoute == BottomNavTab.PLANS.route,
                selectedIcon = Icons.Filled.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                onClick = { onSelectTab(BottomNavTab.PLANS) },
                modifier = Modifier.weight(1f).testTag("nav_plans"),
            )
            NavTabItem(
                tab = BottomNavTab.SETTINGS,
                selected = currentRoute == BottomNavTab.SETTINGS.route,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                onClick = { onSelectTab(BottomNavTab.SETTINGS) },
                modifier = Modifier.weight(1f).testTag("nav_settings"),
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: BottomNavTab,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    val pillBg = if (selected) P.navPill else Color.Transparent
    val iconColor = if (selected) contrastInkFor(P.navPill) else P.subInk

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(28.dp))
            .background(pillBg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = iconColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun SwipeableGlassRow(
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val P = LocalGlassPalette.current
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { (if (onEdit != null) 112.dp else 64.dp).toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onEdit != null) {
                IconButton(
                    onClick = {
                        offsetX = 0f
                        onEdit()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(P.electricViolet),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit item", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(
                onClick = {
                    offsetX = 0f
                    onDelete()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(P.coralRed),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX < -maxOffsetPx * 0.45f) -maxOffsetPx else 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-maxOffsetPx, 0f)
                        },
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
fun UndoGlassBanner(
    message: String?,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        if (message != null) {
            GlassCard(
                tint = P.obsidian,
                cornerRadius = 20.dp,
                padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = {
                            onUndo()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("undo_action_button"),
                    ) {
                        Text("UNDO", color = P.heroYellow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeUnlockGlassToast(
    badge: BadgeDefinition?,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    AnimatedVisibility(
        visible = badge != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        if (badge != null) {
            GlassCard(
                tint = P.acidLime,
                cornerRadius = 22.dp,
                padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(P.obsidian),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconForKey(badge.iconKey),
                            contentDescription = badge.title,
                            tint = P.heroYellow,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("BADGE UNLOCKED", style = MaterialTheme.typography.labelSmall, color = P.obsidian)
                        Text(badge.title, style = MaterialTheme.typography.titleMedium, color = P.obsidian)
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

fun contrastInkFor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF111216) else Color(0xFFFFFFFF)

fun directionArrow(isExpense: Boolean): ImageVector =
    if (isExpense) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived

fun iconForKey(key: String): ImageVector = when (key.lowercase()) {
    "restaurant", "food" -> Icons.Filled.Restaurant
    "directions_car", "transport" -> Icons.Filled.DirectionsCar
    "shopping_bag", "shopping" -> Icons.Filled.ShoppingBag
    "receipt_long", "bills" -> Icons.Filled.ReceiptLong
    "medical_services", "health" -> Icons.Filled.MedicalServices
    "movie", "entertainment" -> Icons.Filled.Movie
    "school", "education" -> Icons.Filled.School
    "payments", "salary", "cash" -> Icons.Filled.Payments
    "laptop_mac", "freelance" -> Icons.Filled.LaptopMac
    "card_giftcard", "bonus" -> Icons.Filled.CardGiftcard
    "account_balance_wallet", "wallet" -> Icons.Filled.AccountBalanceWallet
    "credit_card", "card" -> Icons.Filled.CreditCard
    "phone", "mobile" -> Icons.Filled.PhoneAndroid
    "savings", "bank" -> Icons.Filled.Savings
    "coffee", "cafe" -> Icons.Filled.LocalCafe
    "bolt" -> Icons.Filled.Bolt
    "mic" -> Icons.Filled.Mic
    "flame" -> Icons.Filled.Whatshot
    "crown", "trophy" -> Icons.Filled.EmojiEvents
    "target" -> Icons.Filled.Flag
    "handshake" -> Icons.Filled.Handshake
    "repeat" -> Icons.Filled.Repeat
    "star" -> Icons.Filled.Star
    "heart" -> Icons.Filled.Favorite
    "sparkles" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Category
}
