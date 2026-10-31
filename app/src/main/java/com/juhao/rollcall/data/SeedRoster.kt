package com.juhao.rollcall.data

import android.content.Context

/**
 * 内置名单：assets/roster.csv（一行一个名字，UTF-8）。
 * 首次启动（或设置里点“恢复内置名单”）时导入。
 */
object SeedRoster {

    private const val ASSET_NAME = "roster.csv"
    private const val FALLBACK_COUNT = 40

    fun load(context: Context): List<String> {
        val fromAsset = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
        val parsed = fromAsset?.let { NameListParser.parse(it) }.orEmpty()
        return parsed.ifEmpty { placeholders(FALLBACK_COUNT) }
    }

    fun placeholders(count: Int): List<String> = (1..count).map { "学生%02d".format(it) }
}
