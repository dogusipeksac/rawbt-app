package com.example.rawbtapp.printer

/**
 * Karakter seti ve encoding kombinasyonları
 * Tek bir kaynak liste - hem ayar listesi hem full test listesi aynı sırada
 * Priority sistemi ile güvenilirlik seviyesi belirtilir
 */
object CharsetEncodingOptions {
    
    data class CharsetEncodingOption(
        val value: String,
        val displayName: String,
        val description: String,
        val priority: Int = 0  // 3 = En güvenli, 2 = Güvenli, 1 = Çalışıyor, 0 = Test gerekli
    )
    
    /**
     * TEK KAYNAK LİSTE - Hem ayar listesi hem full test listesi buradan türetiliyor
     * Priority: 3 = En güvenli, 2 = Güvenli, 1 = Çalışıyor, 0 = Test gerekli
     */
    private val encodingList = listOf(
        // OTOMATİK TESPİT (Priority: 4)
        "AUTO",               // Otomatik tespit - İlk yazdırmada en uygun encoding'i bulur
        
        // EN GÜVENLİ - Her iki printer için (Priority: 3)
        "PC857_CP857",        // ÇALIŞAN - PC857 + CP857 (Turkish)
        "NONE_CP857",         // ÇALIŞAN
        "PC3846_CP3846",      // ÇALIŞAN
        
        // ÇALIŞAN - Birinci printer için (Priority: 1)
        "PC857_61_CP857",     // Model 1'de çalışıyor
        "PC857_61_CP852",     // Model 1'de çalışıyor
        
        // ALTERNATİF PC857 (Priority: 2)
        "PC857_ISO88599",
        "PC857_Windows1254",
        
        // DİĞER PC857_61 (Priority: 0)
        "PC857_61_ISO88599",
        "PC857_61_Windows1254",
        "PC857_61_CP850",
        "PC857_61_CP853",
        
        // DİĞER ALTERNATİFLER (Priority: 0)
        "PC857_CP850",
        "PC857_CP852",
        "PC857_CP853",
        "PC850_CP857",
        "PC850_Windows1254",
        "PC850_ISO88599",
        "PC850_CP850",
        "PC852_CP852",
        "PC852_CP857",
        "PC852_CP850",
        "NONE_CP852",
        "PC853_CP853",
        "PC853_CP857",
        "PC853_CP850",
        "NONE_CP850",
        "NONE_CP853",
        "NONE_Windows1254",
        "NONE_ISO88599"
    )
    
    /**
     * Ayar listesi (dropdown menü) - encodingList'ten türetiliyor
     */
    val options: List<CharsetEncodingOption> = encodingList.map { value ->
        CharsetEncodingOption(
            value = value,
            displayName = getDisplayName(value),
            description = getDescription(value),
            priority = getPriority(value)
        )
    }
    
    /**
     * Full test listesi - encodingList'ten türetiliyor (aynı sırada)
     */
    val allEncodingsForFullTest: List<String> = encodingList
    
    /**
     * Encoding için display name
     */
    private fun getDisplayName(value: String): String {
        return when (value) {
            "AUTO" -> "🤖 Otomatik Tespit (ÖNERİLEN)"
            "PC857_CP857" -> "PC857 + CP857 (Turkish)"
            "NONE_CP857" -> "CP857 (Karakter seti yok)"
            "PC3846_CP3846" -> "PC3846 + CP857 (Turkish)"
            "PC857_61_CP857" -> "PC857 (61) + CP857"
            "PC857_61_CP852" -> "PC857 (61) + CP852"
            "PC857_ISO88599" -> "PC857 + ISO-8859-9"
            "PC857_Windows1254" -> "PC857 + Windows-1254"
            "PC857_61_ISO88599" -> "PC857 (61) + ISO-8859-9"
            "PC857_61_Windows1254" -> "PC857 (61) + Windows-1254"
            "PC857_61_CP850" -> "PC857 (61) + CP850"
            "PC857_61_CP853" -> "PC857 (61) + CP853"
            "PC857_CP850" -> "PC857 + CP850"
            "PC857_CP852" -> "PC857 + CP852"
            "PC857_CP853" -> "PC857 + CP853"
            "PC850_CP857" -> "PC850 + CP857"
            "PC850_Windows1254" -> "PC850 + Windows-1254"
            "PC850_ISO88599" -> "PC850 + ISO-8859-9"
            "PC850_CP850" -> "PC850 + CP850"
            "PC852_CP852" -> "PC852 + CP852"
            "PC852_CP857" -> "PC852 + CP857"
            "PC852_CP850" -> "PC852 + CP850"
            "NONE_CP852" -> "CP852 (Karakter seti yok)"
            "PC853_CP853" -> "PC853 + CP853"
            "PC853_CP857" -> "PC853 + CP857"
            "PC853_CP850" -> "PC853 + CP850"
            "NONE_CP850" -> "CP850 (Karakter seti yok)"
            "NONE_CP853" -> "CP853 (Karakter seti yok)"
            "NONE_Windows1254" -> "Windows-1254 (Karakter seti yok)"
            "NONE_ISO88599" -> "ISO-8859-9 (Karakter seti yok)"
            else -> value
        }
    }
    
