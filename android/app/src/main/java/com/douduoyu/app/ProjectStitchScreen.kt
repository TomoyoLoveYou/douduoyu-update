@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.graphics.Paint as AndroidPaint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class StitchScreenStage {
    Select,
    Arrange,
}

private typealias StitchLayerBitmap = GridPreviewBitmap

private data class StitchConflictBounds(
    val left: Long,
    val top: Long,
    val rightExclusive: Long,
    val bottomExclusive: Long,
)

/**
 * Two-stage project stitching flow. The caller owns navigation and persists the composed project.
 */
@Composable
fun ProjectStitchScreen(
    projects: List<BeadProject>,
    initialProjectId: String?,
    newProjectTitle: String,
    onBack: () -> Unit,
    onComplete: (BeadProject) -> Unit,
) {
    var stage by remember { mutableStateOf(StitchScreenStage.Select) }
    var selectedProjectIds by remember(projects, initialProjectId) {
        mutableStateOf(initialProjectId?.takeIf { id -> projects.any { it.id == id } }?.let(::setOf).orEmpty())
    }
    var layers by remember { mutableStateOf<List<StitchLayer>>(emptyList()) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var referenceLayerId by remember { mutableStateOf<String?>(null) }
    var snapEnabled by remember { mutableStateOf(true) }
    var trimEnabled by remember { mutableStateOf(false) }
    var messageDialog by remember { mutableStateOf<String?>(null) }
    var paletteChoices by remember { mutableStateOf<List<StitchPalette>?>(null) }
    var selectedPaletteChoice by remember { mutableStateOf<StitchPalette?>(null) }
    var activeTargetPalette by remember { mutableStateOf<StitchPalette?>(null) }
    var pendingConversionPlan by remember { mutableStateOf<PaletteMappingPlan?>(null) }

    val paletteContentKey =
        layers.map { layer ->
            listOf(
                layer.project.id,
                layer.project.updatedAt,
                layer.project.colorScheme,
                layer.project.customColorSchemeId,
            )
        }
    val targetPaletteOptions = remember(paletteContentKey) { ProjectStitching.targetPaletteOptions(layers) }
    val previewTargetPalette =
        activeTargetPalette
            ?.let { active ->
                targetPaletteOptions.choices.firstOrNull { choice -> choice.sameTargetAs(active) }
            }
            ?: targetPaletteOptions.automatic
            ?: targetPaletteOptions.choices.firstOrNull()
    val previewPalettePlan = remember(paletteContentKey, previewTargetPalette) {
        previewTargetPalette?.let { ProjectStitching.buildPalettePlan(layers, it) }
    }
    val overlapConflicts = remember(layers, previewPalettePlan) {
        previewPalettePlan?.let { ProjectStitching.detectOverlaps(layers, it) }.orEmpty()
    }

    fun finishWithPlan(
        plan: PaletteMappingPlan,
        allowNearestColorMapping: Boolean = false,
    ) {
        when (
            val composed =
                ProjectStitching.compose(
                    layers = layers,
                    plan = plan,
                    overlapPolicy = OverlapPolicy.Reject,
                    allowNearestColorMapping = allowNearestColorMapping,
                )
        ) {
            is StitchComposeResult.Success -> {
                val result =
                    ProjectStitching.buildProject(
                        composed = composed,
                        target = plan.target,
                        sourceRegions = ProjectStitching.sourceRegions(layers, composed),
                        title = newProjectTitle,
                        beadsPerPack = layers.firstOrNull()?.project?.beadsPerPack ?: 500,
                    )
                onComplete(result)
            }

            is StitchComposeResult.Failure -> {
                messageDialog = stitchFailureMessage(composed)
            }
        }
    }

    fun reviewTargetPalette(
        target: StitchPalette,
        requireConversionConfirmation: Boolean,
    ) {
        activeTargetPalette = target
        val plan = ProjectStitching.buildPalettePlan(layers, target)
        if (plan.hasUnresolvedColors) {
            messageDialog = "有 ${plan.impact.unresolvedCells} 格颜色无法转换到 ${target.label}，请改选其他目标配色。"
            return
        }
        val conflicts = ProjectStitching.detectOverlaps(layers, plan)
        if (conflicts.isNotEmpty()) {
            messageDialog = "有 ${conflicts.size} 格不同颜色互相重叠，红框处调整后才能完成拼接。"
            return
        }
        if (requireConversionConfirmation || plan.impact.nearestColorCells > 0) {
            pendingConversionPlan = plan
        } else {
            finishWithPlan(plan)
        }
    }

    fun requestCompletion() {
        if (overlapConflicts.isNotEmpty()) {
            messageDialog =
                "检测到 ${overlapConflicts.size} 格不同颜色重叠。请先移动项目，消除画布中的红框后再完成拼接。"
            return
        }
        val options = ProjectStitching.targetPaletteOptions(layers)
        if (options.requiresUserChoice) {
            paletteChoices = options.choices
            selectedPaletteChoice =
                activeTargetPalette
                    ?.let { active -> options.choices.firstOrNull { it.sameTargetAs(active) } }
                    ?: options.choices.firstOrNull()
            return
        }
        val automatic = options.automatic
        if (automatic == null) {
            messageDialog = "没有可用于拼接的目标配色。"
        } else {
            reviewTargetPalette(automatic, requireConversionConfirmation = false)
        }
    }

    fun enterArrangement() {
        val selectedProjects = projects.filter { it.id in selectedProjectIds }
        if (selectedProjects.size < 2) {
            messageDialog = "请至少选择两个项目。"
            return
        }
        val emptyProject =
            selectedProjects.firstOrNull { project ->
                EditorTools.occupiedBounds(project.cells, project.width, project.height) == null
            }
        if (emptyProject != null) {
            messageDialog = "“${emptyProject.title}”没有可拼接的色块，请重新选择。"
            return
        }
        layers = ProjectStitching.initialHorizontalLayout(selectedProjects, trimmed = trimEnabled, gap = 0)
        val preferredId = initialProjectId?.takeIf { id -> selectedProjects.any { it.id == id } }
        selectedLayerId = preferredId ?: selectedProjects.first().id
        referenceLayerId = selectedProjects.firstOrNull { it.id != selectedLayerId }?.id
        stage = StitchScreenStage.Arrange
    }

    BackHandler(
        enabled = paletteChoices == null && pendingConversionPlan == null && messageDialog == null,
    ) {
        if (stage == StitchScreenStage.Arrange) {
            stage = StitchScreenStage.Select
        } else {
            onBack()
        }
    }

    when (stage) {
        StitchScreenStage.Select ->
            StitchProjectSelection(
                projects = projects,
                selectedIds = selectedProjectIds,
                onToggle = { projectId ->
                    selectedProjectIds =
                        if (projectId in selectedProjectIds) {
                            selectedProjectIds - projectId
                        } else {
                            selectedProjectIds + projectId
                        }
                },
                onBack = onBack,
                onContinue = ::enterArrangement,
            )

        StitchScreenStage.Arrange ->
            StitchArrangement(
                layers = layers,
                selectedLayerId = selectedLayerId,
                referenceLayerId = referenceLayerId,
                snapEnabled = snapEnabled,
                trimEnabled = trimEnabled,
                overlapConflicts = overlapConflicts,
                onBack = { stage = StitchScreenStage.Select },
                onSelectLayer = { projectId ->
                    if (projectId != selectedLayerId) {
                        referenceLayerId = selectedLayerId
                        selectedLayerId = projectId
                    }
                },
                onMoveLayer = { projectId, x, y ->
                    val moving = layers.firstOrNull { it.project.id == projectId } ?: return@StitchArrangement
                    val moved = ProjectStitching.move(moving, x = x, y = y)
                    val settled =
                        if (snapEnabled) {
                            ProjectStitching.snap(
                                moving = moved,
                                others = layers.filterNot { it.project.id == projectId },
                            ).layer
                        } else {
                            moved
                        }
                    layers = layers.map { if (it.project.id == projectId) settled else it }
                },
                onToggleSnap = { snapEnabled = !snapEnabled },
                onToggleTrim = {
                    trimEnabled = !trimEnabled
                    layers = layers.map { ProjectStitching.setTrimmed(it, trimEnabled) }
                },
                onPlaceRelative = { side ->
                    val selected = layers.firstOrNull { it.project.id == selectedLayerId }
                    val reference = layers.firstOrNull { it.project.id == referenceLayerId }
                    if (selected == null || reference == null || selected === reference) {
                        messageDialog = "先点选要移动的项目，再点选另一个项目作为参照。"
                    } else {
                        val placed = ProjectStitching.placeRelative(selected, reference, side)
                        layers = layers.map { if (it.project.id == selected.project.id) placed else it }
                    }
                },
                onAlignEdge = { edge ->
                    val selected = layers.firstOrNull { it.project.id == selectedLayerId }
                    val reference = layers.firstOrNull { it.project.id == referenceLayerId }
                    if (selected == null || reference == null || selected === reference) {
                        messageDialog = "先点选要移动的项目，再点选另一个项目作为参照。"
                    } else {
                        val aligned = ProjectStitching.alignEdge(selected, reference, edge)
                        layers = layers.map { if (it.project.id == selected.project.id) aligned else it }
                    }
                },
                onComplete = ::requestCompletion,
            )
    }

    paletteChoices?.let { choices ->
        val bodyMaxHeight = stitchDialogBodyMaxHeight()
        AlertDialog(
            onDismissRequest = {
                paletteChoices = null
                selectedPaletteChoice = null
            },
            title = { Text("选择目标配色") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = bodyMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Text(
                            "这些项目来自不同品牌。所有颜色会统一转换到一个配色方案。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                    items(choices) { choice ->
                        val selected = selectedPaletteChoice?.sameTargetAs(choice) == true
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaletteChoice = choice }
                                    .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { selectedPaletteChoice = choice },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${choice.colors.size} 色",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedPaletteChoice != null,
                    onClick = {
                        val target = selectedPaletteChoice ?: return@TextButton
                        paletteChoices = null
                        selectedPaletteChoice = null
                        reviewTargetPalette(target, requireConversionConfirmation = true)
                    },
                ) {
                    Text("查看转换影响")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        paletteChoices = null
                        selectedPaletteChoice = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    pendingConversionPlan?.let { plan ->
        val impact = plan.impact
        val nearestMappings =
            remember(plan) {
                plan.mappings.values
                    .filter { it.kind == ColorMappingKind.NearestRgb }
                    .sortedByDescending(StitchColorMapping::cellCount)
            }
        val bodyMaxHeight = stitchDialogBodyMaxHeight()
        AlertDialog(
            onDismissRequest = { pendingConversionPlan = null },
            title = { Text("确认颜色转换") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = bodyMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("目标配色：${plan.target.label}（${plan.target.colors.size} 色）")
                    Text(
                        "共 ${impact.totalCells} 格；${impact.rewrittenCells} 格会改用目标色号，" +
                            "其中 ${impact.colorChangedCells} 格颜色会发生变化。",
                    )
                    if (impact.nearestColorCells > 0) {
                        Text(
                            "${impact.nearestColorCells} 格将使用最接近的颜色。",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "近似色明细",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        nearestMappings.take(STITCH_MAX_MAPPING_DETAILS).forEach { mapping ->
                            NearestColorMappingRow(mapping)
                        }
                        val remainingMappings = nearestMappings.size - STITCH_MAX_MAPPING_DETAILS
                        if (remainingMappings > 0) {
                            Text(
                                "另有 $remainingMappings 种近似色转换",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "原项目不会被修改。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConversionPlan = null
                        finishWithPlan(plan, allowNearestColorMapping = true)
                    },
                ) {
                    Text("转换并完成")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConversionPlan = null }) {
                    Text("返回调整")
                }
            },
        )
    }

    messageDialog?.let { message ->
        val bodyMaxHeight = stitchDialogBodyMaxHeight()
        AlertDialog(
            onDismissRequest = { messageDialog = null },
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text("无法完成") },
            text = {
                Column(modifier = Modifier.heightIn(max = bodyMaxHeight).verticalScroll(rememberScrollState())) {
                    Text(message)
                }
            },
            confirmButton = {
                TextButton(onClick = { messageDialog = null }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun stitchDialogBodyMaxHeight() =
    (
        LocalConfiguration.current.screenHeightDp -
            (STITCH_DIALOG_NON_BODY_HEIGHT_DP * LocalDensity.current.fontScale.coerceAtLeast(1f)).roundToInt()
    )
        .coerceIn(STITCH_DIALOG_MIN_BODY_HEIGHT_DP, STITCH_DIALOG_MAX_BODY_HEIGHT_DP)
        .dp

@Composable
private fun NearestColorMappingRow(mapping: StitchColorMapping) {
    val source = mapping.sourceColor
    val target = mapping.targetColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (source != null) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(source.argb)),
            )
        }
        Text(
            "${source?.code ?: mapping.source.colorId} → ${target?.code ?: "未匹配"}（${mapping.cellCount} 格）",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (target != null) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(target.argb)),
            )
        }
    }
}

@Composable
private fun StitchProjectSelection(
    projects: List<BeadProject>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择拼接项目") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "已选 ${selectedIds.size} 个",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        onClick = onContinue,
                        enabled = selectedIds.size >= 2,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    ) {
                        Text("确认并排列")
                    }
                }
            }
        },
    ) { contentPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有可拼接的项目", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    StitchProjectChoiceCard(
                        project = project,
                        selected = project.id in selectedIds,
                        onClick = { onToggle(project.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchProjectChoiceCard(
    project: BeadProject,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(92.dp)
                        .heightIn(max = 72.dp)
                        .aspectRatio((project.width.toFloat() / project.height.coerceAtLeast(1)).coerceIn(0.7f, 1.8f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF6EC)),
            ) {
                StitchProjectThumbnail(project)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${project.width} x ${project.height} · ${BeadPalette.schemeLabel(project.colorScheme, project.customColorSchemeId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
    }
}

@Composable
private fun StitchProjectThumbnail(project: BeadProject) {
    val bitmap = remember(project.id, project.updatedAt, project.width, project.height) {
        createStitchBitmap(
            cells = project.cells,
            width = project.width,
            height = project.height,
            maxBitmapEdge = STITCH_THUMBNAIL_MAX_BITMAP_EDGE,
        ).image
    }
    Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
        val scale =
            min(
                size.width / bitmap.width.coerceAtLeast(1).toFloat(),
                size.height / bitmap.height.coerceAtLeast(1).toFloat(),
            )
        val previewWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val previewHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        drawImage(
            image = bitmap,
            dstOffset =
                IntOffset(
                    ((size.width - previewWidth) / 2f).roundToInt(),
                    ((size.height - previewHeight) / 2f).roundToInt(),
                ),
            dstSize = IntSize(previewWidth, previewHeight),
            filterQuality = FilterQuality.None,
        )
    }
}

@Composable
private fun StitchArrangement(
    layers: List<StitchLayer>,
    selectedLayerId: String?,
    referenceLayerId: String?,
    snapEnabled: Boolean,
    trimEnabled: Boolean,
    overlapConflicts: List<OverlapConflict>,
    onBack: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onMoveLayer: (projectId: String, x: Int, y: Int) -> Unit,
    onToggleSnap: () -> Unit,
    onToggleTrim: () -> Unit,
    onPlaceRelative: (StitchSide) -> Unit,
    onAlignEdge: (StitchEdge) -> Unit,
    onComplete: () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingInsets = WindowInsets.safeDrawing
    val safeLeftPx = safeDrawingInsets.getLeft(density, layoutDirection).toFloat()
    val safeRightPx = safeDrawingInsets.getRight(density, layoutDirection).toFloat()
    val horizontalViewportMarginPx = with(density) { STITCH_VIEWPORT_HORIZONTAL_MARGIN_DP.dp.toPx() }
    val verticalViewportMarginPx = with(density) { STITCH_VIEWPORT_VERTICAL_MARGIN_DP.dp.toPx() }
    val layerIdentity = layers.map { it.project.id }
    var viewportPan by remember { mutableStateOf(Offset.Zero) }
    var cellSizePx by remember { mutableFloatStateOf(16f) }
    var dragPreviewWorld by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var topOverlayHeightPx by remember { mutableStateOf(0) }
    var bottomOverlayHeightPx by remember { mutableStateOf(0) }
    var viewportInitialized by remember(layerIdentity) { mutableStateOf(false) }
    var viewportContentCenter by remember(layerIdentity) { mutableStateOf<Offset?>(null) }
    var quickMoveExpanded by remember { mutableStateOf(false) }

    val selectedLayer = layers.firstOrNull { it.project.id == selectedLayerId }
    val referenceLayer = layers.firstOrNull { it.project.id == referenceLayerId }
    val bitmapKey = layers.map { listOf(it.project.id, it.project.updatedAt, it.trimmed, it.width, it.height) }
    val bitmaps = remember(bitmapKey) {
        layers.associate { layer ->
            layer.project.id to
                createStitchBitmap(
                    cells = layer.cells,
                    width = layer.width,
                    height = layer.height,
                    maxBitmapEdge = STITCH_CANVAS_MAX_BITMAP_EDGE,
                )
        }
    }

    fun contentCenter(
        size: IntSize,
        topOverlayHeight: Int,
        bottomOverlayHeight: Int,
    ): Offset {
        val contentTop = topOverlayHeight.coerceIn(0, size.height).toFloat()
        val contentBottom = (size.height - bottomOverlayHeight).coerceIn(0, size.height).toFloat()
        val safeBottom = max(contentTop, contentBottom)
        val contentLeft = safeLeftPx.coerceIn(0f, size.width.toFloat())
        val contentRight = (size.width - safeRightPx).coerceIn(contentLeft, size.width.toFloat())
        return Offset(contentLeft + (contentRight - contentLeft) / 2f, contentTop + (safeBottom - contentTop) / 2f)
    }

    fun fitViewport(
        size: IntSize,
        topOverlayHeight: Int,
        bottomOverlayHeight: Int,
    ) {
        if (
            size.width <= 0 ||
            size.height <= 0 ||
            topOverlayHeight <= 0 ||
            bottomOverlayHeight <= 0 ||
            layers.isEmpty()
        ) {
            return
        }
        val left = layers.minOf { it.bounds.left }
        val top = layers.minOf { it.bounds.top }
        val right = layers.maxOf { it.bounds.rightExclusive }
        val bottom = layers.maxOf { it.bounds.bottomExclusive }
        val worldWidth = (right - left).coerceAtLeast(1L)
        val worldHeight = (bottom - top).coerceAtLeast(1L)
        val center = contentCenter(size, topOverlayHeight, bottomOverlayHeight)
        val contentHeight =
            (size.height - topOverlayHeight - bottomOverlayHeight).toFloat().coerceAtLeast(1f)
        val usableWidth =
            (size.width - safeLeftPx - safeRightPx - horizontalViewportMarginPx * 2f).coerceAtLeast(1f)
        val usableHeight = (contentHeight - verticalViewportMarginPx * 2f).coerceAtLeast(1f)
        val fittedCellSize =
            min(
                usableWidth.toDouble() / worldWidth.toDouble(),
                usableHeight.toDouble() / worldHeight.toDouble(),
            )
        cellSizePx = fittedCellSize.coerceIn(STITCH_MIN_CELL_SIZE_PX.toDouble(), STITCH_FIT_MAX_CELL_SIZE_PX.toDouble()).toFloat()
        val worldCenterX = left.toDouble() + worldWidth.toDouble() / 2.0
        val worldCenterY = top.toDouble() + worldHeight.toDouble() / 2.0
        viewportPan =
            Offset(
                x = (center.x.toDouble() - worldCenterX * cellSizePx.toDouble()).toFloat(),
                y = (center.y.toDouble() - worldCenterY * cellSizePx.toDouble()).toFloat(),
            )
        viewportContentCenter = center
        viewportInitialized = true
    }

    fun updateViewportLayout(
        size: IntSize,
        topOverlayHeight: Int,
        bottomOverlayHeight: Int,
    ) {
        if (
            size.width <= 0 ||
            size.height <= 0 ||
            topOverlayHeight <= 0 ||
            bottomOverlayHeight <= 0
        ) {
            return
        }
        if (!viewportInitialized) {
            fitViewport(size, topOverlayHeight, bottomOverlayHeight)
            return
        }
        val nextCenter = contentCenter(size, topOverlayHeight, bottomOverlayHeight)
        viewportContentCenter?.let { previousCenter ->
            viewportPan += nextCenter - previousCenter
        }
        viewportContentCenter = nextCenter
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF1E8DB))
                .onSizeChanged { newSize ->
                    viewportSize = newSize
                    updateViewportLayout(newSize, topOverlayHeightPx, bottomOverlayHeightPx)
                },
    ) {
        if (viewportInitialized) {
            StitchCanvas(
                modifier = Modifier.fillMaxSize(),
                layers = layers,
                bitmaps = bitmaps,
                selectedLayerId = selectedLayerId,
                dragPreviewWorld = dragPreviewWorld,
                viewportPan = viewportPan,
                cellSizePx = cellSizePx,
                overlapConflicts = overlapConflicts,
                onSelectLayer = onSelectLayer,
                onDragPreviewChange = { dragPreviewWorld = it },
                onDropLayer = { projectId, worldDelta ->
                    val layer = layers.firstOrNull { it.project.id == projectId } ?: return@StitchCanvas
                    onMoveLayer(
                        projectId,
                        (layer.x.toDouble() + worldDelta.x.toDouble()).roundToInt(),
                        (layer.y.toDouble() + worldDelta.y.toDouble()).roundToInt(),
                    )
                    dragPreviewWorld = Offset.Zero
                },
                onViewportPan = { delta -> viewportPan += delta },
                onViewportTransform = { centroid, panDelta, zoomChange ->
                    if (!zoomChange.isFinite() || zoomChange <= 0f) return@StitchCanvas
                    val oldScale = cellSizePx
                    val newScale = (oldScale * zoomChange).coerceIn(STITCH_MIN_CELL_SIZE_PX, STITCH_MAX_CELL_SIZE_PX)
                    val ratio = newScale / oldScale
                    viewportPan = centroid - (centroid - panDelta - viewportPan) * ratio
                    cellSizePx = newScale
                },
            )
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { size ->
                        topOverlayHeightPx = size.height
                        updateViewportLayout(viewportSize, size.height, bottomOverlayHeightPx)
                    }
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                IconButton(modifier = Modifier.size(40.dp), onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回选择")
                }
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("完成拼接", maxLines = 1)
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        bottomOverlayHeightPx = size.height
                        updateViewportLayout(viewportSize, topOverlayHeightPx, size.height)
                    }
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 3.dp,
            ) {
                Text(
                    text =
                        if (overlapConflicts.isNotEmpty()) {
                            "有 ${overlapConflicts.size} 格颜色冲突，请调整红框位置"
                        } else if (selectedLayer == null) {
                            "点选一个项目后拖动"
                        } else if (referenceLayer == null) {
                            "当前：${selectedLayer.project.title}"
                        } else {
                            "当前：${selectedLayer.project.title} · 参照：${referenceLayer.project.title}"
                        },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (overlapConflicts.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (quickMoveExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 3.dp,
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val enabled = selectedLayer != null && referenceLayer != null
                        StitchPlacementButton("上方", enabled) { onPlaceRelative(StitchSide.Above) }
                        StitchPlacementButton("下方", enabled) { onPlaceRelative(StitchSide.Below) }
                        StitchPlacementButton("左侧", enabled) { onPlaceRelative(StitchSide.Left) }
                        StitchPlacementButton("右侧", enabled) { onPlaceRelative(StitchSide.Right) }
                        StitchPlacementButton("上边对齐", enabled) { onAlignEdge(StitchEdge.Top) }
                        StitchPlacementButton("下边对齐", enabled) { onAlignEdge(StitchEdge.Bottom) }
                        StitchPlacementButton("左边对齐", enabled) { onAlignEdge(StitchEdge.Left) }
                        StitchPlacementButton("右边对齐", enabled) { onAlignEdge(StitchEdge.Right) }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 5.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = snapEnabled,
                        onClick = onToggleSnap,
                        label = { Text("吸附") },
                        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        colors = stitchToggleColors(),
                    )
                    FilterChip(
                        selected = trimEnabled,
                        onClick = onToggleTrim,
                        label = { Text("裁空") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        colors = stitchToggleColors(),
                    )
                    FilterChip(
                        selected = quickMoveExpanded,
                        onClick = { quickMoveExpanded = !quickMoveExpanded },
                        label = { Text("快捷移动") },
                        leadingIcon = { Icon(Icons.Outlined.OpenWith, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        colors = stitchToggleColors(),
                    )
                }
            }
        }
    }
}

@Composable
private fun stitchToggleColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    )

@Composable
private fun StitchPlacementButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
private fun StitchCanvas(
    modifier: Modifier,
    layers: List<StitchLayer>,
    bitmaps: Map<String, StitchLayerBitmap>,
    selectedLayerId: String?,
    dragPreviewWorld: Offset,
    viewportPan: Offset,
    cellSizePx: Float,
    overlapConflicts: List<OverlapConflict>,
    onSelectLayer: (String) -> Unit,
    onDragPreviewChange: (Offset) -> Unit,
    onDropLayer: (String, Offset) -> Unit,
    onViewportPan: (Offset) -> Unit,
    onViewportTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
) {
    val currentLayers by rememberUpdatedState(layers)
    val currentSelectedLayerId by rememberUpdatedState(selectedLayerId)
    val currentViewportPan by rememberUpdatedState(viewportPan)
    val currentCellSize by rememberUpdatedState(cellSizePx)
    val currentOnSelectLayer by rememberUpdatedState(onSelectLayer)
    val currentOnDragPreviewChange by rememberUpdatedState(onDragPreviewChange)
    val currentOnDropLayer by rememberUpdatedState(onDropLayer)
    val currentOnViewportPan by rememberUpdatedState(onViewportPan)
    val currentOnViewportTransform by rememberUpdatedState(onViewportTransform)
    val conflictBounds =
        remember(overlapConflicts) {
            overlapConflicts.takeIf(List<OverlapConflict>::isNotEmpty)?.let { conflicts ->
                StitchConflictBounds(
                    left = conflicts.minOf(OverlapConflict::x),
                    top = conflicts.minOf(OverlapConflict::y),
                    rightExclusive = conflicts.maxOf(OverlapConflict::x) + 1L,
                    bottomExclusive = conflicts.maxOf(OverlapConflict::y) + 1L,
                )
            }
        }

    Canvas(
        modifier =
            modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val hitLayer =
                        hitTestStitchLayer(
                            layers = orderedStitchLayers(currentLayers, currentSelectedLayerId),
                            screenPosition = firstDown.position,
                            viewportPan = currentViewportPan,
                            cellSizePx = currentCellSize,
                        )
                    hitLayer?.project?.id?.let(currentOnSelectLayer)

                    var draggedProjectId = hitLayer?.project?.id
                    var preview = Offset.Zero
                    var accumulatedScreenDrag = Offset.Zero
                    var isDragging = false
                    var usedMultiplePointers = false
                    val touchSlop = viewConfiguration.touchSlop

                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            usedMultiplePointers = true
                            draggedProjectId = null
                            preview = Offset.Zero
                            currentOnDragPreviewChange(Offset.Zero)
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            currentOnViewportTransform(centroid, pan, zoom)
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                        } else if (pressedCount == 1 && !usedMultiplePointers) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                accumulatedScreenDrag += pan
                                if (!isDragging && accumulatedScreenDrag.getDistance() > touchSlop) {
                                    isDragging = true
                                }
                                if (isDragging) {
                                    if (draggedProjectId != null) {
                                        preview += pan / currentCellSize
                                        currentOnDragPreviewChange(preview)
                                    } else {
                                        currentOnViewportPan(pan)
                                    }
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) change.consume()
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isDragging && !usedMultiplePointers && draggedProjectId != null) {
                        currentOnDropLayer(draggedProjectId, preview)
                    } else {
                        currentOnDragPreviewChange(Offset.Zero)
                    }
                }
            },
    ) {
        orderedStitchLayers(layers, selectedLayerId).forEach { layer ->
            val preview = if (layer.project.id == selectedLayerId) dragPreviewWorld else Offset.Zero
            bitmaps[layer.project.id]?.let { bitmap ->
                drawStitchLayer(
                    layer = layer,
                    bitmap = bitmap,
                    viewportPan = viewportPan,
                    cellSizePx = cellSizePx,
                    previewWorld = preview,
                    selected = layer.project.id == selectedLayerId,
                )
            }
        }
        drawStitchConflicts(
            conflicts = overlapConflicts,
            bounds = conflictBounds,
            viewportPan = viewportPan,
            cellSizePx = cellSizePx,
        )
    }
}

