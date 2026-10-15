package com.douduoyu.app

import android.content.Context
import java.security.MessageDigest
import java.util.Locale

internal enum class BirthdayCelebrationTemplate {
    BirthdayCake,
}

data class BirthdayCelebrationConfig(
    val id: String,
    val nickname: String,
    val message: String,
    val projectTitle: String,
)

internal object BirthdayCelebration {
    const val GIFT_ID = "birthday-xiaoyu-cake-v1"
    const val RECIPIENT_NAME = "\u5c0f\u9c7c"
    const val GREETING = "\u5c0f\u9c7c\u751f\u65e5\u5feb\u4e50"
    const val PROJECT_TITLE = "\u5c0f\u9c7c\u7684\u751f\u65e5\u86cb\u7cd5"
    val candleColumns = intArrayOf(13, 19, 25)
    val template = BirthdayCelebrationTemplate.BirthdayCake
    val config =
        BirthdayCelebrationConfig(
            id = GIFT_ID,
            nickname = RECIPIENT_NAME,
            message = GREETING,
            projectTitle = PROJECT_TITLE,
        )

    private const val PASSCODE_SHA256 =
        "8b87deebbf9c0afdf5f2d44503d9db440f05d753d98ae802547b6a408d9dccdf"

    fun normalizePasscode(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun matchesPasscode(value: String): Boolean {
        val actual = sha256Hex(normalizePasscode(value))
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            PASSCODE_SHA256.toByteArray(Charsets.US_ASCII),
        )
    }

