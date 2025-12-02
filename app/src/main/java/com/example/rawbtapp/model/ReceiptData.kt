package com.example.rawbtapp.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fiş verisi için data class
 * Web'den gelen JSON verisini temsil eder
 */
data class ReceiptData(
    val receiptId: String,
    val timestamp: String,
    val merchant: MerchantInfo,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val taxRate: Int,
    val totalAmount: Double,
    val paymentMethod: String? = null,
    val currency: String = "TRY"
) {
    companion object {
        /**
         * JSON string'den ReceiptData oluşturur
         */
        fun fromJson(jsonString: String): ReceiptData {
            val json = JSONObject(jsonString)
            
            return ReceiptData(
                receiptId = json.getString("receiptId"),
                timestamp = json.getString("timestamp"),
                merchant = MerchantInfo.fromJson(json.getJSONObject("merchant")),
                items = parseItems(json.getJSONArray("items")),
                subtotal = json.getDouble("subtotal"),
                tax = json.getDouble("tax"),
                taxRate = json.getInt("taxRate"),
                totalAmount = json.getDouble("totalAmount"),
                paymentMethod = json.optString("paymentMethod", "Nakit"),
                currency = json.optString("currency", "TRY")
            )
        }
        
        private fun parseItems(jsonArray: JSONArray): List<ReceiptItem> {
            val items = mutableListOf<ReceiptItem>()
            for (i in 0 until jsonArray.length()) {
                items.add(ReceiptItem.fromJson(jsonArray.getJSONObject(i)))
            }
            return items
        }
    }
    
    /**
     * Tarih formatını düzenler
     */
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            date?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }
    
    /**
     * Para birimi sembolünü döndürür
     */
    fun getCurrencySymbol(): String {
        return when (currency) {
            "TRY" -> "₺"
            "USD" -> "$"
            "EUR" -> "€"
            else -> "₺"
        }
    }
    
    /**
     * Fiyat formatlar
     */
    fun formatPrice(amount: Double): String {
        return "${getCurrencySymbol()}${String.format("%.2f", amount)}"
    }
    
    /**
     * Validasyon
     */
    fun validate(): ValidationResult {
        if (receiptId.isBlank()) {
            return ValidationResult.Error("Fiş numarası boş olamaz")
        }
        
        if (items.isEmpty()) {
            return ValidationResult.Error("En az bir ürün olmalı")
        }
        
        if (totalAmount <= 0) {
            return ValidationResult.Error("Toplam tutar sıfırdan büyük olmalı")
        }
        
        if (merchant.name.isBlank()) {
            return ValidationResult.Error("İşletme adı boş olamaz")
        }
        
        return ValidationResult.Success
    }
}

/**
 * İşletme bilgileri
 */
data class MerchantInfo(
    val name: String,
    val address: String,
    val city: String,
    val phone: String,
    val taxNumber: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): MerchantInfo {
            return MerchantInfo(
                name = json.getString("name"),
                address = json.getString("address"),
                city = json.getString("city"),
                phone = json.getString("phone"),
                taxNumber = json.optString("taxNumber").takeIf { it.isNotEmpty() }
            )
        }
    }
}

/**
 * Fiş kalemi
 */
data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
) {
    companion object {
        fun fromJson(json: JSONObject): ReceiptItem {
            return ReceiptItem(
                name = json.getString("name"),
                quantity = json.getInt("quantity"),
                unitPrice = json.getDouble("unitPrice"),
                totalPrice = json.getDouble("totalPrice")
            )
        }
    }
}

/**
 * Validasyon sonucu
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
