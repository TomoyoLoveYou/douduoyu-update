package com.douduoyu.app

data class ColorConfidenceBrandMatrix(
    val brand: ColorSchemeBrand,
    val scores: ColorConfidenceScore,
    val schemeScores: Map<String, ColorConfidenceScore>,
)

enum class ColorConfidenceDimension(
    val label: String,
) {
    Overall("\u603b\u4f53"),
    ColorData("\u989c\u8272\u51c6\u786e\u5ea6"),
    SchemeIntegrity("\u65b9\u6848\u5b8c\u6574\u6027"),
}

data class ColorConfidenceScore(
    val overall: Double,
    val colorData: Double,
    val schemeIntegrity: Double,
) {
    fun valueFor(dimension: ColorConfidenceDimension): Double {
        return when (dimension) {
            ColorConfidenceDimension.Overall -> overall
            ColorConfidenceDimension.ColorData -> colorData
            ColorConfidenceDimension.SchemeIntegrity -> schemeIntegrity
        }
    }
}

data class ColorConfidenceVariantRow(
    val key: String,
    val label: String,
)

data class ColorConfidenceSource(
    val id: String,
    val label: String,
    val summary: String,
    val url: String = "",
)

object ColorConfidenceData {
    const val highConfidenceMin = 0.85
    const val mediumConfidenceMin = 0.70

    // BEGIN GENERATED PALETTE CONFIDENCE
    const val referenceGeneratedAt = "2026-07-13T04:03:13.639Z"
    const val referenceSha256 = "8137bdd733bb3db30dad8a7ea46d41e749b6df6857cd5e42bf2a0f026d916c30"
    const val referenceBrandCount = 12
    const val referenceSchemeCount = 88
    const val referenceColorEntryCount = 14781

    private val brandScoresByBrandId: Map<String, ColorConfidenceScore> =
        mapOf(
            "huangdoudou" to ColorConfidenceScore(overall = 0.8313, colorData = 0.929, schemeIntegrity = 0.98),
            "kaka" to ColorConfidenceScore(overall = 0.7137, colorData = 0.7137, schemeIntegrity = 0.98),
            "manman" to ColorConfidenceScore(overall = 0.9387, colorData = 0.9625, schemeIntegrity = 0.96),
            "mixiaowo" to ColorConfidenceScore(overall = 0.9478, colorData = 0.954, schemeIntegrity = 0.9455),
            "panpan" to ColorConfidenceScore(overall = 0.9617, colorData = 0.9635, schemeIntegrity = 0.98),
            "shishi" to ColorConfidenceScore(overall = 0.7778, colorData = 0.818, schemeIntegrity = 0.9206),
            "tongqu" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
            "xiaowu" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
            "artkal" to ColorConfidenceScore(overall = 0.8143, colorData = 0.8143, schemeIntegrity = 0.98),
            "coco" to ColorConfidenceScore(overall = 0.8833, colorData = 0.9321, schemeIntegrity = 0.9419),
            "dodo" to ColorConfidenceScore(overall = 0.8491, colorData = 0.8889, schemeIntegrity = 0.9285),
            "mard" to ColorConfidenceScore(overall = 0.9704, colorData = 0.98, schemeIntegrity = 0.98),
        )

