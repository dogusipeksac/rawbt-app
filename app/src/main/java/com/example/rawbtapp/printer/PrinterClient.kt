package com.example.rawbtapp.printer

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
        var socket: Socket? = null
        var outputStream: OutputStream? = null
        
        try {
            // IP adresi validasyonu
            if (!isValidIpAddress(ipAddress)) {
                return@withContext PrintResult.Error("Geçersiz IP adresi formatı")
            }
            
            // Port validasyonu
            if (port !in 1..65535) {
                return@withContext PrintResult.Error("Geçersiz port numarası (1-65535 arası olmalı)")
            }
            
            // Veri kontrolü
            if (data.isEmpty()) {
                return@withContext PrintResult.Error("Gönderilecek veri boş olamaz")
            }
            
            // Socket oluştur ve bağlan
            socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
            socket.soTimeout = READ_TIMEOUT
            
            // Output stream al
            outputStream = socket.getOutputStream()
            
            // Veriyi gönder
            outputStream.write(data)
            outputStream.flush()
            
            // Başarılı
            PrintResult.Success("Yazdırma başarılı")
            
        } catch (e: SocketTimeoutException) {
            PrintResult.Error("Bağlantı zaman aşımına uğradı. Yazıcının açık ve aynı ağda olduğundan emin olun.")
        } catch (e: UnknownHostException) {
            PrintResult.Error("Yazıcı bulunamadı. IP adresini kontrol edin.")
        } catch (e: java.net.ConnectException) {
            PrintResult.Error("Yazıcıya bağlanılamadı. IP ve port numarasını kontrol edin.")
        } catch (e: java.io.IOException) {
            PrintResult.Error("Veri gönderme hatası: ${e.message}")
        } catch (e: Exception) {
            PrintResult.Error("Beklenmeyen hata: ${e.message}")
        } finally {
            // Kaynakları temizle
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Kapatma hatalarını yoksay
            }
        }
    }
    
    /**
     * Test yazdırma - basit bir test sayfası yazdırır
     */
    suspend fun printTest(
        ipAddress: String,
        port: Int = DEFAULT_PORT
    ): PrintResult {
        val testData = buildEscPosCommand {
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
            feedPaper(3)
            cutPaper()
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
