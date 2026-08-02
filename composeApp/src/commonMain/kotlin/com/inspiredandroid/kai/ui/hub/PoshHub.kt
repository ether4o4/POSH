package com.inspiredandroid.kai.ui.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
/**
 * POSH's home — a "command deck" hub that replaces the old chat-first launch screen.
 * Shows live system attributes (RAM/disk/CPU) as HUD gauges and routes to the app's
 * surfaces from a tile grid. Monospace, near-black, red-accented — POSH's own identity.
 */
@Composable
fun PoshHub(
    onOpenChat: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val dim = cs.onSurface.copy(alpha = 0.5f)
    val faint = cs.onSurface.copy(alpha = 0.28f)
    val hair = cs.onSurface.copy(alpha = 0.12f)
    val track = cs.onSurface.copy(alpha = 0.10f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            // All four insets, not just the status bar: in landscape the navigation
            // bar and the camera cutout sit along the *sides*, so without these the
            // hub draws underneath them and the edges are unreachable.
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cap the content width so a landscape/tablet window doesn't stretch the HUD
        // across the whole screen; same 900.dp ceiling the settings screen uses.
        Column(
            modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(16.dp),
        ) {
            // ---- header ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text("P", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 30.sp, color = cs.onBackground, letterSpacing = 3.sp)
                        Text("O", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 30.sp, color = cs.primary, letterSpacing = 3.sp)
                        Text("SH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 30.sp, color = cs.onBackground, letterSpacing = 3.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "PROMPT-ORCHESTRATED SHELL HUB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        color = dim,
                        letterSpacing = 2.5.sp,
                    )
                }
                Box(
                    modifier = Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(cs.primary),
                )
                Spacer(Modifier.size(6.dp))
                Text("ONLINE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = cs.primary, letterSpacing = 1.5.sp)
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("ATTRIBUTES", "SYS·01", dim, faint, hair)
            Spacer(Modifier.height(10.dp))

            // ---- gauges (static placeholders for v1; live system stats wired in a follow-up) ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GaugeArc(Modifier.weight(1f), 0.62f, "RAM", "5G", cs.primary, track, cs.onSurface, dim)
                GaugeArc(Modifier.weight(1f), 0.47f, "DISK", "45G", cs.primary, track, cs.onSurface, dim)
                InfoCell(Modifier.weight(1f), "CPU", "8C", "cores", cs.onSurface, dim, faint)
                InfoCell(Modifier.weight(1f), "MODEL", "IDLE", "on-device", cs.onSurface, dim, faint)
            }

            Spacer(Modifier.height(18.dp))

            // ---- core panel ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.surface)
                    .border(1.dp, hair, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(cs.primary))
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("POSH CORE", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurface, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("Alpine proot shell live · engines ready", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = dim)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("CONSOLE", "NAV·02", dim, faint, hair)
            Spacer(Modifier.height(10.dp))

            // ---- tiles ----
            val tiles = listOf(
                HubTile("◉", "CHAT", "talk to the AI", onOpenChat),
                HubTile("▚", "TERMINAL", "the live shell", onOpenTerminal),
                HubTile("⬡", "MODELS", "gguf on-device", onOpenModels),
                HubTile("✦", "SKILLS", "create · equip", onOpenSkills),
                HubTile("⌘", "PROJECTS", "start · resume", onOpenProjects),
                HubTile("▤", "FILES", "the sandbox", onOpenSettings),
                HubTile("✜", "MEMORY", "what it knows", onOpenMemory),
                HubTile("◈", "CONFIG", "persona · engine", onOpenConfig),
                HubTile("⚙", "SETTINGS", "sandbox · tools", onOpenSettings),
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Add columns as the window widens instead of stretching three tiles
                // across a landscape screen. Never drops below the portrait count of 3.
                val columns = when {
                    maxWidth >= 700.dp -> 5
                    maxWidth >= 520.dp -> 4
                    else -> 3
                }
                Column {
                    tiles.chunked(columns).forEach { rowTiles ->
                        Row(
                            // IntrinsicSize.Min keeps every tile in a row the same height
                            // even when their hint text wraps differently.
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowTiles.forEach { t ->
                                TileView(Modifier.weight(1f).fillMaxHeight(), t, cs, dim, hair)
                            }
                            // Hold the grid open on a short final row so its tiles stay
                            // the same width as the rows above instead of stretching.
                            repeat(columns - rowTiles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "POSH · IMMUTABLE ORDER · v0.9",
                modifier = Modifier.fillMaxWidth(),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = faint,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private data class HubTile(val glyph: String, val name: String, val hint: String, val onClick: () -> Unit)

@Composable
private fun SectionLabel(label: String, index: String, dim: Color, faint: Color, hair: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = dim, letterSpacing = 3.sp)
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(hair))
        Spacer(Modifier.size(10.dp))
        Text(index, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = faint, letterSpacing = 1.sp)
    }
}

@Composable
private fun GaugeArc(
    modifier: Modifier,
    pct: Float,
    cap: String,
    value: String,
    accent: Color,
    track: Color,
    onc: Color,
    dim: Color,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = 5.dp.toPx()
                val d = size.minDimension - sw
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arcSize = Size(d, d)
                drawArc(track, -90f, 360f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(accent, -90f, 360f * pct.coerceIn(0f, 1f), false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = onc)
        }
        Spacer(Modifier.height(6.dp))
        Text(cap, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = dim, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun InfoCell(
    modifier: Modifier,
    cap: String,
    value: String,
    sub: String,
    onc: Color,
    dim: Color,
    faint: Color,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onc)
        }
        Spacer(Modifier.height(6.dp))
        Text(cap, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = dim, letterSpacing = 1.5.sp)
        Text(sub, fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = faint)
    }
}

@Composable
private fun TileView(
    modifier: Modifier,
    tile: HubTile,
    cs: androidx.compose.material3.ColorScheme,
    dim: Color,
    hair: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surface)
            .border(1.dp, hair, RoundedCornerShape(12.dp))
            .clickable { tile.onClick() }
            .padding(12.dp),
    ) {
        Column {
            Text(tile.glyph, fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = cs.primary)
            Spacer(Modifier.height(9.dp))
            Text(tile.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurface, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(tile.hint, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = dim)
        }
    }
}
