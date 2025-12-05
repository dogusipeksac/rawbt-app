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
     * PC857 (DOS Turkish) kullanır çünkü ESC/POS printer'lar PC857'yi kullanır
     */
    fun encodeForPrinter(text: String): ByteArray {
        return try {
            Log.d(TAG, "Encoding text for printer: ${text.take(50)}...")

            // Türkçe karakterleri kontrol et
            val turkishChars = "ÇçĞğİıÖöŞşÜü"
            val hasTurkish = text.any { it in turkishChars }

            if (hasTurkish) {
                Log.d(TAG, "Turkish characters detected, using CP857")
            }

            // CP857 (Turkish) encoding - PC857 karakter seti ile uyumlu
            // getEscPosInitCommands() fonksiyonunda CHARSET_PC857 seçiliyor (ESC t 13)
            val encoded = text.toByteArray(Charset.forName("CP857"))
            Log.d(TAG, "✓ Successfully encoded ${encoded.size} bytes with CP857")
            encoded
        } catch (e: Exception) {
            Log.e(TAG, "CP857 encoding failed, trying ISO-8859-9", e)
            try {
                // Fallback: ISO-8859-9 (Latin-5, Turkish)
                val encoded = text.toByteArray(Charset.forName("ISO-8859-9"))
                Log.d(TAG, "✓ Successfully encoded ${encoded.size} bytes with ISO-8859-9")
                encoded
            } catch (e2: Exception) {
                Log.e(TAG, "ISO-8859-9 encoding failed, trying CP850", e2)
                try {
                    // Fallback: CP850 (Multilingual)
                    val encoded = text.toByteArray(Charset.forName("CP850"))
                    Log.d(TAG, "✓ Successfully encoded ${encoded.size} bytes with CP850")
                    encoded
                } catch (e3: Exception) {
                    Log.e(TAG, "CP850 encoding failed, trying Windows-1254", e3)
                    try {
                        // Fallback: Windows-1254 (Turkish)
                        val encoded = text.toByteArray(Charset.forName("Windows-1254"))
                        Log.d(TAG, "✓ Successfully encoded ${encoded.size} bytes with Windows-1254")
                        encoded
                    } catch (e4: Exception) {
                        Log.e(TAG, "All encodings failed, using UTF-8", e4)
                        text.toByteArray(Charsets.UTF_8)
                    }
                }
            }
        }
    }
    
    /**
     * HTML içeriğinden metni çıkar ve Türkçe karakterleri koru
     */
    fun extractTextFromHtml(html: String): String {
        var text = html
        
        // ÖNCE: Tüm script, style ve gereksiz içerikleri kaldır
        text = text.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<noscript[^>]*>.*?</noscript>", RegexOption.DOT_MATCHES_ALL), "")
        
        // HTML header ve meta tag'lerini kaldır
        text = text.replace(Regex("<head[^>]*>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<html[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("</html>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<body[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("</body>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<link[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<title[^>]*>.*?</title>", RegexOption.IGNORE_CASE), "")
        
        // Gereksiz UI elementlerini kaldır
        text = text.replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<svg[^>]*>.*?</svg>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<button[^>]*>.*?</button>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<input[^>]*>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<form[^>]*>.*?</form>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<select[^>]*>.*?</select>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<textarea[^>]*>.*?</textarea>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<nav[^>]*>.*?</nav>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<header[^>]*>.*?</header>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<footer[^>]*>.*?</footer>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<aside[^>]*>.*?</aside>", RegexOption.DOT_MATCHES_ALL), "")
        text = text.replace(Regex("<a[^>]*>.*?</a>", RegexOption.DOT_MATCHES_ALL), "") // Link'lerin içeriğini al ama tag'leri kaldır
        text = text.replace(Regex("<span[^>]*>", RegexOption.IGNORE_CASE), "") // Span açılış tag'leri
        text = text.replace(Regex("</span>", RegexOption.IGNORE_CASE), "") // Span kapanış tag'leri
        
        // JavaScript kodlarını temizle (inline event handler'lar)
        text = text.replace(Regex("on\\w+\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
        
        // CSS class ve style attribute'larını kaldır (sadece tag'lerden)
        text = text.replace(Regex("class\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("style\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("id\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("data-[^=]*\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")
        
        // Satır sonları için tag'ler
        text = text.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</p>", "\n")
        text = text.replace("</div>", "\n")
        text = text.replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</tr>", "\n")
        text = text.replace("</li>", "\n")
        text = text.replace("</td>", " ")
        text = text.replace("</th>", " ")
        text = text.replace("</table>", "\n")
        text = text.replace("</ul>", "\n")
        text = text.replace("</ol>", "\n")
        
        // Tüm kalan HTML tag'lerini kaldır
        text = text.replace(Regex("<[^>]*>"), "")
        
        // HTML entity'leri decode et
        text = decodeHtmlEntities(text)
        
        // Gereksiz karakterleri temizle
        text = text.replace(Regex("[\\u200B-\\u200D\\uFEFF]", RegexOption.IGNORE_CASE), "") // Zero-width characters
        text = text.replace(Regex("\\u00A0", RegexOption.IGNORE_CASE), " ") // Non-breaking space -> normal space
        
        // Fazla boşlukları temizle
        text = text.replace(Regex("[ \\t]+"), " ") // Birden fazla boşluk -> tek boşluk
        text = text.replace(Regex("\n[ \\t]+"), "\n") // Satır başındaki boşluklar
        text = text.replace(Regex("[ \\t]+\n"), "\n") // Satır sonundaki boşluklar
        text = text.replace(Regex("\n{3,}"), "\n\n") // 3+ boş satır -> 2 boş satır
        
        // Boş satırları temizle (sadece boşluk içeren satırlar)
        text = text.replace(Regex("^[ \\t]+$", RegexOption.MULTILINE), "")
        
        // Başta ve sonda gereksiz boşlukları temizle
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
     * Türkçe karakterleri İngilizce karşılıklarına çevir
     * İ -> I, ı -> i, Ö -> O, ö -> o, Ü -> U, ü -> u, Ş -> S, ş -> s, Ğ -> G, ğ -> g
     */
    fun cancelTurkishCharacters(text: String): String {
        return text
            .replace("İ", "I")
            .replace("ı", "i")
            .replace("Ö", "O")
            .replace("ö", "o")
            .replace("Ü", "U")
            .replace("ü", "u")
            .replace("Ş", "S")
            .replace("ş", "s")
            .replace("Ğ", "G")
            .replace("ğ", "g")
            .replace("Ç", "C")
            .replace("ç", "c")
    }
    
}
