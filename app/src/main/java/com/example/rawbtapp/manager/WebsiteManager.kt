package com.example.rawbtapp.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.rawbtapp.model.Website
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Web sitelerini yöneten sınıf
 * SharedPreferences kullanarak local storage'da saklar
 */
class WebsiteManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "WebsiteManager"
        private const val PREFS_NAME = "websites_prefs"
        private const val KEY_WEBSITES = "websites"
        private const val KEY_SELECTED_WEBSITE = "selected_website"
    }
    
    /**
     * Tüm siteleri getir
     */
    fun getAllWebsites(): List<Website> {
        return try {
            val websitesJson = prefs.getString(KEY_WEBSITES, null)
            if (websitesJson != null) {
                parseWebsitesFromJson(websitesJson)
            } else {
                // İlk açılışta varsayılan template'i ekle
                val defaultSite = Website.getDefaultTemplate()
                saveWebsites(listOf(defaultSite))
                listOf(defaultSite)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading websites", e)
            emptyList()
        }
    }
    
    /**
     * Site ekle
     */
    fun addWebsite(name: String, url: String, description: String = ""): Boolean {
        return try {
            val websites = getAllWebsites().toMutableList()
            val newWebsite = Website(
                id = UUID.randomUUID().toString(),
                name = name,
                url = url,
                description = description,
                isDefault = false
            )
            websites.add(newWebsite)
            saveWebsites(websites)
            Log.d(TAG, "Website added: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding website", e)
            false
        }
    }
    
    /**
     * Site sil
     */
    fun deleteWebsite(id: String): Boolean {
        return try {
            val websites = getAllWebsites().toMutableList()
            val website = websites.find { it.id == id }
            
            // Varsayılan site silinemez
            if (website?.isDefault == true) {
                Log.w(TAG, "Cannot delete default website")
                return false
            }
            
            websites.removeAll { it.id == id }
            saveWebsites(websites)
            
            // Seçili site silinirse, seçimi temizle
            if (getSelectedWebsite()?.id == id) {
                clearSelectedWebsite()
            }
            
            Log.d(TAG, "Website deleted: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting website", e)
            false
        }
    }
    
    /**
     * Site güncelle
     */
    fun updateWebsite(id: String, name: String, url: String, description: String = ""): Boolean {
        return try {
            val websites = getAllWebsites().toMutableList()
            val index = websites.indexOfFirst { it.id == id }
            
            if (index != -1) {
                val website = websites[index]
                websites[index] = website.copy(
                    name = name,
                    url = url,
                    description = description
                )
                saveWebsites(websites)
                Log.d(TAG, "Website updated: $id")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating website", e)
            false
        }
    }
    
    /**
     * Seçili siteyi kaydet
     */
    fun selectWebsite(website: Website) {
        prefs.edit().putString(KEY_SELECTED_WEBSITE, website.id).apply()
        Log.d(TAG, "Website selected: ${website.name}")
    }
    
    /**
     * Seçili siteyi getir
     */
    fun getSelectedWebsite(): Website? {
        val selectedId = prefs.getString(KEY_SELECTED_WEBSITE, null)
        return if (selectedId != null) {
            getAllWebsites().find { it.id == selectedId }
        } else {
            null
        }
    }
    
    /**
     * Seçili siteyi temizle
     */
    fun clearSelectedWebsite() {
        prefs.edit().remove(KEY_SELECTED_WEBSITE).apply()
        Log.d(TAG, "Selected website cleared")
    }
    
    /**
     * Siteleri kaydet
     */
    private fun saveWebsites(websites: List<Website>) {
        val websitesJson = websitesToJson(websites)
        prefs.edit().putString(KEY_WEBSITES, websitesJson).apply()
    }
    
    /**
     * Website listesini JSON'a çevir
     */
    private fun websitesToJson(websites: List<Website>): String {
        val jsonArray = JSONArray()
        websites.forEach { website ->
            val jsonObject = JSONObject().apply {
                put("id", website.id)
                put("name", website.name)
                put("url", website.url)
                put("description", website.description)
                put("isDefault", website.isDefault)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
    
    /**
     * JSON'dan website listesi oluştur
     */
    private fun parseWebsitesFromJson(json: String): List<Website> {
        val websites = mutableListOf<Website>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val website = Website(
                id = jsonObject.getString("id"),
                name = jsonObject.getString("name"),
                url = jsonObject.getString("url"),
                description = jsonObject.optString("description", ""),
                isDefault = jsonObject.optBoolean("isDefault", false)
            )
            websites.add(website)
        }
        return websites
    }
    
    /**
     * Tüm siteleri sil (reset)
     */
    fun clearAllWebsites() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All websites cleared")
    }
}