private fun StitchPalette.sameTargetAs(other: StitchPalette): Boolean =
    colorScheme == other.colorScheme && customColorSchemeId == other.customColorSchemeId

private fun stitchFailureMessage(failure: StitchComposeResult.Failure): String =
    when (failure.reason) {
        StitchComposeFailureReason.NoLayers -> "没有可拼接的项目。"
        StitchComposeFailureReason.InvalidGrid -> "有项目的画布数据不完整，无法拼接。"
        StitchComposeFailureReason.UnresolvedColors -> "有颜色无法转换到目标配色，请改选其他方案。"
        StitchComposeFailureReason.ConversionConfirmationRequired -> "存在近似色转换，请先确认颜色转换影响。"
        StitchComposeFailureReason.OverlapConflict -> "有 ${failure.conflicts.size} 格不同颜色互相重叠，请调整红框位置。"
        StitchComposeFailureReason.CanvasTooLarge -> "拼接后的画布尺寸过大，请缩小项目间距后重试。"
    }

private fun orderedStitchLayers(
    layers: List<StitchLayer>,
    selectedLayerId: String?,
): List<StitchLayer> =
    layers.filterNot { it.project.id == selectedLayerId } + layers.filter { it.project.id == selectedLayerId }

private fun hitTestStitchLayer(
    layers: List<StitchLayer>,
    screenPosition: Offset,
    viewportPan: Offset,
    cellSizePx: Float,
): StitchLayer? {
    if (cellSizePx <= 0f) return null
    val world = (screenPosition - viewportPan) / cellSizePx
    return layers.asReversed().firstOrNull { layer ->
        val localColumn = floor((world.x - layer.x).toDouble()).toInt()
        val localRow = floor((world.y - layer.y).toDouble()).toInt()
        if (localColumn !in 0 until layer.width || localRow !in 0 until layer.height) {
            false
        } else {
            val index = localRow.toLong() * layer.width.toLong() + localColumn.toLong()
            index in 0..Int.MAX_VALUE.toLong() && layer.cells.getOrNull(index.toInt()) != null
        }
    }
}

