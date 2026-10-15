@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.douduoyu.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class CustomSchemeManagerPage {
    Library,
    Templates,
    Editor,
}

private data class SchemeManagementTarget(
    val scheme: CustomColorScheme,
    val archiveIds: List<String>,
    val deleteDescription: String,
)

@Composable
fun CustomColorSchemeManagerScreen(
    schemes: List<CustomColorScheme>,
    initialEditSchemeId: String? = null,
    startCreating: Boolean = false,
    onBack: () -> Unit,
    onSave: (CustomColorScheme) -> Unit,
    onArchive: (String) -> Unit,
) {
    val activeSchemes = schemes.filterNot { it.archived }
    val initialScheme = activeSchemes.firstOrNull { it.id == initialEditSchemeId }
    var page by remember(initialEditSchemeId, startCreating) {
        mutableStateOf(
            when {
                startCreating -> CustomSchemeManagerPage.Templates
                initialScheme != null -> CustomSchemeManagerPage.Editor
                activeSchemes.isEmpty() -> CustomSchemeManagerPage.Templates
                else -> CustomSchemeManagerPage.Library
            },
        )
    }
    var draft by remember(initialEditSchemeId, startCreating) { mutableStateOf(initialScheme) }
    var original by remember(initialEditSchemeId, startCreating) { mutableStateOf(initialScheme) }
    var isCalibrationMode by remember(initialEditSchemeId, startCreating) { mutableStateOf(false) }
    var managementTarget by remember { mutableStateOf<SchemeManagementTarget?>(null) }
    var renameTarget by remember { mutableStateOf<SchemeManagementTarget?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<SchemeManagementTarget?>(null) }
    var pendingSwitchScheme by remember { mutableStateOf<CustomColorScheme?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorCountDialog by remember { mutableStateOf(false) }
    var colorCountValue by remember { mutableStateOf("") }
    val calibrationTooltipState = rememberTooltipState(isPersistent = true)
    var hasShownCalibrationHelp by remember { mutableStateOf(false) }

    fun openEditor(scheme: CustomColorScheme) {
        calibrationTooltipState.dismiss()
        draft = scheme.copy(
            selectedCodes = scheme.selectedCodes.toList(),
            colorOverrides = scheme.colorOverrides.toMap(),
        )
        original = scheme
        isCalibrationMode = false
        page = CustomSchemeManagerPage.Editor
    }

    fun leaveEditorNow() {
        calibrationTooltipState.dismiss()
        draft = null
        original = null
        isCalibrationMode = false
        page = if (activeSchemes.isEmpty()) CustomSchemeManagerPage.Templates else CustomSchemeManagerPage.Library
    }

    fun hasUnsavedEditorChanges(): Boolean {
        val current = draft ?: return false
        val isPersisted = activeSchemes.any { it.id == current.id }
        return !isPersisted || !hasSameEditableContent(current, original)
    }

    fun leaveEditor() {
        if (hasUnsavedEditorChanges()) {
            calibrationTooltipState.dismiss()
            pendingSwitchScheme = null
            showDiscardDialog = true
        } else {
            leaveEditorNow()
        }
    }

    fun requestSchemeSwitch(scheme: CustomColorScheme) {
        if (scheme.id == draft?.id) return
        if (hasUnsavedEditorChanges()) {
            calibrationTooltipState.dismiss()
            pendingSwitchScheme = scheme
            showDiscardDialog = true
        } else {
            openEditor(scheme)
        }
    }

    fun saveDraft() {
        val current = draft ?: return
        val duplicateCount =
            activeSchemes.any {
                it.customGroupId == current.customGroupId &&
                    it.id != current.id &&
                    it.selectedCodes.size == current.selectedCodes.size
            }
        if (duplicateCount) return
        val saved =
            current
                .copy(updatedAt = System.currentTimeMillis())
                .normalizedOrNull()
                ?: return
        onSave(saved)
        pendingSwitchScheme = null
        showDiscardDialog = false
        original = saved
        draft = null
        isCalibrationMode = false
        calibrationTooltipState.dismiss()
        page = CustomSchemeManagerPage.Library
    }

    fun handleBack() {
        when (page) {
            CustomSchemeManagerPage.Library -> onBack()
            CustomSchemeManagerPage.Templates -> {
                if (activeSchemes.isEmpty()) onBack() else page = CustomSchemeManagerPage.Library
            }
            CustomSchemeManagerPage.Editor -> leaveEditor()
        }
    }

    BackHandler(onBack = ::handleBack)

    val editorDraft = draft
    val editorCatalog =
        remember(editorDraft?.baseScheme) {
            editorDraft?.let { CustomColorScheme.catalogColors(it.baseScheme) }.orEmpty()
        }
    val editorSelectedCodes = editorDraft?.selectedCodes?.toSet().orEmpty()
    val areAllEditorColorsSelected =
        editorCatalog.isNotEmpty() && editorCatalog.all { it.code in editorSelectedCodes }
    val hasDuplicateEditorColorCount =
        editorDraft != null &&
            activeSchemes.any {
                it.customGroupId == editorDraft.customGroupId &&
                    it.id != editorDraft.id &&
                    it.selectedCodes.size == editorDraft.selectedCodes.size
            }
    val canSaveEditorDraft =
        editorDraft != null &&
            editorDraft.name.trim().isNotEmpty() &&
            editorDraft.selectedCodes.size >= 2 &&
            !hasDuplicateEditorColorCount

    LaunchedEffect(isCalibrationMode, editorDraft?.id) {
        if (isCalibrationMode && !hasShownCalibrationHelp) {
            hasShownCalibrationHelp = true
            calibrationTooltipState.show()
        } else if (!isCalibrationMode) {
            calibrationTooltipState.dismiss()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title =
                            when (page) {
                                CustomSchemeManagerPage.Library -> "自定义配色"
                                CustomSchemeManagerPage.Templates -> "选择内置模板"
                                CustomSchemeManagerPage.Editor -> null
                            }
                        title?.let {
                            Text(
                                it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = ::handleBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (page == CustomSchemeManagerPage.Library) {
                            IconButton(onClick = { page = CustomSchemeManagerPage.Templates }) {
                                Icon(Icons.Outlined.Add, contentDescription = "新建自定义配色")
                            }
                        } else if (page == CustomSchemeManagerPage.Editor && editorDraft != null) {
                            CustomSchemeToolButton(
                                label = if (areAllEditorColorsSelected) "反选" else "全选",
                                icon = if (areAllEditorColorsSelected) Icons.Outlined.Deselect else Icons.Outlined.SelectAll,
                                onClick = {
                                    val nextCodes =
                                        if (areAllEditorColorsSelected) {
                                            editorCatalog
                                                .asSequence()
                                                .map { it.code }
                                                .filterNot { it in editorSelectedCodes }
                                                .toList()
                                        } else {
                                            editorCatalog.map { it.code }
                                        }
                                    draft = editorDraft.copy(selectedCodes = nextCodes, updatedAt = System.currentTimeMillis())
                                },
                            )
                            CustomSchemeToolButton(
                                label = "恢复",
                                icon = Icons.Outlined.Restore,
                                onClick = {
                                    draft =
                                        editorDraft.copy(
                                            selectedCodes = editorDraft.savedTemplateCodes(),
                                            updatedAt = System.currentTimeMillis(),
                                        )
                                },
                            )
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                                tooltip = {
                                    RichTooltip(
                                        title = { Text("颜色校准") },
                                        action = {
                                            TextButton(onClick = calibrationTooltipState::dismiss) {
                                                Text("知道了")
                                            }
                                        },
                                        caretSize = TooltipDefaults.caretSize,
                                    ) {
                                        Text("开启后点击色块，在弹窗里输入 HEX 或 RGB。左下角圆点表示已校准，保存后会同步到同品牌的少色方案。")
                                    }
                                },
                                state = calibrationTooltipState,
                            ) {
                                CustomSchemeToolButton(
                                    label = "校准",
                                    icon = Icons.Outlined.Tune,
                                    selected = isCalibrationMode,
                                    onClick = {
                                        if (isCalibrationMode) {
                                            calibrationTooltipState.dismiss()
                                            isCalibrationMode = false
                                        } else {
                                            isCalibrationMode = true
                                        }
                                    },
                                )
                            }
                            CustomSchemeToolButton(
                                label = "保存",
                                icon = Icons.Outlined.Save,
                                enabled = canSaveEditorDraft,
                                onClick = { saveDraft() },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
            bottomBar = {
                if (page == CustomSchemeManagerPage.Editor && editorDraft != null) {
                    CustomSchemeVariantBar(
                        current = editorDraft,
                        schemes = activeSchemes,
                        onSelect = ::requestSchemeSwitch,
                        onManage = { scheme ->
                            managementTarget =
                                SchemeManagementTarget(
                                    scheme = activeSchemes.firstOrNull { it.id == scheme.id } ?: scheme,
                                    archiveIds = listOf(scheme.id),
                                    deleteDescription = "删除 ${scheme.selectedCodes.size} 色方案",
                                )
                        },
                        onAdd = {
                            val usedCounts =
                                activeSchemes
                                    .asSequence()
                                    .filter { it.customGroupId == editorDraft.customGroupId }
                                    .map { it.selectedCodes.size }
                                    .toSet() + editorDraft.selectedCodes.size
                            val suggestedCount =
                                (editorDraft.selectedCodes.size - 1 downTo 2).firstOrNull { it !in usedCounts }
                                    ?: (2..editorCatalog.size).firstOrNull { it !in usedCounts }
                                    ?: editorDraft.selectedCodes.size.coerceAtLeast(2)
                            colorCountValue = suggestedCount.toString()
                            showColorCountDialog = true
                        },
                    )
                }
            },
        ) { innerPadding ->
            when (page) {
                CustomSchemeManagerPage.Library ->
                    CustomSchemeLibrary(
                        schemes = activeSchemes,
                        contentPadding = innerPadding,
                        onCreate = { page = CustomSchemeManagerPage.Templates },
                        onEdit = ::openEditor,
                        onManageGroup = { representative, variants ->
                            managementTarget =
                                SchemeManagementTarget(
                                    scheme = representative,
                                    archiveIds = variants.map { it.id },
                                    deleteDescription = "删除自定义品牌“${representative.name}”及其 ${variants.size} 个方案",
                                )
                        },
                    )

                CustomSchemeManagerPage.Templates ->
                    CustomSchemeTemplatePicker(
                        contentPadding = innerPadding,
                        onSelect = { baseScheme ->
                            openEditor(
                                CustomColorScheme.create(
                                    base = baseScheme,
                                    name =
                                        nextAvailableCustomGroupName(
                                            baseName = "${baseScheme.brandLabel} 自定义",
                                            schemes = activeSchemes,
                                        ),
                                ),
                            )
                        },
                    )

                CustomSchemeManagerPage.Editor -> {
                    val currentDraft = draft
                    if (currentDraft != null) {
                        CustomSchemeEditor(
                            scheme = currentDraft,
                            contentPadding = innerPadding,
                            isCalibrationMode = isCalibrationMode,
                            hasDuplicateColorCount = hasDuplicateEditorColorCount,
                            onSchemeChange = { draft = it },
                        )
                    }
                }
            }
        }
    }

    managementTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { managementTarget = null },
            title = { Text(target.scheme.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            managementTarget = null
                            renameTarget = target
                            renameValue = target.scheme.name
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("重命名")
                    }
                    OutlinedButton(
                        onClick = {
                            managementTarget = null
                            deleteTarget = target
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("删除")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { managementTarget = null }) { Text("取消") }
            },
        )
    }

    renameTarget?.let { target ->
        val normalizedRename = normalizedColorSchemeName(renameValue)
        val conflictsWithBuiltIn = isBuiltInColorSchemeName(renameValue)
        val conflictsWithCustom =
            activeSchemes.any { scheme ->
                scheme.customGroupId != target.scheme.customGroupId &&
                    normalizedColorSchemeName(scheme.name) == normalizedRename
            }
        val renameError =
            when {
                conflictsWithBuiltIn -> "不能与内置品牌或方案重名"
                conflictsWithCustom -> "已存在同名自定义品牌"
                else -> null
            }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            modifier = Modifier.imePadding(),
            title = { Text("重命名自定义品牌") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("名称") },
                    isError = renameError != null,
                    supportingText = renameError?.let { message -> ({ Text(message) }) },
                )
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val renamed = target.scheme.copy(name = renameValue.trim(), updatedAt = System.currentTimeMillis())
                        val persistedTarget = activeSchemes.firstOrNull { it.id == target.scheme.id }
                        if (persistedTarget == null) {
                            if (draft?.id == target.scheme.id) {
                                draft = draft?.copy(name = renamed.name, updatedAt = renamed.updatedAt)
                                original = original?.copy(name = renamed.name, updatedAt = renamed.updatedAt)
                            }
                        } else {
                            onSave(persistedTarget.copy(name = renamed.name, updatedAt = renamed.updatedAt))
                            if (draft?.customGroupId == renamed.customGroupId) {
                                draft = draft?.copy(name = renamed.name)
                                original = original?.copy(name = renamed.name)
                            }
                        }
                        renameTarget = null
                    },
                    enabled =
                        renameValue.trim().isNotEmpty() &&
                            normalizedRename != normalizedColorSchemeName(target.scheme.name) &&
                            renameError == null,
                ) { Text("保存") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除？") },
            text = { Text("${target.deleteDescription}。已有项目仍会保留显示所需的数据。") },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        target.archiveIds.forEach(onArchive)
                        val deletedCurrent = draft?.id in target.archiveIds
                        deleteTarget = null
                        if (deletedCurrent) {
                            val next =
                                activeSchemes
                                    .filterNot { it.id in target.archiveIds }
                                    .filter { it.customGroupId == target.scheme.customGroupId }
                                    .maxByOrNull { it.selectedCodes.size }
                            if (next != null) openEditor(next) else leaveEditorNow()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
        )
    }

    if (showDiscardDialog) {
        val switchTarget = pendingSwitchScheme
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingSwitchScheme = null
            },
            title = { Text("保存当前修改？") },
            text = {
                Text(
                    if (switchTarget == null) {
                        "退出前可以保存颜色选择和颜色校准。"
                    } else {
                        "保存后将返回方案列表；不保存会直接切换方案。"
                    },
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            pendingSwitchScheme = null
                        },
                    ) {
                        Text("继续编辑")
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            pendingSwitchScheme = null
                            if (switchTarget != null) openEditor(switchTarget) else leaveEditorNow()
                        },
                    ) {
                        Text("不保存")
                    }
                    Button(
                        onClick = { saveDraft() },
                        enabled = canSaveEditorDraft,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text("保存并退出", maxLines = 1)
                    }
                }
            },
        )
    }

    if (showColorCountDialog && editorDraft != null) {
        val requestedCount = colorCountValue.toIntOrNull()
        val existingCounts =
            activeSchemes
                .asSequence()
                .filter { it.customGroupId == editorDraft.customGroupId }
                .map { it.selectedCodes.size }
                .toSet() + editorDraft.selectedCodes.size
        val isDuplicateCount = requestedCount != null && requestedCount in existingCounts
        val isValidCount = requestedCount != null && requestedCount in 2..editorCatalog.size && !isDuplicateCount
        AlertDialog(
            onDismissRequest = { showColorCountDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("创建少色方案") },
            text = {
                OutlinedTextField(
                    value = colorCountValue,
                    onValueChange = { colorCountValue = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("颜色数量") },
                    supportingText = {
                        Text(if (isDuplicateCount) "该颜色数已存在" else "可输入 2–${editorCatalog.size}")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = colorCountValue.isNotEmpty() && !isValidCount,
                )
            },
            dismissButton = {
                TextButton(onClick = { showColorCountDialog = false }) { Text("取消") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = requestedCount ?: return@Button
                        val selectedInOrder = editorDraft.selectedCodes.distinct()
                        val supplemented = selectedInOrder + editorCatalog.map { it.code }.filterNot { it in selectedInOrder }
                        val nextCodes = supplemented.take(count)
                        val created =
                            CustomColorScheme
                                .create(
                                    base = editorDraft.baseScheme,
                                    name = editorDraft.name,
                                ).copy(
                                    customGroupId = editorDraft.customGroupId,
                                    selectedCodes = nextCodes,
                                    templateSelectedCodes = nextCodes,
                                    colorOverrides = editorDraft.colorOverrides.toMap(),
                                )
                        showColorCountDialog = false
                        openEditor(created)
                    },
                    enabled = isValidCount,
                ) { Text("创建") }
            },
        )
    }
}

