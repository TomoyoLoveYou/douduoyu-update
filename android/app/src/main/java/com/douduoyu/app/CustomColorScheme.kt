package com.douduoyu.app

import java.util.UUID
import java.util.Locale

data class CustomColorScheme(
    val id: String,
    val name: String,
    val baseScheme: ColorSchemePreset,
    val selectedCodes: List<String>,
    val colorOverrides: Map<String, Int>,
    val createdAt: Long,
    val updatedAt: Long,
    val customGroupId: String = "",
    val templateSelectedCodes: List<String> = emptyList(),
    val archived: Boolean = false,
) {
    fun normalizedOrNull(): CustomColorScheme? {
        val normalizedId = id.trim().takeIf { it.isNotEmpty() } ?: return null
        val normalizedGroupId = customGroupId.trim().ifEmpty { normalizedId }
        val normalizedName = name.trim().takeIf { it.isNotEmpty() } ?: return null
        val catalog = catalogColors(baseScheme)
        val catalogCodes = catalog.mapTo(linkedSetOf()) { it.code }
        val normalizedSelectedCodes =
            selectedCodes
                .asSequence()
                .map(String::trim)
                .filter { it in catalogCodes }
                .distinct()
                .toList()
        if (normalizedSelectedCodes.size < MIN_SELECTED_COLORS) {
            return null
        }
        val normalizedTemplateCodes =
            templateSelectedCodes
                .asSequence()
                .map(String::trim)
                .filter { it in catalogCodes }
                .distinct()
                .toList()
                .takeIf { it.size >= MIN_SELECTED_COLORS }
                ?: normalizedSelectedCodes

        val overridesByCode =
            buildMap {
                colorOverrides.forEach { (rawCode, argb) ->
                    val code = rawCode.trim()
                    if (code in catalogCodes) {
                        put(code, argb.withOpaqueAlpha())
                    }
                }
            }
        val normalizedOverrides =
            buildMap {
                catalog.forEach { color ->
                    overridesByCode[color.code]?.let { put(color.code, it) }
                }
            }
        val normalizedCreatedAt = createdAt.coerceAtLeast(0L)
        val normalizedUpdatedAt = updatedAt.coerceAtLeast(normalizedCreatedAt)
        return copy(
            id = normalizedId,
            customGroupId = normalizedGroupId,
            name = normalizedName,
            selectedCodes = normalizedSelectedCodes,
            colorOverrides = normalizedOverrides,
            templateSelectedCodes = normalizedTemplateCodes,
            createdAt = normalizedCreatedAt,
            updatedAt = normalizedUpdatedAt,
        )
    }

    fun palette(includeDisabled: Boolean = false): List<PaletteColor> {
        val normalized = normalizedOrNull() ?: return emptyList()
        val selected = normalized.selectedCodes.toHashSet()
        return catalogColors(normalized.baseScheme)
            .asSequence()
            .filter { includeDisabled || it.code in selected }
            .map { color ->
                PaletteColor(
                    id = normalized.customColorId(color.code),
                    code = color.code,
                    series = normalized.baseScheme.brandLabel,
                    argb = normalized.colorOverrides[color.code] ?: color.argb,
                )
            }
            .toList()
    }

    fun customColorId(code: String): String = customColorId(id, code)

    companion object {
        private const val MIN_SELECTED_COLORS = 2
        const val COLOR_ID_PREFIX = "custom:"

        fun create(
            base: ColorSchemePreset,
            name: String,
            now: Long = System.currentTimeMillis(),
        ): CustomColorScheme {
            val normalizedNow = now.coerceAtLeast(0L)
            val id = UUID.randomUUID().toString()
            return CustomColorScheme(
                id = id,
                name = name.trim().ifEmpty { "${base.brandLabel} \u81ea\u5b9a\u4e49" },
                baseScheme = base,
                selectedCodes = templateCodes(base),
                colorOverrides = emptyMap(),
                createdAt = normalizedNow,
                updatedAt = normalizedNow,
                customGroupId = id,
                templateSelectedCodes = templateCodes(base),
            )
        }

        fun catalogColors(base: ColorSchemePreset): List<PaletteColor> {
            val sameBrandSchemes =
                ColorSchemePreset.entries
                    .filter { it.showInLogicPanel && it.brandId == base.brandId }
                    .sortedByDescending { it.maxColors }
            val largestScheme = sameBrandSchemes.firstOrNull()
            val orderedSchemes =
                buildList {
                    largestScheme?.let(::add)
                    add(base)
                    addAll(sameBrandSchemes)
                }.distinct()
            val colorsByCode = linkedMapOf<String, PaletteColor>()
            orderedSchemes.forEach { scheme ->
                BeadPalette.palette(scheme).forEach { color ->
                    colorsByCode.putIfAbsent(color.code, color)
                }
            }
            BeadPalette.palette(base).forEach { color ->
                colorsByCode[color.code] = color
            }
            return colorsByCode.values.toList()
        }

        fun templateCodes(base: ColorSchemePreset): List<String> =
            BeadPalette.palette(base).map { it.code }.distinct()

        fun customColorId(
            schemeId: String,
            code: String,
        ): String = "$COLOR_ID_PREFIX${schemeId.trim()}:${code.trim()}"
    }
}

private fun Int.withOpaqueAlpha(): Int = this or 0xFF000000.toInt()

internal fun normalizedColorSchemeName(value: String): String =
    value
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

internal fun isBuiltInColorSchemeName(value: String): Boolean {
    val normalized = normalizedColorSchemeName(value)
    if (normalized.isEmpty()) return false
    return ColorSchemePreset.entries.any { scheme ->
        sequenceOf(scheme.label, scheme.brandLabel, scheme.variantLabel)
            .any { candidate -> normalizedColorSchemeName(candidate) == normalized }
    }
}

internal fun nextAvailableCustomGroupName(
    baseName: String,
    schemes: List<CustomColorScheme>,
): String {
    val normalizedNames = schemes.filterNot { it.archived }.mapTo(hashSetOf()) { normalizedColorSchemeName(it.name) }
    val trimmedBase = baseName.trim().ifEmpty { "自定义配色" }
    if (normalizedColorSchemeName(trimmedBase) !in normalizedNames) return trimmedBase
    return generateSequence(2) { it + 1 }
        .map { suffix -> "$trimmedBase $suffix" }
        .first { candidate -> normalizedColorSchemeName(candidate) !in normalizedNames }
}
