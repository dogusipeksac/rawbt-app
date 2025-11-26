package com.example.rawbtapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawbtapp.printer.PrintResult
import com.example.rawbtapp.printer.PrinterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Yazıcı ekranı için ViewModel
 * UI state'ini yönetir ve yazıcı işlemlerini koordine eder
 */
class PrinterViewModel : ViewModel() {
    
    private val repository = PrinterRepository()
    
    // UI State
    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()
    
    /**
     * IP adresini güncelle
     */
    fun updateIpAddress(ip: String) {
        _uiState.update { it.copy(ipAddress = ip) }
    }
    
    /**
     * Port numarasını güncelle
     */
    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port) }
    }
    
    /**
     * Yazdırılacak metni güncelle
     */
    fun updatePrintText(text: String) {
        _uiState.update { it.copy(printText = text) }
    }
    
    /**
     * Test yazdırma
     */
    fun printTest() {
        val currentState = _uiState.value
        
        // Validasyon
        if (currentState.ipAddress.isBlank()) {
            showMessage("IP adresi boş olamaz", isError = true)
            return
        }
        
        val port = currentState.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return
        }
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            val result = repository.printTest(
                ipAddress = currentState.ipAddress,
                port = port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Özel metin yazdır
     */
    fun printCustomText() {
        val currentState = _uiState.value
        
        // Validasyon
        if (currentState.ipAddress.isBlank()) {
            showMessage("IP adresi boş olamaz", isError = true)
            return
        }
        
        val port = currentState.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return
        }
        
        if (currentState.printText.isBlank()) {
            showMessage("Yazdırılacak metin boş olamaz", isError = true)
            return
        }
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            val result = repository.printCustomText(
                ipAddress = currentState.ipAddress,
                port = port,
                text = currentState.printText
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Örnek fiş yazdır
     */
    fun printSampleReceipt() {
        val currentState = _uiState.value
        
        // Validasyon
        if (currentState.ipAddress.isBlank()) {
            showMessage("IP adresi boş olamaz", isError = true)
            return
        }
        
        val port = currentState.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return
        }
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            val result = repository.printSampleReceipt(
                ipAddress = currentState.ipAddress,
                port = port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Demo yazdırma (tüm özellikler)
     */
    fun printDemo() {
        val currentState = _uiState.value
        
        // Validasyon
        if (currentState.ipAddress.isBlank()) {
            showMessage("IP adresi boş olamaz", isError = true)
            return
        }
        
        val port = currentState.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            showMessage("Geçerli bir port numarası girin (1-65535)", isError = true)
            return
        }
        
        // Yazdırma işlemini başlat
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            
            val result = repository.printDemo(
                ipAddress = currentState.ipAddress,
                port = port
            )
            
            handlePrintResult(result)
        }
    }
    
    /**
     * Yazdırma sonucunu işle
     */
    private fun handlePrintResult(result: PrintResult) {
        when (result) {
            is PrintResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false
                    )
                }
            }
            is PrintResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
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
}

/**
 * Yazıcı ekranı UI state'i
 */
data class PrinterUiState(
    val ipAddress: String = "192.168.1.100",
    val port: String = "9100",
    val printText: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)