private fun DrawScope.drawStitchLayer(
    layer: StitchLayer,
    bitmap: StitchLayerBitmap,
    viewportPan: Offset,
    cellSizePx: Float,
    previewWorld: Offset,
    selected: Boolean,
) {
    val left = viewportPan.x + (layer.x + previewWorld.x) * cellSizePx
    val top = viewportPan.y + (layer.y + previewWorld.y) * cellSizePx
    val widthPx = bitmap.width * cellSizePx
    val heightPx = bitmap.height * cellSizePx
    if (widthPx <= 0f || heightPx <= 0f) return
    if (left >= size.width || top >= size.height || left + widthPx <= 0f || top + heightPx <= 0f) return

    drawImage(
        image = bitmap.image,
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(widthPx.roundToInt().coerceAtLeast(1), heightPx.roundToInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.None,
    )

    if (cellSizePx >= 6f) {
        val gridColor = Color(0xFF6E5A46).copy(alpha = if (cellSizePx >= 16f) 0.32f else 0.18f)
        val gridWidth = if (cellSizePx >= 18f) 1f else 0.6f
        val firstVisibleColumn = ceil((-left / cellSizePx).toDouble()).toInt().coerceIn(0, bitmap.width)
        val lastVisibleColumn = floor(((size.width - left) / cellSizePx).toDouble()).toInt().coerceIn(0, bitmap.width)
        for (column in firstVisibleColumn..lastVisibleColumn) {
            val x = left + column * cellSizePx
            drawLine(gridColor, Offset(x, top), Offset(x, top + heightPx), gridWidth, StrokeCap.Butt)
        }
        val firstVisibleRow = ceil((-top / cellSizePx).toDouble()).toInt().coerceIn(0, bitmap.height)
        val lastVisibleRow = floor(((size.height - top) / cellSizePx).toDouble()).toInt().coerceIn(0, bitmap.height)
        for (row in firstVisibleRow..lastVisibleRow) {
            val y = top + row * cellSizePx
            drawLine(gridColor, Offset(left, y), Offset(left + widthPx, y), gridWidth, StrokeCap.Butt)
        }
    }

    drawRect(
        color = if (selected) Color(0xFF181511) else Color(0xFF6E5A46).copy(alpha = 0.75f),
        topLeft = Offset(left, top),
        size = Size(widthPx, heightPx),
        style = Stroke(width = if (selected) 3.5f else 1.5f),
    )

    if (cellSizePx >= 8f && top >= 14.sp.toPx()) {
        val paint =
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(32, 27, 23)
                textSize = 12.sp.toPx()
                isFakeBoldText = selected
                setShadowLayer(3f, 0f, 0f, android.graphics.Color.WHITE)
            }
        drawContext.canvas.nativeCanvas.drawText(
            layer.project.title.take(20),
            left + 4f,
            top - 5f,
            paint,
        )
    }
}

