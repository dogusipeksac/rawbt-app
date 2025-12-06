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

    /**
     * Çince karakterleri iptal et - XPrinter için kritik!
     * Bazı XPrinter modelleri varsayılan olarak Çince modunda geliyor
     */
    private fun cancelChineseMode(): ByteArray {
        return byteArrayOf(0x1C.toByte(), 0x2E.toByte())  // FS . (Cancel Chinese Mode)
    }

    /**
     * Her yazdırma öncesi çalıştırılacak başlangıç komutları
     */
    private fun getInitCommands(): ByteArray {
        return byteArrayOf(
            0x1C.toByte(), 0x2E.toByte(),  // 1. ÖNCELİKLE Çince modu iptal et!
            0x1B.toByte(), 0x40.toByte()   // 2. Printer'ı initialize et
        )
    }

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
     * Yazıcı için otomatik encoding tespiti yap ve güncelle
     * İlk yazdırmada çağrılır
     */
    suspend fun autoDetectAndUpdateEncoding(printerId: String): String {
        val printer = getPrinterById(printerId) ?: return "PC857_CP857"
        
        Log.d(TAG, "Auto-detecting encoding for printer: ${printer.name}")
        
        // Otomatik tespit
        val bestEncoding = AutoEncodingDetector.detectBestEncoding(
            printer.ipAddress,
            printer.port
        )
        
        // Encoding'i güncelle
        val updatedPrinter = printer.copy(charsetEncoding = bestEncoding)
        updatePrinter(updatedPrinter)
        
        Log.d(TAG, "✓ Auto-detected encoding: $bestEncoding")
        return bestEncoding
    }
    
    /**
     * Yeni yazıcı ekle
     */
    fun addPrinter(
        name: String,
        number: String,
        ipAddress: String,
        port: Int,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "AUTO",  // Varsayılan: Otomatik tespit
        cancelTurkishChars: Boolean = false
    ): Printer {
        Log.d(TAG, "========================================")
        Log.d(TAG, "addPrinter - Yeni yazıcı ekleniyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Name: $name")
        Log.d(TAG, "Number: $number")
        Log.d(TAG, "IP: $ipAddress")
        Log.d(TAG, "Port: $port")
        Log.d(TAG, "Cut Paper: $cutPaper")
        Log.d(TAG, "Cut Feed Lines: $cutFeedLines")
        Log.d(TAG, "Charset Encoding: $charsetEncoding")

        val printer = Printer(
            id = UUID.randomUUID().toString(),
            name = name,
            number = number,
            ipAddress = ipAddress,
            port = port,
            cutPaper = cutPaper,
            cutFeedLines = cutFeedLines,
            charsetEncoding = charsetEncoding,
            cancelTurkishChars = cancelTurkishChars
        )

        val printers = getAllPrinters().toMutableList()
        printers.add(printer)
        savePrinters(printers)

        Log.d(TAG, "✓ Yazıcı eklendi: ${printer.getDisplayName()}")
        Log.d(TAG, "========================================")

        return printer
    }
    
    /**
     * Yazıcıyı güncelle (Printer objesi ile)
     */
    fun updatePrinter(printer: Printer): Boolean {
        return updatePrinter(
            id = printer.id,
            name = printer.name,
            number = printer.number,
            ipAddress = printer.ipAddress,
            port = printer.port,
            cutPaper = printer.cutPaper,
            cutFeedLines = printer.cutFeedLines,
            charsetEncoding = printer.charsetEncoding,
            cancelTurkishChars = printer.cancelTurkishChars
        )
    }
    
    /**
     * Yazıcıyı güncelle
     */
    fun updatePrinter(
        id: String,
        name: String,
        number: String,
        ipAddress: String,
        port: Int,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): Boolean {
        Log.d(TAG, "updatePrinter - Yazıcı güncelleniyor: $id")

        val printers = getAllPrinters().toMutableList()
        val index = printers.indexOfFirst { it.id == id }

        if (index == -1) {
            Log.e(TAG, "✗ Yazıcı bulunamadı: $id")
            return false
        }

        val existingPrinter = printers[index]
        printers[index] = Printer(
            id = id,
            name = name,
            number = number,
            ipAddress = ipAddress,
            port = port,
            cutPaper = cutPaper,
            cutFeedLines = cutFeedLines,
            charsetEncoding = charsetEncoding,
            cancelTurkishChars = cancelTurkishChars
        )
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
