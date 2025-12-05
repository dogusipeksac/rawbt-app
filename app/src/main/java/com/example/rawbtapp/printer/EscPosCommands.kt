package com.example.rawbtapp.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * ESC/POS komutları için yardımcı sınıf
 * Termal yazıcılar için standart ESC/POS komutlarını oluşturur
 */
class EscPosCommands {
    
    private val buffer = ByteArrayOutputStream()
    var charsetEncoding: String = "PC857_CP857"  // Varsayılan: Çalışan encoding - PC857 + CP857 (Turkish)
    var cancelTurkishChars: Boolean = false  // Türkçe karakterleri İngilizce karşılıklarına çevir
    
    companion object {
        // ESC/POS Kontrol Komutları
        private val ESC = byteArrayOf(0x1B)
        private val GS = byteArrayOf(0x1D)
        
        // Yazıcı başlatma
        val INIT = byteArrayOf(0x1B, 0x40)
        
        // Çince modunu iptal et - XPrinter için kritik!
        // Bazı XPrinter modelleri varsayılan olarak Çince modunda geliyor
        val CANCEL_CHINESE_MODE = byteArrayOf(0x1C, 0x2E)  // FS . (Cancel Chinese Mode)
        
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

        // Farklı kesme komutları - Her printer farklı komut kullanabilir
        val CUT_PAPER_FULL = byteArrayOf(0x1D, 0x56, 0x00)        // GS V 0 - Tam kesim (çoğu printer)
        val CUT_PAPER_PARTIAL = byteArrayOf(0x1D, 0x56, 0x01)     // GS V 1 - Kısmi kesim
        val CUT_PAPER_ESC_i = byteArrayOf(0x1B, 0x69)             // ESC i - Tam kesim (bazı Epson)
        val CUT_PAPER_ESC_m = byteArrayOf(0x1B, 0x6D)             // ESC m - Kısmi kesim (bazı Epson)
        val CUT_PAPER_GS_V_65 = byteArrayOf(0x1D, 0x56, 0x41)     // GS V 65 - Tam kesim (bazı Star)
        val CUT_PAPER_GS_V_66 = byteArrayOf(0x1D, 0x56, 0x42)     // GS V 66 - Kısmi kesim (bazı Star)

        // Geriye uyumluluk için
        val CUT_PAPER = CUT_PAPER_FULL
        
        // Karakter seti (Türkçe karakter desteği için)
        val CHARSET_PC437 = byteArrayOf(0x1B, 0x74, 0x00) // USA (ESC t 0)
        val CHARSET_PC850 = byteArrayOf(0x1B, 0x74, 0x02) // Multilingual (Türkçe dahil) (ESC t 2)
        val CHARSET_PC852 = byteArrayOf(0x1B, 0x74, 0x12) // Central Europe (Türkçe dahil) (ESC t 18)
        val CHARSET_PC853 = byteArrayOf(0x1B, 0x74, 0x08) // Turkish (ESC t 8)
        val CHARSET_PC857 = byteArrayOf(0x1B, 0x74, 0x0D) // Turkish (ESC t 13)
        val CHARSET_PC857_61 = byteArrayOf(0x1B, 0x74, 0x3D) // PC857 Turkish (ESC t 61) - Self-test: 61:PC857 Turkish
        val CHARSET_PC858 = byteArrayOf(0x1B, 0x74, 0x13) // Multilingual Euro (ESC t 19)
        val CHARSET_PC860 = byteArrayOf(0x1B, 0x74, 0x03) // Portuguese (ESC t 3)
        val CHARSET_PC863 = byteArrayOf(0x1B, 0x74, 0x04) // Canadian French (ESC t 4)
        val CHARSET_PC865 = byteArrayOf(0x1B, 0x74, 0x05) // Nordic (ESC t 5)
        val CHARSET_PC866 = byteArrayOf(0x1B, 0x74, 0x11) // Cyrillic (ESC t 17)
        val CHARSET_PC869 = byteArrayOf(0x1B, 0x74, 0x06) // Greek (ESC t 6)
        val CHARSET_PC3846 = byteArrayOf(0x1B, 0x74, 0x4F) // PC3846 Turkish (79 decimal = 0x4F) (ESC t 79)
        val CHARSET_WINDOWS1254 = byteArrayOf(0x1B, 0x74, 0x10) // Windows-1254 (Turkish) (ESC t 16)
        
        /**
         * Her yazdırma öncesi çalıştırılacak başlangıç komutları
         * ÖNCELİKLE Çince modunu iptal et, sonra printer'ı initialize et
         * XPrinter için kritik - bazı modeller varsayılan olarak Çince modunda geliyor
         */
        fun getInitCommands(): ByteArray {
            return byteArrayOf(
                0x1C.toByte(), 0x2E.toByte(),  // 1. ÖNCELİKLE Çince modu iptal et! (FS .)
                0x1B.toByte(), 0x40.toByte()   // 2. Printer'ı initialize et (ESC @)
            )
        }
        
        /**
         * Encoding string'ini parse et ve (charsetCommand, charsetName) döndür
         * @return Pair<ByteArray, String> - (karakter seti komutu, encoding adı)
         */
        fun parseEncoding(encoding: String): Pair<ByteArray, String> {
            val charsetCommand = when (encoding) {
                // PC857 kombinasyonları - ESC t 13 (0x0D)
                "PC857_CP857", "PC857_ISO88599", "PC857_Windows1254", "PC857_CP850", "PC857_CP852", "PC857_CP853" -> CHARSET_PC857
                // PC857_61 kombinasyonları - ESC t 61 (0x3D)
                "PC857_61_CP857", "PC857_61_ISO88599", "PC857_61_Windows1254", "PC857_61_CP850", "PC857_61_CP852", "PC857_61_CP853" -> CHARSET_PC857_61
                // PC850 kombinasyonları - ESC t 2 (0x02)
                "PC850_CP850", "PC850_Windows1254", "PC850_ISO88599", "PC850_CP857" -> CHARSET_PC850
                // PC852 kombinasyonları - ESC t 18 (0x12)
                "PC852_CP852", "PC852_CP857", "PC852_CP850" -> CHARSET_PC852
                // PC853 kombinasyonları - ESC t 8 (0x08)
                "PC853_CP853", "PC853_CP857", "PC853_CP850" -> CHARSET_PC853
                // NONE_ prefix - karakter seti komutu gönderme
                "NONE_CP857", "NONE_CP850", "NONE_CP852", "NONE_CP853", "NONE_Windows1254", "NONE_ISO88599", "PC3846_CP3846" -> byteArrayOf()
                else -> {
                    if (encoding.startsWith("NONE_")) {
                        byteArrayOf()
                    } else {
                        byteArrayOf() // Varsayılan: karakter seti komutu gönderme
                    }
                }
            }
            
            // Encoding adını çıkar
            val charsetName = when {
                encoding.contains("CP857") -> "CP857"
                encoding.contains("CP850") -> "CP850"
                encoding.contains("CP852") -> "CP852"
                encoding.contains("CP853") -> "CP853"
                encoding.contains("Windows1254") || encoding.contains("CP1254") -> "Windows-1254"
                encoding.contains("ISO88599") -> "ISO-8859-9"
                else -> "CP857" // Varsayılan
            }
            
            return Pair(charsetCommand, charsetName)
        }
    }
    