@Composable
private fun CustomSchemeToolButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column(
        modifier =
            Modifier
                .width(52.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = contentColor,
        )
        Text(
            label,
            color = contentColor,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            letterSpacing = 0.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun CustomSchemeLibrary(
    schemes: List<CustomColorScheme>,
    contentPadding: PaddingValues,
    onCreate: () -> Unit,
    onEdit: (CustomColorScheme) -> Unit,
    onManageGroup: (CustomColorScheme, List<CustomColorScheme>) -> Unit,
) {
    if (schemes.isEmpty()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Palette,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "还没有自定义配色",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "选择一个内置方案作为模板即可开始。",
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCreate) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择模板")
            }
        }
        return
    }

    val groups =
        schemes
            .groupBy { it.customGroupId.ifBlank { it.id } }
            .values
            .map { variants -> variants.sortedByDescending { it.selectedCodes.size } }
            .sortedByDescending { variants -> variants.maxOfOrNull { it.updatedAt } ?: 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { variants -> variants.first().customGroupId.ifBlank { variants.first().id } }) { variants ->
            val scheme = variants.first()
            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEdit(scheme) },
                            onLongClick = { onManageGroup(scheme, variants) },
                        ),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            scheme.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "基于 ${scheme.baseScheme.brandLabel} · " +
                                variants.joinToString(" / ") { "${it.selectedCodes.size} 色" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSchemeTemplatePicker(
    contentPadding: PaddingValues,
    onSelect: (ColorSchemePreset) -> Unit,
) {
    val brands = ColorSchemePreset.visibleBrands
    var selectedBrandId by remember { mutableStateOf(brands.firstOrNull()?.id.orEmpty()) }
    val templates = ColorSchemePreset.visibleSchemesForBrand(selectedBrandId)

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
    ) {
        Text(
            "先选择品牌，再选择最接近需求的内置方案。模板本身不会被修改。",
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            brands.forEach { brand ->
                FilterChip(
                    selected = selectedBrandId == brand.id,
                    onClick = { selectedBrandId = brand.id },
                    label = { Text(brand.label) },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(templates, key = { it.name }) { template ->
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(template) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(template.variantLabel, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${CustomColorScheme.templateCodes(template).size} 个模板颜色",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { onSelect(template) }) {
                            Text("基于此方案创建")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSchemeEditor(
    scheme: CustomColorScheme,
    contentPadding: PaddingValues,
    isCalibrationMode: Boolean,
    hasDuplicateColorCount: Boolean,
    onSchemeChange: (CustomColorScheme) -> Unit,
) {
    var query by remember(scheme.id) { mutableStateOf("") }
    var editingColor by remember(scheme.id) { mutableStateOf<PaletteColor?>(null) }
    val catalog = remember(scheme.baseScheme) { CustomColorScheme.catalogColors(scheme.baseScheme) }
    val selectedCodes = scheme.selectedCodes.toSet()
    val normalizedQuery = query.trim()
    val visibleColors =
        remember(catalog, normalizedQuery) {
            catalog.filter { color ->
                normalizedQuery.isEmpty() ||
                    color.code.contains(normalizedQuery, ignoreCase = true) ||
                    color.series.contains(normalizedQuery, ignoreCase = true)
            }
        }
    val colorSections = remember(visibleColors) { visibleColors.groupBy(::paletteSectionLabel) }
    val itemBounds = remember(scheme.id) { mutableMapOf<String, Rect>() }
    val gridOriginInWindow = remember(scheme.id) { mutableStateOf(Offset.Zero) }
    val latestScheme = rememberUpdatedState(scheme)
    val latestOnSchemeChange = rememberUpdatedState(onSchemeChange)
    val dragSampleDistancePx = with(LocalDensity.current) { 8.dp.toPx() }

    fun toggleCode(code: String) {
        val nextCodes =
            if (code in selectedCodes) {
                scheme.selectedCodes.filterNot { it == code }
            } else {
                scheme.selectedCodes + code
            }
        onSchemeChange(scheme.copy(selectedCodes = nextCodes, updatedAt = System.currentTimeMillis()))
    }

    val dragSelectionModifier =
        if (isCalibrationMode) {
            Modifier
        } else {
            Modifier.pointerInput(scheme.id) {
                var dragShouldSelect = true
                var dragCodes = linkedSetOf<String>()
                val visitedCodes = hashSetOf<String>()
                var previousPosition: Offset? = null

                fun codeAt(localPosition: Offset): String? {
                    val windowPosition = gridOriginInWindow.value + localPosition
                    return itemBounds.entries.firstOrNull { (_, bounds) -> bounds.contains(windowPosition) }?.key
                }

                fun applyToPositions(positions: List<Offset>) {
                    var changed = false
                    positions.forEach { position ->
                        val code = codeAt(position) ?: return@forEach
                        if (!visitedCodes.add(code)) return@forEach
                        if (dragShouldSelect) dragCodes.add(code) else dragCodes.remove(code)
                        changed = true
                    }
                    if (!changed) return
                    val current = latestScheme.value
                    latestOnSchemeChange.value(
                        current.copy(
                            selectedCodes = dragCodes.toList(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }

                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val current = latestScheme.value
                        dragCodes = current.selectedCodes.toCollection(linkedSetOf())
                        visitedCodes.clear()
                        val firstCode = codeAt(start)
                        dragShouldSelect = firstCode == null || firstCode !in dragCodes
                        previousPosition = start
                        applyToPositions(listOf(start))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentPosition = change.position
                        val previous = previousPosition ?: currentPosition
                        val distance = hypot(currentPosition.x - previous.x, currentPosition.y - previous.y)
                        val stepCount = ceil(distance / dragSampleDistancePx).toInt().coerceAtLeast(1)
                        val samples =
                            (1..stepCount).map { step ->
                                val fraction = step / stepCount.toFloat()
                                Offset(
                                    x = previous.x + (currentPosition.x - previous.x) * fraction,
                                    y = previous.y + (currentPosition.y - previous.y) * fraction,
                                )
                            }
                        applyToPositions(samples)
                        previousPosition = currentPosition
                    },
                    onDragEnd = { previousPosition = null },
                    onDragCancel = { previousPosition = null },
                )
            }
        }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .onGloballyPositioned { coordinates ->
                    gridOriginInWindow.value = coordinates.positionInWindow()
                }.then(dragSelectionModifier),
        contentPadding = PaddingValues(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(
            key = "search",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("搜索色号") },
                trailingIcon = {
                    Text(
                        when {
                            hasDuplicateColorCount -> "该颜色数已存在"
                            isCalibrationMode -> "校准"
                            else -> "${scheme.selectedCodes.size}/${catalog.size}"
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            when {
                                hasDuplicateColorCount -> MaterialTheme.colorScheme.error
                                isCalibrationMode -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                },
                singleLine = true,
            )
        }

        if (visibleColors.isEmpty()) {
            item(
                key = "empty-colors",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "没有找到对应色号",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            colorSections.forEach { (section, colors) ->
                item(
                    key = "section:$section",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Text(
                        section,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                gridItems(colors, key = { it.id }) { color ->
                    CustomSchemeColorTile(
                        color = color,
                        displayedArgb = scheme.colorOverrides[color.code] ?: color.argb,
                        selected = color.code in selectedCodes,
                        modified = color.code in scheme.colorOverrides,
                        onClick = {
                            if (isCalibrationMode) editingColor = color else toggleCode(color.code)
                        },
                        onBoundsChanged = { bounds ->
                            if (bounds == null) itemBounds.remove(color.code) else itemBounds[color.code] = bounds
                        },
                    )
                }
            }
        }
    }

    editingColor?.let { color ->
        CustomSchemeColorDialog(
            color = color,
            currentArgb = scheme.colorOverrides[color.code] ?: color.argb,
            onDismiss = { editingColor = null },
            onApply = { argb ->
                val nextOverrides = scheme.colorOverrides.toMutableMap()
                if (argb == color.argb) {
                    nextOverrides.remove(color.code)
                } else {
                    nextOverrides[color.code] = argb
                }
                onSchemeChange(
                    scheme.copy(
                        colorOverrides = nextOverrides,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                editingColor = null
            },
        )
    }
}

@Composable
private fun CustomSchemeColorTile(
    color: PaletteColor,
    displayedArgb: Int,
    selected: Boolean,
    modified: Boolean,
    onClick: () -> Unit,
    onBoundsChanged: (Rect?) -> Unit,
) {
    val swatchColor = Color(displayedArgb)
    val contentColor = if (swatchColor.luminance() > 0.55f) Color(0xFF17130F) else Color.White
    DisposableEffect(color.code) {
        onDispose { onBoundsChanged(null) }
    }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) }
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(5.dp),
        color = swatchColor,
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Black else contentColor.copy(alpha = 0.28f),
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                color.code,
                color = contentColor,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(13.dp),
                    tint = contentColor,
                )
            }
            if (modified) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(contentColor),
                )
            }
        }
    }
}

@Composable
private fun CustomSchemeVariantBar(
    current: CustomColorScheme,
    schemes: List<CustomColorScheme>,
    onSelect: (CustomColorScheme) -> Unit,
    onManage: (CustomColorScheme) -> Unit,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val variants =
        remember(schemes, current) {
            (listOf(current) + schemes.filter { it.customGroupId == current.customGroupId })
                .distinctBy { it.id }
                .sortedByDescending { it.selectedCodes.size }
        }
    Surface(
        modifier = Modifier.imePadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            variants.forEach { scheme ->
                val selected = scheme.id == current.id
                Surface(
                    modifier =
                        Modifier.combinedClickable(
                            onClick = { onSelect(scheme) },
                            onLongClick = { onManage(scheme) },
                        ),
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border =
                        BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                ) {
                    Text(
                        "${scheme.selectedCodes.size} 色",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "创建少色方案")
            }
            IconButton(
                onClick = {
                    Toast.makeText(context, "长按方案可删除", Toast.LENGTH_SHORT).show()
                },
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "删除方案提示")
            }
        }
    }
}

@Composable
private fun CustomSchemeColorDialog(
    color: PaletteColor,
    currentArgb: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var editedArgb by remember(color.id, currentArgb) { mutableIntStateOf(currentArgb or 0xFF000000.toInt()) }
    var hexValue by remember(color.id, currentArgb) { mutableStateOf(rgbHex(editedArgb)) }

    fun updateColor(argb: Int) {
        editedArgb = argb or 0xFF000000.toInt()
        hexValue = rgbHex(editedArgb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("校正色号 ${color.code}") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(editedArgb)),
                    )
                    OutlinedTextField(
                        value = hexValue,
                        onValueChange = { input ->
                            val sanitized =
                                input
                                    .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
                                    .take(6)
                                    .uppercase()
                            hexValue = sanitized
                            if (sanitized.length == 6) {
                                sanitized.toLongOrNull(16)?.let { rgb ->
                                    editedArgb = 0xFF000000.toInt() or rgb.toInt()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        label = { Text("HEX") },
                        prefix = { Text("#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        isError = hexValue.length != 6,
                    )
                }
                RgbSliderRow(
                    label = "R",
                    value = editedArgb shr 16 and 0xFF,
                    onValueChange = { red ->
                        updateColor(rgbArgb(red, editedArgb shr 8 and 0xFF, editedArgb and 0xFF))
                    },
                )
                RgbSliderRow(
                    label = "G",
                    value = editedArgb shr 8 and 0xFF,
                    onValueChange = { green ->
                        updateColor(rgbArgb(editedArgb shr 16 and 0xFF, green, editedArgb and 0xFF))
                    },
                )
                RgbSliderRow(
                    label = "B",
                    value = editedArgb and 0xFF,
                    onValueChange = { blue ->
                        updateColor(rgbArgb(editedArgb shr 16 and 0xFF, editedArgb shr 8 and 0xFF, blue))
                    },
                )
                TextButton(
                    onClick = { updateColor(color.argb) },
                    enabled = editedArgb != color.argb,
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("恢复内置色")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Button(
                onClick = { onApply(editedArgb) },
                enabled = hexValue.length == 6,
            ) { Text("应用") }
        },
    )
}

@Composable
private fun RgbSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.width(20.dp), fontWeight = FontWeight.SemiBold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..255f,
        )
        Text(
            value.toString(),
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun rgbHex(argb: Int): String = "%06X".format(argb and 0xFFFFFF)

private fun CustomColorScheme.savedTemplateCodes(): List<String> {
    return templateSelectedCodes.takeIf { it.size >= 2 }
        ?: CustomColorScheme.templateCodes(baseScheme)
}

private fun paletteSectionLabel(color: PaletteColor): String {
    val code = color.code.trim()
    val letterPrefix =
        code
            .takeWhile { character -> character in 'A'..'Z' || character in 'a'..'z' }
            .uppercase()
    if (letterPrefix.isNotEmpty()) return letterPrefix

    val series = color.series.trim()
    if (series.isNotEmpty()) return series

    val number = code.takeWhile(Char::isDigit).toIntOrNull()
    if (number != null) {
        val lower = number / 50 * 50
        return "$lower–${lower + 49}"
    }
    return "其他"
}

private fun hasSameEditableContent(
    left: CustomColorScheme?,
    right: CustomColorScheme?,
): Boolean {
    return left?.copy(updatedAt = 0L) == right?.copy(updatedAt = 0L)
}

private fun rgbArgb(
    red: Int,
    green: Int,
    blue: Int,
): Int {
    return 0xFF000000.toInt() or
        (red.coerceIn(0, 255) shl 16) or
        (green.coerceIn(0, 255) shl 8) or
        blue.coerceIn(0, 255)
}
