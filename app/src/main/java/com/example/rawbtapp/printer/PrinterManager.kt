package com.example.rawbtapp.printer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.rawbtapp.model.Printer
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Yazıcı yönetimi için singleton class
 * SharedPreferences ile yazıcıları kaydeder ve yönetir
 */
class PrinterManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "PrinterManager"
        private const val PREFS_NAME = "RawBTAppPrinters"
        private const val KEY_PRINTERS = "printers"
        private const val KEY_SELECTED_PRINTER_ID = "selected_printer_id"
    }
    
    /**
     * Tüm kayıtlı yazıcıları getir
     */
    fun getAllPrinters(): List<Printer> {
        Log.d(TAG, "getAllPrinters - Kayıtlı yazıcılar getiriliyor")
        
        val printersJson = sharedPreferences.getString(KEY_PRINTERS, null)
        if (printersJson == null) {
            Log.d(TAG, "Kayıtlı yazıcı bulunamadı")
            return emptyList()
        }
        
        return try {
            val jsonArray = JSONArray(printersJson)
            val printers = mutableListOf<Printer>()
            
            for (i in 0 until jsonArray.length()) {
                val printerJson = jsonArray.getString(i)
                printers.add(Printer.fromJson(printerJson))
            }
            
            Log.d(TAG, "✓ ${printers.size} yazıcı yüklendi")
            printers
        } catch (e: Exception) {
            Log.e(TAG, "✗ Yazıcılar yüklenirken hata", e)
            emptyList()
        }
    }
    
    /**
     * Yeni yazıcı ekle
     */
    fun addPrinter(name: String, number: String, ipAddress: String, port: Int): Printer {
        Log.d(TAG, "========================================")
        Log.d(TAG, "addPrinter - Yeni yazıcı ekleniyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Name: $name")
        Log.d(TAG, "Number: $number")
        Log.d(TAG, "IP: $ipAddress")
        Log.d(TAG, "Port: $port")
        
        val printer = Printer(
            id = UUID.randomUUID().toString(),
            name = name,
            number = number,
            ipAddress = ipAddress,
            port = port
        )
        
        val printers = getAllPrinters().toMutableList()
        printers.add(printer)
        savePrinters(printers)
        
        Log.d(TAG, "✓ Yazıcı eklendi: ${printer.getDisplayName()}")
        Log.d(TAG, "========================================")
        
        return printer
    }
    
    /**
     * Yazıcıyı güncelle
     */
    fun updatePrinter(id: String, name: String, number: String, ipAddress: String, port: Int): Boolean {
        Log.d(TAG, "updatePrinter - Yazıcı güncelleniyor: $id")
        
        val printers = getAllPrinters().toMutableList()
        val index = printers.indexOfFirst { it.id == id }
        
        if (index == -1) {
            Log.e(TAG, "✗ Yazıcı bulunamadı: $id")
            return false
        }
        
        printers[index] = Printer(id, name, number, ipAddress, port)
        savePrinters(printers)
        
        Log.d(TAG, "✓ Yazıcı güncellendi")
        return true
    }
    
    /**
     * Yazıcıyı sil
     */
    fun deletePrinter(id: String): Boolean {
        Log.d(TAG, "deletePrinter - Yazıcı siliniyor: $id")
        
        val printers = getAllPrinters().toMutableList()
        val removed = printers.removeIf { it.id == id }
        
        if (removed) {
            savePrinters(printers)
            
            // Eğer silinen yazıcı seçili yazıcıysa, seçimi temizle
            if (getSelectedPrinterId() == id) {
                clearSelectedPrinter()
            }
            
            Log.d(TAG, "✓ Yazıcı silindi")
        } else {
            Log.e(TAG, "✗ Yazıcı bulunamadı")
        }
        
        return removed
    }
    
    /**
     * ID'ye göre yazıcı getir
     */
    fun getPrinterById(id: String): Printer? {
        return getAllPrinters().find { it.id == id }
    }
    
    /**
     * Numaraya göre yazıcı getir
     */
    fun getPrinterByNumber(number: String): Printer? {
        return getAllPrinters().find { it.number == number }
    }
    
    /**
     * Seçili yazıcıyı ayarla
     */
    fun setSelectedPrinter(id: String) {
        Log.d(TAG, "setSelectedPrinter: $id")
        sharedPreferences.edit().putString(KEY_SELECTED_PRINTER_ID, id).apply()
    }
    
    /**
     * Seçili yazıcı ID'sini getir
     */
    fun getSelectedPrinterId(): String? {
        return sharedPreferences.getString(KEY_SELECTED_PRINTER_ID, null)
    }
    
    /**
     * Seçili yazıcıyı getir
     */
    fun getSelectedPrinter(): Printer? {
        val id = getSelectedPrinterId() ?: return null
        return getPrinterById(id)
    }
    
    /**
     * Seçili yazıcıyı temizle
     */
    fun clearSelectedPrinter() {
        Log.d(TAG, "clearSelectedPrinter")
        sharedPreferences.edit().remove(KEY_SELECTED_PRINTER_ID).apply()
    }
    
    /**
     * Yazıcıları kaydet
     */
    private fun savePrinters(printers: List<Printer>) {
        Log.d(TAG, "savePrinters - ${printers.size} yazıcı kaydediliyor")
        
        val jsonArray = JSONArray()
        printers.forEach { printer ->
            jsonArray.put(printer.toJson())
        }
        
        sharedPreferences.edit()
            .putString(KEY_PRINTERS, jsonArray.toString())
            .apply()
        
        Log.d(TAG, "✓ Yazıcılar kaydedildi")
    }
    
    /**
     * Tüm yazıcıları temizle
     */
    fun clearAllPrinters() {
        Log.d(TAG, "clearAllPrinters - Tüm yazıcılar siliniyor")
        sharedPreferences.edit()
            .remove(KEY_PRINTERS)
            .remove(KEY_SELECTED_PRINTER_ID)
            .apply()
        Log.d(TAG, "✓ Tüm yazıcılar silindi")
    }
}
