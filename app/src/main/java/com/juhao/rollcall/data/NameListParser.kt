package com.juhao.rollcall.data

/**
 * 名单文本解析：支持一行一个，也支持用逗号 / 顿号 / 分号 / 空格 / Tab 分隔。
 * 会去掉行首的序号（"1 "、"01、"、"3." 之类），忽略表头词。
 */
object NameListParser {

    private val separators = Regex("[,，、;；\\t\\r\\n]+")
    private val numbering = Regex("^\\d{1,3}\\s*[.．、:：)）]?\\s*")
    private val headerWords = setOf(
        "姓名", "名字", "序号", "学号", "学生", "名单", "班级", "name", "no",
    )
    private const val MAX_NAME_LENGTH = 20

    fun parse(raw: String): List<String> {
        return raw
            .split(separators)
            .flatMap { chunk -> chunk.split(' ') }
            .map { it.trim() }
            .map { stripNumbering(it) }
            .filter { it.isNotEmpty() }
            .filter { it.length <= MAX_NAME_LENGTH }
            .filterNot { isHeader(it) }
    }

    private fun stripNumbering(value: String): String = value.replace(numbering, "").trim()

    private fun isHeader(value: String): Boolean {
        val lower = value.lowercase()
        if (lower in headerWords) return true
        // "姓名：张三" 这种整行表头不处理，交给上面的分隔符拆开
        return value.all { it.isDigit() }
    }
}
