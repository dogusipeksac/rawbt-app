package com.example.rawbtapp.printer

/**
 * Yazdırma için sabit değerler ve özelleştirilebilir ayarlar
 * Logo, footer metni ve diğer görsel öğeleri buradan yönetebilirsiniz
 */
object PrintConstants {
    
    // ============================================
    // LOGO AYARLARI
    // ============================================
    
    /**
     * Fiş başlığında gösterilecek ASCII logo
     * Değiştirmek için bu metni düzenleyin
     */
    const val RECEIPT_LOGO = """
         ___
        /   \
       |  7  |
        \___/
         DAYS
    """
    
    /**
     * Logo gösterilsin mi?
     * NOT: Generic mode'da logo kullanılmaz, sadece web içeriği basılır
     */
    const val SHOW_LOGO = false
    
    /**
     * Logo'dan sonra kaç satır boşluk bırakılsın
     */
    const val LOGO_SPACING = 1
    
    /**
     * Web içeriğinden önce kaç satır boşluk bırakılsın
     */
    const val WEB_CONTENT_TOP_SPACING = 2
    
    /**
     * Web içeriğinden sonra kaç satır boşluk bırakılsın
     */
    const val WEB_CONTENT_BOTTOM_SPACING = 3
    
    // ============================================
    // FOOTER (ALT BİLGİ) AYARLARI
    // ============================================
    
    /**
     * Fiş altında gösterilecek teşekkür mesajı
     */
    const val FOOTER_THANK_YOU = "Bizi tercih ettiğiniz için\nteşekkür ederiz!"
    
    /**
     * Fiş altında gösterilecek web sitesi
     */
    const val FOOTER_WEBSITE = "www.7dayshavacilik.com"
    
    /**
     * Fiş altında gösterilecek telefon numarası
     */
    const val FOOTER_PHONE = "Tel: 0212 123 45 67"
    
    /**
     * Fiş altında gösterilecek adres
     */
    const val FOOTER_ADDRESS = "İstanbul, Türkiye"
    
    /**
     * Footer gösterilsin mi?
     * NOT: Generic mode'da footer kullanılmaz, sadece web içeriği basılır
     */
    const val SHOW_FOOTER = false
    
    /**
     * Footer'dan önce kaç satır boşluk bırakılsın
     */
    const val FOOTER_TOP_SPACING = 2
    
    /**
     * Footer'dan sonra kaç satır boşluk bırakılsın (kağıt besleme)
     */
    const val FOOTER_BOTTOM_SPACING = 4
    
    // ============================================
    // GENEL YAZDIRMA AYARLARI
    // ============================================
    
    /**
     * Fiş genişliği (karakter sayısı)
     * Standart 80mm termal yazıcı için 32-48 karakter
     */
    const val RECEIPT_WIDTH = 32
    
    /**
     * Yatay çizgi karakteri
     */
    const val HORIZONTAL_LINE_CHAR = "-"
    
    /**
     * Kalın yatay çizgi karakteri
     */
    const val HORIZONTAL_LINE_BOLD_CHAR = "="
    
    /**
     * Tarih formatı
     */
    const val DATE_FORMAT = "dd/MM/yyyy HH:mm:ss"
    
    // ============================================
    // TÜRKÇE KARAKTER AYARLARI
    // ============================================
    
    /**
     * Türkçe karakter encoding
     * Windows-1254: Türkçe karakterler için en uygun
     */
    const val TURKISH_CHARSET = "Windows-1254"
    
    /**
     * ESC/POS karakter seti
     * 0x0D = PC857 (Turkish)
     */
    const val ESCPOS_CHARSET_TURKISH: Byte = 0x0D
    
    /**
     * Türkçe karakter mapping (yedek)
     * Bazı yazıcılar için alternatif karakterler
     */
    val TURKISH_CHAR_MAP = mapOf(
        'Ç' to 'C',
        'ç' to 'c',
        'Ğ' to 'G',
        'ğ' to 'g',
        'İ' to 'I',
        'ı' to 'i',
        'Ö' to 'O',
        'ö' to 'o',
        'Ş' to 'S',
        'ş' to 's',
        'Ü' to 'U',
        'ü' to 'u'
    )
    
