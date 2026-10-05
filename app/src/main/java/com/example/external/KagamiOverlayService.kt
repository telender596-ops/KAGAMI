package com.example.external

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.ui.components.KagamiCrestEmblem
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSurface
import com.example.ui.theme.KagamiTheme
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SubtleMagenta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class KagamiOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var controllerView: ComposeView? = null
    private var controllerParams: WindowManager.LayoutParams? = null
    private var calibrationOverlayView: ComposeView? = null
    private var calibrationParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        startForegroundSafely()

        if (!canDrawOverlays(this)) {
            KagamiExternalDrawingManager.setOverlayServiceRunning(false)
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initScreenMetrics()
        attachFloatingViews()
        KagamiExternalDrawingManager.setOverlayServiceRunning(true)

        serviceScope.launch {
            KagamiExternalDrawingManager.state
                .map { it.jobStatus == ExternalJobStatus.CALIBRATING || it.jobStatus == ExternalJobStatus.PREVIEWING }
                .distinctUntilChanged()
                .collect { showFullQuadWindow ->
                    updateCalibrationWindowVisibility(showFullQuadWindow)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                KagamiExternalDrawingManager.emergencyStop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (!canDrawOverlays(this)) {
            KagamiExternalDrawingManager.setOverlayServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }
        initScreenMetrics()
        return START_STICKY
    }

    private fun initScreenMetrics() {
        try {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealMetrics(dm)
            KagamiExternalDrawingManager.updateScreenDimensions(dm.widthPixels, dm.heightPixels)
        } catch (_: Exception) {
        }
    }

    private fun startForegroundSafely() {
        try {
            val channelId = "kagami_overlay_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "KAGAMI Floating Controller",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps the KAGAMI floating controller active above target drawing apps"
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(channel)
            }

            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingOpen = PendingIntent.getActivity(
                this,
                101,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("KAGAMI Floating Controller")
                .setContentText("Tap to open KAGAMI or use the compact overlay pill.")
                .setContentIntent(pendingOpen)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }
    }

    private fun attachFloatingViews() {
        val wm = windowManager ?: return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val calParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        calibrationParams = calParams

        val calView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KagamiOverlayService)
            setViewTreeViewModelStoreOwner(this@KagamiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@KagamiOverlayService)
            visibility = android.view.View.GONE
            setContent {
                KagamiTheme(pureOledBlack = true) {
                    SmartVisualCalibrationOverlaySurface()
                }
            }
        }
        calibrationOverlayView = calView
        try {
            wm.addView(calView, calParams)
        } catch (_: Exception) {
        }

        val ctrlParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 110
        }
        controllerParams = ctrlParams

        val ctrlView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KagamiOverlayService)
            setViewTreeViewModelStoreOwner(this@KagamiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@KagamiOverlayService)
            setContent {
                KagamiTheme(pureOledBlack = true) {
                    CompactFloatingControllerCard(
                        onMoveWindowBy = { dx, dy ->
                            val p = controllerParams ?: return@CompactFloatingControllerCard
                            p.x = (p.x + dx.roundToInt()).coerceAtLeast(0)
                            p.y = (p.y + dy.roundToInt()).coerceAtLeast(0)
                            try {
                                wm.updateViewLayout(this, p)
                            } catch (_: Exception) {
                            }
                        },
                        onReturnToKagamiApp = {
                            val launch = Intent(this@KagamiOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(launch)
                        },
                        onCloseOverlay = {
                            KagamiExternalDrawingManager.emergencyStop()
                            stopSelf()
                        }
                    )
                }
            }
        }
        controllerView = ctrlView
        try {
            wm.addView(ctrlView, ctrlParams)
        } catch (_: Exception) {
        }
    }

    private fun updateCalibrationWindowVisibility(visible: Boolean) {
        val view = calibrationOverlayView ?: return
        view.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onDestroy() {
        KagamiExternalDrawingManager.emergencyStop()
        KagamiExternalDrawingManager.setOverlayServiceRunning(false)
        try {
            controllerView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        try {
            calibrationOverlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        controllerView = null
        calibrationOverlayView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 4409
        const val ACTION_STOP_OVERLAY = "com.example.external.STOP_OVERLAY"

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun startOverlayService(context: Context): Boolean {
            if (!canDrawOverlays(context)) return false
            return try {
                val intent = Intent(context, KagamiOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        fun stopOverlayService(context: Context) {
            try {
                val intent = Intent(context, KagamiOverlayService::class.java)
                context.stopService(intent)
                KagamiExternalDrawingManager.setOverlayServiceRunning(false)
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * Compact KAGAMI Floating Controller:
 * - Collapsed state: Small movable KAGAMI icon + direct Start / Emergency STOP access.
 * - Expanded state: Compact 256dp glassmorphism card with Start, Pause, Resume, Stop,
 *   Progress, Speed (Slow/Normal/Fast), Auto Calibrate, and Settings.
 */
@Composable
private fun CompactFloatingControllerCard(
    onMoveWindowBy: (Float, Float) -> Unit,
    onReturnToKagamiApp: () -> Unit,
    onCloseOverlay: () -> Unit
) {
    val state by KagamiExternalDrawingManager.state.collectAsState()
    val isDrawing = state.jobStatus == ExternalJobStatus.DRAWING
    val isPaused = state.jobStatus == ExternalJobStatus.PAUSED

    if (state.isOverlayCollapsed) {
        // Compact Collapsed Pill: Small KAGAMI Icon + Direct Start / Stop Access
        Row(
            modifier = Modifier
                .shadow(14.dp, CircleShape, ambientColor = ElectricViolet, spotColor = SubtleMagenta)
                .clip(CircleShape)
                .background(Color(0xEB080412))
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(LuminousViolet, SubtleMagenta)),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMoveWindowBy(dragAmount.x, dragAmount.y)
                    }
                }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Small KAGAMI Icon (Tap to expand full overlay)
            Image(
                painter = painterResource(id = R.drawable.img_kagami_mascot_icon),
                contentDescription = "Expand KAGAMI Overlay",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { KagamiExternalDrawingManager.setOverlayCollapsed(false) }
            )

            if (isDrawing || isPaused) {
                // Pause/Resume mini button
                Surface(
                    shape = CircleShape,
                    color = DeepPurple,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (isDrawing) {
                                KagamiGestureService.pauseExternalDrawing()
                            } else {
                                KagamiGestureService.resumeExternalDrawing()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDrawing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isDrawing) "Pause" else "Resume",
                            tint = KagamiWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Prominent Emergency STOP pill
                Surface(
                    shape = CircleShape,
                    color = StatusDanger,
                    border = BorderStroke(1.dp, KagamiWhite),
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .clickable { KagamiExternalDrawingManager.emergencyStop() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Emergency Stop",
                            tint = KagamiWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "STOP ${(state.progressFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = KagamiWhite
                        )
                    }
                }
            } else {
                // Direct 1-tap Start button right from the collapsed pill
                Surface(
                    shape = CircleShape,
                    color = if (state.extractedPaths.isNotEmpty()) ElectricViolet else Color(0xFF221836),
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = state.extractedPaths.isNotEmpty()) {
                            KagamiGestureService.startExternalDrawingFromBeginning()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Auto-Draw",
                            tint = KagamiWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = KagamiWhite
                        )
                    }
                }
            }
        }
    } else {
        // Compact Expanded Glassmorphism Controller (254dp wide so it never crowds the target canvas)
        Column(
            modifier = Modifier
                .width(254.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = ElectricViolet,
                    spotColor = SubtleMagenta
                )
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xEB080412))
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(ElectricViolet, SubtleMagenta.copy(alpha = 0.7f)))
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Draggable Compact Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMoveWindowBy(dragAmount.x, dragAmount.y)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KagamiCrestEmblem(size = 18.dp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "KAGAMI",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = KagamiWhite
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (state.isWePlayModeActive) SubtleMagenta.copy(alpha = 0.25f) else ElectricViolet.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (state.isWePlayModeActive) "WEPLAY" else "AUTO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = if (state.isWePlayModeActive) SubtleMagenta else LuminousViolet,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.totalStrokes} strokes",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = KagamiMutedText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReturnToKagamiApp,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open KAGAMI",
                            tint = LuminousViolet,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = { KagamiExternalDrawingManager.setOverlayCollapsed(true) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Collapse",
                            tint = KagamiWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onCloseOverlay,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StatusDanger,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Progress & Status Row
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.jobStatus == ExternalJobStatus.UNSUPPORTED_CANVAS_ERROR) {
                            "External drawing is not supported on this canvas."
                        } else {
                            state.statusMessage
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                        color = if (state.jobStatus == ExternalJobStatus.UNSUPPORTED_CANVAS_ERROR) {
                            StatusDanger
                        } else {
                            KagamiMutedText
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(state.progressFraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = LuminousViolet
                    )
                }

                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = ElectricViolet,
                    trackColor = DeepPurple.copy(alpha = 0.35f)
                )
            }

            // Start / Pause / Resume + Emergency STOP Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state.jobStatus) {
                    ExternalJobStatus.DRAWING -> {
                        CompactOverlayButton(
                            label = "Pause",
                            icon = Icons.Default.Pause,
                            containerColor = DeepPurple,
                            borderColor = LuminousViolet,
                            onClick = { KagamiGestureService.pauseExternalDrawing() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ExternalJobStatus.PAUSED -> {
                        CompactOverlayButton(
                            label = "Resume",
                            icon = Icons.Default.PlayArrow,
                            containerColor = ElectricViolet,
                            borderColor = LuminousViolet,
                            onClick = { KagamiGestureService.resumeExternalDrawing() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    else -> {
                        CompactOverlayButton(
                            label = "Start",
                            icon = Icons.Default.PlayArrow,
                            containerColor = ElectricViolet,
                            borderColor = LuminousViolet,
                            enabled = state.extractedPaths.isNotEmpty(),
                            onClick = {
                                KagamiExternalDrawingManager.setOverlayCollapsed(true)
                                KagamiGestureService.startExternalDrawingFromBeginning()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Emergency STOP Button
                CompactOverlayButton(
                    label = "STOP",
                    icon = Icons.Default.Stop,
                    containerColor = StatusDanger,
                    borderColor = KagamiWhite,
                    onClick = { KagamiExternalDrawingManager.emergencyStop() },
                    modifier = Modifier.weight(0.95f)
                )
            }

            // Speed Presets Row: Slow / Normal / Fast
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = KagamiMutedText
                )
                SpeedPreset.entries.forEach { preset ->
                    val selected = state.activeSpeedPreset == preset
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) ElectricViolet else Color(0xFF160E28),
                        border = BorderStroke(
                            0.8.dp,
                            if (selected) LuminousViolet else KagamiCardBorder.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { KagamiExternalDrawingManager.selectSpeedPreset(preset) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = KagamiWhite
                            )
                        }
                    }
                }
            }

            // Auto Calibrate, Preview & Settings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val isCalibrating = state.jobStatus == ExternalJobStatus.CALIBRATING
                val isPreviewing = state.jobStatus == ExternalJobStatus.PREVIEWING

                CompactSmallPill(
                    label = if (isCalibrating) "Confirm" else "Calibrate",
                    icon = if (isCalibrating) Icons.Default.Check else Icons.Default.CropFree,
                    active = isCalibrating,
                    onClick = {
                        if (isCalibrating) {
                            KagamiExternalDrawingManager.exitCalibrationOrPreviewMode()
                        } else {
                            KagamiExternalDrawingManager.enterCalibrationMode(CalibrationUiMode.AUTO_VISUAL)
                        }
                    },
                    modifier = Modifier.weight(1.1f)
                )

                CompactSmallPill(
                    label = if (isPreviewing) "Hide" else "Preview",
                    icon = Icons.Default.Visibility,
                    active = isPreviewing,
                    onClick = {
                        if (isPreviewing) {
                            KagamiExternalDrawingManager.exitCalibrationOrPreviewMode()
                        } else {
                            KagamiExternalDrawingManager.enterPreviewMode()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                CompactSmallPill(
                    label = "Mode",
                    icon = Icons.Default.Tune,
                    active = state.showOverlaySettingsPanel,
                    onClick = { KagamiExternalDrawingManager.toggleOverlaySettingsPanel() },
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Expandable Quick Mode & Aspect Settings
            AnimatedVisibility(visible = state.showOverlaySettingsPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KagamiSurface)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WePlay Auto-Fit Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = KagamiWhite
                        )
                        Switch(
                            checked = state.isWePlayModeActive,
                            onCheckedChange = { KagamiExternalDrawingManager.setWePlayMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = KagamiWhite,
                                checkedTrackColor = SubtleMagenta
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keep Image Aspect Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = KagamiMutedText
                        )
                        Switch(
                            checked = state.pathConfig.preserveAspectRatio,
                            onCheckedChange = { keep ->
                                KagamiExternalDrawingManager.updatePathConfig(
                                    state.pathConfig.copy(preserveAspectRatio = keep)
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = KagamiWhite,
                                checkedTrackColor = ElectricViolet
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOverlayButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) containerColor else Color(0xFF1F1730),
        border = BorderStroke(1.dp, if (enabled) borderColor else KagamiCardBorder),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = KagamiWhite,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = KagamiWhite
            )
        }
    }
}

@Composable
private fun CompactSmallPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (active) DeepPurple else Color(0xFF140D24),
        border = BorderStroke(0.8.dp, if (active) LuminousViolet else KagamiCardBorder),
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) SubtleMagenta else LuminousViolet,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = KagamiWhite,
                maxLines = 1
            )
        }
    }
}

/**
 * Beginner-friendly Smart Visual Calibration & Fine-Tune Surface:
 * - Primary Mode (`AUTO_VISUAL`): Shows a clean visual guide rectangle with the fitted artwork preview,
 *   1-tap preset chips (WePlay Party, WePlay 1:1, Smart Auto, Wide), and a 1-tap "Confirm Canvas" button.
 * - Optional Fine-Tune Mode (`FINE_TUNE_TOUCH`): Allows finger dragging to Move, pinch/buttons to Resize/Zoom,
 *   and Rotate without any X/Y coordinate fields.
 * - Optional Advanced Manual Mode (`MANUAL_CORNERS`): Provides the 4 corner handles (TL, TR, BL, BR) only if requested.
 */
@Composable
private fun SmartVisualCalibrationOverlaySurface() {
    val state by KagamiExternalDrawingManager.state.collectAsState()
    val rawQuad = state.calibrationQuad
    val effectiveQuad = rawQuad.withAppliedFineTune()
    val uiMode = state.calibrationUiMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x3805020A))
            .then(
                if (state.jobStatus == ExternalJobStatus.CALIBRATING && uiMode != CalibrationUiMode.MANUAL_CORNERS) {
                    // Direct finger gesture support for Move, Resize/Zoom, and Rotate!
                    Modifier.pointerInput(uiMode) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            KagamiExternalDrawingManager.applyFineTuneTransform(
                                panDelta = pan,
                                scaleMultiplier = zoom,
                                rotationDeltaDeg = if (uiMode == CalibrationUiMode.FINE_TUNE_TOUCH) rotation else 0f
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val quadPath = Path().apply {
                moveTo(effectiveQuad.topLeft.x, effectiveQuad.topLeft.y)
                lineTo(effectiveQuad.topRight.x, effectiveQuad.topRight.y)
                lineTo(effectiveQuad.bottomRight.x, effectiveQuad.bottomRight.y)
                lineTo(effectiveQuad.bottomLeft.x, effectiveQuad.bottomLeft.y)
                close()
            }

            drawPath(
                path = quadPath,
                color = ElectricViolet.copy(alpha = 0.12f)
            )
            drawPath(
                path = quadPath,
                brush = Brush.linearGradient(listOf(ElectricViolet, SubtleMagenta)),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Center alignment crosshairs
            val topMid = Offset(
                (effectiveQuad.topLeft.x + effectiveQuad.topRight.x) / 2f,
                (effectiveQuad.topLeft.y + effectiveQuad.topRight.y) / 2f
            )
            val botMid = Offset(
                (effectiveQuad.bottomLeft.x + effectiveQuad.bottomRight.x) / 2f,
                (effectiveQuad.bottomLeft.y + effectiveQuad.bottomRight.y) / 2f
            )
            val leftMid = Offset(
                (effectiveQuad.topLeft.x + effectiveQuad.bottomLeft.x) / 2f,
                (effectiveQuad.topLeft.y + effectiveQuad.bottomLeft.y) / 2f
            )
            val rightMid = Offset(
                (effectiveQuad.topRight.x + effectiveQuad.bottomRight.x) / 2f,
                (effectiveQuad.topRight.y + effectiveQuad.bottomRight.y) / 2f
            )

            drawLine(
                color = LuminousViolet.copy(alpha = 0.28f),
                start = topMid,
                end = botMid,
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = LuminousViolet.copy(alpha = 0.28f),
                start = leftMid,
                end = rightMid,
                strokeWidth = 1.dp.toPx()
            )

            // Live Preview of Mapped Paths inside the visual rectangle
            val baseThickness = state.pathConfig.lineThickness.coerceIn(1.5f, 7f)
            for (normPath in state.extractedPaths) {
                val pts = normPath.points
                if (pts.size < 2) continue
                val mappedPath = Path()
                val firstScreen = KagamiPathPipeline.mapNormalizedToScreen(
                    pt = pts.first(),
                    sourceWidth = state.sourceWidth,
                    sourceHeight = state.sourceHeight,
                    quad = rawQuad,
                    preserveAspectRatio = state.pathConfig.preserveAspectRatio
                )
                mappedPath.moveTo(firstScreen.x, firstScreen.y)
                for (i in 1 until pts.size) {
                    val sp = KagamiPathPipeline.mapNormalizedToScreen(
                        pt = pts[i],
                        sourceWidth = state.sourceWidth,
                        sourceHeight = state.sourceHeight,
                        quad = rawQuad,
                        preserveAspectRatio = state.pathConfig.preserveAspectRatio
                    )
                    mappedPath.lineTo(sp.x, sp.y)
                }
                drawPath(
                    path = mappedPath,
                    color = KagamiWhite.copy(alpha = 0.88f),
                    style = Stroke(
                        width = baseThickness * normPath.weightFactor,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Show 4-corner handles ONLY if user explicitly switched to Advanced Manual Corners mode
        if (state.jobStatus == ExternalJobStatus.CALIBRATING && uiMode == CalibrationUiMode.MANUAL_CORNERS) {
            CornerDragHandle(
                label = "TL",
                positionPx = rawQuad.topLeft,
                onDragBy = { delta ->
                    val current = KagamiExternalDrawingManager.state.value.calibrationQuad
                    KagamiExternalDrawingManager.updateCalibrationQuad(
                        current.copy(topLeft = current.topLeft + delta)
                    )
                }
            )
            CornerDragHandle(
                label = "TR",
                positionPx = rawQuad.topRight,
                onDragBy = { delta ->
                    val current = KagamiExternalDrawingManager.state.value.calibrationQuad
                    KagamiExternalDrawingManager.updateCalibrationQuad(
                        current.copy(topRight = current.topRight + delta)
                    )
                }
            )
            CornerDragHandle(
                label = "BL",
                positionPx = rawQuad.bottomLeft,
                onDragBy = { delta ->
                    val current = KagamiExternalDrawingManager.state.value.calibrationQuad
                    KagamiExternalDrawingManager.updateCalibrationQuad(
                        current.copy(bottomLeft = current.bottomLeft + delta)
                    )
                }
            )
            CornerDragHandle(
                label = "BR",
                positionPx = rawQuad.bottomRight,
                onDragBy = { delta ->
                    val current = KagamiExternalDrawingManager.state.value.calibrationQuad
                    KagamiExternalDrawingManager.updateCalibrationQuad(
                        current.copy(bottomRight = current.bottomRight + delta)
                    )
                }
            )
        }

        // Bottom Compact Visual Calibration Control Dock (No X/Y Numbers!)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEB090514))
                .border(1.dp, ElectricViolet, RoundedCornerShape(20.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.jobStatus == ExternalJobStatus.CALIBRATING) {
                // Mode Switcher: Auto Calibrate (Primary) | Fine Tune | Manual (Advanced)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        CalibrationUiMode.AUTO_VISUAL to "Auto Calibrate",
                        CalibrationUiMode.FINE_TUNE_TOUCH to "Fine Tune",
                        CalibrationUiMode.MANUAL_CORNERS to "Advanced"
                    )
                    modes.forEach { (m, title) ->
                        val active = uiMode == m
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) DeepPurple else Color(0xFF150E26),
                            border = BorderStroke(1.dp, if (active) LuminousViolet else KagamiCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { KagamiExternalDrawingManager.setCalibrationUiMode(m) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = KagamiWhite
                                )
                            }
                        }
                    }
                }

                when (uiMode) {
                    CalibrationUiMode.AUTO_VISUAL -> {
                        Text(
                            text = "Drag rectangle with 1 finger to move, or pick a 1-tap preset:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                            color = KagamiMutedText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TargetCanvasPreset.entries.forEach { preset ->
                                val selected = state.activePreset == preset
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) ElectricViolet else KagamiSurface,
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (selected) KagamiWhite else KagamiCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            KagamiExternalDrawingManager.applySmartAutoCalibration(preset)
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    ) {
                                        Text(
                                            text = when (preset) {
                                                TargetCanvasPreset.WEPLAY_PARTY -> "WePlay"
                                                TargetCanvasPreset.WEPLAY_SQUARE -> "1:1 Board"
                                                TargetCanvasPreset.UNIVERSAL_AUTO -> "Auto Fit"
                                                TargetCanvasPreset.UNIVERSAL_FULL -> "Wide"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = KagamiWhite,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    CalibrationUiMode.FINE_TUNE_TOUCH -> {
                        Text(
                            text = "Drag on screen to Move • Pinch or tap buttons to Resize, Zoom & Rotate:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                            color = KagamiMutedText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CompactSmallPill(
                                label = "Zoom -",
                                icon = Icons.Default.ZoomOut,
                                active = false,
                                onClick = {
                                    KagamiExternalDrawingManager.applyFineTuneTransform(scaleMultiplier = 0.9f)
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                            CompactSmallPill(
                                label = "Zoom +",
                                icon = Icons.Default.ZoomIn,
                                active = false,
                                onClick = {
                                    KagamiExternalDrawingManager.applyFineTuneTransform(scaleMultiplier = 1.1f)
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                            CompactSmallPill(
                                label = "Rotate",
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                active = false,
                                onClick = {
                                    KagamiExternalDrawingManager.applyFineTuneTransform(rotationDeltaDeg = 15f)
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                            CompactSmallPill(
                                label = "Reset",
                                icon = Icons.Default.AutoFixHigh,
                                active = false,
                                onClick = {
                                    KagamiExternalDrawingManager.resetFineTuneTransform()
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                        }
                    }

                    CalibrationUiMode.MANUAL_CORNERS -> {
                        Text(
                            text = "Advanced: Drag the 4 violet corner handles (TL, TR, BL, BR) to match custom canvas corners.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                            color = KagamiMutedText
                        )
                    }
                }
            }

            // One-Tap "Confirm Canvas" Primary Button
            Surface(
                shape = CircleShape,
                color = ElectricViolet,
                border = BorderStroke(1.dp, LuminousViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(CircleShape)
                    .clickable { KagamiExternalDrawingManager.exitCalibrationOrPreviewMode() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm Canvas",
                        tint = KagamiWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.jobStatus == ExternalJobStatus.CALIBRATING) {
                            "CONFIRM CANVAS"
                        } else {
                            "CLOSE PREVIEW"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = KagamiWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun CornerDragHandle(
    label: String,
    positionPx: Offset,
    onDragBy: (Offset) -> Unit
) {
    val handleSizeDp = 42.dp
    val density = LocalDensity.current
    val halfSizePx = with(density) { (handleSizeDp / 2).toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (positionPx.x - halfSizePx).roundToInt(),
                    y = (positionPx.y - halfSizePx).roundToInt()
                )
            }
            .size(handleSizeDp)
            .shadow(8.dp, CircleShape, ambientColor = ElectricViolet)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(SubtleMagenta, ElectricViolet, DeepPurple)
                )
            )
            .border(1.5.dp, KagamiWhite, CircleShape)
            .pointerInput(label) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragBy(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = KagamiWhite
        )
    }
}
