package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AutoDrawMode
import com.example.external.SmartImageProfile
import com.example.ui.components.KagamiAtmosphericBackground
import com.example.ui.components.KagamiCrestEmblem
import com.example.ui.components.KagamiGlassCard
import com.example.ui.components.KagamiPillButton
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiGlassSurface
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSurface
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.SubtleMagenta

@Composable
fun KagamiImageSelectionScreen(
    pureOled: Boolean,
    showParticles: Boolean,
    pendingBitmap: Bitmap?,
    pendingSourceLabel: String,
    smartProfile: SmartImageProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onImagePicked: (Uri?) -> Unit,
    onLoadSampleClicked: () -> Unit,
    onConfirmSelection: (AutoDrawMode) -> Unit,
    onSendToExternalOverlay: () -> Unit,
    onCancelAndReturn: () -> Unit
) {
    BackHandler {
        onCancelAndReturn()
    }

    var selectedInitialMode by remember { mutableStateOf(AutoDrawMode.EDGE_OUTLINE) }
    var showAdvancedFilterList by remember { mutableStateOf(false) }

    // Automatically sync to Smart Auto's recommended mode whenever a new image is analyzed
    LaunchedEffect(smartProfile) {
        if (smartProfile != null) {
            selectedInitialMode = smartProfile.recommendedInternalMode
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onImagePicked(uri)
    }

    val legacyDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImagePicked(uri)
    }

    KagamiAtmosphericBackground(
        pureOled = pureOled,
        showParticles = showParticles,
        modifier = Modifier.testTag("image_selection_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCancelAndReturn,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(KagamiGlassSurface)
                            .border(1.dp, KagamiCardBorder, CircleShape)
                            .testTag("image_select_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KagamiWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SELECT IMAGE & SMART AUTO",
                            style = MaterialTheme.typography.titleLarge,
                            color = KagamiWhite
                        )
                        Text(
                            text = "Select Image → Smart Auto Analysis → Preview → Start",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = KagamiMutedText
                        )
                    }
                }
            }

            // Compact Image Preview Viewport Card
            KagamiGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp),
                cornerRadius = 18.dp,
                glowAccent = pendingBitmap != null,
                contentPadding = PaddingValues(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A0514)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricViolet,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Running Smart Auto image analysis...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LuminousViolet
                                )
                            }
                        }

                        pendingBitmap != null -> {
                            Image(
                                bitmap = pendingBitmap.asImageBitmap(),
                                contentDescription = "Selected Reference Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color(0xD9090514),
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = smartProfile?.summaryLabel ?: pendingSourceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LuminousViolet,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        else -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KagamiCrestEmblem(size = 38.dp)
                                Text(
                                    text = "No Reference Image Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "Pick any gallery photo or tap Shrine Sample below. KAGAMI automatically configures optimal line & contrast settings.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    borderColor = StatusDanger.copy(alpha = 0.6f),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Notice",
                            tint = StatusDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = KagamiWhite
                        )
                    }
                }
            }

            // Picker Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KagamiPillButton(
                    text = "Pick Gallery Photo",
                    icon = Icons.Default.PhotoLibrary,
                    isPrimary = pendingBitmap == null,
                    onClick = {
                        try {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } catch (e: Exception) {
                            legacyDocumentPicker.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("pick_gallery_button")
                )

                KagamiPillButton(
                    text = "Shrine Sample",
                    icon = Icons.Default.AutoAwesome,
                    isPrimary = false,
                    onClick = onLoadSampleClicked,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_sample_button")
                )
            }

            // Smart Auto Badge + Optional "Advanced" Manual Mode Selector
            KagamiGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = LuminousViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "SMART AUTO MODE: ${selectedInitialMode.title.uppercase()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = smartProfile?.summaryLabel
                                        ?: "Automatically optimizes contrast, edges, noise & stroke order",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KagamiSurface,
                            border = BorderStroke(0.8.dp, KagamiCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAdvancedFilterList = !showAdvancedFilterList }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = LuminousViolet,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Advanced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KagamiWhite
                                )
                                Icon(
                                    imageVector = if (showAdvancedFilterList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = KagamiMutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showAdvancedFilterList) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            AutoDrawMode.entries.forEach { mode ->
                                val isSelected = selectedInitialMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) DeepPurple.copy(alpha = 0.42f) else KagamiSurface
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) ElectricViolet else KagamiCardBorder.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedInitialMode = mode }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = mode.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                                                color = KagamiWhite
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = mode.japaneseTag,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = SubtleMagenta
                                            )
                                        }
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                            color = KagamiMutedText
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = LuminousViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action Buttons: Open in Internal Studio OR Send to External / WePlay Overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KagamiPillButton(
                    text = "Cancel",
                    icon = Icons.Default.Close,
                    isPrimary = false,
                    onClick = onCancelAndReturn,
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("cancel_image_button")
                )

                KagamiPillButton(
                    text = "External / WePlay",
                    icon = Icons.Default.Layers,
                    isPrimary = false,
                    enabled = pendingBitmap != null && !isLoading,
                    onClick = onSendToExternalOverlay,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("send_to_external_overlay_button")
                )

                KagamiPillButton(
                    text = "Open Studio",
                    icon = Icons.Default.Check,
                    isPrimary = true,
                    enabled = pendingBitmap != null && !isLoading,
                    onClick = { onConfirmSelection(selectedInitialMode) },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("confirm_image_button")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
