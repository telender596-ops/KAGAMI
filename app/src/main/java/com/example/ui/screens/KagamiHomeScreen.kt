package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.KagamiProject
import com.example.data.KagamiSettings
import com.example.engine.AutoDrawMode
import com.example.external.KagamiExternalDrawingManager
import com.example.ui.components.KagamiAtmosphericBackground
import com.example.ui.components.KagamiCrestEmblem
import com.example.ui.components.KagamiGlassCard
import com.example.ui.components.KagamiPillButton
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiDimText
import com.example.ui.theme.KagamiGlassSurface
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSubtleBorder
import com.example.ui.theme.KagamiSurface
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.SubtleMagenta
import com.example.viewmodel.HomeTab
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KagamiHomeScreen(
    currentTab: HomeTab,
    projects: List<KagamiProject>,
    settings: KagamiSettings,
    onSelectTab: (HomeTab) -> Unit,
    onStartBlankDrawing: () -> Unit,
    onSelectImageClicked: () -> Unit,
    onQuickSampleEngine: (AutoDrawMode) -> Unit,
    onOpenProject: (KagamiProject) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onOpenFullSettings: () -> Unit,
    externalOverlayContent: @Composable (PaddingValues) -> Unit,
    settingsContent: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(60.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                KagamiSubtleBorder,
                                ElectricViolet.copy(alpha = 0.45f),
                                KagamiSubtleBorder
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                containerColor = Color(0xF2080411),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeTab.STUDIO,
                    onClick = { onSelectTab(HomeTab.STUDIO) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.STUDIO) Icons.Filled.GridView else Icons.Outlined.GridView,
                            contentDescription = "Studio",
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    label = { Text("Studio", style = MaterialTheme.typography.labelSmall) },
                    colors = kagamiNavColors(),
                    modifier = Modifier.testTag("nav_tab_studio")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.EXTERNAL_OVERLAY,
                    onClick = { onSelectTab(HomeTab.EXTERNAL_OVERLAY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.EXTERNAL_OVERLAY) Icons.Filled.Layers else Icons.Outlined.Layers,
                            contentDescription = "External Overlay",
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    label = { Text("External", style = MaterialTheme.typography.labelSmall) },
                    colors = kagamiNavColors(),
                    modifier = Modifier.testTag("nav_tab_external")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.ARCHIVE,
                    onClick = { onSelectTab(HomeTab.ARCHIVE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.ARCHIVE) Icons.Filled.FolderOpen else Icons.Outlined.FolderOpen,
                            contentDescription = "Archive",
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (projects.isEmpty()) "Archive" else "Archive (${projects.size})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = kagamiNavColors(),
                    modifier = Modifier.testTag("nav_tab_archive")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.CONFIG,
                    onClick = { onSelectTab(HomeTab.CONFIG) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.CONFIG) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Config",
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                    colors = kagamiNavColors(),
                    modifier = Modifier.testTag("nav_tab_config")
                )
            }
        }
    ) { innerPadding ->
        KagamiAtmosphericBackground(
            pureOled = settings.pureOledTheme,
            showParticles = settings.showParticles
        ) {
            AnimatedContent(
                targetState = currentTab,
                label = "home_tab_transition"
            ) { tab ->
                when (tab) {
                    HomeTab.STUDIO -> StudioDashboardContent(
                        innerPadding = innerPadding,
                        projects = projects,
                        onStartBlankDrawing = onStartBlankDrawing,
                        onSelectImageClicked = onSelectImageClicked,
                        onOpenExternalOverlay = { wePlayMode ->
                            KagamiExternalDrawingManager.setWePlayMode(wePlayMode)
                            onSelectTab(HomeTab.EXTERNAL_OVERLAY)
                        },
                        onQuickSampleEngine = onQuickSampleEngine,
                        onOpenProject = onOpenProject,
                        onDeleteProject = onDeleteProject,
                        onViewAllArchive = { onSelectTab(HomeTab.ARCHIVE) },
                        onOpenSettings = { onSelectTab(HomeTab.CONFIG) }
                    )

                    HomeTab.EXTERNAL_OVERLAY -> externalOverlayContent(innerPadding)

                    HomeTab.ARCHIVE -> ArchiveTabContent(
                        innerPadding = innerPadding,
                        projects = projects,
                        onSelectImageClicked = onSelectImageClicked,
                        onStartBlankDrawing = onStartBlankDrawing,
                        onOpenProject = onOpenProject,
                        onDeleteProject = onDeleteProject
                    )

                    HomeTab.CONFIG -> settingsContent(innerPadding)
                }
            }
        }
    }
}

