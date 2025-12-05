package com.example.rawbtapp.printer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * TCP Socket üzerinden termal yazıcıya bağlantı sağlayan sınıf
 * ESC/POS komutlarını RAW TCP soketi üzerinden gönderir
 */
class PrinterClient {
    
    companion object {
        private const val TAG = "PrinterClient"
        private const val CONNECTION_TIMEOUT = 5000 // 5 saniye
        private const val READ_TIMEOUT = 3000 // 3 saniye
        private const val DEFAULT_PORT = 9100
    }
    
    /**
     * Yazıcıya bağlan ve veri gönder
     * @param ipAddress Yazıcının IP adresi
     * @param port Yazıcının port numarası (varsayılan 9100)
     * @param data Gönderilecek ESC/POS komutları (byte array)
     * @return PrintResult başarı veya hata durumu
     */
    suspend fun print(
        ipAddress: String,
        port: Int = DEFAULT_PORT,
        data: ByteArray
    ): PrintResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "print - Yazıcıya Bağlanılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "IP Address: $ipAddress")
        Log.d(TAG, "Port: $port")
        Log.d(TAG, "Data size: ${data.size} bytes")
        
        var socket: Socket? = null
        var outputStream: OutputStream? = null
        
        try {
            // IP adresi validasyonu
            Log.d(TAG, "Validating IP address...")
            if (!isValidIpAddress(ipAddress)) {
                Log.e(TAG, "✗ Geçersiz IP adresi formatı: $ipAddress")
                return@withContext PrintResult.Error("Geçersiz IP adresi formatı")
            }
            Log.d(TAG, "✓ IP address valid")
            
            // Port validasyonu
            Log.d(TAG, "Validating port...")
            if (port !in 1..65535) {
                Log.e(TAG, "✗ Geçersiz port: $port")
                return@withContext PrintResult.Error("Geçersiz port numarası (1-65535 arası olmalı)")
            }
            Log.d(TAG, "✓ Port valid")
            
            // Veri kontrolü
            if (data.isEmpty()) {
                Log.e(TAG, "✗ Data is empty")
                return@withContext PrintResult.Error("Gönderilecek veri boş olamaz")
            }
            Log.d(TAG, "✓ Data validation passed")
            
            // Socket oluştur ve bağlan
            Log.d(TAG, "Creating socket...")
            socket = Socket()
            Log.d(TAG, "Connecting to $ipAddress:$port (timeout: ${CONNECTION_TIMEOUT}ms)...")
            socket.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
            socket.soTimeout = READ_TIMEOUT
            Log.d(TAG, "✓ Socket connected successfully")
            
            // Output stream al
            Log.d(TAG, "Getting output stream...")
            outputStream = socket.getOutputStream()
            Log.d(TAG, "✓ Output stream obtained")
            
            // Veriyi gönder
            Log.d(TAG, "Sending ${data.size} bytes to printer...")
            outputStream.write(data)
            outputStream.flush()
            Log.d(TAG, "✓ Data sent successfully")
            
            // Başarılı
            Log.d(TAG, "✓ Print operation completed successfully")
            Log.d(TAG, "========================================")
            PrintResult.Success("Yazdırma başarılı")
            
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "✗ Socket timeout exception", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            PrintResult.Error("Bağlantı zaman aşımına uğradı. Yazıcının açık ve aynı ağda olduğundan emin olun.")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "✗ Unknown host exception", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            PrintResult.Error("Yazıcı bulunamadı. IP adresini kontrol edin.")
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "✗ Connection exception", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            PrintResult.Error("Yazıcıya bağlanılamadı. IP ve port numarasını kontrol edin.")
        } catch (e: java.io.IOException) {
            Log.e(TAG, "✗ IO exception", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            PrintResult.Error("Veri gönderme hatası: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Unexpected exception", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            PrintResult.Error("Beklenmeyen hata: ${e.message}")
        } finally {
            // Kaynakları temizle
            Log.d(TAG, "Cleaning up resources...")
            try {
                outputStream?.close()
                socket?.close()
                Log.d(TAG, "✓ Resources cleaned up")
            } catch (e: Exception) {
                Log.w(TAG, "Error closing resources: ${e.message}")
            }
        }
    }
    
    /**
     * Test yazdırma - basit bir test sayfası yazdırır
     */
    suspend fun printTest(
        ipAddress: String,
        port: Int = DEFAULT_PORT,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printTest - Test Sayfası Hazırlanıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "IP: $ipAddress, Port: $port")
        Log.d(TAG, "Cut paper: $cutPaper, Feed lines: $cutFeedLines")
        Log.d(TAG, "Charset Encoding: $charsetEncoding")
        Log.d(TAG, "Cancel Turkish Chars: $cancelTurkishChars")
        
        val testData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            alignCenter()
            doubleTextLine("TEST YAZDIR")
            newLine()
            alignLeft()
            textLine("Yazıcı Bağlantı Testi")
            horizontalLine()
            textLine("IP Adresi: $ipAddress")
            textLine("Port: $port")
            horizontalLine()
            textLine("Tarih: ${getCurrentDateTime()}")
            newLine(2)
            alignCenter()
            textLine("Test Başarılı!")
            if (cutPaper) {
                feedPaper(cutFeedLines)
                cutPaper()
            } else {
                feedPaper(3)
            }
        }
        
        return print(ipAddress, port, testData)
    }
    
    /**
     * IP adresi validasyonu
     */
    private fun isValidIpAddress(ip: String): Boolean {
        val ipPattern = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        return ipPattern.matches(ip)
    }
    
    /**
     * Şu anki tarih ve saati al
     */
    private fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}

/**
 * Yazdırma sonucu için sealed class
 */
sealed class PrintResult {
    data class Success(val message: String) : PrintResult()
    data class Error(val message: String) : PrintResult()
}
