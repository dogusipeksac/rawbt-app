package com.example.rawbtapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.rawbtapp.deeplink.DeepLinkHandler
import com.example.rawbtapp.deeplink.DeepLinkResult
import com.example.rawbtapp.ui.PrinterScreen
import com.example.rawbtapp.ui.PrinterViewModel
import com.example.rawbtapp.ui.theme.RawBTAppTheme
import com.example.rawbtapp.webview.WebViewActivity

class MainActivity : ComponentActivity() {
    
    private val viewModel: PrinterViewModel by viewModels()
    private val deepLinkHandler = DeepLinkHandler()
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "MainActivity onCreate - SAYFA: Ana Yazıcı Ekranı")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Intent: ${intent?.action}")
        Log.d(TAG, "Intent Data: ${intent?.data}")
        Log.d(TAG, "Intent Extras: ${intent?.extras}")
        
        // Deep link'i işle
        handleDeepLink(intent)
        
        // Config'den otomatik açılma kontrolü
        val autoOpen = resources.getBoolean(R.bool.webview_auto_open)
        Log.d(TAG, "WebView auto-open config: $autoOpen")
        if (autoOpen) {
            Log.d(TAG, "Auto-opening WebView from config")
            openWebView()
        }
        
        setContent {
            RawBTAppTheme {
                PrinterScreen(
                    viewModel = viewModel,
                    onOpenWebView = { openWebView() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "========================================")
        Log.d(TAG, "MainActivity onNewIntent - Yeni Intent Alındı")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Intent Action: ${intent.action}")
        Log.d(TAG, "Intent Data: ${intent.data}")
        setIntent(intent)
        handleDeepLink(intent)
    }
    
    /**
     * Deep link'i işler ve yazıcıya gönderir
     */
    private fun handleDeepLink(intent: Intent?) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "handleDeepLink - Deep Link İşleniyor")
        Log.d(TAG, "========================================")
        
        when (val result = deepLinkHandler.handleIntent(intent)) {
            is DeepLinkResult.PrintReceipt -> {
                Log.d(TAG, "✓ Deep Link Başarılı - Fiş Yazdırma İsteği")
                Log.d(TAG, "Fiş ID: ${result.receiptData.receiptId}")
                Log.d(TAG, "İşletme: ${result.receiptData.merchant.name}")
                Log.d(TAG, "Ürün Sayısı: ${result.receiptData.items.size}")
                Log.d(TAG, "Toplam Tutar: ${result.receiptData.formatPrice(result.receiptData.totalAmount)}")
                Toast.makeText(
                    this,
                    "Fiş yazdırılıyor: ${result.receiptData.receiptId}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // ViewModel'e fiş verisini gönder
                viewModel.printReceiptFromDeepLink(result.receiptData)
            }
            
            is DeepLinkResult.NoDeepLink -> {
                Log.d(TAG, "ℹ Deep Link Bulunamadı - Normal Başlatma")
            }
            
            else -> {
                val errorMessage = result.getErrorMessage()
                Log.e(TAG, "✗ Deep Link Hatası: $errorMessage")
                errorMessage?.let {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            }
        }
        Log.d(TAG, "========================================")
    }
    
    /**
     * WebView'ı aç
     */
    private fun openWebView() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "openWebView - WebView Açılıyor")
        Log.d(TAG, "Hedef Sayfa: WebViewActivity (POS Web Sistemi)")
        Log.d(TAG, "========================================")
        val intent = Intent(this, WebViewActivity::class.java)
        startActivity(intent)
    }
    
}