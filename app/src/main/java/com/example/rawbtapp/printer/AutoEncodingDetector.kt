package com.example.rawbtapp.printer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Otomatik encoding tespiti
 * Yazıcıya ilk bağlantıda en uygun encoding'i bulur ve kaydeder
 */
object AutoEncodingDetector {
    
    private const val TAG = "AutoEncodingDetector"
    private const val TIMEOUT = 3000 // 3 saniye
    
    /**
     * Yazıcı için en iyi encoding'i otomatik tespit et
     * Türkçe karakterleri test ederek çalışan ilk encoding'i döndürür
     */
    suspend fun detectBestEncoding(
        ipAddress: String,
        port: Int
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Auto-detecting best encoding for $ipAddress:$port")
        Log.d(TAG, "========================================")
        
        // Öncelikli encoding listesi - en yaygın çalışanlar
        val priorityEncodings = listOf(
            "PC857_CP857",      // En yaygın - Standart Turkish
            "NONE_CP857",       // Karakter seti komutu olmadan
            "PC3846_CP3846",    // Alternatif güvenli
            "PC857_61_CP857",   // Model 1 için
            "PC857_ISO88599",   // ISO-8859-9
            "PC857_Windows1254" // Windows-1254
        )
        
        // Her encoding'i test et
        for (encoding in priorityEncodings) {
            try {
                Log.d(TAG, "Testing encoding: $encoding")
                
                if (testEncoding(ipAddress, port, encoding)) {
                    Log.d(TAG, "✓ SUCCESS! Best encoding found: $encoding")
                    Log.d(TAG, "========================================")
                    return@withContext encoding
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error testing encoding $encoding: ${e.message}")
            }
        }
        
        // Hiçbiri çalışmazsa varsayılan
        Log.w(TAG, "No encoding worked, using default: PC857_CP857")
        Log.d(TAG, "========================================")
        return@withContext "PC857_CP857"
    }
    
    /**
     * Belirli bir encoding'i test et
     * Türkçe karakterler içeren kısa bir test yazdırır
     */
    private suspend fun testEncoding(
        ipAddress: String,
        port: Int,
        encoding: String
    ): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        var outputStream: OutputStream? = null
        
        try {
            // Socket bağlantısı
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), TIMEOUT)
            outputStream = socket.getOutputStream()
            
            // Test komutları oluştur
            val commands = EscPosCommands().apply {
                charsetEncoding = encoding
                cancelTurkishChars = false
            }
            
            // Minimal test - sadece Türkçe karakterler
            commands.initialize()
            commands.text("ÇĞİÖŞÜ") // Türkçe karakterler
            commands.newLine(1)
            
            // Gönder
            outputStream.write(commands.build())
            outputStream.flush()
            
            // Başarılı
            Log.d(TAG, "  ✓ Encoding $encoding test successful")
            return@withContext true
            
        } catch (e: Exception) {
            Log.d(TAG, "  ✗ Encoding $encoding test failed: ${e.message}")
            return@withContext false
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Hızlı test - sadece bağlantı kontrolü (encoding testi yok)
     * İlk bağlantıda kullanılır
     */
    suspend fun quickConnectionTest(
        ipAddress: String,
        port: Int
    ): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), TIMEOUT)
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
