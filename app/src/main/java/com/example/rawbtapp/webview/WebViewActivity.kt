package com.example.rawbtapp.webview

import android.os.Bundle
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.HttpURLConnection
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rawbtapp.model.ReceiptData
import com.example.rawbtapp.model.Printer
import com.example.rawbtapp.ui.PrinterViewModel
import com.example.rawbtapp.ui.theme.RawBTAppTheme
import com.example.rawbtapp.R
import com.example.rawbtapp.printer.PrinterManager
import org.json.JSONException
import android.util.Log
import android.webkit.WebSettings
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintAttributes
import android.webkit.WebChromeClient
import android.os.Build
import androidx.annotation.RequiresApi
import android.app.AlertDialog

/**
 * WebView ile POS sistemi entegrasyonu
 * JavaScript bridge ile web'den direkt yazdırma
 */
class WebViewActivity : ComponentActivity() {
    
    private val viewModel: PrinterViewModel by viewModels()
    private lateinit var webView: WebView
    private lateinit var printerManager: PrinterManager
    
    companion object {
        private const val TAG = "WebViewActivity"
        const val EXTRA_URL = "extra_url"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // PrinterManager'ı başlat
        printerManager = PrinterManager(this)

        Log.d(TAG, "========================================")
        Log.d(TAG, "WebViewActivity onCreate - SAYFA: POS Web Sistemi")
        Log.d(TAG, "========================================")
        
        
        // Config dosyasından URL'i al
        val configUrl = getString(R.string.webview_url)
        val url = intent.getStringExtra(EXTRA_URL) ?: configUrl
        
        Log.d(TAG, "Loading URL from config: $url")
        
        // Config'den başlığı al
        val title = getString(R.string.webview_title)
        Log.d(TAG, "WebView Title: $title")
        
        setContent {
            RawBTAppTheme {
                WebViewScreen(
                    url = url,
                    title = title,
                    onWebViewCreated = { wv ->
                        webView = wv
                        setupWebView(wv)
                    },
                    onBackPressed = { finish() }
                )
            }
        }
    }
    
