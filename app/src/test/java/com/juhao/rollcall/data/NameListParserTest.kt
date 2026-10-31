package com.juhao.rollcall.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameListParserTest {

    @Test
    fun `按换行和逗号拆分`() {
        val names = NameListParser.parse("张三\n李四，王五")
        assertEquals(listOf("张三", "李四", "王五"), names)
    }

    @Test
    fun `去掉行首序号`() {
        val names = NameListParser.parse("1 张三\n02、李四\n3.王五")
        assertEquals(listOf("张三", "李四", "王五"), names)
    }

    @Test
    fun `忽略表头和纯数字`() {
        val names = NameListParser.parse("姓名\n1\n2\n张三")
        assertEquals(listOf("张三"), names)
    }

    @Test
    fun `空格分隔也能识别`() {
        val names = NameListParser.parse("张三 李四 王五")
        assertEquals(listOf("张三", "李四", "王五"), names)
    }

    @Test
    fun `空文本返回空列表`() {
        assertTrue(NameListParser.parse("   \n\n ").isEmpty())
    }
}
