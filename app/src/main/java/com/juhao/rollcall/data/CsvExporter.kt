package com.juhao.rollcall.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 导出 CSV：日期,姓名,出勤,作业。
 * 文件写在应用私有外部目录，数据线连电脑即可拷出：
 * /sdcard/Android/data/com.juhao.rollcall/files/
 */
object CsvExporter {

    private const val HEADER = "日期,姓名,出勤,作业"

    fun buildCsv(
        students: List<Student>,
        records: Map<String, DayRecord>,
    ): String {
        val builder = StringBuilder()
        builder.append(HEADER).append("\r\n")
        records.keys.sorted().forEach { date ->
            val record = records.getValue(date)
            // 按名单顺序输出；名单本身是拼音 A→Z 排好的
            students.forEach { student ->
                val id = student.id.toString()
                // 每一天按当天记录的模式解析，不受现在设置影响
                val status = AttendanceLogic.resolve(record.attendance[id], record.mode)
                val attendanceText = status?.label ?: "未点"
                val homeworkText = when {
                    !record.homework.containsKey(id) -> "未记"
                    record.homework.getValue(id) -> "已交"
                    else -> "未交"
                }
                builder.append(date).append(',')
                    .append(escape(student.name)).append(',')
                    .append(attendanceText).append(',')
                    .append(homeworkText)
                    .append("\r\n")
            }
        }
        return builder.toString()
    }

    /** @return 导出文件的绝对路径，失败返回空串。 */
    fun export(
        context: Context,
        students: List<Student>,
        records: Map<String, DayRecord>,
    ): String {
        if (students.isEmpty() || records.isEmpty()) return ""
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())
        val file = File(dir, "点名记录_$stamp.csv")
        return runCatching {
            // BOM 让 Excel 正确识别 UTF-8
            file.writeText("\uFEFF" + buildCsv(students, records), Charsets.UTF_8)
            file.absolutePath
        }.getOrDefault("")
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