    // ============================================
    // HTML YAZDIRMA AYARLARI
    // ============================================
    
    /**
     * HTML yazdırma için CSS stilleri
     */
    const val HTML_PRINT_CSS = """
        @media print {
            body {
                margin: 0;
                padding: 10mm;
                font-family: 'Courier New', monospace;
                font-size: 12pt;
            }
            @page {
                margin: 0;
                size: 80mm auto;
            }
        }
        body {
            font-family: 'Courier New', monospace;
            font-size: 12pt;
            line-height: 1.4;
            max-width: 80mm;
            margin: 0 auto;
            padding: 10px;
        }
        .logo {
            text-align: center;
            font-weight: bold;
            margin-bottom: 20px;
            white-space: pre;
            font-family: monospace;
        }
        .footer {
            text-align: center;
            margin-top: 20px;
            padding-top: 10px;
            border-top: 2px dashed #000;
            font-size: 10pt;
        }
        h1, h2, h3 {
            margin: 10px 0;
            text-align: center;
        }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        hr {
            border: none;
            border-top: 1px dashed #000;
            margin: 10px 0;
        }
    """
    
    // ============================================
    // YARDIMCI FONKSİYONLAR
    // ============================================
    
    /**
     * Metni Türkçe karaktersiz versiyona çevir (yedek)
     */
    fun toAsciiSafe(text: String): String {
        var result = text
        TURKISH_CHAR_MAP.forEach { (turkish, ascii) ->
            result = result.replace(turkish, ascii)
        }
        return result
    }
    
    /**
     * Logo'yu formatlanmış string olarak döndür
     */
    fun getFormattedLogo(): String {
        return if (SHOW_LOGO) {
            RECEIPT_LOGO.trimIndent() + "\n".repeat(LOGO_SPACING)
        } else {
            ""
        }
    }
    
    /**
     * Footer'ı formatlanmış string olarak döndür
     */
    fun getFormattedFooter(): String {
        if (!SHOW_FOOTER) return ""
        
        val footer = StringBuilder()
        footer.append("\n".repeat(FOOTER_TOP_SPACING))
        footer.append(HORIZONTAL_LINE_BOLD_CHAR.repeat(RECEIPT_WIDTH)).append("\n")
        footer.append(centerText(FOOTER_THANK_YOU)).append("\n")
        
        if (FOOTER_WEBSITE.isNotEmpty()) {
            footer.append(centerText(FOOTER_WEBSITE)).append("\n")
        }
        if (FOOTER_PHONE.isNotEmpty()) {
            footer.append(centerText(FOOTER_PHONE)).append("\n")
        }
        if (FOOTER_ADDRESS.isNotEmpty()) {
            footer.append(centerText(FOOTER_ADDRESS)).append("\n")
        }
        
        footer.append(HORIZONTAL_LINE_BOLD_CHAR.repeat(RECEIPT_WIDTH)).append("\n")
        footer.append("\n".repeat(FOOTER_BOTTOM_SPACING))
        
        return footer.toString()
    }
    
    /**
     * Metni ortala
     */
    fun centerText(text: String, width: Int = RECEIPT_WIDTH): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            val padding = (width - line.length) / 2
            if (padding > 0) {
                " ".repeat(padding) + line
            } else {
                line
            }
        }
    }
    
    /**
     * İki sütunlu metin oluştur
     */
    fun twoColumnText(left: String, right: String, width: Int = RECEIPT_WIDTH): String {
        val spaces = width - left.length - right.length
        return if (spaces > 0) {
            left + " ".repeat(spaces) + right
        } else {
            "$left $right"
        }
    }
    
    /**
     * Yatay çizgi oluştur
     */
    fun horizontalLine(char: String = HORIZONTAL_LINE_CHAR, width: Int = RECEIPT_WIDTH): String {
        return char.repeat(width)
    }
}