private fun DrawScope.drawStitchConflicts(
    conflicts: List<OverlapConflict>,
    bounds: StitchConflictBounds?,
    viewportPan: Offset,
    cellSizePx: Float,
) {
    if (conflicts.isEmpty() || bounds == null || cellSizePx <= 0f || !cellSizePx.isFinite()) return

    if (cellSizePx < STITCH_INDIVIDUAL_CONFLICT_MIN_SIZE_PX) {
        val left = viewportPan.x + bounds.left.toFloat() * cellSizePx
        val top = viewportPan.y + bounds.top.toFloat() * cellSizePx
        val width = (bounds.rightExclusive - bounds.left).toFloat() * cellSizePx
        val height = (bounds.bottomExclusive - bounds.top).toFloat() * cellSizePx
        if (left >= size.width || top >= size.height || left + width <= 0f || top + height <= 0f) return
        drawRect(
            color = Color(0x33E53935),
            topLeft = Offset(left, top),
            size = Size(width, height),
        )
        drawRect(
            color = Color(0xFFD11C17),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 2f),
        )
        return
    }

    val minWorldX = floor((-viewportPan.x / cellSizePx).toDouble()).toLong() - 1L
    val maxWorldX = ceil(((size.width - viewportPan.x) / cellSizePx).toDouble()).toLong() + 1L
    val minWorldY = floor((-viewportPan.y / cellSizePx).toDouble()).toLong() - 1L
    val maxWorldY = ceil(((size.height - viewportPan.y) / cellSizePx).toDouble()).toLong() + 1L
    var row = minWorldY
    var drawn = 0
    while (row <= maxWorldY && drawn < STITCH_MAX_VISIBLE_CONFLICT_BOXES) {
        var index = lowerBoundConflict(conflicts, row, minWorldX)
        while (index < conflicts.size && drawn < STITCH_MAX_VISIBLE_CONFLICT_BOXES) {
            val conflict = conflicts[index]
            if (conflict.y != row || conflict.x > maxWorldX) break
            val left = viewportPan.x + conflict.x.toFloat() * cellSizePx
            val top = viewportPan.y + conflict.y.toFloat() * cellSizePx
            drawRect(
                color = Color(0x66E53935),
                topLeft = Offset(left, top),
                size = Size(cellSizePx, cellSizePx),
            )
            drawRect(
                color = Color(0xFFD11C17),
                topLeft = Offset(left, top),
                size = Size(cellSizePx, cellSizePx),
                style = Stroke(width = max(2f, cellSizePx * 0.12f)),
            )
            drawn += 1
            index += 1
        }
        if (row == Long.MAX_VALUE) break
        row += 1L
    }
}

