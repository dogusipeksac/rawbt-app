package com.example.rawbtapp.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * ESC/POS komutları için yardımcı sınıf
 * Termal yazıcılar için standart ESC/POS komutlarını oluşturur
 */
class EscPosCommands {
    
    private val buffer = ByteArrayOutputStream()
    
    companion object {
        // ESC/POS Kontrol Komutları
        private val ESC = byteArrayOf(0x1B)
        private val GS = byteArrayOf(0x1D)
        
        // Yazıcı başlatma
        val INIT = byteArrayOf(0x1B, 0x40)
        
        // Hizalama komutları
        val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        
        // Metin stili
        val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        
        val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
        val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)
        
        // Çift boyut
        val DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
        val DOUBLE_WIDTH_ON = byteArrayOf(0x1B, 0x21, 0x20)
        val DOUBLE_SIZE_ON = byteArrayOf(0x1B, 0x21, 0x30)
        val NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)
        
        // Satır besleme ve kesme
        val LINE_FEED = byteArrayOf(0x0A)
        val FEED_PAPER = byteArrayOf(0x1B, 0x64, 0x02) // 2 satır besle
        val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x00) // Tam kesim
        val CUT_PAPER_PARTIAL = byteArrayOf(0x1D, 0x56, 0x01) // Kısmi kesim
        
        // Karakter seti (Türkçe karakter desteği için)
        val CHARSET_PC437 = byteArrayOf(0x1B, 0x74, 0x00) // USA
        val CHARSET_PC850 = byteArrayOf(0x1B, 0x74, 0x02) // Multilingual
        val CHARSET_PC857 = byteArrayOf(0x1B, 0x74, 0x0D) // Turkish
    }
    
    /**
     * Yazıcıyı başlat
     */
    fun initialize(): EscPosCommands {
        buffer.write(INIT)
        buffer.write(CHARSET_PC857) // Türkçe karakter desteği
        return this
    }
    
    /**
     * Normal metin ekle
     */
    fun text(text: String): EscPosCommands {
        buffer.write(text.toByteArray(Charset.forName("Windows-1254")))
        return this
    }
    
    /**
     * Metin ekle ve satır atla
     */
    fun textLine(text: String): EscPosCommands {
        text(text)
        newLine()
        return this
    }
    
    /**
     * Kalın metin ekle
     */
    fun boldText(text: String): EscPosCommands {
        buffer.write(BOLD_ON)
        buffer.write(text.toByteArray(Charset.forName("Windows-1254")))
        buffer.write(BOLD_OFF)
        return this
    }
    
    /**
     * Kalın metin ekle ve satır atla
     */
    fun boldTextLine(text: String): EscPosCommands {
        boldText(text)
        newLine()
        return this
    }
    
    /**
     * Çift boyutlu metin ekle
     */
    fun doubleText(text: String): EscPosCommands {
        buffer.write(DOUBLE_SIZE_ON)
        buffer.write(text.toByteArray(Charset.forName("Windows-1254")))
        buffer.write(NORMAL_SIZE)
        return this
    }
    
    /**
     * Çift boyutlu metin ekle ve satır atla
     */
    fun doubleTextLine(text: String): EscPosCommands {
        doubleText(text)
        newLine()
        return this
    }
    
    /**
     * Sola hizala
     */
    fun alignLeft(): EscPosCommands {
        buffer.write(ALIGN_LEFT)
        return this
    }
    
    /**
     * Ortaya hizala
     */
    fun alignCenter(): EscPosCommands {
        buffer.write(ALIGN_CENTER)
        return this
    }
    
    /**
     * Sağa hizala
     */
    fun alignRight(): EscPosCommands {
        buffer.write(ALIGN_RIGHT)
        return this
    }
    
    /**
     * Yeni satır ekle
     */
    fun newLine(lines: Int = 1): EscPosCommands {
        repeat(lines) {
            buffer.write(LINE_FEED)
        }
        return this
    }
    
    /**
     * Yatay çizgi ekle
     */
    fun horizontalLine(char: String = "-", length: Int = 32): EscPosCommands {
        textLine(char.repeat(length))
        return this
    }
    
    /**
     * Kağıt besle
     */
    fun feedPaper(lines: Int = 3): EscPosCommands {
        buffer.write(byteArrayOf(0x1B, 0x64, lines.toByte()))
        return this
    }
    
    /**
     * Kağıdı kes
     */
    fun cutPaper(partial: Boolean = false): EscPosCommands {
        if (partial) {
            buffer.write(CUT_PAPER_PARTIAL)
        } else {
            buffer.write(CUT_PAPER)
        }
        return this
    }
    
    /**
     * İki sütunlu metin (sol ve sağ hizalı)
     */
    fun twoColumnText(left: String, right: String, totalWidth: Int = 32): EscPosCommands {
        val spaces = totalWidth - left.length - right.length
        if (spaces > 0) {
            textLine(left + " ".repeat(spaces) + right)
        } else {
            textLine(left + " " + right)
        }
        return this
    }
    
    /**
     * Komutları byte array olarak al
     */
    fun build(): ByteArray {
        return buffer.toByteArray()
    }
    
    /**
     * Buffer'ı temizle
     */
    fun clear(): EscPosCommands {
        buffer.reset()
        return this
    }
}

/**
 * ESC/POS komutları oluşturmak için DSL stili builder
 */
fun buildEscPosCommand(block: EscPosCommands.() -> Unit): ByteArray {
    return EscPosCommands().apply(block).build()
}
