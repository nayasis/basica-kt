package com.github.nayasis.kotlin.basica.core.character

import java.lang.Character.UnicodeBlock

private val CHINESE = hashSetOf(
    UnicodeBlock.CJK_COMPATIBILITY,
    UnicodeBlock.CJK_COMPATIBILITY_FORMS,
    UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
    UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
    UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
    UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
    UnicodeBlock.KANGXI_RADICALS,
    UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS,
)

private val KOREAN = hashSetOf(
    UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
    UnicodeBlock.HANGUL_JAMO,
    UnicodeBlock.HANGUL_JAMO_EXTENDED_A,
    UnicodeBlock.HANGUL_JAMO_EXTENDED_B,
    UnicodeBlock.HANGUL_SYLLABLES,
)
private val JAPANESE = hashSetOf(
    UnicodeBlock.HIRAGANA,
    UnicodeBlock.KATAKANA,
    UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
    UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
)
private val CJK = HashSet<UnicodeBlock>().apply {
    addAll(CHINESE)
    addAll(KOREAN)
    addAll(JAPANESE)
    add(UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS)
}

var NULL_CHAR = '\u0000'

/** Hangul Chosung  */
private val HANGUL_1ST = charArrayOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
/** Hangul Joongsung  */
private val HANGUL_2ND = charArrayOf('ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ')
/** Hangul Jongsung  */
private val HANGUL_3RD = charArrayOf(NULL_CHAR,'ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ','ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ','ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ')

class Characters { companion object {

    /** font width of Full-width character  */
    var fullwidth = 1.0

    /** font width of Half-width character  */
    var halfwidth = 1.0

    fun isFontWidthModified(): Boolean = fullwidth != 1.0 || halfwidth != 1.0

}}

/**
 * disassemble Korean character to Chosung, Joongsung, Jonsung
 *
 * <pre>
 * '롱'.disassembleHangul() -&gt; [ 'ㄹ','ㅗ','ㅇ']
 * '수'.disassembleHangul() -&gt; ['ㅅ','ㅜ','\0' ]
 * 'H'.disassembleHangul() -&gt; null
 * </pre>
 *
 * @return character array (null if it can not be resolved.)
 */
fun Char?.disassembleHangul(): CharArray? {
    if( this == null ) return null
    var c = this.code
    if (c < 0xAC00 || c > 0xD79F) return null
    c -= 0xAC00
    val idx3rd = c % 28
    val idx2nd = (c - idx3rd) / 28 % 21
    val idx1st = (c - idx3rd) / 28 / 21
    val result = CharArray(3)
    result[0] = HANGUL_1ST[idx1st]
    result[1] = HANGUL_2ND[idx2nd]
    result[2] = HANGUL_3RD[idx3rd]
    return result
}

/**
 * check if character has Korean Jonsung.
 *
 * <pre>
 * 'H'.hasHangulJongsung( 'H'  ) -&gt; false
 * '수'.hasHangulJongsung( '수' ) -&gt; false
 * '롱' .hasHangulJongsung( '롱' ) -&gt; true
 * </pre>
 *
 * @return true if character has Korean Jonsung.
 */
fun Char?.hasHangulJongsung(): Boolean {
    return disassembleHangul()?.get(2) != NULL_CHAR
}

/**
 * check if character is half-width
 *
 * @return true if character is half-width
 * @see [http://unicode.org/reports/tr11](http://unicode.org/reports/tr11)
 * @see [http://unicode.org/charts/PDF/UFF00.pdf](http://unicode.org/charts/PDF/UFF00.pdf)
 */
fun Char?.isHalfWidth(): Boolean {
    if( this == null ) return false
    val c = this.code
    if (c < 0x0020) return true // special character
    if (c in 0x0020..0x007F) return true // ASCII (Latin characters, symbols, punctuation,numbers)
    if (c in 0xFF61..0xFF64) return true // Halfwidth CJK punctuation
    if (c in 0xFF65..0xFF9F) return true // Halfwidth Katakanana variants
    if (c in 0xFFA0..0xFFDC) return true // Halfwidth Hangul variants
    // FFE8 ~ FFEE : Halfwidth symbol variants
    return c in 0xFFE8..0xFFEE
}

/**
 * convert double byte character to half-width.
 *
 * target :
 * - ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ
 * - ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ
 * - ０１２３４５６７８９
 * - ！＃＄％＆（）＊＋，－．／：；＜＝＞？＠［］＾＿｀｛｜｝”’￥～
 * - ー、。・「」
 * - 「　」
 */
fun Char.toHalfWidth(): Char {
    return when(this.code) {
        in 0xFF21..0XFF5A -> this - 65248 // Ａ~Ｚ,ａ~ｚ
        in 0xFF01..0XFF5D -> this - 65248 // ０~９, ！~～
        else -> when(this) {
            '”' -> '"'
            '’' -> '\''
            '￥' -> '\\'
            '～' -> '~'
            '・' -> '･'
            'ー' -> 'ｰ'
            '、' -> '､'
            '。' -> '｡'
            '「' -> '｢'
            '」' -> '｣'
            '　' -> ' '
            else -> this
        }
    }
}

/**
 * get font width to print
 *
 * @return font width to print
 */
val Char?.fontWidth: Double
    get() {
        return if (isHalfWidth()) Characters.halfwidth else if (isCJK()) Characters.fullwidth else 1.0
    }

/**
 * check if character is korean
 *
 * @return true if character is korean
 */
fun Char?.isKorean(): Boolean {
    return if (this == null) false else if (KOREAN.contains(UnicodeBlock.of(this))) true else this.code in 0xFFA0..0xFFDC
}

/**
 * check if character is japanese
 *
 * @return true if character is japanese
 */
fun Char?.isJapanese(): Boolean {
    return if (this == null) false else if (JAPANESE.contains(UnicodeBlock.of(this))) true else this.code in 0xFF65..0xFF9F
}

/**
 * check if character is chinese
 *
 * @return true if character is chinese
 */
fun Char?.isChinese(): Boolean {
    return if (this == null) false else if (CHINESE.contains(UnicodeBlock.of(this))) true else this.code in 0xFF65..0xFF9F
}

/**
 * check if character is chinese or japanese or korean
 *
 * @return true if character is chinese or japanese or korean
 */
fun Char?.isCJK(): Boolean {
    return this != null && CJK.contains(UnicodeBlock.of(this))
}

fun Char.repeat(n:Int): String {
    return this.toString().repeat(n)
}