    fun createCakeProject(updatedAt: Long = System.currentTimeMillis()): BeadProject {
        val scheme = ColorSchemePreset.MardBrand221
        val palette = BeadPalette.palette(scheme)
        require(palette.isNotEmpty()) { "Birthday project requires a non-empty palette" }

        fun nearest(red: Int, green: Int, blue: Int): String =
            BeadPalette.nearest(red, green, blue, palette).id

        val width = 40
        val height = 40
        val cells = MutableList<String?>(width * height) { null }
        val outlineColor = nearest(253, 192, 208)
        val colorByMarker =
            mapOf(
                'K' to nearest(193, 68, 74),
                'Y' to nearest(254, 235, 76),
                'C' to nearest(254, 203, 145),
                'A' to nearest(254, 164, 195),
                'P' to nearest(254, 86, 138),
                'R' to nearest(254, 55, 104),
                'W' to nearest(255, 255, 255),
                'E' to nearest(252, 237, 225),
                'L' to nearest(254, 220, 175),
                'G' to nearest(254, 209, 146),
                'O' to nearest(254, 193, 89),
                'D' to nearest(254, 170, 42),
            )

        // Restored from the poster's 44x41 logical grid, then compressed to a strict 40x40.
        val art =
            listOf(
                ".............KK....KK....KK.............",
                "............KYKK..KYKK..KKYK............",
                "............KYKK..KYKK..KKYK............",
                "............KYKK..KYKK..KKYK............",
                ".............KK....KK....KK.............",
                "............KGKK..KGRK..KKCK............",
                "............KLKK..KCRK..KKCK............",
                "............KLKK..KGRK..KKGK............",
                "............KLKK..KGRK..KKCK............",
                "............KGKK..KCRK..KKCK............",
                "............KCKK..KCRK..KKOK............",
                "......KKKKKKKKKKKKKKKKKKKKKKKKKKKK......",
                ".....KAAAAAAAAAAAAAAAAAAAPAAAAPPPAK.....",
                ".....KPPPPPPPPPPPPPPPPPPRRRRRRRRRRK.....",
                ".....KPPPWPPPPPPPPPPPPPPRRRRRRRRRRK.....",
                ".....KPPWPPPPPPPPPPPPPPPRRRRRRRRRRK.....",
                ".....KAPPPPAAPPPPAPPPPAPRRRAARRRRAK.....",
                "......KAPPAKKAPPAKAPPAKARRAKKARRAK......",
                ".....KWKAAKEEKAAKEKAAKEKAAKEEKPAKEK.....",
                ".....KWEKKEWWEKKEWEKKEWEKKEEEEKKEEK.....",
                ".....KWWEEWWWWEEWWWEEWWWEEEEEELLEEK.....",
                ".....KWWWWWWWWWWWWWWWWWWEEEEEEEEEEK.....",
                ".....KWWWWWWWWWWWWWWWWWWEEEEEEEEEEK.....",
                "......KWWWWWWWWWWWWWWWWWEEEEEEEEEK......",
                ".KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK.",
                "KAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPPPPAK",
                "KPPPPAPPPPPPPPPPPPPPPPPPRRRRRRRRRRRRRRRK",
                "KPPPWAPPPWPPPPPPPPPPPPPPRRRRRRRRRRRRRRRK",
                "KPPWPAPPWPPPPPPPPPPPPPPPRRRRRRRRRRRRRRRK",
                "KAPPPAAPPPPAAPPPPAPPPPAPRRRAARRRRAPRRRPK",
                ".KAPPKKAPPAKKAPPAKAPPAKARRAKKPRRAKKRRPK.",
                "KCKAAOOKAAKOOKAAKOKAAKOKPPKDDKPPKDDPPKDK",
                "KGOKKOOOKKOOODKKDODKKOOOKKDDDDKKDDDKKDDK",
                "KLOOOLCODDOGGODDOGODDOGODDDOODDDDODDDDDK",
                "KLLOOLGGDDGGGGDDGGGDDGGGDDOOOODDOODDDDDK",
                "KLLLLLGGGGGGGGGGGGGGGGGGOOOOOOOOOODDDDDK",
                "KLLLLLGGGGGGGGGGGGGGGGGGOOOOOOOOOODDDDDK",
                "KGCCCCOOOOOOOOOOOOOOOOOODDDDDDDDDDDDDDDK",
                ".KCCCCOOOOOOOOOOOOOOOOOODDDDDDDDDDDDDDK.",
                "..KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK..",
            )
        require(art.size == height && art.all { it.length == width }) {
            "Birthday cake sprite must remain 40x40"
        }
        art.forEachIndexed { y, row ->
            row.forEachIndexed { x, marker ->
                if (marker != '.') {
                    val isOuterOutline =
                        marker == 'K' &&
                            (x == 0 || x == width - 1 || y == 0 || y == height - 1 ||
                                art[y][x - 1] == '.' || art[y][x + 1] == '.' ||
                                art[y - 1][x] == '.' || art[y + 1][x] == '.')
                    cells[y * width + x] =
                        if (isOuterOutline) {
                            outlineColor
                        } else {
                            requireNotNull(colorByMarker[marker]) { "Unknown cake marker: $marker" }
                        }
                }
            }
        }

        return BeadProject(
            id = GIFT_ID,
            title = PROJECT_TITLE,
            width = width,
            height = height,
            cells = cells,
            paletteMode = scheme.paletteMode,
            colorScheme = scheme,
            sourceKind = ProjectSourceKind.Celebration,
            updatedAt = updatedAt,
        )
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
}

internal class BirthdayCelebrationStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isRedeemed(): Boolean = preferences.getBoolean(KEY_REDEEMED, false)

    fun redeemedAtMillis(): Long? =
        preferences.getLong(KEY_REDEEMED_AT, NO_REDEMPTION_TIME)
            .takeUnless { it == NO_REDEMPTION_TIME }

    /** Returns true for a valid code. A redeemed gift remains available and is never consumed. */
    fun redeem(
        passcode: String,
        redeemedAt: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!BirthdayCelebration.matchesPasscode(passcode)) return false
        if (!isRedeemed()) {
            preferences.edit()
                .putBoolean(KEY_REDEEMED, true)
                .putLong(KEY_REDEEMED_AT, redeemedAt)
                .apply()
        }
        return true
    }

    private companion object {
        const val PREFERENCES_NAME = "douduoyu-birthday-celebration"
        const val KEY_REDEEMED = "redeemed"
        const val KEY_REDEEMED_AT = "redeemed_at"
        const val NO_REDEMPTION_TIME = Long.MIN_VALUE
    }
}
