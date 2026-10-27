package com.douduoyu.app

import android.content.Context
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

/**
 * 「小鱼生快」寄语页。
 *
 * 与生日蛋糕相同的设计口径：兑换码只做一次性解锁，解锁状态写进本地偏好；
 * 真正的口令不以明文入库，只保存 SHA-256，避免代码/安装包里直接搜到兑换码。
 */
internal object XiangXiangGift {
    const val GIFT_ID = "xiangxiang-gift-v1"

    /** 对应 res/raw/xiangxiang.mp4（已去掉音轨，循环静音播放） */
    const val VIDEO_RES_NAME = "xiangxiang"

    /** sha256("xiangxiang") */
    private const val PASSCODE_SHA256 =
        "01a991d4a3cc68bce3b359bca220ec6090188c3a72558038890bf0d59d476200"

    const val POEM_TITLE = "小鱼生快"

    /** 题款：放在标题下方，代替分隔线 */
    const val POEM_AUTHOR = "小香香"

    /** 四句对句，按原图逐字录入 */
    val POEM_LINES =
        listOf(
            "榆安添岁，祝岁岁平安。",
            "向天祈福，祝健健康康。",
            "来日可期，祝日日心欢。",
            "与君共勉，祝步步昂扬。",
        )

    /** 诗的最后一行：以小字、低对比度呈现，单击屏幕可隐藏 / 显示。
     *  在逗号处固定断行，避免自动换行把结尾挤到下一行；
     *  末尾颜文字从 [POEM_EMOTICONS] 里随机取，每次重新显示都换一个。 */
    const val POEM_SECRET_BODY = "生日快乐，然后，\n你只是香香我第二喜欢的人"

    /** 结尾颜文字候选 */
    val POEM_EMOTICONS =
        listOf(
            "(*・ω・*)",
            "(๑>◡<๑)",
            "(´｡• ᵕ •｡`)",
            "٩(ˊᗜˋ*)و",
            "(｡•̀ᴗ-)✧",
        )

    /** 拼出整行小字：[emoticonIndex] 对应 [POEM_EMOTICONS] 的下标，越界时退回第一个 */
    fun poemSecret(emoticonIndex: Int): String {
        val emoticon = POEM_EMOTICONS[emoticonIndex.coerceIn(POEM_EMOTICONS.indices)]
        return "$POEM_SECRET_BODY $emoticon"
    }

    /** 随机挑一个与 [current] 不同的颜文字下标，避免连续两次看到同一个；
     *  [current] 越界（首次使用传 -1）时就是纯随机。 */
    fun nextEmoticonIndex(current: Int): Int {
        val count = POEM_EMOTICONS.size
        if (count <= 1) return 0
        if (current !in POEM_EMOTICONS.indices) return Random.nextInt(count)
        val picked = Random.nextInt(count - 1)
        return if (picked >= current) picked + 1 else picked
    }

    fun normalizePasscode(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun matchesPasscode(value: String): Boolean {
        val actual = sha256Hex(normalizePasscode(value))
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            PASSCODE_SHA256.toByteArray(Charsets.US_ASCII),
        )
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
}

internal class XiangXiangGiftStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isRedeemed(): Boolean = preferences.getBoolean(KEY_REDEEMED, false)

    /** 返回 true 表示兑换码正确。已解锁的页面不会因为再次输入而失效。 */
    fun redeem(passcode: String): Boolean {
        if (!XiangXiangGift.matchesPasscode(passcode)) return false
        if (!isRedeemed()) {
            preferences.edit()
                .putBoolean(KEY_REDEEMED, true)
                .putLong(KEY_REDEEMED_AT, System.currentTimeMillis())
                .apply()
        }
        return true
    }

    private companion object {
        const val PREFERENCES_NAME = "douduoyu-xiangxiang-gift"
        const val KEY_REDEEMED = "redeemed"
        const val KEY_REDEEMED_AT = "redeemed_at"
    }
}
