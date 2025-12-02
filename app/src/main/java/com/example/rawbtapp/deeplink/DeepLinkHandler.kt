package com.example.rawbtapp.deeplink

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.rawbtapp.model.ReceiptData
import com.example.rawbtapp.model.ValidationResult
import java.net.URLDecoder

/**
 * Deep link işlemlerini yöneten sınıf
 * Web'den gelen deep link'leri parse eder ve işler
 */
class DeepLinkHandler {
    
    companion object {
        private const val TAG = "DeepLinkHandler"
        private const val SCHEME = "rawbtapp"
        private const val HOST_PRINT = "print"
        private const val PARAM_DATA = "data"
    }
    
    /**
     * Intent'ten deep link verisini parse eder
     */
    fun handleIntent(intent: Intent?): DeepLinkResult {
        Log.d(TAG, "========================================")
        Log.d(TAG, "handleIntent - Deep Link Parse Ediliyor")
        Log.d(TAG, "========================================")
        
        if (intent == null) {
            Log.d(TAG, "Intent null - Deep link yok")
            return DeepLinkResult.NoDeepLink
        }
        
        val action = intent.action
        val data = intent.data
        
        Log.d(TAG, "Intent action: $action")
        Log.d(TAG, "Intent data: $data")
        
        // VIEW action kontrolü
        if (action != Intent.ACTION_VIEW || data == null) {
            Log.d(TAG, "Action VIEW değil veya data null - Deep link yok")
            return DeepLinkResult.NoDeepLink
        }
        
        return parseDeepLink(data)
    }
    
    /**
     * URI'yi parse eder
     */
    private fun parseDeepLink(uri: Uri): DeepLinkResult {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "parseDeepLink - URI Parse Ediliyor")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Parsing URI: $uri")
            Log.d(TAG, "Scheme: ${uri.scheme}, Host: ${uri.host}")
            
            // Scheme ve host kontrolü
            if (uri.scheme != SCHEME) {
                Log.e(TAG, "✗ Geçersiz scheme: ${uri.scheme} (beklenen: $SCHEME)")
                return DeepLinkResult.InvalidScheme(uri.scheme ?: "null")
            }
            Log.d(TAG, "✓ Scheme geçerli: ${uri.scheme}")
            
            if (uri.host != HOST_PRINT) {
                Log.e(TAG, "✗ Geçersiz host: ${uri.host} (beklenen: $HOST_PRINT)")
                return DeepLinkResult.InvalidHost(uri.host ?: "null")
            }
            Log.d(TAG, "✓ Host geçerli: ${uri.host}")
            
            // Data parametresini al
            val encodedData = uri.getQueryParameter(PARAM_DATA)
            if (encodedData.isNullOrBlank()) {
                Log.e(TAG, "✗ Data parametresi eksik")
                return DeepLinkResult.MissingData
            }
            
            Log.d(TAG, "✓ Data parametresi bulundu")
            Log.d(TAG, "Encoded data length: ${encodedData.length}")
            
            // URL decode
            Log.d(TAG, "URL decode ediliyor...")
            val decodedData = URLDecoder.decode(encodedData, "UTF-8")
            Log.d(TAG, "✓ URL decode başarılı")
            Log.d(TAG, "Decoded data length: ${decodedData.length}")
            Log.d(TAG, "Decoded data: $decodedData")
            
            // JSON parse
            Log.d(TAG, "JSON parse ediliyor...")
            val receiptData = ReceiptData.fromJson(decodedData)
            Log.d(TAG, "✓ JSON parse başarılı")
            Log.d(TAG, "Fiş ID: ${receiptData.receiptId}")
            Log.d(TAG, "İşletme: ${receiptData.merchant.name}")
            Log.d(TAG, "Ürün sayısı: ${receiptData.items.size}")
            
            // Validasyon
            Log.d(TAG, "Validasyon yapılıyor...")
            when (val validationResult = receiptData.validate()) {
                is ValidationResult.Success -> {
                    Log.d(TAG, "✓ Validasyon başarılı")
                    Log.d(TAG, "========================================")
                    return DeepLinkResult.PrintReceipt(receiptData)
                }
                is ValidationResult.Error -> {
                    Log.e(TAG, "✗ Validasyon hatası: ${validationResult.message}")
                    Log.d(TAG, "========================================")
                    return DeepLinkResult.ValidationError(validationResult.message)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Deep link parse hatası", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            return DeepLinkResult.ParseError(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Deep link'in geçerli olup olmadığını kontrol eder
     */
    fun isValidDeepLink(intent: Intent?): Boolean {
        Log.d(TAG, "isValidDeepLink - Kontrol ediliyor")
        if (intent?.action != Intent.ACTION_VIEW) {
            Log.d(TAG, "Action VIEW değil")
            return false
        }
        val uri = intent.data ?: run {
            Log.d(TAG, "URI null")
            return false
        }
        val isValid = uri.scheme == SCHEME && uri.host == HOST_PRINT
        Log.d(TAG, "Deep link geçerli: $isValid (scheme: ${uri.scheme}, host: ${uri.host})")
        return isValid
    }
}

/**
 * Deep link parse sonucu
 */
sealed class DeepLinkResult {
    /**
     * Deep link yok
     */
    object NoDeepLink : DeepLinkResult()
    
    /**
     * Fiş yazdırma isteği
     */
    data class PrintReceipt(val receiptData: ReceiptData) : DeepLinkResult()
    
    /**
     * Geçersiz scheme
     */
    data class InvalidScheme(val scheme: String) : DeepLinkResult()
    
    /**
     * Geçersiz host
     */
    data class InvalidHost(val host: String) : DeepLinkResult()
    
    /**
     * Data parametresi eksik
     */
    object MissingData : DeepLinkResult()
    
    /**
     * Parse hatası
     */
    data class ParseError(val message: String) : DeepLinkResult()
    
    /**
     * Validasyon hatası
     */
    data class ValidationError(val message: String) : DeepLinkResult()
    
    /**
     * Başarılı mı?
     */
    fun isSuccess(): Boolean = this is PrintReceipt
    
    /**
     * Hata mesajını döndürür
     */
    fun getErrorMessage(): String? {
        return when (this) {
            is InvalidScheme -> "Geçersiz URL scheme: $scheme"
            is InvalidHost -> "Geçersiz host: $host"
            is MissingData -> "Fiş verisi eksik"
            is ParseError -> "Veri parse hatası: $message"
            is ValidationError -> "Validasyon hatası: $message"
            else -> null
        }
    }
}
