package info.muge.appshare.utils

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

/**
 * 拼音工具类
 */
object PinyinUtil {

    /**
     * 将字符串中的中文转化为拼音,其他字符不变
     * @param inputString 输入字符串
     * @return 拼音字符串
     */
    @JvmStatic
    fun getPinYin(inputString: String): String {
        val format = HanyuPinyinOutputFormat()
        format.caseType = HanyuPinyinCaseType.LOWERCASE
        format.toneType = HanyuPinyinToneType.WITHOUT_TONE
        format.vCharType = HanyuPinyinVCharType.WITH_V

        val input = inputString.trim().toCharArray()
        val output = StringBuilder()

        try {
            for (i in input.indices) {
                if (input[i].toString().matches(Regex("[\\u4E00-\\u9FA5]+"))) {
                    val temp = PinyinHelper.toHanyuPinyinStringArray(input[i], format)
                    output.append(temp[0])
                } else {
                    output.append(input[i])
                }
            }
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            e.printStackTrace()
        } catch (npex: NullPointerException) {
            npex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return output.toString()
    }

    /**
     * 获取汉字串拼音首字母，英文字符不变
     * @param chinese 汉字串
     * @return 汉语拼音首字母
     */
    @JvmStatic
    fun getFirstSpell(chinese: String): String {
        val pybf = StringBuilder()
        val arr = chinese.toCharArray()
        val defaultFormat = HanyuPinyinOutputFormat()
        defaultFormat.caseType = HanyuPinyinCaseType.LOWERCASE
        defaultFormat.toneType = HanyuPinyinToneType.WITHOUT_TONE
        
        try {
            for (i in arr.indices) {
                if (arr[i] > 128.toChar()) {
                    val temp = PinyinHelper.toHanyuPinyinStringArray(arr[i], defaultFormat)
                    if (temp != null) {
                        pybf.append(temp[0][0])
                    }
                } else {
                    pybf.append(arr[i])
                }
            }
        } catch (bhpe: BadHanyuPinyinOutputFormatCombination) {
            bhpe.printStackTrace()
        } catch (npex: NullPointerException) {
            npex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return pybf.toString().replace(Regex("\\W"), "").trim()
    }

    /**
     * 获取汉字串拼音，英文字符不变
     * @param chinese 汉字串
     * @return 汉语拼音
     */
    @JvmStatic
    fun getFullSpell(chinese: String): String {
        val pybf = StringBuilder()
        val arr = chinese.toCharArray()
        val defaultFormat = HanyuPinyinOutputFormat()
        defaultFormat.caseType = HanyuPinyinCaseType.LOWERCASE
        defaultFormat.toneType = HanyuPinyinToneType.WITHOUT_TONE
        
        try {
            for (i in arr.indices) {
                if (arr[i] > 128.toChar()) {
                    pybf.append(PinyinHelper.toHanyuPinyinStringArray(arr[i], defaultFormat)[0])
                } else {
                    pybf.append(arr[i])
                }
            }
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            e.printStackTrace()
        } catch (npex: NullPointerException) {
            npex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return pybf.toString()
    }

    /**
     * 获取一个字符串中的所有汉字内容
     * @param content 要过滤的字符串
     * @return 所有汉字字符串
     */
    @JvmStatic
    fun getAllChineseCharacters(content: String): String {
        return try {
            content.replace(Regex("[^\u4e00-\u9fa5]"), "")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 判断一个char是否为汉字（不包含中文符号）
     * @return true 为汉字
     */
    @JvmStatic
    fun isChineseChar(c: Char): Boolean {
        return c in '\u4e00'..'\u9fbb'
    }
}