    private fun setupWebView(webView: WebView) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "setupWebView - WebView Yapılandırılıyor")
        Log.d(TAG, "========================================")
        
        // Config dosyasından ayarları oku
        val jsEnabled = resources.getBoolean(R.bool.webview_javascript_enabled)
        val domStorageConfig = resources.getBoolean(R.bool.webview_dom_storage_enabled)
        val fileAccessEnabled = resources.getBoolean(R.bool.webview_file_access_enabled)
        val zoomEnabled = resources.getBoolean(R.bool.webview_zoom_enabled)
        val cacheModeConfig = resources.getInteger(R.integer.webview_cache_mode)
        val debugEnabled = resources.getBoolean(R.bool.webview_debug_enabled)
        
        Log.d(TAG, "WebView Ayarları:")
        Log.d(TAG, "  - JavaScript: $jsEnabled")
        Log.d(TAG, "  - DOM Storage: $domStorageConfig")
        Log.d(TAG, "  - File Access: $fileAccessEnabled")
        Log.d(TAG, "  - Zoom: $zoomEnabled")
        Log.d(TAG, "  - Cache Mode: $cacheModeConfig")
        Log.d(TAG, "  - Debug: $debugEnabled")
        
        // Debug mode
        if (debugEnabled) {
            WebView.setWebContentsDebuggingEnabled(true)
            Log.d(TAG, "WebView debugging enabled - chrome://inspect")
        }
        
        webView.apply {
            settings.apply {
                javaScriptEnabled = jsEnabled
                domStorageEnabled = domStorageConfig
                @Suppress("DEPRECATION")
                databaseEnabled = true
                allowFileAccess = fileAccessEnabled
                allowContentAccess = fileAccessEnabled
                builtInZoomControls = zoomEnabled
                displayZoomControls = false
                cacheMode = when (cacheModeConfig) {
                    1 -> WebSettings.LOAD_CACHE_ELSE_NETWORK
                    2 -> WebSettings.LOAD_NO_CACHE
                    3 -> WebSettings.LOAD_CACHE_ONLY
                    else -> WebSettings.LOAD_DEFAULT
                }
                
                Log.d(TAG, "WebView settings: JS=$jsEnabled, DOM=$domStorageConfig, FileAccess=$fileAccessEnabled")
            }
            
            // JavaScript Interface ekle
            addJavascriptInterface(
                PrinterJavaScriptInterface(),
                "AndroidPrinter"
            )
            
            // Android interface (sizin kodunuz için)
            addJavascriptInterface(
                PrinterJavaScriptInterface(),
                "Android"
            )
            
            // PDF yakalama için özel WebViewClient
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    
                    // PDF kontrolü
                    if (url.endsWith(".pdf", ignoreCase = true) || 
                        request.requestHeaders["Accept"]?.contains("application/pdf") == true) {
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "PDF Tespit Edildi - Otomatik Yazdırma")
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "PDF URL: $url")
                        
                        // PDF'i arka planda indir ve yazdır
                        downloadAndPrintPdf(url)
                        
                        // Boş response döndür (PDF gösterilmesin)
                        return WebResourceResponse("text/html", "UTF-8", null)
                    }
                    
                    return super.shouldInterceptRequest(view, request)
                }
                
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "Page started loading: $url")
                    
                    // URL'de PDF varsa
                    if (url?.endsWith(".pdf", ignoreCase = true) == true) {
                        Log.d(TAG, "PDF URL detected in page load: $url")
                        downloadAndPrintPdf(url)
                        view?.stopLoading()
                    }
                }
            }
            
            // window.print() çağrılarını yakala (Android 4.4+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                interceptWindowPrint()
            }
        }
    }
    
    /**
     * JavaScript'ten çağrılabilen interface
     * Web sitesinden window.AndroidPrinter.print() ile erişilebilir
     */
    inner class PrinterJavaScriptInterface {
        
        /**
         * Native Android printer dialog'ını aç
         * Web'den: window.AndroidPrinter.triggerNativePrint("content", "Document Title")
         */
        @JavascriptInterface
        fun triggerNativePrint(contentSelector: String, documentTitle: String) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "JavaScript Interface - triggerNativePrint() Çağrıldı")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Content Selector: $contentSelector")
            Log.d(TAG, "Document Title: $documentTitle")
            
            runOnUiThread {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        Log.d(TAG, "Opening native printer selection dialog...")
                        openNativePrinterDialog(documentTitle)
                    } else {
                        Log.e(TAG, "✗ Native printing requires Android 4.4 (KitKat) or higher")
                        showToast("Native printing requires Android 4.4 or higher")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error opening native printer dialog", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    showToast("Error opening printer dialog: ${e.message}")
                }
            }
            Log.d(TAG, "========================================")
        }
        
        /**
         * Native printer dialog'ını aç (HTML içerik ile)
         * Web'den: window.AndroidPrinter.printHtml("<html>...</html>", "Title")
         */
        @JavascriptInterface
        fun printHtml(htmlContent: String, documentTitle: String) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "JavaScript Interface - printHtml() Çağrıldı")
            Log.d(TAG, "========================================")
            Log.d(TAG, "HTML Content Length: ${htmlContent.length}")
            Log.d(TAG, "Document Title: $documentTitle")
            
            runOnUiThread {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        Log.d(TAG, "Creating print job with HTML content...")
                        printHtmlContent(htmlContent, documentTitle)
                    } else {
                        Log.e(TAG, "✗ Native printing requires Android 4.4 (KitKat) or higher")
                        showToast("Native printing requires Android 4.4 or higher")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error printing HTML", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    showToast("Error printing: ${e.message}")
                }
            }
            Log.d(TAG, "========================================")
        }
        
        /**
         * Web'den fiş yazdırma
         * JavaScript'ten çağrılır: window.AndroidPrinter.print(jsonString)
         */
        @JavascriptInterface
        fun print(jsonData: String) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "JavaScript Interface - print() Çağrıldı")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Print request received from web")
            Log.d(TAG, "JSON Data Length: ${jsonData.length}")
            Log.d(TAG, "JSON Data: $jsonData")
            
            runOnUiThread {
                try {
                    // JSON'u parse et
                    Log.d(TAG, "Parsing JSON to ReceiptData...")
                    val receiptData = ReceiptData.fromJson(jsonData)
                    Log.d(TAG, "✓ JSON Parse Başarılı")
                    Log.d(TAG, "Fiş ID: ${receiptData.receiptId}")
                    Log.d(TAG, "İşletme: ${receiptData.merchant.name}")
                    Log.d(TAG, "Ürün Sayısı: ${receiptData.items.size}")
                    
                    // Validasyon
                    Log.d(TAG, "Validating receipt data...")
                    val validationResult = receiptData.validate()
                    if (validationResult is com.example.rawbtapp.model.ValidationResult.Error) {
                        Log.e(TAG, "✗ Validasyon Hatası: ${validationResult.message}")
                        showToast("Hata: ${validationResult.message}")
                        callJavaScriptCallback("onPrintError", validationResult.message)
                        return@runOnUiThread
                    }
                    Log.d(TAG, "✓ Validasyon Başarılı")
                    
                    // Yazdır
                    Log.d(TAG, "Sending to printer...")
                    viewModel.printReceiptFromDeepLink(receiptData)
                    showToast("Fiş yazdırılıyor: ${receiptData.receiptId}")
                    callJavaScriptCallback("onPrintSuccess", receiptData.receiptId)
                    Log.d(TAG, "✓ Print request sent to ViewModel")
                    
                } catch (e: JSONException) {
                    Log.e(TAG, "✗ JSON parse error", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    showToast("Geçersiz JSON formatı")
                    callJavaScriptCallback("onPrintError", "Geçersiz JSON formatı")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Print error", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    showToast("Yazdırma hatası: ${e.message}")
                    callJavaScriptCallback("onPrintError", e.message ?: "Bilinmeyen hata")
                }
            }
            Log.d(TAG, "========================================")
        }
        
        /**
         * HTML içeriğini yazdır (POS için)
         * JavaScript'ten çağrılır: window.Android.printPos(jsonString)
         * 
         * Payload format:
         * {
         *   "html": "<div>...</div>",
         *   "css": "/css/pos_print.css",
         *   "title": "POS Invoice"
         * }
         */
        @JavascriptInterface
        fun printPos(jsonData: String) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "JavaScript Interface - printPos() Çağrıldı")
            Log.d(TAG, "========================================")
            Log.d(TAG, "PrintPos request received from web")
            Log.d(TAG, "JSON Data Length: ${jsonData.length}")
            Log.d(TAG, "JSON Data: $jsonData")
            
            runOnUiThread {
                try {
                    // JSON parse
                    val json = org.json.JSONObject(jsonData)
                    val html = json.getString("html")
                    val css = json.optString("css", "")
                    val title = json.optString("title", "POS Invoice")
                    
                    Log.d(TAG, "✓ JSON Parse Başarılı")
                    Log.d(TAG, "HTML content length: ${html.length}")
                    Log.d(TAG, "CSS path: $css")
                    Log.d(TAG, "Title: $title")
                    Log.d(TAG, "HTML Preview (first 500 chars):")
                    Log.d(TAG, html.take(500))
                    
                    // Native printer dialog ile yazdır
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        Log.d(TAG, "Opening native printer dialog with HTML content...")
                        
                        // Tam HTML dokümantı oluştur
                        val fullHtml = buildFullHtmlDocument(html, css, title)
                        Log.d(TAG, "Full HTML document created: ${fullHtml.length} chars")
                        
                        // Yazıcı seçim dialog'unu göster
                        showPrinterSelectionDialog(fullHtml, title)
                        
                    } else {
                        Log.e(TAG, "✗ Native printing requires Android 4.4+")
                        showToast("Native printing requires Android 4.4 or higher")
                        callJavaScriptCallback("onPrintError", "Android version too old")
                    }
                    
                } catch (e: JSONException) {
                    Log.e(TAG, "✗ JSON parse error in printPos", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    Log.e(TAG, "Invalid JSON: $jsonData")
                    showToast("Geçersiz JSON formatı")
                    callJavaScriptCallback("onPrintError", "Geçersiz JSON formatı")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ PrintPos error", e)
                    Log.e(TAG, "Error details: ${e.message}")
                    showToast("Yazdırma hatası: ${e.message}")
                    callJavaScriptCallback("onPrintError", e.message ?: "Bilinmeyen hata")
                }
            }
            Log.d(TAG, "========================================")
        }
    }
    
    /**
     * Yazıcı seçim dialog'unu göster
     */
    private fun showPrinterSelectionDialog(htmlContent: String, documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "showPrinterSelectionDialog - Yazıcı Seçim Dialog")
        Log.d(TAG, "========================================")
        
        // Kayıtlı yazıcıları al
        val printers = printerManager.getAllPrinters()
        
        if (printers.isEmpty()) {
            Log.e(TAG, "✗ Kayıtlı yazıcı bulunamadı")
            showToast("Lütfen önce ana ekrandan yazıcı ekleyin")
            callJavaScriptCallback("onPrintError", "Kayıtlı yazıcı bulunamadı")
            return
        }
        
        Log.d(TAG, "${printers.size} yazıcı bulundu")
        
        // Yazıcı isimlerini hazırla
        val printerNames = printers.map { it.getDisplayName() }.toTypedArray()
        
        // Dialog oluştur
        val dialog = AlertDialog.Builder(this)
            .setTitle("Yazıcı Seçin")
            .setItems(printerNames) { dialogInterface, which ->
                val selectedPrinter = printers[which]
                Log.d(TAG, "✓ Yazıcı seçildi: ${selectedPrinter.getDisplayName()}")
                Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
                
                // HTML'e yazıcı bilgisini ekle
                val htmlWithPrinterInfo = addPrinterInfoToHtml(htmlContent, selectedPrinter)
                
                // Native printer dialog'u aç
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    printHtmlContentWithNativeDialog(htmlWithPrinterInfo, "$documentTitle - ${selectedPrinter.getDisplayName()}")
                    showToast("${selectedPrinter.getDisplayName()} için yazdırma başlatılıyor...")
                    callJavaScriptCallback("onPrintSuccess", documentTitle)
                }
                
                dialogInterface.dismiss()
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                Log.d(TAG, "Yazdırma iptal edildi")
                showToast("Yazdırma iptal edildi")
                callJavaScriptCallback("onPrintError", "Kullanıcı iptal etti")
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
        Log.d(TAG, "✓ Yazıcı seçim dialog gösterildi")
        Log.d(TAG, "========================================")
    }
    
    /**
     * HTML içeriğine yazıcı bilgisini ekle
     */
    private fun addPrinterInfoToHtml(htmlContent: String, printer: Printer): String {
        Log.d(TAG, "Adding printer info to HTML: ${printer.getDisplayName()}")
        
        // HTML'in başına yazıcı bilgisini ekle
        val printerHeader = """
            <div style="text-align: center; font-weight: bold; font-size: 14pt; margin-bottom: 10px; border-bottom: 2px solid #000; padding-bottom: 5px;">
                ${printer.getDisplayName()}
            </div>
            <div style="text-align: center; font-size: 10pt; margin-bottom: 10px; color: #666;">
                ${printer.getDetails()}
            </div>
        """.trimIndent()
        
        // Body tag'inden sonra ekle
        val modifiedHtml = if (htmlContent.contains("<body>", ignoreCase = true)) {
            htmlContent.replaceFirst(
                Regex("<body[^>]*>", RegexOption.IGNORE_CASE),
                "$0\n$printerHeader"
            )
        } else {
            // Body tag yoksa başa ekle
            printerHeader + htmlContent
        }
        
        Log.d(TAG, "✓ Printer info added to HTML")
        return modifiedHtml
    }
    
    /**
     * HTML'i basit text'e çevir
     */
    private fun parseHtmlToText(html: String): String {
        // HTML tag'lerini temizle
        var text = html
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("</p>", "\n")
            .replace("</div>", "\n")
            .replace("</h1>", "\n")
            .replace("</h2>", "\n")
            .replace("</h3>", "\n")
            .replace("</tr>", "\n")
            .replace("</li>", "\n")
        
        // Tüm HTML tag'lerini kaldır
        text = text.replace(Regex("<[^>]*>"), "")
        
        // HTML entity'leri decode et
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        
        // Fazla boşlukları temizle
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\n[ \\t]+"), "\n")
        text = text.replace(Regex("[ \\t]+\n"), "\n")
        text = text.replace(Regex("\n{3,}"), "\n\n")
        
        return text.trim()
    }
    
    /**
     * JavaScript callback çağır
     */
    private fun callJavaScriptCallback(functionName: String, data: String) {
        val safeData = data.replace("\"", "\\\"")
        val script = "if(typeof $functionName === 'function') { $functionName('$safeData'); }"
        
        runOnUiThread {
            if (::webView.isInitialized) {
                webView.evaluateJavascript(script, null)
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * PDF'i indir ve yazdır
     */
    private fun downloadAndPrintPdf(pdfUrl: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "downloadAndPrintPdf - PDF İndiriliyor ve Yazdırılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "PDF URL: $pdfUrl")
        
        lifecycleScope.launch {
            try {
                showToast("PDF yazdırılıyor...")
                
                // PDF'i indir
                val pdfData = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    downloadPdf(pdfUrl)
                }
                
                if (pdfData == null) {
                    Log.e(TAG, "✗ PDF indirilemedi")
                    showToast("PDF indirilemedi")
                    return@launch
                }
                
                Log.d(TAG, "✓ PDF indirildi: ${pdfData.size} bytes")
                
                // PDF'i local dosyaya kaydet
                val savedFile = savePdfToFile(pdfData, pdfUrl)
                if (savedFile != null) {
                    Log.d(TAG, "✓ PDF dosyaya kaydedildi: ${savedFile.absolutePath}")
                    Log.d(TAG, "Dosya boyutu: ${savedFile.length()} bytes")
                    Log.d(TAG, "Dosya adı: ${savedFile.name}")
                } else {
                    Log.e(TAG, "✗ PDF dosyaya kaydedilemedi")
                }
                
                // PDF içeriğini logla (hex dump)
                logPdfContent(pdfData)
                
                // PDF'i yazdır
                printPdfData(pdfData)
                
            } catch (e: Exception) {
                Log.e(TAG, "✗ PDF yazdırma hatası", e)
                Log.e(TAG, "Error details: ${e.message}")
                showToast("PDF yazdırma hatası: ${e.message}")
            }
            Log.d(TAG, "========================================")
        }
    }
    
    /**
     * PDF'i URL'den indir
     */
    private suspend fun downloadPdf(urlString: String): ByteArray? {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading PDF from: $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                val responseCode = connection.responseCode
                Log.d(TAG, "HTTP Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream: InputStream = connection.inputStream
                    val outputStream = ByteArrayOutputStream()
                    
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytes = 0
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                    
                    inputStream.close()
                    connection.disconnect()
                    
                    Log.d(TAG, "✓ PDF downloaded successfully: $totalBytes bytes")
                    outputStream.toByteArray()
                } else {
                    Log.e(TAG, "✗ HTTP error: $responseCode")
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Download error", e)
                null
            }
        }
    }
    
    /**
     * PDF verisini yazdır
     */
    private fun printPdfData(pdfData: ByteArray) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printPdfData - PDF Yazdırılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "PDF Size: ${pdfData.size} bytes")
        
        val selectedPrinter = printerManager.getSelectedPrinter()
        
        // Validasyon
        if (selectedPrinter == null) {
            Log.e(TAG, "✗ Yazıcı seçilmemiş")
            showToast("Lütfen bir yazıcı seçin")
            return
        }
        
        Log.d(TAG, "✓ Printer selected")
        Log.d(TAG, "Printer: ${selectedPrinter.getDisplayName()}")
        Log.d(TAG, "IP: ${selectedPrinter.ipAddress}, Port: ${selectedPrinter.port}")
        
        // PDF'i yazdır (ESC/POS formatına çevirerek)
        lifecycleScope.launch {
            try {
                // PDF'ten metin çıkar ve yazdır
                val pdfText = extractTextFromPdf(pdfData)
                
                if (pdfText.isBlank()) {
                    Log.e(TAG, "✗ PDF'ten metin çıkarılamadı")
                    showToast("PDF içeriği okunamadı")
                    return@launch
                }
                
                Log.d(TAG, "✓ PDF text extracted: ${pdfText.length} characters")
                Log.d(TAG, "PDF Content Preview: ${pdfText.take(200)}...")
                
                val repository = com.example.rawbtapp.printer.PrinterRepository()
                val result = repository.printHtmlContent(
                    ipAddress = selectedPrinter.ipAddress,
                    port = selectedPrinter.port,
                    htmlContent = pdfText,
                    title = "PDF Makbuz"
                )
                
                when (result) {
                    is com.example.rawbtapp.printer.PrintResult.Success -> {
                        Log.d(TAG, "✓ Print Success: ${result.message}")
                        showToast("PDF yazdırıldı!")
                    }
                    is com.example.rawbtapp.printer.PrintResult.Error -> {
                        Log.e(TAG, "✗ Print Error: ${result.message}")
                        showToast("Hata: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Print exception", e)
                Log.d(TAG, "Error details: ${e.message}")
                showToast("Yazdırma hatası: ${e.message}")
            }
            Log.d(TAG, "========================================")
        }
    }
    
    /**
     * PDF'ten metin çıkar (basit implementasyon)
     * Not: Gerçek PDF parse için Apache PDFBox veya iText kullanılabilir
     */
    private fun extractTextFromPdf(pdfData: ByteArray): String {
        Log.d(TAG, "Extracting text from PDF...")
        
        try {
            // PDF'i string'e çevir ve temizle
            val pdfString = String(pdfData, Charsets.ISO_8859_1)
            
            // PDF stream'lerini bul ve metin çıkar
            val streamPattern = Regex("stream\\s*(.+?)\\s*endstream", RegexOption.DOT_MATCHES_ALL)
            val matches = streamPattern.findAll(pdfString)
            
            val extractedText = StringBuilder()
            
            for (match in matches) {
                val streamContent = match.groupValues[1]
                
                // Basit metin çıkarma (BT...ET blokları)
                val textPattern = Regex("\\((.+?)\\)", RegexOption.DOT_MATCHES_ALL)
                val textMatches = textPattern.findAll(streamContent)
                
                for (textMatch in textMatches) {
                    val text = textMatch.groupValues[1]
                        .replace("\\\\n", "\n")
                        .replace("\\\\r", "")
                        .replace("\\\\t", "\t")
                    extractedText.append(text).append("\n")
                }
            }
            
            val result = extractedText.toString().trim()
            
            // Eğer metin çıkarılamadıysa, alternatif yöntem
            if (result.isBlank()) {
                Log.d(TAG, "Using alternative text extraction method...")
                return extractTextAlternative(pdfString)
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
            return "PDF İçeriği\n\nPDF başarıyla alındı ve yazdırılıyor.\n\nBoyut: ${pdfData.size} bytes"
        }
    }
    
    /**
     * Alternatif PDF metin çıkarma
     */
    private fun extractTextAlternative(pdfString: String): String {
        val result = StringBuilder()
        
        // Tj ve TJ operatörlerini bul
        val tjPattern = Regex("\\(([^)]+)\\)\\s*Tj")
        val matches = tjPattern.findAll(pdfString)
        
        for (match in matches) {
            val text = match.groupValues[1]
                .replace("\\\\n", "\n")
                .replace("\\\\r", "")
            result.append(text).append("\n")
        }
        
        return result.toString().trim().ifBlank {
            "PDF Makbuz\n\nİçerik otomatik olarak yazdırılıyor..."
        }
    }
    
    /**
     * PDF'i local dosyaya kaydet
     */
    private fun savePdfToFile(pdfData: ByteArray, pdfUrl: String): File? {
        return try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "savePdfToFile - PDF Dosyaya Kaydediliyor")
            Log.d(TAG, "========================================")
            
            // Dosya adını URL'den oluştur
            val fileName = "receipt_${System.currentTimeMillis()}.pdf"
            
            // App'in private storage'ına kaydet
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            Log.d(TAG, "Hedef dosya: ${file.absolutePath}")
            Log.d(TAG, "Dosya boyutu: ${pdfData.size} bytes")
            
            // Dosyayı yaz
            FileOutputStream(file).use { fos ->
                fos.write(pdfData)
                fos.flush()
            }
            
            Log.d(TAG, "✓ PDF başarıyla kaydedildi")
            Log.d(TAG, "Dosya yolu: ${file.absolutePath}")
            Log.d(TAG, "Dosya var mı: ${file.exists()}")
            Log.d(TAG, "Dosya okunabilir mi: ${file.canRead()}")
            Log.d(TAG, "Dosya boyutu: ${file.length()} bytes")
            Log.d(TAG, "========================================")
            
            file
        } catch (e: Exception) {
            Log.e(TAG, "✗ PDF dosyaya kaydedilemedi", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.d(TAG, "========================================")
            null
        }
    }
    
    /**
     * PDF içeriğini detaylı logla
     */
    private fun logPdfContent(pdfData: ByteArray) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "PDF İÇERİK DETAYLARI")
            Log.d(TAG, "========================================")
            
            // Toplam boyut
            Log.d(TAG, "Toplam Boyut: ${pdfData.size} bytes (${pdfData.size / 1024.0} KB)")
            
            // PDF Header
            val header = String(pdfData.take(8).toByteArray(), Charsets.ISO_8859_1)
            Log.d(TAG, "PDF Header: $header")
            
            // PDF Version
            if (header.startsWith("%PDF-")) {
                val version = header.substring(5, 8)
                Log.d(TAG, "PDF Version: $version")
            }
            
            // İlk 200 byte'ı göster
            Log.d(TAG, "\n--- İLK 200 BYTE (RAW) ---")
            val first200 = pdfData.take(200).toByteArray()
            val first200String = String(first200, Charsets.ISO_8859_1)
            Log.d(TAG, first200String)
            
            // Hex dump (ilk 100 byte)
            Log.d(TAG, "\n--- İLK 100 BYTE (HEX DUMP) ---")
            val hexDump = StringBuilder()
            for (i in 0 until minOf(100, pdfData.size)) {
                if (i % 16 == 0) {
                    if (i > 0) hexDump.append("\n")
                    hexDump.append(String.format("%04X: ", i))
                }
                hexDump.append(String.format("%02X ", pdfData[i]))
            }
            Log.d(TAG, hexDump.toString())
            
            // PDF string'e çevir
            val pdfString = String(pdfData, Charsets.ISO_8859_1)
            
            // PDF objelerini say
            val objCount = Regex("\\d+ \\d+ obj").findAll(pdfString).count()
            Log.d(TAG, "\nPDF Obje Sayısı: $objCount")
            
            // Stream sayısı
            val streamCount = Regex("stream").findAll(pdfString).count()
            Log.d(TAG, "Stream Sayısı: $streamCount")
            
            // Font bilgileri
            val fontPattern = Regex("/BaseFont\\s*/([^\\s/>]+)")
            val fonts = fontPattern.findAll(pdfString).map { it.groupValues[1] }.toSet()
            if (fonts.isNotEmpty()) {
                Log.d(TAG, "\nKullanılan Fontlar:")
                fonts.forEach { font ->
                    Log.d(TAG, "  - $font")
                }
            }
            
            // Sayfa sayısı
            val pagePattern = Regex("/Type\\s*/Page[^s]")
            val pageCount = pagePattern.findAll(pdfString).count()
            Log.d(TAG, "\nSayfa Sayısı: $pageCount")
            
            // Metin içeriği (ilk 500 karakter)
            Log.d(TAG, "\n--- ÇIKARILMIŞ METİN İÇERİĞİ (İLK 500 KARAKTER) ---")
            val extractedText = extractTextFromPdf(pdfData)
            val preview = if (extractedText.length > 500) {
                extractedText.substring(0, 500) + "..."
            } else {
                extractedText
            }
            Log.d(TAG, preview)
            
            // Tüm metin içeriği
            Log.d(TAG, "\n--- TAM METİN İÇERİĞİ ---")
            Log.d(TAG, "Toplam Karakter: ${extractedText.length}")
            Log.d(TAG, "Satır Sayısı: ${extractedText.lines().size}")
            Log.d(TAG, "\nTam İçerik:")
            Log.d(TAG, "---BEGIN---")
            Log.d(TAG, extractedText)
            Log.d(TAG, "---END---")
            
            // PDF metadata
            Log.d(TAG, "\n--- PDF METADATA ---")
            val titlePattern = Regex("/Title\\s*\\(([^)]+)\\)")
            val authorPattern = Regex("/Author\\s*\\(([^)]+)\\)")
            val creatorPattern = Regex("/Creator\\s*\\(([^)]+)\\)")
            val producerPattern = Regex("/Producer\\s*\\(([^)]+)\\)")
            
            titlePattern.find(pdfString)?.let {
                Log.d(TAG, "Title: ${it.groupValues[1]}")
            }
            authorPattern.find(pdfString)?.let {
                Log.d(TAG, "Author: ${it.groupValues[1]}")
            }
            creatorPattern.find(pdfString)?.let {
                Log.d(TAG, "Creator: ${it.groupValues[1]}")
            }
            producerPattern.find(pdfString)?.let {
                Log.d(TAG, "Producer: ${it.groupValues[1]}")
            }
            
            Log.d(TAG, "========================================")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ PDF içerik loglama hatası", e)
            Log.e(TAG, "Error details: ${e.message}")
        }
    }
    
    /**
     * Native Android printer dialog'ını aç
     * Android'in kendi PrintManager API'sini kullanır
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun openNativePrinterDialog(documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "openNativePrinterDialog - Native Printer Dialog Açılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Document Title: $documentTitle")
        
        try {
            // PrintManager'i al
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            Log.d(TAG, "PrintManager obtained")
            
            // WebView'in kendi PrintDocumentAdapter'ını kullan
            val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(documentTitle)
            Log.d(TAG, "PrintDocumentAdapter created")
            
            // Print job'ı başlat
            val jobName = "${getString(R.string.app_name)} - $documentTitle"
            Log.d(TAG, "Starting print job: $jobName")
            
            val printJob = printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
            
            Log.d(TAG, "✓ Print job created successfully")
            Log.d(TAG, "Print Job ID: ${printJob.id}")
            Log.d(TAG, "Print Job Label: ${printJob.info?.label}")
            
            // Print job durumunu logla
            when {
                printJob.isCompleted -> Log.d(TAG, "Print job status: COMPLETED")
                printJob.isFailed -> Log.e(TAG, "Print job status: FAILED")
                printJob.isCancelled -> Log.d(TAG, "Print job status: CANCELLED")
                printJob.isStarted -> Log.d(TAG, "Print job status: STARTED")
                printJob.isBlocked -> Log.d(TAG, "Print job status: BLOCKED")
                printJob.isQueued -> Log.d(TAG, "Print job status: QUEUED")
                else -> Log.d(TAG, "Print job status: UNKNOWN")
            }
            
            showToast("Printer selection dialog opened")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error opening native printer dialog", e)
            Log.e(TAG, "Error details: ${e.message}")
            showToast("Error: ${e.message}")
        }
        
        Log.d(TAG, "========================================")
    }
    
    /**
     * HTML içeriğini native printer dialog ile yazdır
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun printHtmlContent(htmlContent: String, documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printHtmlContent - HTML İçerik Yazdırılıyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Document Title: $documentTitle")
        Log.d(TAG, "HTML Length: ${htmlContent.length}")
        
        try {
            // Geçici bir WebView oluştur (sadece print için)
            val printWebView = WebView(this)
            printWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "HTML content loaded, creating print job...")
                    
                    // PrintManager'i al
                    val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                    
                    // PrintDocumentAdapter oluştur
                    val printAdapter = printWebView.createPrintDocumentAdapter(documentTitle)
                    
                    // Print job'ı başlat
                    val jobName = "${getString(R.string.app_name)} - $documentTitle"
                    
                    val printJob = printManager.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder().build()
                    )
                    
                    Log.d(TAG, "✓ Print job created: ${printJob.id}")
                }
            }
            
            // HTML içeriğini yükle
            Log.d(TAG, "Loading HTML content into temporary WebView...")
            printWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error printing HTML content", e)
            Log.e(TAG, "Error details: ${e.message}")
            showToast("Error printing: ${e.message}")
        }
        
        Log.d(TAG, "========================================")
    }
    
    /**
     * HTML içeriğini native printer dialog ile yazdır (POS için)
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun printHtmlContentWithNativeDialog(htmlContent: String, documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printHtmlContentWithNativeDialog - Native Dialog ile Yazdırma")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Document Title: $documentTitle")
        Log.d(TAG, "HTML Length: ${htmlContent.length}")
        
        try {
            // Geçici bir WebView oluştur (sadece print için)
            val printWebView = WebView(this)
            
            // WebView ayarları
            printWebView.settings.apply {
                javaScriptEnabled = false
                blockNetworkLoads = true
            }
            
            printWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "HTML content loaded in temporary WebView")
                    Log.d(TAG, "Creating native print job...")
                    
                    try {
                        // PrintManager'i al
                        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                        Log.d(TAG, "PrintManager obtained")
                        
                        // PrintDocumentAdapter oluştur
                        val printAdapter = printWebView.createPrintDocumentAdapter(documentTitle)
                        Log.d(TAG, "PrintDocumentAdapter created")
                        
                        // Print job'ı başlat
                        val jobName = "${getString(R.string.app_name)} - $documentTitle"
                        Log.d(TAG, "Starting print job: $jobName")
                        
                        val printJob = printManager.print(
                            jobName,
                            printAdapter,
                            PrintAttributes.Builder().build()
                        )
                        
                        Log.d(TAG, "✓ Print job created successfully")
                        Log.d(TAG, "Print Job ID: ${printJob.id}")
                        Log.d(TAG, "Print Job Label: ${printJob.info?.label}")
                        
                        // Print job durumunu logla
                        when {
                            printJob.isCompleted -> Log.d(TAG, "Print job status: COMPLETED")
                            printJob.isFailed -> Log.e(TAG, "Print job status: FAILED")
                            printJob.isCancelled -> Log.d(TAG, "Print job status: CANCELLED")
                            printJob.isStarted -> Log.d(TAG, "Print job status: STARTED")
                            printJob.isBlocked -> Log.d(TAG, "Print job status: BLOCKED")
                            printJob.isQueued -> Log.d(TAG, "Print job status: QUEUED")
                            else -> Log.d(TAG, "Print job status: UNKNOWN")
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Error creating print job", e)
                        Log.e(TAG, "Error details: ${e.message}")
                        showToast("Print job error: ${e.message}")
                    }
                }
                
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "Loading HTML content for printing...")
                }
            }
            
            // HTML içeriğini yükle
            Log.d(TAG, "Loading HTML into temporary WebView...")
            printWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error in printHtmlContentWithNativeDialog", e)
            Log.e(TAG, "Error details: ${e.message}")
            showToast("Error: ${e.message}")
        }
        
        Log.d(TAG, "========================================")
    }
    
    /**
     * Tam HTML dokümantı oluştur (CSS dahil)
     */
    private fun buildFullHtmlDocument(htmlContent: String, cssPath: String, title: String): String {
        Log.d(TAG, "buildFullHtmlDocument - Tam HTML oluşturuluyor")
        Log.d(TAG, "CSS Path: $cssPath")
        
        // CSS içeriğini ekle (eğer varsa)
        val cssLink = if (cssPath.isNotEmpty()) {
            """<link rel="stylesheet" href="$cssPath">"""
        } else {
            ""
        }
        
        // Varsayılan print CSS
        val defaultPrintCss = """
            <style>
                @media print {
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: 'Courier New', monospace;
                        font-size: 12pt;
                    }
                    @page {
                        margin: 0;
                        size: 80mm auto;
                    }
                }
                body {
                    font-family: 'Courier New', monospace;
                    font-size: 12pt;
                    line-height: 1.4;
                    max-width: 80mm;
                    margin: 0 auto;
                    padding: 10px;
                }
                h1, h2, h3 {
                    margin: 10px 0;
                    text-align: center;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                hr {
                    border: none;
                    border-top: 1px dashed #000;
                    margin: 10px 0;
                }
            </style>
        """.trimIndent()
        
        val fullHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                $cssLink
                $defaultPrintCss
            </head>
            <body>
                $htmlContent
            </body>
            </html>
        """.trimIndent()
        
        Log.d(TAG, "✓ Full HTML document created: ${fullHtml.length} chars")
        return fullHtml
    }
    
    /**
     * WebView'in window.print() çağrısını yakala
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun interceptWindowPrint() {
        Log.d(TAG, "Intercepting window.print() calls...")
        
        webView.webChromeClient = object : WebChromeClient() {
            // WebChromeClient ayarlandı - window.print() için JavaScript interface kullanın
        }
        
        Log.d(TAG, "Note: For window.print() support, use JavaScript interface triggerNativePrint()")
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}

/**
 * WebView Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String = "POS Web Sistemi",
    onWebViewCreated: (WebView) -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { context ->
                WebView(context).also { webView ->
                    onWebViewCreated(webView)
                    webView.loadUrl(url)
                }
            }
        )
    }
}