private fun lowerBoundConflict(
    conflicts: List<OverlapConflict>,
    y: Long,
    x: Long,
): Int {
    var low = 0
    var high = conflicts.size
    while (low < high) {
        val middle = (low + high).ushr(1)
        val conflict = conflicts[middle]
        if (conflict.y < y || (conflict.y == y && conflict.x < x)) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}

private fun createStitchBitmap(
    cells: List<String?>,
    width: Int,
    height: Int,
    maxBitmapEdge: Int,
): StitchLayerBitmap =
    createGridPreviewBitmap(
        cells = cells,
        width = width,
        height = height,
        maxBitmapEdge = maxBitmapEdge,
        emptyCellArgb = STITCH_EMPTY_CELL_ARGB,
    )

private const val STITCH_EMPTY_CELL_ARGB = 0x00FFFFFF
private const val STITCH_THUMBNAIL_MAX_BITMAP_EDGE = 128
private const val STITCH_CANVAS_MAX_BITMAP_EDGE = 512
private const val STITCH_DIALOG_NON_BODY_HEIGHT_DP = 240
private const val STITCH_DIALOG_MIN_BODY_HEIGHT_DP = 24
private const val STITCH_DIALOG_MAX_BODY_HEIGHT_DP = 360
private const val STITCH_MAX_MAPPING_DETAILS = 8
private const val STITCH_VIEWPORT_HORIZONTAL_MARGIN_DP = 18
private const val STITCH_VIEWPORT_VERTICAL_MARGIN_DP = 12
private const val STITCH_MIN_CELL_SIZE_PX = 0.001f
private const val STITCH_FIT_MAX_CELL_SIZE_PX = 36f
private const val STITCH_MAX_CELL_SIZE_PX = 96f
private const val STITCH_INDIVIDUAL_CONFLICT_MIN_SIZE_PX = 2f
private const val STITCH_MAX_VISIBLE_CONFLICT_BOXES = 5_000
