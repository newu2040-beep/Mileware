package com.example.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnonymousPersona
import com.example.data.model.ConfessionPost
import com.example.data.model.PastelTheme
import com.example.ui.components.AnonymousPersonaHeader
import com.example.ui.components.AudioVoiceRecorderBar
import com.example.ui.components.PastelThemeSelector
import com.example.ui.components.PersonaCustomizerDialog
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.util.HapticsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConfessionScreen(
    viewModel: ConfessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val appPastelTheme by viewModel.appPastelTheme.collectAsState()
    val currentUserId = viewModel.currentUserId

    var contentText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Confessions") }
    var selectedPostType by remember { mutableStateOf("Confession") }
    var selectedPastelTheme by remember { mutableStateOf(appPastelTheme) }
    var selectedWarning by remember { mutableStateOf("None") }
    var commentsEnabled by remember { mutableStateOf(true) }
    var reactionsEnabled by remember { mutableStateOf(true) }
    var audioDurationSec by remember { mutableIntStateOf(0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    var currentPersona by remember(userProfile) {
        mutableStateOf(
            userProfile?.let {
                AnonymousPersona(it.anonymousHandle, it.anonymousAvatar, it.anonymousColor)
            } ?: AnonymousPersona.generateNumbered(currentUserId)
        )
    }

    var showPersonaCustomizer by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            HapticsHelper.playSuccess(context)
            selectedImageUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Anonymous Confession",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticsHelper.playLightTick(context)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (contentText.isNotBlank() && !isPosting) {
                                isPosting = true
                                viewModel.createConfession(
                                    content = contentText,
                                    category = selectedCategory,
                                    postType = selectedPostType,
                                    pastelTheme = selectedPastelTheme,
                                    contentWarning = selectedWarning,
                                    commentsEnabled = commentsEnabled,
                                    reactionsEnabled = reactionsEnabled,
                                    mediaUrl = selectedImageUri?.toString() ?: "",
                                    audioDurationSec = audioDurationSec,
                                    customPersona = currentPersona,
                                    onSuccess = {
                                        isPosting = false
                                        onNavigateBack()
                                    }
                                )
                            }
                        },
                        enabled = contentText.isNotBlank() && !isPosting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post Anonymously")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Anonymous Persona Header & Randomizer
            AnonymousPersonaHeader(
                currentPersona = currentPersona,
                onRollNew = {
                    currentPersona = AnonymousPersona.generateRandom()
                },
                onCustomize = {
                    showPersonaCustomizer = true
                }
            )

            // Category & Type Selectors
            Text(
                text = "Confession Type & Category",
                style = MaterialTheme.typography.labelLarge
            )

            // Post Types row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfessionPost.POST_TYPES.forEach { (type, emoji) ->
                    val isSelected = type == selectedPostType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            selectedPostType = type
                        },
                        leadingIcon = { Text(emoji) },
                        label = { Text(type) }
                    )
                }
            }

            // Category selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfessionPost.CATEGORIES.filter { it != "All" }.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            selectedCategory = cat
                        },
                        label = { Text("#$cat") }
                    )
                }
            }

            // Confession Text Field with dynamic character count
            OutlinedTextField(
                value = contentText,
                onValueChange = {
                    if (it.length <= 3000) contentText = it
                },
                placeholder = {
                    Text(
                        "What is on your mind? Confess a secret, share a personal experience, or ask a question you cannot say out loud...",
                        fontFamily = FontFamily.Serif
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 280.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔒 End-to-end encrypted identity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${contentText.length}/3000",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (contentText.length > 2800) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pastel Theme Selector
            PastelThemeSelector(
                selectedTheme = selectedPastelTheme,
                onThemeSelected = { selectedPastelTheme = it }
            )

            // Audio Voice Recorder
            AudioVoiceRecorderBar(
                recordedDurationSec = audioDurationSec,
                onRecordingFinished = { audioDurationSec = it },
                onRemoveRecording = { audioDurationSec = 0 }
            )

            // Content Warning selector
            Text(
                text = "Content Warning (Optional)",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfessionPost.WARNING_PRESETS.forEach { warning ->
                    val isSelected = warning == selectedWarning
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            selectedWarning = warning
                        },
                        label = { Text(warning) }
                    )
                }
            }

            // Privacy & Interaction Toggles
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Allow Anonymous Comments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Let other users reply anonymously", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = commentsEnabled,
                            onCheckedChange = {
                                HapticsHelper.playLightTick(context)
                                commentsEnabled = it
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Allow Anonymous Reactions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("❤️, 😢, 😂, 😮, 😡, 💭 reactions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = reactionsEnabled,
                            onCheckedChange = {
                                HapticsHelper.playLightTick(context)
                                reactionsEnabled = it
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPersonaCustomizer) {
        PersonaCustomizerDialog(
            initialPersona = currentPersona,
            onDismiss = { showPersonaCustomizer = false },
            onSave = { updated, _ ->
                currentPersona = updated
            }
        )
    }
}
