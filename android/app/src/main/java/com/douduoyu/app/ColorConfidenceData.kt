package com.douduoyu.app

data class ColorConfidenceBrandMatrix(
    val brand: ColorSchemeBrand,
    val schemeScores: Map<String, Double>,
)

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

    private val scoresByBrandId: Map<String, Map<String, Double>> =
        mapOf(
            "huangdoudou" to
                mapOf(
                    "24" to 0.72,
                    "48" to 0.72,
                    "72" to 0.72,
                    "96" to 0.72,
                    "120" to 0.72,
                    "144" to 0.72,
                    "168" to 0.72,
                    "291" to 0.901,
                ),
            "kaka" to
                mapOf(
                    "284" to 0.6795,
                    "286" to 0.72,
                ),
            "manman" to
                mapOf(
                    "24" to 0.96,
                    "48" to 0.96,
                    "72" to 0.96,
                    "96" to 0.96,
                    "120" to 0.96,
                    "144" to 0.96,
                    "168" to 0.96,
                    "192" to 0.9,
                    "216" to 0.8254,
                    "278" to 0.98,
                    "290" to 0.96,
                ),
            "mixiaowo" to
                mapOf(
                    "24" to 0.98,
                    "48" to 0.98,
                    "72" to 0.98,
                    "96" to 0.98,
                    "120" to 0.98,
                    "144" to 0.98,
                    "168" to 0.98,
                    "192" to 0.98,
                    "230" to 0.6016,
                    "290" to 0.9463,
                    "291" to 0.98,
                    "292" to 0.6016,
                ),
            "panpan" to
                mapOf(
                    "24" to 0.98,
                    "48" to 0.98,
                    "72" to 0.98,
                    "96" to 0.98,
                    "120" to 0.98,
                    "144" to 0.98,
                    "168" to 0.98,
                    "192" to 0.98,
                    "289" to 0.9463,
                    "290" to 0.6816,
                    "291" to 0.98,
                ),
            "shishi" to
                mapOf(
                    "217" to 0.8192,
                    "220" to 0.6568,
                ),
            "tongqu" to
                mapOf(
                    "120" to 0.98,
                ),
            "xiaowu" to
                mapOf(
                    "290" to 0.97,
                    "291" to 0.98,
                ),
            "artkal" to
                mapOf(
                    "174" to 0.655,
                    "197" to 0.98,
                    "221" to 0.8467,
                    "418" to 0.78,
                    "418-New" to 0.72,
                ),
            "coco" to
                mapOf(
                    "24" to 0.98,
                    "48" to 0.98,
                    "72" to 0.98,
                    "96" to 0.98,
                    "120" to 0.98,
                    "144" to 0.98,
                    "168" to 0.98,
                    "192" to 0.98,
                    "221" to 0.8392,
                    "291" to 0.98,
                    "293" to 0.46,
                ),
            "dodo" to
                mapOf(
                    "24" to 0.98,
                    "48" to 0.98,
                    "72" to 0.98,
                    "96" to 0.98,
                    "120" to 0.98,
                    "144" to 0.98,
                    "168" to 0.94,
                    "192" to 0.94,
                    "216" to 0.64,
                    "288" to 0.64,
                    "290" to 0.72,
                    "291" to 0.901,
                ),
            "mard" to
                mapOf(
                    "24" to 0.98,
                    "48" to 0.98,
                    "72" to 0.98,
                    "96" to 0.98,
                    "120" to 0.98,
                    "144" to 0.98,
                    "168" to 0.78,
                    "221" to 0.98,
                    "264" to 0.98,
                    "291" to 0.98,
                    "295" to 0.98,
                ),
        )

    val brands: List<ColorConfidenceBrandMatrix> =
        ColorSchemePreset.visibleBrands.map { brand ->
            ColorConfidenceBrandMatrix(
                brand = brand,
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
        )

    private fun variantPrimarySortKey(key: String): Int {
        return key.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun variantSuffixSortKey(key: String): String {
        return key.dropWhile { it.isDigit() }.trimStart('-')
    }
}
