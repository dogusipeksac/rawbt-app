package com.example.rawbtapp.model

import org.json.JSONObject

/**
 * Yazıcı bilgilerini tutan model
 */
data class Printer(
    val id: String,
    val name: String,
    val number: String,
    val ipAddress: String,
    val port: Int,
    val cutPaper: Boolean = true,  // Kağıt kesme aktif mi?
    val cutFeedLines: Int = 3,  // Kesme öncesi boşluk satır sayısı (0-10 arası)
    val charsetEncoding: String = "PC857_CP857",  // Karakter seti ve encoding kombinasyonu
    val cancelTurkishChars: Boolean = false  // Türkçe karakterleri İngilizce karşılıklarına çevir
) {
    companion object {
        /**
         * JSON'dan Printer nesnesi oluştur
         */
        fun fromJson(json: String): Printer {
            val jsonObject = JSONObject(json)
            return Printer(
                id = jsonObject.getString("id"),
                name = jsonObject.getString("name"),
                number = jsonObject.getString("number"),
                ipAddress = jsonObject.getString("ipAddress"),
                port = jsonObject.getInt("port"),
                cutPaper = jsonObject.optBoolean("cutPaper", true),
                cutFeedLines = jsonObject.optInt("cutFeedLines", 3),
                charsetEncoding = jsonObject.optString("charsetEncoding", "PC857_CP857"),
                cancelTurkishChars = jsonObject.optBoolean("cancelTurkishChars", false)
            )
        }
    }
    
    /**
     * Printer nesnesini JSON'a çevir
     */
    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("number", number)
        jsonObject.put("ipAddress", ipAddress)
        jsonObject.put("port", port)
        jsonObject.put("cutPaper", cutPaper)
        jsonObject.put("cutFeedLines", cutFeedLines)
        jsonObject.put("charsetEncoding", charsetEncoding)
        jsonObject.put("cancelTurkishChars", cancelTurkishChars)
        return jsonObject.toString()
    }
    
    /**
     * Görüntüleme için string
     */
    fun getDisplayName(): String {
        return "Yazıcı #$number - $name"
    }
    
    /**
     * Detaylı bilgi
     */
    fun getDetails(): String {
        return "$ipAddress:$port"
    }
}
