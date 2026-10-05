package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.KagamiDrawingWorkspaceScreen
import com.example.ui.screens.KagamiExternalAutoDrawScreen
import com.example.ui.screens.KagamiHomeScreen
import com.example.ui.screens.KagamiImageSelectionScreen
import com.example.ui.screens.KagamiSettingsContent
import com.example.ui.theme.KagamiTheme
import com.example.viewmodel.KagamiScreen
import com.example.viewmodel.KagamiViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KagamiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            KagamiApp(viewModel = viewModel)
        }
    }
}

@Composable
fun KagamiApp(viewModel: KagamiViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val homeTab by viewModel.homeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val recentProjects by viewModel.recentProjects.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()

    KagamiTheme(pureOledBlack = settings.pureOledTheme) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 260),
            modifier = Modifier.fillMaxSize(),
            label = "kagami_screen_router"
        ) { screen ->
            when (screen) {
                KagamiScreen.SPLASH,
                KagamiScreen.HOME -> {
                    KagamiHomeScreen(
                        currentTab = homeTab,
                        projects = recentProjects,
                        settings = settings,
                        onSelectTab = viewModel::selectHomeTab,
                        onStartBlankDrawing = viewModel::startBlankCanvasWorkspace,
                        onSelectImageClicked = viewModel::openImageSelectionScreen,
                        onQuickSampleEngine = { mode ->
                            viewModel.loadSampleReferenceForSelection(
                                andOpenWorkspaceImmediately = true,
                                presetMode = mode
                            )
                        },
                        onOpenProject = viewModel::openSavedProject,
                        onDeleteProject = viewModel::deleteProject,
                        onOpenFullSettings = { viewModel.navigateTo(KagamiScreen.SETTINGS) },
                        externalOverlayContent = { innerPadding ->
                            KagamiExternalAutoDrawScreen(
                                innerPadding = innerPadding,
                                onPickImageForExternal = viewModel::pickImageForExternalAutoDraw,
                                onLoadShrineSampleForExternal = viewModel::loadSampleForExternalAutoDraw,
                                onReapplySmartAutoSetup = viewModel::reapplySmartAutoSetupForExternal,
                                onUpdateExternalPathConfig = viewModel::updateExternalPathConfigAndReprocess
                            )
                        },
                        settingsContent = { innerPadding ->
                            KagamiSettingsContent(
                                innerPadding = innerPadding,
                                settings = settings,
                                onPureOledChanged = viewModel::updatePureOledTheme,
                                onShowParticlesChanged = viewModel::updateShowParticles,
                                onDefaultBrushSizeChanged = viewModel::updateDefaultBrushSize,
                                onBrushSmoothingChanged = viewModel::updateBrushSmoothing,
                                onShowCanvasGridChanged = viewModel::updateShowCanvasGrid,
                                onDefaultPaperToneChanged = viewModel::updateDefaultPaperTone,
                                onExportFormatChanged = viewModel::updateExportFormat,
                                onJpgQualityChanged = viewModel::updateJpgQuality,
                                onResetPreferences = viewModel::resetAllPreferences
                            )
                        }
                    )
                }

                KagamiScreen.IMAGE_SELECT -> {
                    KagamiImageSelectionScreen(
                        pureOled = settings.pureOledTheme,
                        showParticles = settings.showParticles,
                        pendingBitmap = workspaceState.pendingPreviewBitmap,
                        pendingSourceLabel = workspaceState.pendingSourceLabel,
                        smartProfile = workspaceState.pendingSmartProfile,
                        isLoading = workspaceState.isLoadingImage,
                        errorMessage = workspaceState.imageSelectionError,
                        onImagePicked = viewModel::onGalleryImagePicked,
                        onLoadSampleClicked = {
                            viewModel.loadSampleReferenceForSelection(andOpenWorkspaceImmediately = false)
                        },
                        onConfirmSelection = { chosenMode ->
                            viewModel.confirmSelectedImageAndStartWorkspace(initialMode = chosenMode)
                        },
                        onSendToExternalOverlay = viewModel::sendPendingImageToExternalAutoDraw,
                        onCancelAndReturn = { viewModel.navigateTo(KagamiScreen.HOME) }
                    )
                }

                KagamiScreen.WORKSPACE -> {
                    KagamiDrawingWorkspaceScreen(
                        state = workspaceState,
                        settings = settings,
                        onBack = { viewModel.navigateBack() },
                        onSelectNewImage = viewModel::openImageSelectionScreen,
                        onSelectTool = viewModel::selectTool,
                        onBrushColorSelected = viewModel::setBrushColor,
                        onBrushSizeChanged = viewModel::setBrushSize,
                        onBrushOpacityChanged = viewModel::setBrushOpacity,
                        onReferenceAlphaChanged = viewModel::setReferenceAlpha,
                        onToggleReferenceVisibility = viewModel::toggleReferenceVisibility,
                        onPaperColorChanged = viewModel::setCanvasPaperColor,
                        onStrokeStart = viewModel::onStrokeStart,
                        onStrokeMove = viewModel::onStrokeMove,
                        onStrokeEnd = viewModel::onStrokeEnd,
                        onStrokeCancel = viewModel::onStrokeCancel,
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                        onClearCanvas = viewModel::clearCanvasStrokes,
                        onZoomAndPan = viewModel::updateZoomAndPan,
                        onStepZoom = viewModel::stepZoom,
                        onResetFit = viewModel::resetViewportFit,
                        onToggleFullscreen = viewModel::toggleFullscreenMode,
                        onOpenAutoDrawEngine = viewModel::openAutoDrawEngineSheet,
                        onCloseAutoDrawEngine = viewModel::closeAutoDrawEngineSheet,
                        onUpdateAutoDrawPreview = viewModel::updateAutoDrawPreview,
                        onCommitAutoDrawToCanvas = viewModel::commitAutoDrawPreviewToCanvas,
                        onTriggerLiveVectorAutoTrace = viewModel::triggerLiveVectorAutoTrace,
                        onSetExportSheetVisible = viewModel::setExportSheetVisible,
                        onUpdateProjectTitle = viewModel::updateProjectTitle,
                        onSaveProjectLocal = { viewModel.saveProjectToLocalArchive(showConfirmation = true) },
                        onExportArtwork = viewModel::exportFinishedArtwork,
                        onDismissBanner = viewModel::clearStatusBanner
                    )
                }

                KagamiScreen.SETTINGS -> {
                    KagamiSettingsContent(
                        innerPadding = PaddingValues(),
                        settings = settings,
                        onPureOledChanged = viewModel::updatePureOledTheme,
                        onShowParticlesChanged = viewModel::updateShowParticles,
                        onDefaultBrushSizeChanged = viewModel::updateDefaultBrushSize,
                        onBrushSmoothingChanged = viewModel::updateBrushSmoothing,
                        onShowCanvasGridChanged = viewModel::updateShowCanvasGrid,
                        onDefaultPaperToneChanged = viewModel::updateDefaultPaperTone,
                        onExportFormatChanged = viewModel::updateExportFormat,
                        onJpgQualityChanged = viewModel::updateJpgQuality,
                        onResetPreferences = viewModel::resetAllPreferences
                    )
                }
            }
        }
    }
}