    /**
     * Yazıcıyı başlat
     * ÖNCELİKLE Çince modunu iptal et, sonra karakter seti seç
     * charsetEncoding ayarına göre karakter seti seçiliyor
     */
    fun initialize(): EscPosCommands {
        // 1. ÖNCELİKLE ÇİNCE MODU İPTAL ET ve PRINTER'I İNİTİALİZE ET!
        buffer.write(getInitCommands())
        
        // 2. charsetEncoding'e göre karakter seti seç
        when (charsetEncoding) {
            // PC857 kombinasyonları - ESC t 13 (0x0D) - Standart PC857
            "PC857_CP857", "PC857_ISO88599", "PC857_Windows1254", "PC857_CP850", "PC857_CP852", "PC857_CP853" -> buffer.write(CHARSET_PC857)  // ESC t 13
            // PC857_61 kombinasyonları - ESC t 61 (0x3D) - Self-test: 61:PC857 Turkish
            "PC857_61_CP857", "PC857_61_ISO88599", "PC857_61_Windows1254", "PC857_61_CP850", "PC857_61_CP852", "PC857_61_CP853" -> buffer.write(CHARSET_PC857_61)  // ESC t 61
            // PC850 kombinasyonları - ESC t 2 (0x02)
            "PC850_CP850", "PC850_Windows1254", "PC850_ISO88599", "PC850_CP857" -> buffer.write(CHARSET_PC850)  // ESC t 2
            // PC852 kombinasyonları - ESC t 18 (0x12) - Central Europe
            "PC852_CP852", "PC852_CP857", "PC852_CP850" -> buffer.write(CHARSET_PC852)  // ESC t 18
            // PC853 kombinasyonları - ESC t 8 (0x08) - Turkish
            "PC853_CP853", "PC853_CP857", "PC853_CP850" -> buffer.write(CHARSET_PC853)  // ESC t 8
            // Karakter seti seçimi olmadan encoding'ler (NONE_ prefix) - PC3846 komutu Çince karakterlere neden oluyor
            "NONE_CP857", "NONE_CP850", "NONE_CP852", "NONE_CP853", "NONE_Windows1254", "NONE_ISO88599" -> {
                // Karakter seti seçimi yapmıyoruz - sadece encoding kullanıyoruz
            }
            // PC3846_CP3846 - Önceki çalışan encoding (karakter seti komutu göndermeden sadece CP857 encoding)
            "PC3846_CP3846" -> {
                // PC3846 komutu Çince karakterlere neden oluyor, bu yüzden karakter seti komutu göndermiyoruz
                // Sadece CP857 encoding kullanıyoruz
            }
            else -> {
                // NONE_ prefix'i ile başlayan tüm encoding'ler için karakter seti seçimi yapmıyoruz
                if (!charsetEncoding.startsWith("NONE_")) {
                    // Varsayılan: Karakter seti komutu göndermeden (PC3846 komutu Çince karakterlere neden oluyor)
                }
            }
        }
        return this
    }
    
