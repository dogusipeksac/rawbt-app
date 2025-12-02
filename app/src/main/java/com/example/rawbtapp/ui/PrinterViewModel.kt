package com.example.rawbtapp.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawbtapp.model.ReceiptData
import com.example.rawbtapp.model.Printer
import com.example.rawbtapp.printer.PrintResult
import com.example.rawbtapp.printer.PrinterRepository
import com.example.rawbtapp.printer.PrinterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Yazıcı ekranı için ViewModel
 * UI state'ini yönetir ve yazıcı işlemlerini koordine eder
 */
class PrinterViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PrinterRepository()
    private val printerManager = PrinterManager(application)
    
    // UI State
    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "PrinterViewModel"
    }
    
    init {
        // Kayıtlı yazıcıları yükle
        loadSavedPrinters()
        // Seçili yazıcıyı yükle
        printerManager.getSelectedPrinter()?.let { printer ->
            _uiState.update { it.copy(selectedPrinter = printer) }
        }
    }
    
    /**
     * Test yazdırma
     */
    fun printTest() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printTest - Test Yazdırma Başlatılıyor")
        Log.d(TAG, "========================================")
        
        val selectedPrinter = _uiState.value.selectedPrinter
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showMessage("Lütfen bir yazıcı seçin", isError = true)
            return
        }
        
        Log.d(TAG, "✓ Validation successful")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            Log.d(TAG, "Calling repository.printTest()...")
            val result = repository.printTest(
                ipAddress = selectedPrinter.ipAddress,
                port = selectedPrinter.port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Özel metin yazdır
     */
    fun printCustomText() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printCustomText - Özel Metin Yazdırma Başlatılıyor")
        Log.d(TAG, "========================================")
        
        val selectedPrinter = _uiState.value.selectedPrinter
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showMessage("Lütfen bir yazıcı seçin", isError = true)
            return
        }
        
        Log.d(TAG, "✓ Validation successful")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        
        val testText = "Test Metin Yazdırma\n${selectedPrinter.getDisplayName()}\n${selectedPrinter.getDetails()}"
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            Log.d(TAG, "Calling repository.printCustomText()...")
            val result = repository.printCustomText(
                ipAddress = selectedPrinter.ipAddress,
                port = selectedPrinter.port,
                text = testText
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Örnek fiş yazdır
     */
    fun printSampleReceipt() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printSampleReceipt - Örnek Fiş Yazdırma Başlatılıyor")
        Log.d(TAG, "========================================")
        
        val selectedPrinter = _uiState.value.selectedPrinter
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showMessage("Lütfen bir yazıcı seçin", isError = true)
            return
        }
        
        Log.d(TAG, "✓ Validation successful")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            Log.d(TAG, "Calling repository.printSampleReceipt()...")
            val result = repository.printSampleReceipt(
                ipAddress = selectedPrinter.ipAddress,
                port = selectedPrinter.port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Demo yazdırma (tüm özellikler)
     */
    fun printDemo() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printDemo - ESC/POS Demo Yazdırma Başlatılıyor")
        Log.d(TAG, "========================================")
        
        val selectedPrinter = _uiState.value.selectedPrinter
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showMessage("Lütfen bir yazıcı seçin", isError = true)
            return
        }
        
        Log.d(TAG, "✓ Validation successful")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            Log.d(TAG, "Calling repository.printDemo()...")
            val result = repository.printDemo(
                ipAddress = selectedPrinter.ipAddress,
                port = selectedPrinter.port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Yazdırma sonucunu işle
     */
    private fun handlePrintResult(result: PrintResult) {
        Log.d(TAG, "handlePrintResult - Sonuç işleniyor")
        when (result) {
            is PrintResult.Success -> {
                Log.d(TAG, "✓ Print Success: ${result.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false
                    )
                }
            }
            is PrintResult.Error -> {
                Log.e(TAG, "✗ Print Error: ${result.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
        Log.d(TAG, "========================================")
    }
    
    /**
     * Mesaj göster
     */
    private fun showMessage(message: String, isError: Boolean = false) {
        _uiState.update {
            it.copy(
                message = message,
                isError = isError,
                isLoading = false
            )
        }
    }
    
    /**
     * Mesajı temizle
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    // ========================================
    // YAZICI YÖNETİMİ FONKSİYONLARI
    // ========================================
    
    /**
     * Kayıtlı yazıcıları yükle
     */
    fun loadSavedPrinters() {
        Log.d(TAG, "loadSavedPrinters - Kayıtlı yazıcılar yükleniyor")
        val printers = printerManager.getAllPrinters()
        _uiState.update { it.copy(savedPrinters = printers) }
        Log.d(TAG, "✓ ${printers.size} yazıcı yüklendi")
    }
    
    /**
     * Yeni yazıcı ekle
     */
    fun addPrinter(name: String, number: String, ipAddress: String, port: String): Boolean {
        Log.d(TAG, "========================================")
        Log.d(TAG, "addPrinter - Yeni yazıcı ekleniyor")
        Log.d(TAG, "========================================")
        
        // Validasyon
        if (name.isBlank()) {
            showMessage("Yazıcı adı boş olamaz", isError = true)
            return false
        }
        
        if (number.isBlank()) {
            showMessage("Yazıcı numarası boş olamaz", isError = true)
            return false
        }
        
        if (ipAddress.isBlank()) {
            showMessage("IP adresi boş olamaz", isError = true)
            return false
        }
        
        val portInt = port.toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return false
        }
        
        // Aynı numarada yazıcı var mı kontrol et
        if (printerManager.getPrinterByNumber(number) != null) {
            showMessage("Bu numarada bir yazıcı zaten kayıtlı", isError = true)
            return false
        }
        
        // Yazıcıyı ekle
        val printer = printerManager.addPrinter(name, number, ipAddress, portInt)
        loadSavedPrinters()
        
        showMessage("Yazıcı eklendi: ${printer.getDisplayName()}")
        Log.d(TAG, "✓ Yazıcı başarıyla eklendi")
        Log.d(TAG, "========================================")
        return true
    }
    
    /**
     * Yazıcıyı güncelle
     */
    fun updatePrinter(id: String, name: String, number: String, ipAddress: String, port: String): Boolean {
        Log.d(TAG, "updatePrinter - Yazıcı güncelleniyor: $id")
        
        // Validasyon
        if (name.isBlank() || number.isBlank() || ipAddress.isBlank()) {
            showMessage("Tüm alanları doldurun", isError = true)
            return false
        }
        
        val portInt = port.toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return false
        }
        
        // Aynı numarada başka yazıcı var mı kontrol et
        val existingPrinter = printerManager.getPrinterByNumber(number)
        if (existingPrinter != null && existingPrinter.id != id) {
            showMessage("Bu numarada başka bir yazıcı kayıtlı", isError = true)
            return false
        }
        
        val success = printerManager.updatePrinter(id, name, number, ipAddress, portInt)
        if (success) {
            loadSavedPrinters()
            showMessage("Yazıcı güncellendi")
        } else {
            showMessage("Yazıcı güncellenemedi", isError = true)
        }
        
        return success
    }
    
    /**
     * Yazıcıyı sil
     */
    fun deletePrinter(id: String) {
        Log.d(TAG, "deletePrinter - Yazıcı siliniyor: $id")
        
        val success = printerManager.deletePrinter(id)
        if (success) {
            loadSavedPrinters()
            showMessage("Yazıcı silindi")
        } else {
            showMessage("Yazıcı silinemedi", isError = true)
        }
    }
    
    /**
     * Yazıcı seç
     */
    fun selectPrinter(printer: Printer) {
        Log.d(TAG, "selectPrinter - Yazıcı seçildi: ${printer.getDisplayName()}")
        printerManager.setSelectedPrinter(printer.id)
        _uiState.update { it.copy(selectedPrinter = printer) }
    }
    
    /**
     * Seçili yazıcıyı getir
     */
    fun getSelectedPrinter(): Printer? {
        return printerManager.getSelectedPrinter()
    }
    
    
    /**
     * Deep link'ten gelen fiş verisini yazdır
     * Retry mekanizması ile 3 kez dener
     */
    fun printReceiptFromDeepLink(receiptData: ReceiptData) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printReceiptFromDeepLink - Fiş Yazdırma Başlatılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Fiş ID: ${receiptData.receiptId}")
        Log.d(TAG, "İşletme: ${receiptData.merchant.name}")
        Log.d(TAG, "Ürün Sayısı: ${receiptData.items.size}")
        Log.d(TAG, "Toplam: ${receiptData.formatPrice(receiptData.totalAmount)}")
        
        val selectedPrinter = _uiState.value.selectedPrinter
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showMessage("Lütfen bir yazıcı seçin", isError = true)
            return
        }
        
        Log.d(TAG, "✓ Validation successful")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        Log.d(TAG, "Max retries: 3, Delay: 1000ms")
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            Log.d(TAG, "Calling repository.printReceiptWithRetry()...")
            val result = repository.printReceiptWithRetry(
                ipAddress = selectedPrinter.ipAddress,
                port = selectedPrinter.port,
                receiptData = receiptData,
                maxRetries = 3,
                delayMillis = 1000
            )
            
            handlePrintResult(result)
        }
    }
}

/**
 * Yazıcı ekranı UI state'i
 */
data class PrinterUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val savedPrinters: List<Printer> = emptyList(),
    val selectedPrinter: Printer? = null
)
