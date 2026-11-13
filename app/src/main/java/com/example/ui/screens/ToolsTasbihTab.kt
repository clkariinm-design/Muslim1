package com.nuur.muslim.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.ui.theme.AmiriFontFamily
import com.nuur.muslim.ui.theme.NuurMotion

private val presetDhikrList = listOf(
    "سُبْحَانَ ٱللَّهِ",
    "ٱلْحَمْدُ لِلَّهِ",
    "ٱللَّهُ أَكْبَرُ",
    "لَا إِلَٰهَ إِلَّا ٱللَّهُ",
    "أَسْتَغْفِرُ ٱللَّهَ"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TasbihTab(
    settings: UserSettings,
    strings: AppStrings,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onSelectTarget: (Int) -> Unit,
    onSelectLabel: (String) -> Unit,
    onToggleHaptic: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showCustomDialog by remember { mutableStateOf(false) }

    val target = settings.tasbihTarget
    val targetFraction = if (target > 0) {
        (settings.tasbihCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = NuurMotion.gentleSpring,
        label = "tasbihArc"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.tasbihTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${strings.tasbihTodayTotalLabel}: ${settings.tasbihTotalToday}",
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presetDhikrList.forEach { preset ->
                            FilterChip(
                                selected = settings.tasbihLabel == preset,
                                onClick = { onSelectLabel(preset) },
                                label = {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = preset,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            )
                        }
                        FilterChip(
                            selected = settings.tasbihLabel !in presetDhikrList,
                            onClick = { showCustomDialog = true },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(strings.tasbihCustomDhikr) },
                            modifier = Modifier.testTag("tasbih_custom_chip")
                        )
                    }
                }
            }
        }

        // Tactile Circular Tap Ring
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (settings.tasbihHapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onIncrement()
                    }
                    .testTag("tasbih_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 14.dp.toPx())
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = settings.tasbihLabel,
                            fontFamily = AmiriFontFamily,
                            fontSize = 22.sp,
                            color = primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = settings.tasbihCount.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (target > 0) "/ $target" else strings.tasbihUnlimitedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.tasbihTapPrompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                }
            }
        }

        // Target & Haptic Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.tasbihTargetLabel, style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            33 to "33",
                            99 to "99",
                            100 to "100",
                            0 to strings.tasbihUnlimitedLabel
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = settings.tasbihTarget == value,
                                onClick = { onSelectTarget(value) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("tasbih_target_$value")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.tasbihHapticLabel)
                        Switch(
                            checked = settings.tasbihHapticEnabled,
                            onCheckedChange = onToggleHaptic
                        )
                    }

                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tasbih_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.resetLabel)
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var customText by remember { mutableStateOf(settings.tasbihLabel) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(strings.tasbihCustomDhikr) },
            text = {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text(strings.tasbihCustomHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasbih_custom_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectLabel(customText)
                        showCustomDialog = false
                    },
                    enabled = customText.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
