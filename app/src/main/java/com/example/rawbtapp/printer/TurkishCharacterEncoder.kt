package com.example.rawbtapp.printer

import android.util.Log
import java.nio.charset.Charset

/**
 * Türkçe karakter encoding yöneticisi
 * Farklı encoding yöntemlerini dener ve en uygun olanını kullanır
 */
object TurkishCharacterEncoder {
    
    private const val TAG = "TurkishCharEncoder"
    
    /**
     * Metni termal yazıcı için uygun byte array'e çevir
     * Türkçe karakterleri koruyarak encoding yapar
     */
    fun encodeForPrinter(text: String): ByteArray {
        return try {
            // Önce Windows-1254 ile dene (Türkçe için en uygun)
            text.toByteArray(Charset.forName("Windows-1254"))
        } catch (e: Exception) {
            Log.w(TAG, "Windows-1254 encoding failed, trying ISO-8859-9", e)
            try {
                // ISO-8859-9 (Latin-5, Turkish) dene
                text.toByteArray(Charset.forName("ISO-8859-9"))
            } catch (e2: Exception) {
                Log.w(TAG, "ISO-8859-9 encoding failed, using UTF-8", e2)
                // Son çare UTF-8
                text.toByteArray(Charsets.UTF_8)
            }
        }
    }
    
    /**
     * HTML içeriğinden metni çıkar ve Türkçe karakterleri koru
     */
    fun extractTextFromHtml(html: String): String {
        var text = html
        
        // Script ve style tag'lerini kaldır
        text = text.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
        
        // HTML header tag'lerini kaldır
        text = text.replace(Regex("<head[^>]*>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<html[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("</html>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<body[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("</body>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<link[^>]*>", RegexOption.IGNORE_CASE), "")
        
        // Satır sonları için tag'ler
        text = text.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</p>", "\n")
        text = text.replace("</div>", "\n")
        text = text.replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</tr>", "\n")
        text = text.replace("</li>", "\n")
        text = text.replace("</td>", " ")
        text = text.replace("</th>", " ")
        
        // Tüm kalan HTML tag'lerini kaldır
        text = text.replace(Regex("<[^>]*>"), "")
        
        // HTML entity'leri decode et
        text = decodeHtmlEntities(text)
        
        // Fazla boşlukları temizle
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\n[ \\t]+"), "\n")
        text = text.replace(Regex("[ \\t]+\n"), "\n")
        text = text.replace(Regex("\n{3,}"), "\n\n")
        
        return text.trim()
    }
    
    /**
     * HTML entity'lerini decode et (Türkçe karakterler dahil)
     */
    fun decodeHtmlEntities(text: String): String {
        var result = text
        
        // Standart HTML entity'leri
        result = result
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
        
        // Türkçe karakterler - Decimal format (&#xxx;)
        result = result
            .replace("&#199;", "Ç")
            .replace("&#231;", "ç")
            .replace("&#286;", "Ğ")
            .replace("&#287;", "ğ")
            .replace("&#304;", "İ")
            .replace("&#305;", "ı")
            .replace("&#214;", "Ö")
            .replace("&#246;", "ö")
            .replace("&#350;", "Ş")
            .replace("&#351;", "ş")
            .replace("&#220;", "Ü")
            .replace("&#252;", "ü")
        
        // Türkçe karakterler - Hex format (&#xXX;)
        result = result
            .replace("&#xC7;", "Ç")
            .replace("&#xe7;", "ç")
            .replace("&#x11E;", "Ğ")
            .replace("&#x11F;", "ğ")
            .replace("&#x130;", "İ")
            .replace("&#x131;", "ı")
            .replace("&#xD6;", "Ö")
            .replace("&#xF6;", "ö")
            .replace("&#x15E;", "Ş")
            .replace("&#x15F;", "ş")
            .replace("&#xDC;", "Ü")
            .replace("&#xFC;", "ü")
        
        // Named entity format
        result = result
            .replace("&Ccedil;", "Ç")
            .replace("&ccedil;", "ç")
            .replace("&Ouml;", "Ö")
            .replace("&ouml;", "ö")
            .replace("&Uuml;", "Ü")
            .replace("&uuml;", "ü")
        
        return result
    }
    
    /**
     * PDF'den çıkarılan metindeki Unicode escape sequence'leri decode et
     */
    fun decodePdfUnicode(text: String): String {
        var result = text
        
        // UTF-8 octal escape sequences (Türkçe karakterler)
        result = result
            .replace("\\303\\207", "Ç")  // Ç
            .replace("\\303\\247", "ç")  // ç
            .replace("\\304\\236", "Ğ")  // Ğ
            .replace("\\304\\237", "ğ")  // ğ
            .replace("\\304\\260", "İ")  // İ
            .replace("\\304\\261", "ı")  // ı
            .replace("\\303\\226", "Ö")  // Ö
            .replace("\\303\\266", "ö")  // ö
            .replace("\\305\\236", "Ş")  // Ş
            .replace("\\305\\237", "ş")  // ş
            .replace("\\303\\234", "Ü")  // Ü
            .replace("\\303\\274", "ü")  // ü
        
        // Unicode escape sequences (\uXXXX format)
        result = result
            .replace("\\u00C7", "Ç")
            .replace("\\u00E7", "ç")
            .replace("\\u011E", "Ğ")
            .replace("\\u011F", "ğ")
            .replace("\\u0130", "İ")
            .replace("\\u0131", "ı")
            .replace("\\u00D6", "Ö")
            .replace("\\u00F6", "ö")
            .replace("\\u015E", "Ş")
            .replace("\\u015F", "ş")
            .replace("\\u00DC", "Ü")
            .replace("\\u00FC", "ü")
        
        return result
    }
    
    /**
     * ESC/POS başlatma komutlarını oluştur (Türkçe karakter desteği ile)
     */
    fun getEscPosInitCommands(): ByteArray {
        return byteArrayOf(
            0x1B, 0x40,                    // ESC @ - Initialize printer
            0x1B, 0x74, 0x0D               // ESC t 13 - Select character code table (PC857 - Turkish)
        )
    }
    
    /**
     * Metni test et ve encoding sorunlarını logla
     */
    fun testEncoding(text: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "ENCODING TEST")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Original text: $text")
        Log.d(TAG, "Text length: ${text.length}")
        
        // Türkçe karakterleri say
        val turkishChars = "ÇçĞğİıÖöŞşÜü"
        val foundChars = text.filter { it in turkishChars }
        Log.d(TAG, "Turkish characters found: $foundChars")
        
        // Farklı encoding'leri test et
        try {
            val windows1254 = text.toByteArray(Charset.forName("Windows-1254"))
            Log.d(TAG, "Windows-1254 bytes: ${windows1254.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Windows-1254 encoding failed", e)
        }
        
        try {
            val iso88599 = text.toByteArray(Charset.forName("ISO-8859-9"))
            Log.d(TAG, "ISO-8859-9 bytes: ${iso88599.size}")
        } catch (e: Exception) {
            Log.e(TAG, "ISO-8859-9 encoding failed", e)
        }
        
        val utf8 = text.toByteArray(Charsets.UTF_8)
        Log.d(TAG, "UTF-8 bytes: ${utf8.size}")
        
        Log.d(TAG, "========================================")
    }
}
