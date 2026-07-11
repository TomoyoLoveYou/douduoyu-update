package com.douduoyu.app

import java.util.UUID

enum class AppScreen {
    Home,
    ImageEditor,
    PatternEditor,
    BuildMode,
}

enum class ProjectSourceKind {
    ImageConversion,
    ChartRecognition,
}

enum class ToolMode {
    Pencil,
    Eraser,
    Fill,
    Remove,
    Recolor,
    Picker,
    Mirror,
}

enum class RemoveMode {
    SameConnected,
    AllSameColor,
    Connected,
}

enum class MirrorDirection {
    Horizontal,
    Vertical,
}

enum class RotationDirection {
    Clockwise,
    Counterclockwise,
}

enum class CanvasExpandDirection {
    Up,
    Down,
    Left,
    Right,
}

enum class BuildMode {
    Color,
    ColorBlock,
    Row,
    Column,
}

enum class PaletteMode {
    Basic,
    Complete,
}

data class ColorSchemeBrand(
    val id: String,
    val label: String,
    val confidenceScore: Double,
)

enum class ColorSchemePreset(
    val label: String,
    val brandId: String,
    val brandLabel: String,
    val variantKey: String,
    val variantLabel: String,
    val maxColors: Int,
    val paletteMode: PaletteMode,
    val showInLogicPanel: Boolean = true,
) {
    Huangdoudou24("\u9ec4\u8c46\u8c46 24\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "24", "24\u8272", 24, PaletteMode.Basic),
    Huangdoudou48("\u9ec4\u8c46\u8c46 48\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "48", "48\u8272", 48, PaletteMode.Basic),
    Huangdoudou72("\u9ec4\u8c46\u8c46 72\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "72", "72\u8272", 72, PaletteMode.Basic),
    Huangdoudou96("\u9ec4\u8c46\u8c46 96\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "96", "96\u8272", 96, PaletteMode.Basic),
    Huangdoudou120("\u9ec4\u8c46\u8c46 120\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "120", "120\u8272", 120, PaletteMode.Basic),
    Huangdoudou144("\u9ec4\u8c46\u8c46 144\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "144", "144\u8272", 144, PaletteMode.Basic),
    Huangdoudou168("\u9ec4\u8c46\u8c46 168\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "168", "168\u8272", 168, PaletteMode.Basic),
    Huangdoudou291("\u9ec4\u8c46\u8c46 291\u8272", "huangdoudou", "\u9ec4\u8c46\u8c46", "291", "291\u8272", 291, PaletteMode.Basic),
    Kaka284("\u5361\u5361 284\u8272", "kaka", "\u5361\u5361", "284", "284\u8272", 284, PaletteMode.Basic),
    Kaka286("\u5361\u5361 286\u8272", "kaka", "\u5361\u5361", "286", "286\u8272", 286, PaletteMode.Basic),
    Manman24("\u6f2b\u6f2b 24\u8272", "manman", "\u6f2b\u6f2b", "24", "24\u8272", 24, PaletteMode.Basic),
    Manman48("\u6f2b\u6f2b 48\u8272", "manman", "\u6f2b\u6f2b", "48", "48\u8272", 48, PaletteMode.Basic),
    Manman72("\u6f2b\u6f2b 72\u8272", "manman", "\u6f2b\u6f2b", "72", "72\u8272", 72, PaletteMode.Basic),
    Manman96("\u6f2b\u6f2b 96\u8272", "manman", "\u6f2b\u6f2b", "96", "96\u8272", 96, PaletteMode.Basic),
    Manman120("\u6f2b\u6f2b 120\u8272", "manman", "\u6f2b\u6f2b", "120", "120\u8272", 120, PaletteMode.Basic),
    Manman144("\u6f2b\u6f2b 144\u8272", "manman", "\u6f2b\u6f2b", "144", "144\u8272", 144, PaletteMode.Basic),
    Manman168("\u6f2b\u6f2b 168\u8272", "manman", "\u6f2b\u6f2b", "168", "168\u8272", 168, PaletteMode.Basic),
    Manman192("\u6f2b\u6f2b 192\u8272", "manman", "\u6f2b\u6f2b", "192", "192\u8272", 192, PaletteMode.Basic),
    Manman216("\u6f2b\u6f2b 216\u8272", "manman", "\u6f2b\u6f2b", "216", "216\u8272", 216, PaletteMode.Basic),
    Manman278("\u6f2b\u6f2b 278\u8272", "manman", "\u6f2b\u6f2b", "278", "278\u8272", 278, PaletteMode.Basic),
    Manman290("\u6f2b\u6f2b 290\u8272", "manman", "\u6f2b\u6f2b", "290", "290\u8272", 289, PaletteMode.Basic),
    Mixiaowo24("\u54aa\u5c0f\u7a9d 24\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "24", "24\u8272", 24, PaletteMode.Basic),
    Mixiaowo48("\u54aa\u5c0f\u7a9d 48\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "48", "48\u8272", 48, PaletteMode.Basic),
    Mixiaowo72("\u54aa\u5c0f\u7a9d 72\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "72", "72\u8272", 72, PaletteMode.Basic),
    Mixiaowo96("\u54aa\u5c0f\u7a9d 96\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "96", "96\u8272", 96, PaletteMode.Basic),
    Mixiaowo120("\u54aa\u5c0f\u7a9d 120\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "120", "120\u8272", 120, PaletteMode.Basic),
    Mixiaowo144("\u54aa\u5c0f\u7a9d 144\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "144", "144\u8272", 144, PaletteMode.Basic),
    Mixiaowo168("\u54aa\u5c0f\u7a9d 168\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "168", "168\u8272", 168, PaletteMode.Basic),
    Mixiaowo192("\u54aa\u5c0f\u7a9d 192\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "192", "192\u8272", 192, PaletteMode.Basic),
    Mixiaowo230("\u54aa\u5c0f\u7a9d 230\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "230", "230\u8272", 229, PaletteMode.Basic),
    Mixiaowo290("\u54aa\u5c0f\u7a9d 290\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "290", "290\u8272", 290, PaletteMode.Basic),
    Mixiaowo291("\u54aa\u5c0f\u7a9d 291\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "291", "291\u8272", 291, PaletteMode.Basic),
    Mixiaowo292("\u54aa\u5c0f\u7a9d 292\u8272", "mixiaowo", "\u54aa\u5c0f\u7a9d", "292", "292\u8272", 289, PaletteMode.Basic),
    Panpan24("\u76fc\u76fc 24\u8272", "panpan", "\u76fc\u76fc", "24", "24\u8272", 24, PaletteMode.Basic),
    Panpan48("\u76fc\u76fc 48\u8272", "panpan", "\u76fc\u76fc", "48", "48\u8272", 48, PaletteMode.Basic),
    Panpan72("\u76fc\u76fc 72\u8272", "panpan", "\u76fc\u76fc", "72", "72\u8272", 72, PaletteMode.Basic),
    Panpan96("\u76fc\u76fc 96\u8272", "panpan", "\u76fc\u76fc", "96", "96\u8272", 96, PaletteMode.Basic),
    Panpan120("\u76fc\u76fc 120\u8272", "panpan", "\u76fc\u76fc", "120", "120\u8272", 120, PaletteMode.Basic),
    Panpan144("\u76fc\u76fc 144\u8272", "panpan", "\u76fc\u76fc", "144", "144\u8272", 144, PaletteMode.Basic),
    Panpan168("\u76fc\u76fc 168\u8272", "panpan", "\u76fc\u76fc", "168", "168\u8272", 168, PaletteMode.Basic),
    Panpan192("\u76fc\u76fc 192\u8272", "panpan", "\u76fc\u76fc", "192", "192\u8272", 192, PaletteMode.Basic),
    Panpan289("\u76fc\u76fc 289\u8272", "panpan", "\u76fc\u76fc", "289", "289\u8272", 289, PaletteMode.Basic),
    Panpan290("\u76fc\u76fc 290\u8272", "panpan", "\u76fc\u76fc", "290", "290\u8272", 290, PaletteMode.Basic),
    Panpan291("\u76fc\u76fc 291\u8272", "panpan", "\u76fc\u76fc", "291", "291\u8272", 291, PaletteMode.Basic),
    Shishi217("\u67ff\u67ff 217\u8272", "shishi", "\u67ff\u67ff", "217", "217\u8272", 217, PaletteMode.Basic),
    Shishi220("\u67ff\u67ff 220\u8272", "shishi", "\u67ff\u67ff", "220", "220\u8272", 219, PaletteMode.Basic),
    Tongqu120("\u7ae5\u8da3 120\u8272", "tongqu", "\u7ae5\u8da3", "120", "120\u8272", 120, PaletteMode.Basic),
    Xiaowu290("\u5c0f\u821e 290\u8272", "xiaowu", "\u5c0f\u821e", "290", "290\u8272", 290, PaletteMode.Basic),
    Xiaowu291("\u5c0f\u821e 291\u8272", "xiaowu", "\u5c0f\u821e", "291", "291\u8272", 291, PaletteMode.Basic),
    Artkal174("\u4f18\u80af 174\u8272", "artkal", "\u4f18\u80af", "174", "174\u8272", 174, PaletteMode.Basic),
    Artkal197("\u4f18\u80af 197\u8272", "artkal", "\u4f18\u80af", "197", "197\u8272", 197, PaletteMode.Basic),
    Artkal221("\u4f18\u80af 221\u8272", "artkal", "\u4f18\u80af", "221", "221\u8272", 221, PaletteMode.Basic),
    Artkal418("\u4f18\u80af 418\u8272", "artkal", "\u4f18\u80af", "418", "418\u8272", 418, PaletteMode.Basic),
    Artkal418New("\u4f18\u80af 418-New\u8272", "artkal", "\u4f18\u80af", "418-New", "418-New\u8272", 418, PaletteMode.Basic),
    Coco24("COCO 24\u8272", "coco", "COCO", "24", "24\u8272", 24, PaletteMode.Basic),
    Coco48("COCO 48\u8272", "coco", "COCO", "48", "48\u8272", 48, PaletteMode.Basic),
    Coco72("COCO 72\u8272", "coco", "COCO", "72", "72\u8272", 72, PaletteMode.Basic),
    Coco96("COCO 96\u8272", "coco", "COCO", "96", "96\u8272", 96, PaletteMode.Basic),
    Coco120("COCO 120\u8272", "coco", "COCO", "120", "120\u8272", 120, PaletteMode.Basic),
    Coco144("COCO 144\u8272", "coco", "COCO", "144", "144\u8272", 144, PaletteMode.Basic),
    Coco168("COCO 168\u8272", "coco", "COCO", "168", "168\u8272", 168, PaletteMode.Basic),
    Coco192("COCO 192\u8272", "coco", "COCO", "192", "192\u8272", 192, PaletteMode.Basic),
    Coco221("COCO 221\u8272", "coco", "COCO", "221", "221\u8272", 221, PaletteMode.Basic),
    Coco291("COCO 291\u8272", "coco", "COCO", "291", "291\u8272", 291, PaletteMode.Basic),
    Coco293("COCO 293\u8272", "coco", "COCO", "293", "293\u8272", 284, PaletteMode.Basic),
    Dodo24("DODO 24\u8272", "dodo", "DODO", "24", "24\u8272", 24, PaletteMode.Basic),
    Dodo48("DODO 48\u8272", "dodo", "DODO", "48", "48\u8272", 48, PaletteMode.Basic),
    Dodo72("DODO 72\u8272", "dodo", "DODO", "72", "72\u8272", 72, PaletteMode.Basic),
    Dodo96("DODO 96\u8272", "dodo", "DODO", "96", "96\u8272", 96, PaletteMode.Basic),
    Dodo120("DODO 120\u8272", "dodo", "DODO", "120", "120\u8272", 120, PaletteMode.Basic),
    Dodo144("DODO 144\u8272", "dodo", "DODO", "144", "144\u8272", 144, PaletteMode.Basic),
    Dodo168("DODO 168\u8272", "dodo", "DODO", "168", "168\u8272", 168, PaletteMode.Basic),
    Dodo192("DODO 192\u8272", "dodo", "DODO", "192", "192\u8272", 192, PaletteMode.Basic),
    Dodo216("DODO 216\u8272", "dodo", "DODO", "216", "216\u8272", 212, PaletteMode.Basic),
    Dodo288("DODO 288\u8272", "dodo", "DODO", "288", "288\u8272", 279, PaletteMode.Basic),
    Dodo290("DODO 290\u8272", "dodo", "DODO", "290", "290\u8272", 290, PaletteMode.Basic),
    Dodo291("DODO 291\u8272", "dodo", "DODO", "291", "291\u8272", 291, PaletteMode.Basic),
    MardBrand24("Mard 24\u8272", "mard", "Mard", "24", "24\u8272", 24, PaletteMode.Basic),
    MardBrand48("Mard 48\u8272", "mard", "Mard", "48", "48\u8272", 48, PaletteMode.Basic),
    MardBrand72("Mard 72\u8272", "mard", "Mard", "72", "72\u8272", 72, PaletteMode.Basic),
    MardBrand96("Mard 96\u8272", "mard", "Mard", "96", "96\u8272", 96, PaletteMode.Basic),
    MardBrand120("Mard 120\u8272", "mard", "Mard", "120", "120\u8272", 120, PaletteMode.Basic),
    MardBrand144("Mard 144\u8272", "mard", "Mard", "144", "144\u8272", 144, PaletteMode.Basic),
    MardBrand168("Mard 168\u8272", "mard", "Mard", "168", "168\u8272", 168, PaletteMode.Basic),
    MardBrand221("Mard 221\u8272", "mard", "Mard", "221", "221\u8272", 221, PaletteMode.Basic),
    MardBrand264("Mard 264\u8272", "mard", "Mard", "264", "264\u8272", 264, PaletteMode.Basic),
    MardBrand291("Mard 291\u8272", "mard", "Mard", "291", "291\u8272", 291, PaletteMode.Basic),
    MardBrand295("Mard 295\u8272", "mard", "Mard", "295", "295\u8272", 295, PaletteMode.Basic),
    Mard24("\u65e7\u7248 Mard 24\u8272", "legacy-mard", "\u65e7\u7248 Mard", "24", "24\u8272", 24, PaletteMode.Basic, showInLogicPanel = false),
    Mard48("\u65e7\u7248 Mard 48\u8272", "legacy-mard", "\u65e7\u7248 Mard", "48", "48\u8272", 48, PaletteMode.Basic, showInLogicPanel = false),
    Mard72("\u65e7\u7248 Mard 72\u8272", "legacy-mard", "\u65e7\u7248 Mard", "72", "72\u8272", 72, PaletteMode.Basic, showInLogicPanel = false),
    Mard96("\u65e7\u7248 Mard 96\u8272", "legacy-mard", "\u65e7\u7248 Mard", "96", "96\u8272", 96, PaletteMode.Basic, showInLogicPanel = false),
    Mard120("\u65e7\u7248 Mard 120\u8272", "legacy-mard", "\u65e7\u7248 Mard", "120", "120\u8272", 120, PaletteMode.Basic, showInLogicPanel = false),
    Mard144("\u65e7\u7248 Mard 144\u8272", "legacy-mard", "\u65e7\u7248 Mard", "144", "144\u8272", 144, PaletteMode.Basic, showInLogicPanel = false),
    Mard168("\u65e7\u7248 Mard 168\u8272", "legacy-mard", "\u65e7\u7248 Mard", "168", "168\u8272", 168, PaletteMode.Basic, showInLogicPanel = false),
    Mard221("\u65e7\u7248 Mard 221\u8272", "legacy-mard", "\u65e7\u7248 Mard", "221", "221\u8272", 221, PaletteMode.Basic, showInLogicPanel = false),
    Complete291("\u65e7\u7248 Mard 291\u8272", "legacy-mard", "\u65e7\u7248 Mard", "291", "291\u8272", 291, PaletteMode.Complete, showInLogicPanel = false),
    ;

    companion object {
        val visibleBrands: List<ColorSchemeBrand> =
            listOf(
                ColorSchemeBrand("mard", "Mard", 0.9607),
                ColorSchemeBrand("tongqu", "\u7ae5\u8da3", 0.98),
                ColorSchemeBrand("xiaowu", "\u5c0f\u821e", 0.975),
                ColorSchemeBrand("manman", "\u6f2b\u6f2b", 0.9387),
                ColorSchemeBrand("panpan", "\u76fc\u76fc", 0.9245),
                ColorSchemeBrand("mixiaowo", "\u54aa\u5c0f\u7a9d", 0.8752),
                ColorSchemeBrand("coco", "COCO", 0.8723),
                ColorSchemeBrand("dodo", "DODO", 0.8355),
                ColorSchemeBrand("artkal", "\u4f18\u80af", 0.7851),
                ColorSchemeBrand("huangdoudou", "\u9ec4\u8c46\u8c46", 0.7747),
                ColorSchemeBrand("shishi", "\u67ff\u67ff", 0.7376),
                ColorSchemeBrand("kaka", "\u5361\u5361", 0.6998)
            )

        fun visibleSchemesForBrand(brandId: String): List<ColorSchemePreset> {
            return entries.filter { it.showInLogicPanel && it.brandId == brandId }
        }

        fun selectableBrands(currentScheme: ColorSchemePreset): List<ColorSchemeBrand> {
            return if (currentScheme.showInLogicPanel) {
                visibleBrands
            } else {
                visibleBrands + ColorSchemeBrand(currentScheme.brandId, currentScheme.brandLabel, -1.0)
            }
        }

        fun selectableSchemesForBrand(
            brandId: String,
            currentScheme: ColorSchemePreset,
        ): List<ColorSchemePreset> {
            return entries.filter {
                (it.showInLogicPanel && it.brandId == brandId) ||
                    (!currentScheme.showInLogicPanel && it == currentScheme && it.brandId == brandId)
            }
        }
    }
}
enum class BackgroundMode {
    Keep,
    RemoveWhite,
}

enum class GenerationStyle {
    Cartoon,
    Realistic,
}

data class GenerationSettings(
    val title: String = "豆豆鱼新图纸",
    val width: Int = 20,
    val height: Int = 20,
    val maxColors: Int = 24,
    val paletteMode: PaletteMode = PaletteMode.Basic,
    val colorScheme: ColorSchemePreset = ColorSchemePreset.MardBrand221,
    val backgroundMode: BackgroundMode = BackgroundMode.Keep,
    val tolerance: Int = 32,
    val speckleReduction: Int = 0,
    val generationStyle: GenerationStyle = GenerationStyle.Cartoon,
    val cropZoom: Float = 1f,
    val cropX: Float = 0f,
    val cropY: Float = 0f,
)

data class BeadProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val width: Int,
    val height: Int,
    val cells: List<String?>,
    val paletteMode: PaletteMode = PaletteMode.Basic,
    val colorScheme: ColorSchemePreset = ColorSchemePreset.MardBrand221,
    val beadsPerPack: Int = 500,
    val sourceKind: ProjectSourceKind = ProjectSourceKind.ImageConversion,
    val sourceImagePath: String? = null,
    val updatedAt: Long,
)

data class BoardViewportState(
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val zoom: Float = 20f,
    val centerOnRestore: Boolean = false,
)

data class MaterialStat(
    val code: String,
    val displayCode: String,
    val count: Int,
    val argb: Int,
    val series: String,
)
