package com.example.rawbtapp.model

/**
 * Web sitesi modeli
 * Kullanıcı istediği siteleri ekleyebilir ve yönetebilir
 */
data class Website(
    val id: String,
    val name: String,
    val url: String,
    val description: String = "",
    val isDefault: Boolean = false
) {
    fun getDisplayName(): String = name
    
    fun getDisplayUrl(): String = url
    
    companion object {
        /**
         * Varsayılan site - 7 Days Havacılık Stok Sistemi
         */
        fun getDefaultTemplate(): Website {
            return Website(
                id = "default_7days",
                name = "7 Days Stok Sistemi",
                url = "https://stock.7dayshavacilik.com/",
                description = "7 Days Havacılık Stok Yönetim Sistemi",
                isDefault = true
            )
        }
    }
}