@Composable
private fun kagamiNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = KagamiWhite,
    selectedTextColor = LuminousViolet,
    indicatorColor = DeepPurple.copy(alpha = 0.65f),
    unselectedIconColor = KagamiMutedText.copy(alpha = 0.7f),
    unselectedTextColor = KagamiMutedText.copy(alpha = 0.7f)
)

@Composable
private fun StudioDashboardContent(
    innerPadding: PaddingValues,
    projects: List<KagamiProject>,
    onStartBlankDrawing: () -> Unit,
    onSelectImageClicked: () -> Unit,
    onOpenExternalOverlay: (Boolean) -> Unit,
    onQuickSampleEngine: (AutoDrawMode) -> Unit,
    onOpenProject: (KagamiProject) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onViewAllArchive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact Top Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KagamiCrestEmblem(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "KAGAMI",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = KagamiWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricViolet.copy(alpha = 0.18f),
                                    border = BorderStroke(0.8.dp, ElectricViolet.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "鏡 STUDIO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = LuminousViolet,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Smart Auto-Draw & Anime Line Studio",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = KagamiMutedText
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(KagamiGlassSurface)
                            .border(1.dp, KagamiCardBorder, CircleShape)
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LuminousViolet,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Compact Mascot & Primary Action Card
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    glowAccent = true,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            OledBlack.copy(alpha = 0.92f),
                                            Color(0xDD0B0518),
                                            Color(0x99170B2E)
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SubtleMagenta.copy(alpha = 0.16f),
                                    border = BorderStroke(0.8.dp, SubtleMagenta.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "SMART AUTO-SETUP ENGINE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = SubtleMagenta,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "Select Image → Auto-Setup → Draw",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = KagamiWhite
                                )

                                Text(
                                    text = "Automatically analyzes contrast & contours for internal canvas or external floating overlay.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = KagamiMutedText
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KagamiPillButton(
                                        text = "Select Image",
                                        icon = Icons.Default.AddPhotoAlternate,
                                        isPrimary = true,
                                        onClick = onSelectImageClicked,
                                        modifier = Modifier.testTag("select_image_card")
                                    )
                                    KagamiPillButton(
                                        text = "Start Drawing",
                                        icon = Icons.Default.Brush,
                                        isPrimary = false,
                                        onClick = onStartBlankDrawing,
                                        modifier = Modifier.testTag("start_drawing_button")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Compact Mascot Avatar (74x96dp)
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .width(74.dp)
                                    .height(98.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        BorderStroke(
                                            1.2.dp,
                                            Brush.verticalGradient(
                                                listOf(LuminousViolet, SubtleMagenta.copy(alpha = 0.6f))
                                            )
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_kagami_mascot_bust),
                                    contentDescription = "Kagami Mascot Character",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, OledBlack.copy(alpha = 0.88f))
                                            )
                                        )
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "KAGAMI",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                        color = LuminousViolet
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Dedicated WePlay Mode & Universal External Overlay Quick-Launch Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KagamiGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenExternalOverlay(true) }
                            .testTag("weplay_mode_dashboard_card"),
                        cornerRadius = 14.dp,
                        glowAccent = true,
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SubtleMagenta.copy(alpha = 0.2f))
                                    .border(1.dp, SubtleMagenta, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "WePlay Mode",
                                    tint = SubtleMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "WePlay Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "1-tap board auto-fit",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }
                    }

                    KagamiGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenExternalOverlay(false) }
                            .testTag("external_overlay_dashboard_card"),
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricViolet.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricViolet, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Universal Overlay",
                                    tint = LuminousViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "External Overlay",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "Auto calibrate any app",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }
                    }
                }
            }

            // 4. Compact Instant Auto-Draw Engine Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INSTANT AUTO-DRAW ENGINES",
                            style = MaterialTheme.typography.titleSmall,
                            color = LuminousViolet
                        )
                        Text(
                            text = "Tap to launch",
                            style = MaterialTheme.typography.labelSmall,
                            color = KagamiDimText
                        )
                    }

                    val enginePresets = remember {
                        listOf(
                            AutoDrawMode.EDGE_OUTLINE,
                            AutoDrawMode.SKETCH,
                            AutoDrawMode.VIOLET_BLUEPRINT,
                            AutoDrawMode.THRESHOLD,
                            AutoDrawMode.CONTRAST,
                            AutoDrawMode.GRAYSCALE
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(enginePresets, key = { it.id }) { mode ->
                            KagamiGlassCard(
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { onQuickSampleEngine(mode) }
                                    .testTag("preset_engine_${mode.id.lowercase()}"),
                                cornerRadius = 14.dp,
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mode.japaneseTag,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = SubtleMagenta
                                        )
                                        Icon(
                                            imageVector = when (mode) {
                                                AutoDrawMode.EDGE_OUTLINE -> Icons.Default.AutoFixHigh
                                                AutoDrawMode.SKETCH -> Icons.Default.Brush
                                                AutoDrawMode.VIOLET_BLUEPRINT -> Icons.Default.AutoAwesome
                                                AutoDrawMode.CONTRAST -> Icons.Default.Contrast
                                                else -> Icons.Default.Palette
                                            },
                                            contentDescription = mode.title,
                                            tint = LuminousViolet,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                                        color = KagamiWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Recent Projects Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT PROJECTS",
                        style = MaterialTheme.typography.titleSmall,
                        color = LuminousViolet
                    )
                    if (projects.isNotEmpty()) {
                        Text(
                            text = "View All (${projects.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricViolet,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onViewAllArchive)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    KagamiGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KagamiSurface)
                                    .border(1.dp, KagamiCardBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ImageSearch,
                                    contentDescription = null,
                                    tint = LuminousViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Saved Projects Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "Saved drawings and line-art studies appear here for instant reopening.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }
                    }
                }
            } else {
                items(projects.take(4), key = { it.id }) { project ->
                    ProjectItemCard(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onDelete = { onDeleteProject(project.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ArchiveTabContent(
    innerPadding: PaddingValues,
    projects: List<KagamiProject>,
    onSelectImageClicked: () -> Unit,
    onStartBlankDrawing: () -> Unit,
    onOpenProject: (KagamiProject) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KAGAMI ARCHIVE",
                            style = MaterialTheme.typography.titleLarge,
                            color = KagamiWhite
                        )
                        Text(
                            text = "${projects.size} saved local studio projects",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = KagamiMutedText
                        )
                    }
                    KagamiPillButton(
                        text = "New Study",
                        icon = Icons.Default.AddPhotoAlternate,
                        isPrimary = true,
                        onClick = onSelectImageClicked
                    )
                }
            }

            if (projects.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    KagamiGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KagamiCrestEmblem(size = 38.dp)
                            Text(
                                text = "No Saved Projects Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = KagamiWhite
                            )
                            Text(
                                text = "Every project you save preserves its vector stroke history and reference layer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KagamiMutedText
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KagamiPillButton(
                                    text = "Select Image",
                                    icon = Icons.Default.AddPhotoAlternate,
                                    isPrimary = true,
                                    onClick = onSelectImageClicked
                                )
                                KagamiPillButton(
                                    text = "Blank Canvas",
                                    icon = Icons.Default.Brush,
                                    isPrimary = false,
                                    onClick = onStartBlankDrawing
                                )
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    ProjectItemCard(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onDelete = { onDeleteProject(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectItemCard(
    project: KagamiProject,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBitmap = remember(project.thumbnailPath, project.updatedAt) {
        val f = File(project.thumbnailPath)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }
    val dateFormatted = remember(project.updatedAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
    }
    val modeTitle = remember(project.filterMode) {
        AutoDrawMode.fromId(project.filterMode).title
    }

    KagamiGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("project_card_${project.id}"),
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A122B))
                    .border(1.dp, KagamiCardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    KagamiCrestEmblem(size = 24.dp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    color = KagamiWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricViolet.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = modeTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = LuminousViolet,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "${project.width}×${project.height} • $dateFormatted",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = KagamiDimText
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete project",
                    tint = StatusDanger.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
