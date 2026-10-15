package com.douduoyu.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CustomColorSchemeStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<CustomColorScheme> {
        val primary = preferences.getString(KEY_SCHEMES_JSON, null)
        return decode(primary)
            ?: decode(preferences.getString(KEY_SCHEMES_JSON_BACKUP, null))
            ?: emptyList()
    }

    private fun decode(raw: String?): List<CustomColorScheme>? {
        val payload = raw?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val root = JSONObject(payload)
            if (root.optInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION) > SCHEMA_VERSION) {
                return null
            }
            val items = root.optJSONArray(KEY_ITEMS) ?: return null
            val byId = linkedMapOf<String, CustomColorScheme>()
            for (index in 0 until items.length()) {
                val scheme = readScheme(items.optJSONObject(index) ?: continue) ?: continue
                val previous = byId[scheme.id]
                if (previous == null || scheme.updatedAt >= previous.updatedAt) {
                    byId[scheme.id] = scheme
                }
            }
            byId.values.toList()
        }.getOrNull()
    }

    fun saveAll(schemes: List<CustomColorScheme>) {
        val existingPayload = preferences.getString(KEY_SCHEMES_JSON, null)
        if (existingPayload != null && decode(existingPayload) == null) {
            return
        }
        val normalizedById = linkedMapOf<String, CustomColorScheme>()
        schemes.forEach { rawScheme ->
            val scheme = rawScheme.normalizedOrNull() ?: return@forEach
            val previous = normalizedById[scheme.id]
            if (previous == null || scheme.updatedAt >= previous.updatedAt) {
                normalizedById[scheme.id] = scheme
            }
        }
        val root =
            JSONObject()
                .put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
                .put(
                    KEY_ITEMS,
                    JSONArray().apply {
                        normalizedById.values.forEach { put(writeScheme(it)) }
                    },
                )
        preferences
            .edit()
            .apply {
                existingPayload?.takeIf { it.isNotBlank() }?.let { putString(KEY_SCHEMES_JSON_BACKUP, it) }
                putString(KEY_SCHEMES_JSON, root.toString())
            }.apply()
    }

    private fun readScheme(json: JSONObject): CustomColorScheme? =
        runCatching {
            if (json.isNull(KEY_ID) || json.isNull(KEY_NAME)) {
                return null
            }
            val id = json.optString(KEY_ID, "").trim().takeIf { it.isNotEmpty() } ?: return null
            val name = json.optString(KEY_NAME, "").trim().takeIf { it.isNotEmpty() } ?: return null
            val baseScheme = ColorSchemePreset.valueOf(json.optString(KEY_BASE_SCHEME))
            val selectedCodesJson = json.optJSONArray(KEY_SELECTED_CODES) ?: return null
            val selectedCodes =
                buildList {
                    for (index in 0 until selectedCodesJson.length()) {
                        selectedCodesJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            val templateSelectedCodesJson = json.optJSONArray(KEY_TEMPLATE_SELECTED_CODES)
            val templateSelectedCodes =
                buildList {
                    if (templateSelectedCodesJson != null) {
                        for (index in 0 until templateSelectedCodesJson.length()) {
                            templateSelectedCodesJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
            val overridesJson = json.optJSONObject(KEY_COLOR_OVERRIDES)
            val overrides =
                buildMap {
                    if (overridesJson != null) {
                        val keys = overridesJson.keys()
                        while (keys.hasNext()) {
                            val code = keys.next()
                            if (overridesJson.has(code) && !overridesJson.isNull(code)) {
                                runCatching { overridesJson.getInt(code) }
                                    .getOrNull()
                                    ?.let { argb -> put(code, argb) }
                            }
                        }
                    }
                }
            CustomColorScheme(
                id = id,
                name = name,
                baseScheme = baseScheme,
                selectedCodes = selectedCodes,
                colorOverrides = overrides,
                createdAt = json.optLong(KEY_CREATED_AT, 0L),
                updatedAt = json.optLong(KEY_UPDATED_AT, 0L),
                customGroupId = json.optString(KEY_CUSTOM_GROUP_ID, ""),
                templateSelectedCodes = templateSelectedCodes,
                archived = json.optBoolean(KEY_ARCHIVED, false),
            ).normalizedOrNull()
        }.getOrNull()

    private fun writeScheme(scheme: CustomColorScheme): JSONObject =
        JSONObject()
            .put(KEY_ID, scheme.id)
            .put(KEY_NAME, scheme.name)
            .put(KEY_CUSTOM_GROUP_ID, scheme.customGroupId)
            .put(KEY_BASE_SCHEME, scheme.baseScheme.name)
            .put(
                KEY_SELECTED_CODES,
                JSONArray().apply { scheme.selectedCodes.forEach(::put) },
            )
            .put(
                KEY_COLOR_OVERRIDES,
                JSONObject().apply {
                    scheme.colorOverrides.forEach { (code, argb) -> put(code, argb) }
                },
            )
            .put(
                KEY_TEMPLATE_SELECTED_CODES,
                JSONArray().apply { scheme.templateSelectedCodes.forEach(::put) },
            )
            .put(KEY_CREATED_AT, scheme.createdAt)
            .put(KEY_UPDATED_AT, scheme.updatedAt)
            .put(KEY_ARCHIVED, scheme.archived)

    private companion object {
        const val PREFERENCES_NAME = "douduoyu-custom-color-schemes"
        const val KEY_SCHEMES_JSON = "custom_color_schemes_json"
        const val KEY_SCHEMES_JSON_BACKUP = "custom_color_schemes_json_backup"
        const val SCHEMA_VERSION = 1
        const val KEY_SCHEMA_VERSION = "schemaVersion"
        const val KEY_ITEMS = "items"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_CUSTOM_GROUP_ID = "customGroupId"
        const val KEY_BASE_SCHEME = "baseScheme"
        const val KEY_SELECTED_CODES = "selectedCodes"
        const val KEY_COLOR_OVERRIDES = "colorOverrides"
        const val KEY_TEMPLATE_SELECTED_CODES = "templateSelectedCodes"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_ARCHIVED = "archived"
    }
}
