@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.graphics.Paint as AndroidPaint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.BorderAll
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class SplitInteractionMode {
    Draw,
    Pan,
}

private enum class SplitToolPanel {
    EqualSplit,
    BoardSplit,
    Zoom,
}

private data class SplitStroke(
    val vertices: List<GridPoint>,
) {
    val segments: List<GridLineSegment>
        get() = ProjectSplitting.segmentsForVertices(vertices)
}

@Composable
fun ProjectSplitScreen(
    project: BeadProject,
    onBack: () -> Unit,
    onComplete: (List<BeadProject>) -> Unit,
) {
    val context = LocalContext.current
    val projectStore = remember(context) { ProjectStore(context) }
    val rememberedBoardSize = remember(projectStore) { projectStore.loadSplitBoardSize() }
    var strokes by remember(project.id, project.updatedAt) { mutableStateOf(emptyList<SplitStroke>()) }
    var redoStrokes by remember(project.id, project.updatedAt) { mutableStateOf(emptyList<SplitStroke>()) }
    var interactionMode by remember(project.id) { mutableStateOf(SplitInteractionMode.Draw) }
    var pendingResult by remember { mutableStateOf<ProjectSplitResult.Success?>(null) }
    var messageDialog by remember { mutableStateOf<String?>(null) }
    var showMoveTutorial by remember { mutableStateOf(false) }
    var boardPreviewLayout by remember(project.id, project.updatedAt) { mutableStateOf<BoardSplitLayout?>(null) }

    val activeSegments = strokes.flatMap(SplitStroke::segments)
    fun addStroke(vertices: List<GridPoint>) {
        val stroke = SplitStroke(vertices)
        if (stroke.segments.isEmpty()) return
        strokes = strokes + stroke
        redoStrokes = emptyList()
    }
    fun replaceWithPreset(preset: EqualSplitPreset) {
        val segments = ProjectSplitting.presetSegments(project.width, project.height, preset)
        if (segments.isEmpty()) {
            messageDialog = "当前画布尺寸无法使用这个均分方案。"
            return
        }
        strokes = segments.map { SplitStroke(listOf(it.start, it.end)) }
        redoStrokes = emptyList()
        boardPreviewLayout = null
    }
    fun requestCompletion() {
        when (val result = ProjectSplitting.split(project, activeSegments)) {
            is ProjectSplitResult.Success -> pendingResult = result
            is ProjectSplitResult.Failure -> messageDialog = result.message
        }
    }

    BackHandler(onBack = onBack)
    SplitWorkspace(
        project = project,
        initialBoardWidth = rememberedBoardSize.first,
        initialBoardHeight = rememberedBoardSize.second,
        strokes = strokes,
        interactionMode = interactionMode,
        canUndo = strokes.isNotEmpty(),
        canRedo = redoStrokes.isNotEmpty(),
        onBack = onBack,
        onComplete = ::requestCompletion,
        onStrokeFinished = ::addStroke,
        onModeChange = { mode ->
            interactionMode = mode
            if (mode == SplitInteractionMode.Pan && !projectStore.hasShownSplitMoveTutorial()) {
                projectStore.markSplitMoveTutorialShown()
                showMoveTutorial = true
            }
        },
        onUndo = {
            val stroke = strokes.lastOrNull() ?: return@SplitWorkspace
            strokes = strokes.dropLast(1)
            redoStrokes = redoStrokes + stroke
        },
        onRedo = {
            val stroke = redoStrokes.lastOrNull() ?: return@SplitWorkspace
            redoStrokes = redoStrokes.dropLast(1)
            strokes = strokes + stroke
        },
        onClear = {
            strokes = emptyList()
            redoStrokes = emptyList()
            boardPreviewLayout = null
        },
        onPreset = ::replaceWithPreset,
        boardPreviewLayout = boardPreviewLayout,
        onBoardPreviewChange = { boardPreviewLayout = it },
        onApplyBoardSplit = { layout ->
            if (layout == null || layout.segments.isEmpty()) {
                messageDialog = "当前底板尺寸和位置没有在画布内形成分界线。"
            } else {
                strokes = layout.segments.map { SplitStroke(listOf(it.start, it.end)) }
                redoStrokes = emptyList()
                boardPreviewLayout = null
            }
        },
        onRememberBoardSize = projectStore::saveSplitBoardSize,
    )

    pendingResult?.let { result ->
        AlertDialog(
            onDismissRequest = { pendingResult = null },
            title = { Text("确认拆分") },
            text = {
                Text("将生成 ${result.partCount} 个新项目，原项目不会修改。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingResult = null
                        onComplete(result.projects)
                    },
                ) {
                    Text("生成项目")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResult = null }) { Text("继续调整") }
            },
        )
    }

    messageDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { messageDialog = null },
            title = { Text("无法拆分") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { messageDialog = null }) { Text("知道了") }
            },
        )
    }

    if (showMoveTutorial) {
        AlertDialog(
            onDismissRequest = { showMoveTutorial = false },
            title = { Text("移动画布") },
            text = { Text("两根手指同时按住画布并拖动即可移动；双指捏合可以缩放。开启移动工具后，也可以用单指拖动画布。") },
            confirmButton = {
                TextButton(onClick = { showMoveTutorial = false }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun SplitWorkspace(
    project: BeadProject,
    initialBoardWidth: Int,
    initialBoardHeight: Int,
    strokes: List<SplitStroke>,
    interactionMode: SplitInteractionMode,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onStrokeFinished: (List<GridPoint>) -> Unit,
    onModeChange: (SplitInteractionMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onPreset: (EqualSplitPreset) -> Unit,
    boardPreviewLayout: BoardSplitLayout?,
    onBoardPreviewChange: (BoardSplitLayout?) -> Unit,
    onApplyBoardSplit: (BoardSplitLayout?) -> Unit,
    onRememberBoardSize: (width: Int, height: Int) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingInsets = WindowInsets.safeDrawing
    val safeLeftPx = safeDrawingInsets.getLeft(density, layoutDirection).toFloat()
    val safeRightPx = safeDrawingInsets.getRight(density, layoutDirection).toFloat()
    var viewportPan by remember(project.id) { mutableStateOf(Offset.Zero) }
    var cellSizePx by remember(project.id) { mutableFloatStateOf(16f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var topOverlayHeightPx by remember { mutableStateOf(0) }
    var bottomOverlayHeightPx by remember { mutableStateOf(0) }
    var viewportInitialized by remember(project.id) { mutableStateOf(false) }
    var viewportContentCenter by remember(project.id) { mutableStateOf<Offset?>(null) }
    var previewVertices by remember(project.id) { mutableStateOf(emptyList<GridPoint>()) }
    var activeToolPanel by remember(project.id) { mutableStateOf<SplitToolPanel?>(null) }
    var showAxisLabels by remember(project.id) { mutableStateOf(false) }
    var boardWidthText by remember(project.id) { mutableStateOf(initialBoardWidth.toString()) }
    var boardHeightText by remember(project.id) { mutableStateOf(initialBoardHeight.toString()) }

    fun contentCenter(size: IntSize, top: Int, bottom: Int): Offset {
        val contentTop = top.coerceIn(0, size.height).toFloat()
        val contentBottom = (size.height - bottom).coerceIn(0, size.height).toFloat()
        val contentLeft = safeLeftPx.coerceIn(0f, size.width.toFloat())
        val contentRight = (size.width - safeRightPx).coerceIn(contentLeft, size.width.toFloat())
        return Offset(contentLeft + (contentRight - contentLeft) / 2f, contentTop + (max(contentTop, contentBottom) - contentTop) / 2f)
    }
    fun fitViewport(size: IntSize, top: Int, bottom: Int) {
        if (size.width <= 0 || size.height <= 0 || top <= 0 || bottom <= 0) return
        val center = contentCenter(size, top, bottom)
        val horizontalMarginPx = with(density) { 36.dp.toPx() }
        val verticalMarginPx = with(density) { 24.dp.toPx() }
        val usableWidth = (size.width - safeLeftPx - safeRightPx - horizontalMarginPx).coerceAtLeast(1f)
        val usableHeight = (size.height - top - bottom - verticalMarginPx).coerceAtLeast(1f)
        cellSizePx =
            min(
                usableWidth / project.width.coerceAtLeast(1),
                usableHeight / project.height.coerceAtLeast(1),
            ).coerceIn(SPLIT_MIN_CELL_SIZE_PX, SPLIT_FIT_MAX_CELL_SIZE_PX)
        viewportPan =
            Offset(
                center.x - project.width * cellSizePx / 2f,
                center.y - project.height * cellSizePx / 2f,
            )
        viewportContentCenter = center
        viewportInitialized = true
    }
    fun updateLayout(size: IntSize, top: Int, bottom: Int) {
        if (size.width <= 0 || size.height <= 0 || top <= 0 || bottom <= 0) return
        if (!viewportInitialized) {
            fitViewport(size, top, bottom)
            return
        }
        val nextCenter = contentCenter(size, top, bottom)
        viewportContentCenter?.let { viewportPan += nextCenter - it }
        viewportContentCenter = nextCenter
    }
    fun updateBottomOverlay(size: IntSize, top: Int, bottom: Int) {
        if (size.width <= 0 || size.height <= 0 || top <= 0 || bottom <= 0) return
        if (!viewportInitialized) {
            fitViewport(size, top, bottom)
            return
        }
        // Expanded tools overlay the workspace without moving a board the user has positioned.
        viewportContentCenter = contentCenter(size, top, bottom)
    }
    fun refreshBoardPreview() {
        val boardWidth = boardWidthText.toIntOrNull() ?: return onBoardPreviewChange(null)
        val boardHeight = boardHeightText.toIntOrNull() ?: return onBoardPreviewChange(null)
        if (boardWidth <= 0 || boardHeight <= 0) return onBoardPreviewChange(null)
        onRememberBoardSize(boardWidth, boardHeight)
        val imageOffsetX = boardPreviewLayout?.imageOffsetX ?: 0
        val imageOffsetY = boardPreviewLayout?.imageOffsetY ?: 0
        onBoardPreviewChange(
            ProjectSplitting.boardLayout(
                width = project.width,
                height = project.height,
                boardWidth = boardWidth,
                boardHeight = boardHeight,
                imageOffsetX = imageOffsetX,
                imageOffsetY = imageOffsetY,
            ),
        )
    }
    fun keepBoardImagePosition() {
        val layout = boardPreviewLayout ?: return
        viewportPan += Offset(layout.imageOffsetX * cellSizePx, layout.imageOffsetY * cellSizePx)
        onBoardPreviewChange(null)
    }
    fun applyBoardSplit() {
        val layout = boardPreviewLayout
        if (layout != null) {
            viewportPan += Offset(layout.imageOffsetX * cellSizePx, layout.imageOffsetY * cellSizePx)
        }
        activeToolPanel = null
        onApplyBoardSplit(layout)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF1E8DB))
                .onSizeChanged {
                    viewportSize = it
                    updateLayout(it, topOverlayHeightPx, bottomOverlayHeightPx)
                },
    ) {
        if (viewportInitialized) {
            SplitCanvas(
                modifier = Modifier.fillMaxSize(),
                project = project,
                strokes = strokes,
                previewVertices = previewVertices,
                interactionMode = interactionMode,
                showAxisLabels = showAxisLabels,
                boardPreviewLayout = boardPreviewLayout,
                boardDragEnabled = activeToolPanel == SplitToolPanel.BoardSplit,
                viewportPan = viewportPan,
                cellSizePx = cellSizePx,
                onPreviewChange = { previewVertices = it },
                onStrokeFinished = {
                    previewVertices = emptyList()
                    onStrokeFinished(it)
                },
                onViewportPan = { viewportPan += it },
                onViewportTransform = { centroid, pan, zoom ->
                    if (!zoom.isFinite() || zoom <= 0f) return@SplitCanvas
                    val oldScale = cellSizePx
                    val newScale = (oldScale * zoom).coerceIn(SPLIT_MIN_CELL_SIZE_PX, SPLIT_MAX_CELL_SIZE_PX)
                    val ratio = newScale / oldScale
                    viewportPan = centroid - (centroid - pan - viewportPan) * ratio
                    cellSizePx = newScale
                },
                onBoardImageDrag = { deltaColumns, deltaRows ->
                    boardPreviewLayout?.let { layout ->
                        val nextOffsetX =
                            (layout.imageOffsetX.toLong() + deltaColumns)
                                .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                                .toInt()
                        val nextOffsetY =
                            (layout.imageOffsetY.toLong() + deltaRows)
                                .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                                .toInt()
                        onBoardPreviewChange(
                            ProjectSplitting.boardLayout(
                                width = project.width,
                                height = project.height,
                                boardWidth = layout.boardWidth,
                                boardHeight = layout.boardHeight,
                                imageOffsetX = nextOffsetX,
                                imageOffsetY = nextOffsetY,
                            ),
                        )
                    }
                },
            )
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged {
                        topOverlayHeightPx = it.height
                        updateLayout(viewportSize, it.height, bottomOverlayHeightPx)
                    }
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                IconButton(modifier = Modifier.size(40.dp), onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SplitTopIconButton(Icons.AutoMirrored.Outlined.Undo, "撤销", canUndo, onUndo)
                SplitTopIconButton(Icons.AutoMirrored.Outlined.Redo, "重做", canRedo, onRedo)
                Button(
                    onClick = onComplete,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("完成拆分", maxLines = 1)
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged {
                        bottomOverlayHeightPx = it.height
                        updateBottomOverlay(viewportSize, topOverlayHeightPx, it.height)
                    }
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            when (activeToolPanel) {
                SplitToolPanel.EqualSplit ->
                    SplitSubToolSurface {
                        SplitToolChip("左右 2 份", Icons.Outlined.Splitscreen, false) {
                            onPreset(EqualSplitPreset.VerticalHalves)
                        }
                        SplitToolChip("上下 2 份", Icons.Outlined.Splitscreen, false) {
                            onPreset(EqualSplitPreset.HorizontalHalves)
                        }
                        SplitToolChip("均分 4 份", Icons.Outlined.BorderAll, false) {
                            onPreset(EqualSplitPreset.FourParts)
                        }
                        SplitToolChip("均分 6 份", Icons.Outlined.BorderAll, false) {
                            onPreset(EqualSplitPreset.SixParts)
                        }
                        SplitToolChip("均分 8 份", Icons.Outlined.BorderAll, false) {
                            onPreset(EqualSplitPreset.EightParts)
                        }
                        SplitToolChip("均分 9 份", Icons.Outlined.BorderAll, false) {
                            onPreset(EqualSplitPreset.NineParts)
                        }
                        SplitToolChip("均分 16 份", Icons.Outlined.BorderAll, false) {
                            onPreset(EqualSplitPreset.SixteenParts)
                        }
                    }

                SplitToolPanel.BoardSplit ->
                    SplitBoardSurface(
                        widthText = boardWidthText,
                        heightText = boardHeightText,
                        layout = boardPreviewLayout,
                        onWidthChange = {
                            boardWidthText = it.filter(Char::isDigit).take(4)
                            refreshBoardPreview()
                        },
                        onHeightChange = {
                            boardHeightText = it.filter(Char::isDigit).take(4)
                            refreshBoardPreview()
                        },
                        onApply = ::applyBoardSplit,
                    )

                SplitToolPanel.Zoom ->
                    SplitZoomSurface(
                        cellSizePx = cellSizePx,
                        onCellSizeChange = { nextSize ->
                            if (!viewportInitialized) return@SplitZoomSurface
                            val center = contentCenter(viewportSize, topOverlayHeightPx, bottomOverlayHeightPx)
                            val oldScale = cellSizePx
                            val newScale = nextSize.coerceIn(SPLIT_MIN_CELL_SIZE_PX, SPLIT_MAX_CELL_SIZE_PX)
                            viewportPan = center - (center - viewportPan) * (newScale / oldScale)
                            cellSizePx = newScale
                        },
                        onFit = { fitViewport(viewportSize, topOverlayHeightPx, bottomOverlayHeightPx) },
                    )

                null -> Unit
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 5.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SplitToolChip(
                        label = "画笔",
                        icon = Icons.Outlined.Brush,
                        selected = interactionMode == SplitInteractionMode.Draw,
                        onClick = {
                            if (activeToolPanel == SplitToolPanel.BoardSplit) keepBoardImagePosition()
                            activeToolPanel = null
                            onModeChange(SplitInteractionMode.Draw)
                        },
                    )
                    SplitToolChip(
                        label = "移动",
                        icon = Icons.Outlined.OpenWith,
                        selected = interactionMode == SplitInteractionMode.Pan,
                        onClick = {
                            if (activeToolPanel == SplitToolPanel.BoardSplit) keepBoardImagePosition()
                            activeToolPanel = null
                            onModeChange(SplitInteractionMode.Pan)
                        },
                    )
                    SplitToolChip(
                        label = "均分",
                        icon = Icons.Outlined.BorderAll,
                        selected = activeToolPanel == SplitToolPanel.EqualSplit,
                        onClick = {
                            if (activeToolPanel == SplitToolPanel.BoardSplit) keepBoardImagePosition()
                            activeToolPanel =
                                if (activeToolPanel == SplitToolPanel.EqualSplit) null else SplitToolPanel.EqualSplit
                        },
                    )
                    SplitToolChip(
                        label = "底板均分",
                        icon = Icons.Outlined.ViewModule,
                        selected = activeToolPanel == SplitToolPanel.BoardSplit,
                        onClick = {
                            activeToolPanel =
                                if (activeToolPanel == SplitToolPanel.BoardSplit) null else SplitToolPanel.BoardSplit
                            if (activeToolPanel == SplitToolPanel.BoardSplit) refreshBoardPreview() else keepBoardImagePosition()
                        },
                    )
                    SplitToolChip(
                        label = "缩放",
                        icon = Icons.Outlined.ZoomIn,
                        selected = activeToolPanel == SplitToolPanel.Zoom,
                        onClick = {
                            if (activeToolPanel == SplitToolPanel.BoardSplit) keepBoardImagePosition()
                            activeToolPanel = if (activeToolPanel == SplitToolPanel.Zoom) null else SplitToolPanel.Zoom
                        },
                    )
                    SplitToolChip(
                        label = "行列号",
                        icon = Icons.Outlined.Straighten,
                        selected = showAxisLabels,
                        onClick = { showAxisLabels = !showAxisLabels },
                    )
                    SplitToolChip("清空", Icons.Outlined.DeleteSweep, false, enabled = canUndo, onClick = onClear)
                }
            }
        }
    }
}

@Composable
private fun SplitTopIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
        IconButton(modifier = Modifier.size(40.dp), enabled = enabled, onClick = onClick) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SplitToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp)) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

@Composable
private fun SplitSubToolSurface(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun SplitZoomSurface(
    cellSizePx: Float,
    onCellSizeChange: (Float) -> Unit,
    onFit: () -> Unit,
) {
    val safeSize = cellSizePx.coerceIn(SPLIT_MIN_CELL_SIZE_PX, SPLIT_MAX_CELL_SIZE_PX)
    val minLog = ln(SPLIT_MIN_CELL_SIZE_PX)
    val maxLog = ln(SPLIT_MAX_CELL_SIZE_PX)
    val sliderValue = ((ln(safeSize) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { onCellSizeChange(safeSize / SPLIT_ZOOM_STEP) },
            ) {
                Icon(Icons.Outlined.ZoomOut, contentDescription = "缩小")
            }
            Slider(
                modifier = Modifier.weight(1f),
                value = sliderValue,
                onValueChange = { position -> onCellSizeChange(exp(minLog + position * (maxLog - minLog))) },
                valueRange = 0f..1f,
            )
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { onCellSizeChange(safeSize * SPLIT_ZOOM_STEP) },
            ) {
                Icon(Icons.Outlined.ZoomIn, contentDescription = "放大")
            }
            IconButton(modifier = Modifier.size(36.dp), onClick = onFit) {
                Icon(Icons.Outlined.FitScreen, contentDescription = "适应画布")
            }
        }
    }
}

@Composable
private fun SplitBoardSurface(
    widthText: String,
    heightText: String,
    layout: BoardSplitLayout?,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = widthText,
                onValueChange = onWidthChange,
                label = { Text("底板列数") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = heightText,
                onValueChange = onHeightChange,
                label = { Text("底板行数") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("图像偏移", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    layout?.let { "${it.imageOffsetX}, ${it.imageOffsetY}" } ?: "-",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Button(
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("应用", maxLines = 1)
            }
        }
    }
}

@Composable
private fun SplitCanvas(
    modifier: Modifier,
    project: BeadProject,
    strokes: List<SplitStroke>,
    previewVertices: List<GridPoint>,
    interactionMode: SplitInteractionMode,
    showAxisLabels: Boolean,
    boardPreviewLayout: BoardSplitLayout?,
    boardDragEnabled: Boolean,
    viewportPan: Offset,
    cellSizePx: Float,
    onPreviewChange: (List<GridPoint>) -> Unit,
    onStrokeFinished: (List<GridPoint>) -> Unit,
    onViewportPan: (Offset) -> Unit,
    onViewportTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onBoardImageDrag: (deltaColumns: Int, deltaRows: Int) -> Unit,
) {
    val bitmap = remember(project.id, project.updatedAt, project.width, project.height) {
        createGridPreviewBitmap(
            cells = project.cells,
            width = project.width,
            height = project.height,
            maxBitmapEdge = SPLIT_CANVAS_MAX_BITMAP_EDGE,
            emptyCellArgb = SPLIT_EMPTY_CELL_ARGB,
        )
    }
    val axisBounds = remember(project.id, project.updatedAt, project.width, project.height) {
        EditorTools.occupiedBounds(project.cells, project.width, project.height)
            ?: EditorTools.GridBounds(0, project.width - 1, 0, project.height - 1)
    }
    val currentMode by rememberUpdatedState(interactionMode)
    val currentPan by rememberUpdatedState(viewportPan)
    val currentCellSize by rememberUpdatedState(cellSizePx)
    val currentOnPreviewChange by rememberUpdatedState(onPreviewChange)
    val currentOnStrokeFinished by rememberUpdatedState(onStrokeFinished)
    val currentOnViewportPan by rememberUpdatedState(onViewportPan)
    val currentOnViewportTransform by rememberUpdatedState(onViewportTransform)
    val currentBoardDragEnabled by rememberUpdatedState(boardDragEnabled)
    val currentOnBoardImageDrag by rememberUpdatedState(onBoardImageDrag)
    val splitColor = MaterialTheme.colorScheme.error
    val previewColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    fun screenToGrid(position: Offset): GridPoint {
        return GridPoint(
            x = ((position.x - currentPan.x) / currentCellSize).roundToInt().coerceIn(0, project.width),
            y = ((position.y - currentPan.y) / currentCellSize).roundToInt().coerceIn(0, project.height),
        )
    }

    Canvas(
        modifier =
            modifier.pointerInput(project.id) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val rawPoints = mutableListOf(screenToGrid(firstDown.position))
                    var usedMultiplePointers = false
                    var boardDragRemainder = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            usedMultiplePointers = true
                            rawPoints.clear()
                            currentOnPreviewChange(emptyList())
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            currentOnViewportTransform(centroid, pan, zoom)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressedCount == 1 && !usedMultiplePointers) {
                            val pan = event.calculatePan()
                            if (currentBoardDragEnabled) {
                                boardDragRemainder += pan
                                val deltaColumns = (boardDragRemainder.x / currentCellSize).roundToInt()
                                val deltaRows = (boardDragRemainder.y / currentCellSize).roundToInt()
                                if (deltaColumns != 0 || deltaRows != 0) {
                                    currentOnBoardImageDrag(deltaColumns, deltaRows)
                                    boardDragRemainder -= Offset(deltaColumns * currentCellSize, deltaRows * currentCellSize)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (currentMode == SplitInteractionMode.Pan) {
                                if (pan != Offset.Zero) {
                                    currentOnViewportPan(pan)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } else {
                                event.changes.firstOrNull { it.pressed }?.position?.let { position ->
                                    val point = screenToGrid(position)
                                    if (rawPoints.lastOrNull() != point) {
                                        rawPoints += point
                                        currentOnPreviewChange(
                                            ProjectSplitting.orthogonalizeStroke(rawPoints, project.width, project.height),
                                        )
                                    }
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!usedMultiplePointers && currentMode == SplitInteractionMode.Draw) {
                        val vertices = ProjectSplitting.orthogonalizeStroke(rawPoints, project.width, project.height)
                        currentOnPreviewChange(emptyList())
                        currentOnStrokeFinished(vertices)
                    }
                }
            },
    ) {
        val imageOffset =
            boardPreviewLayout?.let { layout ->
                Offset(layout.imageOffsetX * cellSizePx, layout.imageOffsetY * cellSizePx)
            } ?: Offset.Zero
        val imagePan = viewportPan + imageOffset
        val boardWidth = project.width * cellSizePx
        val boardHeight = project.height * cellSizePx
        if (boardPreviewLayout != null) {
            drawInfiniteBoardGrid(
                layout = boardPreviewLayout,
                viewportPan = viewportPan,
                cellSizePx = cellSizePx,
            )
        }
        drawRect(
            color = Color(0xFFFFF6EC),
            topLeft = imagePan,
            size = androidx.compose.ui.geometry.Size(boardWidth, boardHeight),
        )
        drawImage(
            image = bitmap.image,
            dstOffset = IntOffset(imagePan.x.roundToInt(), imagePan.y.roundToInt()),
            dstSize = IntSize(boardWidth.roundToInt().coerceAtLeast(1), boardHeight.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None,
        )
        if (cellSizePx >= 4f) {
            val gridColor = Color(0x4D6F6257)
            val firstVisibleColumn = ((-imagePan.x) / cellSizePx).toInt().coerceIn(0, project.width)
            val lastVisibleColumn =
                ((size.width - imagePan.x) / cellSizePx).toInt().plus(1).coerceIn(firstVisibleColumn, project.width)
            val firstVisibleRow = ((-imagePan.y) / cellSizePx).toInt().coerceIn(0, project.height)
            val lastVisibleRow =
                ((size.height - imagePan.y) / cellSizePx).toInt().plus(1).coerceIn(firstVisibleRow, project.height)
            for (x in firstVisibleColumn..lastVisibleColumn) {
                val px = imagePan.x + x * cellSizePx
                drawLine(gridColor, Offset(px, imagePan.y), Offset(px, imagePan.y + boardHeight), 1f)
            }
            for (y in firstVisibleRow..lastVisibleRow) {
                val py = imagePan.y + y * cellSizePx
                drawLine(gridColor, Offset(imagePan.x, py), Offset(imagePan.x + boardWidth, py), 1f)
            }
        }
        strokes.forEach { stroke ->
            stroke.segments.forEach { segment ->
                drawGridSegment(segment, imagePan, cellSizePx, splitColor, max(2.5f, cellSizePx * 0.12f))
            }
        }
        ProjectSplitting.segmentsForVertices(previewVertices).forEach { segment ->
            drawGridSegment(segment, imagePan, cellSizePx, previewColor, max(2.5f, cellSizePx * 0.12f))
        }
        drawRect(
            color = outlineColor,
            topLeft = imagePan,
            size = androidx.compose.ui.geometry.Size(boardWidth, boardHeight),
            style = Stroke(width = max(1.5f, cellSizePx * 0.06f)),
        )
        if (showAxisLabels) {
            drawSplitAxisLabels(
                projectWidth = project.width,
                projectHeight = project.height,
                axisBounds = axisBounds,
                viewportPan = imagePan,
                cellSizePx = cellSizePx,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInfiniteBoardGrid(
    layout: BoardSplitLayout,
    viewportPan: Offset,
    cellSizePx: Float,
 ) {
    val boardWidthPx = layout.boardWidth * cellSizePx
    val boardHeightPx = layout.boardHeight * cellSizePx
    if (boardWidthPx <= 0f || boardHeightPx <= 0f) return
    val firstColumn = floor((-viewportPan.x / boardWidthPx).toDouble()).toInt() - 1
    val lastColumn = ceil(((size.width - viewportPan.x) / boardWidthPx).toDouble()).toInt() + 1
    val firstRow = floor((-viewportPan.y / boardHeightPx).toDouble()).toInt() - 1
    val lastRow = ceil(((size.height - viewportPan.y) / boardHeightPx).toDouble()).toInt() + 1
    val visibleColumnCount = (lastColumn.toLong() - firstColumn.toLong()).coerceAtLeast(0L)
    val visibleRowCount = (lastRow.toLong() - firstRow.toLong()).coerceAtLeast(0L)
    val fillA = Color(0xFFFFF8ED)
    val fillB = Color(0xFFF5EADC)
    if (visibleColumnCount * visibleRowCount <= SPLIT_MAX_VISIBLE_BOARD_TILES) {
        for (row in firstRow until lastRow) {
            for (column in firstColumn until lastColumn) {
                drawRect(
                    color = if ((row + column) and 1 == 0) fillA else fillB,
                    topLeft = Offset(viewportPan.x + column * boardWidthPx, viewportPan.y + row * boardHeightPx),
                    size = androidx.compose.ui.geometry.Size(boardWidthPx, boardHeightPx),
                )
            }
        }
    } else {
        drawRect(fillA)
    }
    val lineColor = Color(0xFF007A78)
    val lineOutline = Color.White.copy(alpha = 0.96f)
    val firstVertical = floor((-viewportPan.x / boardWidthPx).toDouble()).toInt() - 1
    val lastVertical = ceil(((size.width - viewportPan.x) / boardWidthPx).toDouble()).toInt() + 1
    val verticalStep = ceil((lastVertical.toLong() - firstVertical + 1L).toDouble() / SPLIT_MAX_VISIBLE_BOARD_LINES).toInt().coerceAtLeast(1)
    for (index in firstVertical..lastVertical step verticalStep) {
        val x = viewportPan.x + index * boardWidthPx
        drawLine(lineOutline, Offset(x, 0f), Offset(x, size.height), 6f)
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 3f)
    }
    val firstHorizontal = floor((-viewportPan.y / boardHeightPx).toDouble()).toInt() - 1
    val lastHorizontal = ceil(((size.height - viewportPan.y) / boardHeightPx).toDouble()).toInt() + 1
    val horizontalStep = ceil((lastHorizontal.toLong() - firstHorizontal + 1L).toDouble() / SPLIT_MAX_VISIBLE_BOARD_LINES).toInt().coerceAtLeast(1)
    for (index in firstHorizontal..lastHorizontal step horizontalStep) {
        val y = viewportPan.y + index * boardHeightPx
        drawLine(lineOutline, Offset(0f, y), Offset(size.width, y), 6f)
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 3f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSplitAxisLabels(
    projectWidth: Int,
    projectHeight: Int,
    axisBounds: EditorTools.GridBounds,
    viewportPan: Offset,
    cellSizePx: Float,
) {
    if (projectWidth <= 0 || projectHeight <= 0) return
    val boardLeft = viewportPan.x + axisBounds.startColumn * cellSizePx
    val boardTop = viewportPan.y + axisBounds.startRow * cellSizePx
    val boardRight = viewportPan.x + (axisBounds.endColumn + 1) * cellSizePx
    val boardBottom = viewportPan.y + (axisBounds.endRow + 1) * cellSizePx
    if (boardRight <= 0f || boardBottom <= 0f || boardLeft >= size.width || boardTop >= size.height) return
    val firstColumn = floor((-viewportPan.x / cellSizePx).toDouble()).toInt().coerceIn(axisBounds.startColumn, axisBounds.endColumn)
    val lastColumn = floor(((size.width - viewportPan.x) / cellSizePx).toDouble()).toInt().coerceIn(firstColumn, axisBounds.endColumn)
    val firstRow = floor((-viewportPan.y / cellSizePx).toDouble()).toInt().coerceIn(axisBounds.startRow, axisBounds.endRow)
    val lastRow = floor(((size.height - viewportPan.y) / cellSizePx).toDouble()).toInt().coerceIn(firstRow, axisBounds.endRow)
    val textSize = (cellSizePx * 0.44f).coerceIn(10f, 18f)
    val rowDigits = axisBounds.height.toString().length.coerceAtLeast(1)
    val columnDigits = axisBounds.width.toString().length.coerceAtLeast(1)
    val rowGutterWidth = max(24.dp.toPx(), textSize * rowDigits * 0.66f + 12.dp.toPx())
    val columnGutterHeight = max(22.dp.toPx(), textSize * 1.55f + 8.dp.toPx())
    val rowStep = ceil((textSize * 1.2f + 4.dp.toPx()) / cellSizePx).toInt().coerceAtLeast(1)
    val columnStep = ceil((textSize * columnDigits * 0.68f + 6.dp.toPx()) / cellSizePx).toInt().coerceAtLeast(1)
    val leftGutterLeft = if (boardLeft >= rowGutterWidth) boardLeft - rowGutterWidth else 0f
    val rightGutterLeft = if (boardRight + rowGutterWidth <= size.width) boardRight else size.width - rowGutterWidth
    val topGutterTop = if (boardTop >= columnGutterHeight) boardTop - columnGutterHeight else 0f
    val bottomGutterTop = if (boardBottom + columnGutterHeight <= size.height) boardBottom else size.height - columnGutterHeight
    val sideTop = max(0f, boardTop)
    val sideBottom = min(size.height, boardBottom)
    val horizontalLeft = max(0f, boardLeft)
    val horizontalRight = min(size.width, boardRight)
    val panelColor = Color.White.copy(alpha = 0.86f)
    val outlineColor = Color(0xFF8C8177).copy(alpha = 0.35f)

    fun drawBackdrop(left: Float, top: Float, right: Float, bottom: Float) {
        if (right <= left || bottom <= top) return
        drawRect(panelColor, Offset(left, top), androidx.compose.ui.geometry.Size(right - left, bottom - top))
        drawLine(outlineColor, Offset(left, top), Offset(right, top), 1f)
    }
    drawBackdrop(leftGutterLeft, sideTop, leftGutterLeft + rowGutterWidth, sideBottom)
    drawBackdrop(rightGutterLeft, sideTop, rightGutterLeft + rowGutterWidth, sideBottom)
    drawBackdrop(horizontalLeft, topGutterTop, horizontalRight, topGutterTop + columnGutterHeight)
    drawBackdrop(horizontalLeft, bottomGutterTop, horizontalRight, bottomGutterTop + columnGutterHeight)

    val paint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(28, 25, 23)
            this.textSize = textSize
            textAlign = AndroidPaint.Align.CENTER
            isFakeBoldText = true
            isSubpixelText = true
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.argb(48, 0, 0, 0))
        }

    for (column in firstColumn..lastColumn) {
        val visibleColumn = column - axisBounds.startColumn
        if (visibleColumn != 0 && visibleColumn != axisBounds.width - 1 && visibleColumn % columnStep != 0) continue
        val centerX = viewportPan.x + (column + 0.5f) * cellSizePx
        val topBaseline = topGutterTop + columnGutterHeight / 2f - (paint.ascent() + paint.descent()) / 2f
        val bottomBaseline = bottomGutterTop + columnGutterHeight / 2f - (paint.ascent() + paint.descent()) / 2f
        drawContext.canvas.nativeCanvas.drawText((visibleColumn + 1).toString(), centerX, topBaseline, paint)
        drawContext.canvas.nativeCanvas.drawText((visibleColumn + 1).toString(), centerX, bottomBaseline, paint)
    }
    for (row in firstRow..lastRow) {
        val visibleRow = row - axisBounds.startRow
        if (visibleRow != 0 && visibleRow != axisBounds.height - 1 && visibleRow % rowStep != 0) continue
        val centerY = viewportPan.y + (row + 0.5f) * cellSizePx
        val baseline = centerY - (paint.ascent() + paint.descent()) / 2f
        drawContext.canvas.nativeCanvas.drawText((visibleRow + 1).toString(), leftGutterLeft + rowGutterWidth / 2f, baseline, paint)
        drawContext.canvas.nativeCanvas.drawText((visibleRow + 1).toString(), rightGutterLeft + rowGutterWidth / 2f, baseline, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridSegment(
    segment: GridLineSegment,
    viewportPan: Offset,
    cellSizePx: Float,
    color: Color,
    strokeWidth: Float,
) {
    drawLine(
        color = color,
        start = Offset(viewportPan.x + segment.start.x * cellSizePx, viewportPan.y + segment.start.y * cellSizePx),
        end = Offset(viewportPan.x + segment.end.x * cellSizePx, viewportPan.y + segment.end.y * cellSizePx),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private const val SPLIT_EMPTY_CELL_ARGB = 0x00FFFFFF
private const val SPLIT_CANVAS_MAX_BITMAP_EDGE = 512
private const val SPLIT_MIN_CELL_SIZE_PX = 0.001f
private const val SPLIT_FIT_MAX_CELL_SIZE_PX = 36f
private const val SPLIT_MAX_CELL_SIZE_PX = 96f
private const val SPLIT_ZOOM_STEP = 1.25f
private const val SPLIT_MAX_VISIBLE_BOARD_TILES = 2_000L
private const val SPLIT_MAX_VISIBLE_BOARD_LINES = 256.0