    /**
     * Encoding için description
     */
    private fun getDescription(value: String): String {
        return when (value) {
            "AUTO" -> "İlk yazdırmada yazıcınız için en uygun ayarı otomatik bulur"
            "PC857_CP857" -> "MANUEL: PC857 (ESC t 13) + CP857 encoding"
            "NONE_CP857" -> "GÜVENLİ: Karakter seti komutu yok + CP857"
            "PC3846_CP3846" -> "ALTERNATİF GÜVENLİ: PC3846 + CP857"
            "PC857_61_CP857" -> "Model 1 için ÇALIŞAN: PC857 (ESC t 61) + CP857"
            "PC857_61_CP852" -> "Model 1 için ÇALIŞAN: PC857 (ESC t 61) + CP852"
            else -> when {
                value.contains("_61_") -> "PC857 (ESC t 61) + ${value.substringAfter("_61_")}"
                value.startsWith("PC857_") -> "PC857 (ESC t 13) + ${value.substringAfter("PC857_")}"
                value.startsWith("PC850_") -> "PC850 (ESC t 2) + ${value.substringAfter("PC850_")}"
                value.startsWith("PC852_") -> "PC852 (ESC t 18) + ${value.substringAfter("PC852_")}"
                value.startsWith("PC853_") -> "PC853 (ESC t 8) + ${value.substringAfter("PC853_")}"
                value.startsWith("NONE_") -> "Basit mod: ${value.substringAfter("NONE_")}"
                else -> value
            }
        }
    }
    
    /**
     * Encoding için priority (güvenilirlik seviyesi)
     */
    private fun getPriority(value: String): Int {
        return when (value) {
            "AUTO" -> 4                                          // En öncelikli - Otomatik
            "PC857_CP857", "NONE_CP857", "PC3846_CP3846" -> 3  // En güvenli
            "PC857_ISO88599", "PC857_Windows1254" -> 2          // Güvenli
            "PC857_61_CP857", "PC857_61_CP852" -> 1             // Model 1'de çalışıyor
            else -> 0                                            // Test gerekli
        }
    }
    
    fun getOptionByValue(value: String): CharsetEncodingOption? {
        return options.find { it.value == value } ?: options.first()
    }
    
    fun getDefaultValue(): String {
        return "AUTO" // Otomatik tespit - En iyi seçenek
    }
    
    /**
     * Encoding için fallback charset listesi
     */
    fun getFallbackCharsets(encoding: String): List<String> {
        return when {
            encoding.contains("CP857") -> listOf("CP857", "ISO-8859-9", "Windows-1254", "CP850")
            encoding.contains("CP850") -> listOf("CP850", "Windows-1254", "CP857", "ISO-8859-9")
            encoding.contains("CP852") -> listOf("CP852", "CP857", "CP850", "Windows-1254")
            encoding.contains("CP853") -> listOf("CP853", "CP857", "CP850", "ISO-8859-9")
            encoding.contains("Windows1254") || encoding.contains("CP1254") -> listOf("Windows-1254", "CP857", "ISO-8859-9", "CP850")
            encoding.contains("ISO88599") -> listOf("ISO-8859-9", "CP857", "Windows-1254", "CP850")
            else -> listOf("CP857", "ISO-8859-9", "Windows-1254", "CP850")
        }
    }
    
    /**
     * Encoding için primary charset
     */
    fun getEncodingCharset(encoding: String): String? {
        return when {
            encoding.contains("CP857") -> "CP857"
            encoding.contains("CP850") -> "CP850"
            encoding.contains("CP852") -> "CP852"
            encoding.contains("CP853") -> "CP853"
            encoding.contains("Windows1254") || encoding.contains("CP1254") -> "Windows-1254"
            encoding.contains("ISO88599") -> "ISO-8859-9"
            else -> null
        }
    }
}