    private val scoresByBrandId: Map<String, Map<String, ColorConfidenceScore>> =
        mapOf(
            "huangdoudou" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.8162, colorData = 0.9562, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.794, colorData = 0.934, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.8162, colorData = 0.9562, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.794, colorData = 0.934, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.8162, colorData = 0.9562, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.794, colorData = 0.934, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.794, colorData = 0.934, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.901, colorData = 0.901, schemeIntegrity = 0.98),
                ),
            "kaka" to
                mapOf(
                    "284" to ColorConfidenceScore(overall = 0.7074, colorData = 0.7074, schemeIntegrity = 0.98),
                    "286" to ColorConfidenceScore(overall = 0.72, colorData = 0.72, schemeIntegrity = 0.98),
                ),
            "manman" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.96, colorData = 0.96, schemeIntegrity = 0.98),
                    "192" to ColorConfidenceScore(overall = 0.9, colorData = 0.9, schemeIntegrity = 0.98),
                    "216" to ColorConfidenceScore(overall = 0.8254, colorData = 0.98, schemeIntegrity = 0.98),
                    "278" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "290" to ColorConfidenceScore(overall = 0.96, colorData = 0.98, schemeIntegrity = 0.8662),
                ),
            "mixiaowo" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "192" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "230" to ColorConfidenceScore(overall = 0.8778, colorData = 0.8816, schemeIntegrity = 0.8626),
                    "290" to ColorConfidenceScore(overall = 0.9493, colorData = 0.98, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "292" to ColorConfidenceScore(overall = 0.8731, colorData = 0.8816, schemeIntegrity = 0.8389),
                ),
            "panpan" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "192" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "289" to ColorConfidenceScore(overall = 0.9493, colorData = 0.98, schemeIntegrity = 0.98),
                    "290" to ColorConfidenceScore(overall = 0.9013, colorData = 0.8816, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                ),
            "shishi" to
                mapOf(
                    "217" to ColorConfidenceScore(overall = 0.8192, colorData = 0.8192, schemeIntegrity = 0.98),
                    "220" to ColorConfidenceScore(overall = 0.7368, colorData = 0.8168, schemeIntegrity = 0.8618),
                ),
            "tongqu" to
                mapOf(
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                ),
            "xiaowu" to
                mapOf(
                    "290" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                ),
            "artkal" to
                mapOf(
                    "174" to ColorConfidenceScore(overall = 0.63, colorData = 0.63, schemeIntegrity = 0.98),
                    "197" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "221" to ColorConfidenceScore(overall = 0.8604, colorData = 0.8604, schemeIntegrity = 0.98),
                    "418" to ColorConfidenceScore(overall = 0.883, colorData = 0.883, schemeIntegrity = 0.98),
                    "418-New" to ColorConfidenceScore(overall = 0.72, colorData = 0.72, schemeIntegrity = 0.98),
                ),
            "coco" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "192" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "221" to ColorConfidenceScore(overall = 0.9222, colorData = 0.98, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "293" to ColorConfidenceScore(overall = 0.46, colorData = 0.7, schemeIntegrity = 0.7571),
                ),
            "dodo" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.94, colorData = 0.94, schemeIntegrity = 0.98),
                    "192" to ColorConfidenceScore(overall = 0.94, colorData = 0.94, schemeIntegrity = 0.98),
                    "216" to ColorConfidenceScore(overall = 0.64, colorData = 0.72, schemeIntegrity = 0.8059),
                    "288" to ColorConfidenceScore(overall = 0.64, colorData = 0.72, schemeIntegrity = 0.755),
                    "290" to ColorConfidenceScore(overall = 0.811, colorData = 0.941, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.901, colorData = 0.901, schemeIntegrity = 0.98),
                ),
            "mard" to
                mapOf(
                    "24" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "48" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "72" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "96" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "120" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "144" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "168" to ColorConfidenceScore(overall = 0.88, colorData = 0.98, schemeIntegrity = 0.98),
                    "221" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "264" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "291" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                    "295" to ColorConfidenceScore(overall = 0.98, colorData = 0.98, schemeIntegrity = 0.98),
                ),
        )
    // END GENERATED PALETTE CONFIDENCE

    val brands: List<ColorConfidenceBrandMatrix> =
        ColorSchemePreset.visibleBrands.map { brand ->
            ColorConfidenceBrandMatrix(
                brand = brand,
                scores = brandScoresByBrandId.getValue(brand.id),
                schemeScores = scoresByBrandId[brand.id].orEmpty(),
            )
        }

    val variantRows: List<ColorConfidenceVariantRow> =
        scoresByBrandId.values
            .flatMap { it.keys }
            .distinct()
            .sortedWith(
                compareBy<String>(
                    { variantPrimarySortKey(it) },
                    { variantSuffixSortKey(it) },
                    { it },
                ),
            ).map { key ->
                ColorConfidenceVariantRow(
                    key = key,
                    label = "${key}\u8272",
                )
            }

    val sources: List<ColorConfidenceSource> =
        listOf(
            ColorConfidenceSource(
                id = "pixm_api",
                label = "pixm API",
                summary = "pixm \u5728\u7ebf\u63a5\u53e3\u8fd4\u56de\u7684\u54c1\u724c\u548c\u65b9\u6848\u6570\u636e\uff0c\u7528\u6765\u6838\u5bf9\u5e38\u89c1\u65b9\u6848\u5217\u8868\u4e0e\u90e8\u5206\u989c\u8272\u8868\u3002",
                url = "https://api-pixm.kai0x.top/api/pixelart/colorBrands/listColorBrands",
            ),
            ColorConfidenceSource(
                id = "self_generated_reduced_palette",
                label = "Self-generated reduced palette",
                summary = "\u6309\u5df2\u8bb0\u5f55\u89c4\u5219\u4ece\u53ef\u4fe1\u5b8c\u6574\u76d8\u6295\u5f71\u5f97\u5230\u7684\u81ea\u5236\u5c11\u8272\u65b9\u6848\uff0c\u4e0d\u4f5c\u4e3a\u72ec\u7acb\u5916\u90e8\u6765\u6e90\u3002",
            ),
            ColorConfidenceSource(
                id = "douduoyu_app_mard",
                label = "DouDouYu app Mard presets",
                summary = "App \u65e2\u6709 MARD \u5c11\u8272\u65b9\u6848\uff0c\u7528\u6765\u4fdd\u7559\u5386\u53f2\u517c\u5bb9\u5e76\u53c2\u4e0e\u5f53\u524d\u53c2\u8003\u6bd4\u5bf9\u3002",
            ),
            ColorConfidenceSource(
                id = "color_system_family",
                label = "colorSystemMapping family",
                summary = "colorSystemMapping \u7cfb\u5217\u5f00\u6e90\u8272\u8868\uff0c\u7528\u6765\u4f5c\u4e3a COCO\u3001DODO\u3001\u76fc\u76fc\u3001\u54aa\u5c0f\u7a9d\u7b49\u5b8c\u6574\u76d8\u6620\u5c04\u53c2\u8003\u3002",
                url = "https://github.com/Zippland/perler-beads/blob/main/src/app/colorSystemMapping.json",
            ),
            ColorConfidenceSource(
                id = "dro9an_mard",
                label = "Dro9an MARD palette project",
                summary = "Dro9an \u5f00\u6e90\u50cf\u7d20\u62fc\u8c46\u9879\u76ee\u5185\u7f6e MARD \u8272\u8868\uff0c\u7528\u6765\u9a8c\u8bc1 MARD \u5b8c\u6574\u76d8\u3002",
                url = "https://github.com/Dro9an/pixel_beads_pic_generate/blob/main/src/assets/beadColors.js",
            ),
            ColorConfidenceSource(
                id = "extrafusetool",
                label = "ExtraFuse palette.v4.js",
                summary = "\u5f00\u6e90\u62fc\u8c46\u8f6c\u6362\u5de5\u5177\u7684\u591a\u54c1\u724c\u8c03\u8272\u811a\u672c\uff0c\u7528\u6765\u4ea4\u53c9\u9a8c\u8bc1\u8272\u53f7\u6620\u5c04\u548c\u5b8c\u6574\u76d8\u3002",
                url = "https://github.com/IceDragonFla/extrafusetool.github.io/blob/master/js/palette.v4.js",
            ),
            ColorConfidenceSource(
                id = "hansbug_artkal_official",
                label = "HansBug Artkal official-derived palettes",
                summary = "HansBug \u6574\u7406\u7684\u4f18\u80af\u5b98\u65b9\u6765\u6e90\u8272\u8868\uff0c\u7528\u6765\u63d0\u5347 Artkal \u7cfb\u5217\u53ef\u4fe1\u5ea6\u3002",
                url = "https://github.com/HansBug/pindou-color-data",
            ),
            ColorConfidenceSource(
                id = "hansbug_audited_local",
                label = "HansBug audited palette repo",
                summary = "HansBug \u5ba1\u8ba1\u6574\u7406\u8fc7\u7684\u672c\u5730\u8272\u5361\u4ed3\u5e93\uff0c\u7528\u6765\u4fee\u6b63\u574f\u6570\u636e\u3001\u53bb\u91cd\u548c\u540c\u6b65\u5b9e\u9645\u989c\u8272\u6570\u3002",
                url = "https://github.com/HansBug/pindou-color-data",
            ),
            ColorConfidenceSource(
                id = "hansbug_youken_public_174",
                label = "HansBug YouKen public 174 old table",
                summary = "HansBug \u4fdd\u7559\u7684\u4f18\u80af 174 \u516c\u5f00\u8868\uff0c\u7528\u6765\u8ffd\u8e2a\u65e7\u516c\u5f00\u65b9\u6848\u3002",
                url = "https://github.com/HansBug/pindou-color-data/tree/main/youken-public-174",
            ),
            ColorConfidenceSource(
                id = "jett_wu_mard",
                label = "Jett-Wu MARD palettes",
                summary = "Jett-Wu \u62fc\u8c46\u751f\u6210\u5668\u5185\u7f6e MARD \u8272\u8868\uff0c\u7528\u6765\u8865\u5145 MARD \u5f00\u6e90\u9879\u76ee\u8bc1\u636e\u3002",
                url = "https://github.com/Jett-Wu/Perler_Beads_Generator/blob/main/src/palette.ts",
            ),
            ColorConfidenceSource(
                id = "beadcolors_mard",
                label = "maxcleme/beadcolors Mard CSV",
                summary = "\u516c\u5f00 beadcolors MARD \u8272\u8868\uff0c\u7528\u6765\u8865\u5145 MARD \u989c\u8272\u53c2\u8003\u7ebf\u3002",
                url = "https://github.com/maxcleme/beadcolors/blob/master/raw/mard.csv",
            ),
            ColorConfidenceSource(
                id = "mxbraisedfish_pindou_draw",
                label = "MXBraisedFish pindou-draw palettes",
                summary = "MXBraisedFish/pindou-draw \u751f\u6210\u8272\u8868\uff0c\u7528\u6765\u6838\u5bf9 MARD\u3001COCO \u7b49\u65b9\u6848\u548c\u5b9e\u9645\u989c\u8272\u6570\u3002",
                url = "https://github.com/MXBraisedFish/pindou-draw/tree/main/output",
            ),
            ColorConfidenceSource(
                id = "woleigedou_mard_coco_chart",
                label = "woleigedou reviewed MARD->COCO chart",
                summary = "woleigedou116-ui \u8272\u8868\u9879\u76ee\uff0c\u7528\u6765\u8f85\u52a9\u9a8c\u8bc1 MARD \u4e0e COCO \u516c\u5f00\u6620\u5c04\u3002",
                url = "https://github.com/woleigedou116-ui/perler-beads-blueprint-exchange/blob/main/data/palettes/mard-coco.v1.json",
            ),
            ColorConfidenceSource(
                id = "xbrgb_family",
                label = "XBRGB / Xiu multi-brand palettes",
                summary = "XBRGB / Xiu \u591a\u54c1\u724c\u8272\u5361\u9879\u76ee\uff0c\u7528\u6765\u9a8c\u8bc1 COCO\u3001\u76fc\u76fc\u3001\u54aa\u5c0f\u7a9d\u7b49\u5b8c\u6574\u76d8\u540c\u6e90\u6216\u8fd1\u4f3c\u5173\u7cfb\u3002",
                url = "https://github.com/XiaoB212/XBRGB-Pindou",
            ),
            ColorConfidenceSource(
                id = "ybx77_beads_tables",
                label = "ybx77/beads color_data.json",
                summary = "ybx77/beads \u7684\u673a\u5668\u53ef\u8bfb\u8272\u8868\uff0c\u7528\u6765\u8865\u5145\u591a\u54c1\u724c\u5b8c\u6574\u76d8\u548c Mard \u6269\u5c55\u76d8\u3002",
                url = "https://github.com/ybx77/beads/blob/main/color_data.json",
            ),
            ColorConfidenceSource(
                id = "local_get_colors",
                label = "Local scraped pages",
                summary = "\u672c\u5730\u6293\u53d6\u7684\u516c\u5f00\u54c1\u724c\u8272\u5361\u9875\u9762\uff0c\u8bb0\u5f55\u9875\u9762\u58f0\u660e\u7684\u65b9\u6848\u6570\u91cf\u3001\u8272\u53f7\u548c\u989c\u8272\u503c\u3002",
            ),
            ColorConfidenceSource(
                id = "bitbead_public_charts",
                label = "bitbead.app public color charts",
                summary = "bitbead \u516c\u5f00\u8272\u5361\uff0c\u7ecf\u5f00\u6e90\u9879\u76ee\u5f15\u7528\uff0c\u7528\u6765\u8865\u5145 COCO\u3001\u4f18\u80af\u7b49\u54c1\u724c\u65b9\u6848\u3002",
                url = "https://www.bitbead.app/en/colors",
            ),
            ColorConfidenceSource(
                id = "doudougongfang_mard",
                label = "DouDouGongFang MARD v1 palette page",
                summary = "\u8c46\u8c46\u5de5\u574a\u516c\u5f00 MARD v1 \u9875\u9762\uff0c\u7528\u6765\u4fdd\u7559\u65e7\u7248 MARD \u53c2\u8003\u548c\u5386\u53f2\u7248\u672c\u4fe1\u606f\u3002",
                url = "https://www.doudougongfang.com/kb/beads/mard-palette",
            ),
            ColorConfidenceSource(
                id = "picturesdou_mard",
                label = "picturesdou.uk MARD palette export",
                summary = "picturesdou.uk \u7684 MARD CSV \u5bfc\u51fa\uff0c\u7528\u6765\u6838\u5bf9\u7f16\u7801\u987a\u5e8f\u548c\u90e8\u5206\u516c\u5f00\u989c\u8272\u503c\u3002",
                url = "https://www.picturesdou.uk/data/mard-palette.csv",
            ),
            ColorConfidenceSource(
                id = "pixel_pattern_mard",
                label = "Pixel Pattern MARD palettes",
                summary = "Pixel Pattern \u7684 MARD \u8c03\u8272\u76d8\uff0c\u7528\u6765\u8f85\u52a9\u6838\u5bf9 MARD \u5b8c\u6574\u76d8\u7248\u672c\u548c\u989c\u8272\u5206\u5e03\u3002",
                url = "https://www.pixel-pattern.com/color-converter",
            ),
            ColorConfidenceSource(
                id = "ycs_artkal_cmini",
                label = "YCS Artkal C-Mini/M-Mini palettes",
                summary = "YCS \u5f00\u6e90\u8f6c\u6362\u5668\u4e0e\u5546\u5e97\u6d41\u7a0b\u91cd\u73b0\u4e86 Artkal C-Mini \u548c M-Mini \u8272\u8868\uff0c\u7528\u6765\u72ec\u7acb\u6838\u5bf9\u4f18\u80af\u7cfb\u5217\u3002",
                url = "https://github.com/carolineyang095-code/ycs-beads-converter",
            ),
            ColorConfidenceSource(
                id = "xzgwhjj_perler_pic",
                label = "xzgwhjj/perler-pic static colors",
                summary = "perler-pic \u5f00\u6e90\u9879\u76ee\u7684\u9759\u6001\u8272\u8868\uff0c\u7528\u6765\u68c0\u67e5 colorSystemMapping \u5bb6\u65cf\u7684\u8de8\u9879\u76ee\u590d\u5236\u60c5\u51b5\u3002",
                url = "https://github.com/xzgwhjj/perler-pic/tree/main/static/colors",
            ),
            ColorConfidenceSource(
                id = "gitee_color_card_web",
                label = "Gitee bean-color-card-making-webpage palettes",
                summary = "Gitee \u62fc\u8c46\u8272\u5361\u5de5\u5177\u7684\u591a\u54c1\u724c\u6570\u636e\uff0c\u7528\u6765\u4f5c\u4e3a\u8272\u53f7\u6620\u5c04\u7684\u989d\u5916\u590d\u73b0\u8bc1\u636e\u3002",
                url = "https://gitee.com/menxiang1202/bean-color-card-making-webpage",
            ),
            ColorConfidenceSource(
                id = "gitee_cc_pindou",
                label = "Gitee CC-PinDou multi-brand mapping",
                summary = "CC-PinDou \u5f00\u6e90\u9879\u76ee\u7684\u591a\u54c1\u724c\u6620\u5c04\uff0c\u7528\u6765\u8bc6\u522b\u88ab\u591a\u4e2a\u9879\u76ee\u590d\u5236\u7684\u540c\u6e90\u8272\u8868\u3002",
                url = "https://gitee.com/ccoooool/CC-PinDou",
            ),
            ColorConfidenceSource(
                id = "atonasting_fuse_bead_tool",
                label = "atonasting fuse-bead-tool palettes",
                summary = "atonasting \u62fc\u8c46\u5de5\u5177\u7684\u516c\u5f00\u8c03\u8272\u811a\u672c\uff0c\u7528\u6765\u8865\u5145 MARD \u7b49\u5b8c\u6574\u76d8\u7684\u4ea4\u53c9\u5bf9\u7167\u3002",
                url = "https://github.com/atonasting/fuse-bead-tool/blob/master/js/palette.v2.1.js",
            ),
        )

    private fun variantPrimarySortKey(key: String): Int {
        return key.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun variantSuffixSortKey(key: String): String {
        return key.dropWhile { it.isDigit() }.trimStart('-')
    }
}
