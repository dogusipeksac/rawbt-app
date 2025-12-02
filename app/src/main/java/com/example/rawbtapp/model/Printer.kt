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
    val port: Int
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
                port = jsonObject.getInt("port")
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
