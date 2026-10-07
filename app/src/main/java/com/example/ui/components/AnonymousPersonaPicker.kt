package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnonymousPersona
import com.example.util.HapticsHelper

@Composable
fun AnonymousPersonaHeader(
    currentPersona: AnonymousPersona,
    onRollNew: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val badgeColor = try {
        Color(android.graphics.Color.parseColor(currentPersona.badgeColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(1.5.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentPersona.avatarEmoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentPersona.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Hidden Identity",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                    }
                }
                Text(
                    text = "100% Anonymous • Real account is never revealed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    HapticsHelper.playClick(context)
                    onRollNew()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Roll Random Identity",
                    tint = badgeColor
                )
            }
        }
    }
}

@Composable
fun PersonaCustomizerDialog(
    initialPersona: AnonymousPersona,
    initialBio: String = "",
    onDismiss: () -> Unit,
    onSave: (AnonymousPersona, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialPersona.name) }
    var selectedEmoji by remember { mutableStateOf(initialPersona.avatarEmoji) }
    var selectedColorHex by remember { mutableStateOf(initialPersona.badgeColorHex) }
    var bio by remember { mutableStateOf(initialBio) }

    val emojis = listOf("🎭", "🌌", "🌙", "✨", "🔮", "🦊", "🦉", "🦋", "🌊", "🪶", "🕯️", "🪐", "🌿", "🌸", "🖤", "☁️", "⚡", "🎲")
    val colors = listOf("#9D7BFF", "#4EE2C0", "#FF6584", "#48CAE4", "#FFB84D", "#7C4DFF", "#00B0FF", "#00E676", "#F57F17", "#E91E63")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Anonymous Persona",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    label = { Text("Anonymous Alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Avatar Icon",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    HapticsHelper.playLightTick(context)
                                    selectedEmoji = emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Text(
                    text = "Aura Color",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    HapticsHelper.playLightTick(context)
                                    selectedColorHex = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 120) bio = it },
                    label = { Text("Anonymous Bio / Vibe") },
                    placeholder = { Text("e.g. Dreaming under the neon moon...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val random = AnonymousPersona.generateRandom()
                        name = random.name
                        selectedEmoji = random.avatarEmoji
                        selectedColorHex = random.badgeColorHex
                        HapticsHelper.playClick(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Roll Random Identity")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    HapticsHelper.playSuccess(context)
                    onSave(
                        AnonymousPersona(
                            name = name.ifBlank { "Anonymous Soul" },
                            avatarEmoji = selectedEmoji,
                            badgeColorHex = selectedColorHex
                        ),
                        bio
                    )
                    onDismiss()
                }
            ) {
                Text("Save Persona")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