    /**
     * Normal metin ekle
     * charsetEncoding ayarına göre encoding kullanır
     * cancelTurkishChars aktifse Türkçe karakterleri İngilizce karşılıklarına çevirir
     */
    fun text(text: String): EscPosCommands {
        // Türkçe karakter iptal et (eğer ayar aktifse)
        val processedText = if (cancelTurkishChars) {
            TurkishCharacterEncoder.cancelTurkishCharacters(text)
        } else {
            text
        }
        
        val encoded = when (charsetEncoding) {
            "PC857_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC857_ISO88599" -> {
                try {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC857_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            // PC857_61 kombinasyonları (ESC t 61) - Bazı yazıcılar için
            "PC857_61_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC857_61_ISO88599" -> {
                try {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC857_61_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC857_61_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            "PC857_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            "PC850_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            "PC850_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC850_ISO88599" -> {
                try {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC850_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC858_CP858" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP858"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC858_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC858_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC437_CP437" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP437"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charsets.UTF_8)
                }
            }
            "PC437_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "NONE_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "NONE_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            "NONE_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "NONE_ISO88599" -> {
                try {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "NONE_UTF8" -> {
                processedText.toByteArray(Charsets.UTF_8)
            }
            "PC3846_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC3846_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            "PC3846_Windows1254" -> {
                try {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC3846_ISO88599" -> {
                try {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            // PC3846_CP3846 eski encoding - artık NONE_CP857 kullanılıyor (karakter seti komutu göndermeden)
            "PC3846_CP3846" -> {
                // PC3846 komutu Çince karakterlere neden oluyor, bu yüzden karakter seti komutu göndermeden sadece CP857 encoding kullanıyoruz
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    try {
                        processedText.toByteArray(Charset.forName("ISO-8859-9"))
                    } catch (e2: Exception) {
                        processedText.toByteArray(Charsets.UTF_8)
                    }
                }
            }
            // PC857 ek kombinasyonları
            "PC857_CP852" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP852"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC857_CP853" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP853"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            // PC857_61 ek kombinasyonları
            "PC857_61_CP852" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP852"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC857_61_CP853" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP853"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            // PC850 ek kombinasyonları
            "PC850_CP852" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP852"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            "PC850_CP853" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP853"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP850"))
                }
            }
            // PC852 kombinasyonları
            "PC852_CP852" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP852"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC852_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC852_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            // PC853 kombinasyonları
            "PC853_CP853" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP853"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "PC853_CP857" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP857"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("ISO-8859-9"))
                }
            }
            "PC853_CP850" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP850"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("Windows-1254"))
                }
            }
            // NONE_ prefix'li encoding'ler
            "NONE_CP852" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP852"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            "NONE_CP853" -> {
                try {
                    processedText.toByteArray(Charset.forName("CP853"))
                } catch (e: Exception) {
                    processedText.toByteArray(Charset.forName("CP857"))
                }
            }
            else -> {
                // NONE_ prefix'i ile başlayan encoding'ler için direkt encoding kullan
                if (charsetEncoding.startsWith("NONE_")) {
                    val encodingName = charsetEncoding.substring(5) // "NONE_" kısmını çıkar
                    // ISO-8859-X formatını düzelt (ISO88599 -> ISO-8859-9)
                    val normalizedName = when {
                        encodingName.startsWith("ISO8859") -> {
                            val num = encodingName.substring(7)
                            "ISO-8859-$num"
                        }
                        encodingName.startsWith("IBM") -> {
                            // IBM kodları için özel işlem
                            when (encodingName) {
                                "IBM437" -> "CP437"
                                "IBM850" -> "CP850"
                                "IBM857" -> "CP857"
                                "IBM860" -> "CP860"
                                "IBM861" -> "CP861"
                                "IBM862" -> "CP862"
                                "IBM863" -> "CP863"
                                "IBM864" -> "CP864"
                                "IBM865" -> "CP865"
                                "IBM866" -> "CP866"
                                "IBM869" -> "CP869"
                                "IBM00858" -> "CP858"
                                else -> encodingName
                            }
                        }
                        else -> encodingName
                    }
                    try {
                        processedText.toByteArray(Charset.forName(normalizedName))
                    } catch (e: Exception) {
                        // Encoding bulunamazsa UTF-8 kullan
                        try {
                            processedText.toByteArray(Charsets.UTF_8)
                        } catch (e2: Exception) {
                            processedText.toByteArray(Charsets.UTF_8)
                        }
                    }
                } else {
                    // Varsayılan
                    try {
                        processedText.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        processedText.toByteArray(Charsets.UTF_8)
                    }
                }
            }
        }
        buffer.write(encoded)
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
        text(text)  // Aynı encoding kullan
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
        text(text)  // Aynı encoding kullan
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
     * Kağıdı kes - Kesme türünü belirtebilirsiniz
     * @param cutCommand Kesme komutu: FULL, PARTIAL, ESC_i, ESC_m, GS_V_65, GS_V_66
     */
    fun cutPaper(cutCommand: String = "FULL"): EscPosCommands {
        val command = when (cutCommand.uppercase()) {
            "FULL" -> CUT_PAPER_FULL
            "PARTIAL" -> CUT_PAPER_PARTIAL
            "ESC_I" -> CUT_PAPER_ESC_i
            "ESC_M" -> CUT_PAPER_ESC_m
            "GS_V_65" -> CUT_PAPER_GS_V_65
            "GS_V_66" -> CUT_PAPER_GS_V_66
            else -> CUT_PAPER_FULL
        }
        buffer.write(command)
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
fun buildEscPosCommand(charsetEncoding: String = "PC857_CP857", cancelTurkishChars: Boolean = false, block: EscPosCommands.() -> Unit): ByteArray {
    return EscPosCommands().apply {
        this.charsetEncoding = charsetEncoding
        this.cancelTurkishChars = cancelTurkishChars
        block()
    }.build()
}
