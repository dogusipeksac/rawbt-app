package com.example.rawbtapp.webview

import android.content.Intent
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
import java.nio.charset.Charset
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
import androidx.compose.material.icons.filled.Settings
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
import com.example.rawbtapp.printer.PrintConstants
import com.example.rawbtapp.printer.TurkishCharacterEncoder
import com.example.rawbtapp.printer.EscPosCommands
import org.json.JSONException
import android.util.Log
import android.webkit.WebSettings
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.print.PrintAttributes
import android.webkit.WebChromeClient
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * WebView ile POS sistemi entegrasyonu
 * JavaScript bridge ile web'den direkt yazdırma
 */
class WebViewActivity : ComponentActivity() {

    private val viewModel: PrinterViewModel by viewModels()
    private lateinit var webView: WebView
    private lateinit var printerManager: PrinterManager

    // Pending print data
    private var pendingHtmlContent: String? = null
    private var pendingDocumentTitle: String? = null

    companion object {
        private const val TAG = "WebViewActivity"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_WEBSITE_NAME = "WEBSITE_NAME"
        const val EXTRA_WEBSITE_URL = "WEBSITE_URL"
    }

    // Yazıcı seçim activity launcher
    private val printerSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                // Önizleme mi yoksa yazdırma mı?
                val isPreview = data.getBooleanExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.ACTION_PREVIEW, false)
                
                if (isPreview) {
                    // Önizleme istendi
                    Log.d(TAG, "Önizleme istendi")
                    if (pendingHtmlContent != null) {
                        showPrintPreview(pendingHtmlContent!!, pendingDocumentTitle ?: "Belge")
                    }
                } else {
                    // Yazdırma istendi - Birden fazla printer desteği
                    val printerIds = data.getStringArrayListExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.RESULT_PRINTER_IDS)

                    if (printerIds != null && printerIds.isNotEmpty()) {
                        Log.d(TAG, "${printerIds.size} yazıcı seçildi")

                        val printers = printerIds.mapNotNull { id ->
                            printerManager.getPrinterById(id)
                        }

                        if (printers.isNotEmpty() && pendingHtmlContent != null && pendingDocumentTitle != null) {
                            printToMultiplePrinters(printers, pendingHtmlContent!!, pendingDocumentTitle!!)
                        pendingHtmlContent = null
                        pendingDocumentTitle = null
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Yazıcı seçimi iptal edildi")
            pendingHtmlContent = null
            pendingDocumentTitle = null
            callJavaScriptCallback("onPrintError", "Kullanıcı iptal etti")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PrinterManager'ı başlat
        printerManager = PrinterManager(this)

        Log.d(TAG, "========================================")
        Log.d(TAG, "WebViewActivity onCreate - SAYFA: POS Web Sistemi")
        Log.d(TAG, "========================================")

        // Intent'ten URL ve isim al
        val websiteName = intent.getStringExtra(EXTRA_WEBSITE_NAME)
        val websiteUrl = intent.getStringExtra(EXTRA_WEBSITE_URL)
        
        // Fallback: Config dosyasından al
        val configUrl = getString(R.string.webview_url)
        val configTitle = getString(R.string.webview_title)
        
        val url = websiteUrl ?: intent.getStringExtra(EXTRA_URL) ?: configUrl
        val title = websiteName ?: configTitle

        Log.d(TAG, "Loading URL: $url")
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
                    onSettingsClick = {
                        openPrinterScreen()
                    },
                    showPreviewDialog = _showPreviewDialog.value,
                    previewContent = _previewContent.value,
                    previewTitle = _previewTitle.value,
                    onPreviewDismiss = {
                        _showPreviewDialog.value = false
                    },
                    onPreviewPrint = {
                        _showPreviewDialog.value = false
                        // Preview'dan yazdır butonuna basıldığında printer seçim ekranını aç
                        if (pendingHtmlContent != null && pendingDocumentTitle != null) {
                            val intent = Intent(this, com.example.rawbtapp.printer.PrinterSelectionActivity::class.java)
                            intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_HTML_CONTENT, pendingHtmlContent)
                            intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_DOCUMENT_TITLE, pendingDocumentTitle)
                            printerSelectionLauncher.launch(intent)
                        }
                    }
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

                // Viewport ve rendering ayarları (mobil uyumluluk için kritik)
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)

                // Initial scale ayarı
                setInitialScale(100)

                // Mixed content (HTTP/HTTPS karışık içerik) desteği
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // User agent - tablet için mobil user agent zorla
                val originalUA = userAgentString ?: ""
                userAgentString = when {
                    // Tablet ise mobil user agent kullan
                    originalUA.contains("Android") && !originalUA.contains("Mobile") -> {
                        // Tablet user agent'ı mobil'e çevir
                        originalUA.replace("Android", "Android Mobile")
                    }
                    // Zaten mobil ise olduğu gibi bırak
                    originalUA.contains("Mobile") -> originalUA
                    // Diğer durumlarda Mobile ekle
                    else -> "$originalUA Mobile"
                }

                // Diğer önemli ayarlar
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false

                // Layout ve rendering iyileştirmeleri
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

                Log.d(
                    TAG,
                    "WebView settings: JS=$jsEnabled, DOM=$domStorageConfig, FileAccess=$fileAccessEnabled"
                )
                Log.d(
                    TAG,
                    "Viewport: useWideViewPort=true, loadWithOverviewMode=true, initialScale=100"
                )
                Log.d(TAG, "Original User Agent: $originalUA")
                Log.d(TAG, "Modified User Agent: $userAgentString")
                Log.d(
                    TAG,
                    "Device Type: ${if (originalUA.contains("Mobile")) "Mobile" else "Tablet"}"
                )
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

            // Hardware acceleration etkinleştir
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            // PDF yakalama için özel WebViewClient
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null

                    // PDF kontrolü
                    if (url.endsWith(".pdf", ignoreCase = true) ||
                        request.requestHeaders["Accept"]?.contains("application/pdf") == true
                    ) {
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // DOMContentLoaded event listener ekleyin
                    val setupScript = """
        (function() {
            var css = `
                .card-body, .row, .col-md-12, .card, .list-item, [class*="list-item"] {
                    visibility: visible !important;
                    opacity: 1 !important;
                }
            `;
            var style = document.createElement('style');
            style.type = 'text/css';
            style.appendChild(document.createTextNode(css));
            document.head.appendChild(style);

            function fixElement(elem) {
                // Zaten islendiye atla (loop onlemeki icin)
                if (elem.dataset.metricsFixed) return;
                
                var computedStyle = window.getComputedStyle(elem);
                var isFlex = elem.classList.contains('d-flex') || 
                             elem.classList.contains('row') || 
                             elem.classList.contains('list-item') ||
                             computedStyle.display === 'flex';
                             
                if (isFlex) {
                    elem.style.setProperty('display', 'flex', 'important');
                    elem.style.setProperty('flex-wrap', 'wrap', 'important');
                } else {
                    elem.style.setProperty('display', 'block', 'important');
                }
                
                elem.style.setProperty('visibility', 'visible', 'important');
                elem.style.setProperty('opacity', '1', 'important');
                
                // Isaretle
                elem.dataset.metricsFixed = 'true';
            }

            function processNode(node) {
                if (node.nodeType === 1) { // ELEMENT_NODE
                    // Hedef element mi?
                    if (node.matches && (node.matches('.card-body') || 
                        node.matches('.row') || 
                        node.matches('.col-md-12') || 
                        node.matches('.card') || 
                        node.matches('.list-item') || 
                        node.matches('[class*="list-item"]'))) {
                        fixElement(node);
                    }
                    
                    // Alt elementleri de kontrol et
                    var children = node.querySelectorAll('.card-body, .row, .col-md-12, .card, .list-item, [class*="list-item"]');
                    children.forEach(fixElement);
                }
            }

            // 1. Mevcut elementleri duzelt
            var existingElements = document.querySelectorAll('.card-body, .row, .col-md-12, .card, .list-item, [class*="list-item"]');
            existingElements.forEach(fixElement);

            // 2. Yeni eklenenleri izle (Dynamic Content / SPA)
            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    mutation.addedNodes.forEach(processNode);
                    
                    // Attribute degisikliklerini de izle (or: class degisimi)
                    if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                        processNode(mutation.target);
                    }
                });
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['class', 'style']
            });
            
            console.log('=== Visibility Fix (MutationObserver) Applied ===');
        })();
    """.trimIndent()

                    webView.evaluateJavascript(setupScript, null)
                }

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
                    Log.d(TAG, "HTML Preview")
                    Log.d(TAG, html)

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
     * Yazıcı seçim activity'sini aç
     */
    private fun showPrinterSelectionDialog(htmlContent: String, documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "showPrinterSelectionDialog - Yazıcı Seçim Activity Açılıyor")
        Log.d(TAG, "========================================")

        // Pending data'yı sakla
        pendingHtmlContent = htmlContent
        pendingDocumentTitle = documentTitle

        // PrinterSelectionActivity'yi aç
        val intent = android.content.Intent(this, com.example.rawbtapp.printer.PrinterSelectionActivity::class.java)
        intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_HTML_CONTENT, htmlContent)
        intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_DOCUMENT_TITLE, documentTitle)
        printerSelectionLauncher.launch(intent)

        Log.d(TAG, "✓ PrinterSelectionActivity başlatıldı")
        Log.d(TAG, "========================================")
    }

    /**
     * Seçilen yazıcı ile yazdırma işlemini gerçekleştir
     */
    private fun printWithSelectedPrinter(printer: Printer, htmlContent: String, documentTitle: String) {
        Log.d(TAG, "printWithSelectedPrinter - ${printer.getDisplayName()}")

        // Direkt WiFi yazıcıya gönder (native dialog yok)
        // Printer bilgisi eklenmez, sadece web'den gelen içerik + logo + footer
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Yazdırma başlatılıyor: ${printer.ipAddress}:${printer.port}")

                // HTML'i ESC/POS komutlarına çevir ve gönder
                // Sadece web'den gelen içerik kullanılır
                val success = sendHtmlToPrinter(
                    htmlContent = htmlContent,
                    printer = printer
                )

                runOnUiThread {
                    if (success) {
                        Log.d(TAG, "✓ Yazdırma başarılı")
                        showToast("✓ Yazdırıldı: ${printer.getDisplayName()}")
                        callJavaScriptCallback("onPrintSuccess", documentTitle)
                    } else {
                        Log.e(TAG, "✗ Yazdırma başarısız")
                        showToast("✗ Yazdırma hatası: ${printer.getDisplayName()}")
                        callJavaScriptCallback("onPrintError", "Yazdırma başarısız")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Yazdırma hatası", e)
                runOnUiThread {
                    showToast("✗ Hata: ${e.message}")
                    callJavaScriptCallback("onPrintError", e.message ?: "Bilinmeyen hata")
                }
            }
        }
    }

    /**
     * Birden fazla yazıcıya yazdırma işlemini gerçekleştir
     */
    private fun printToMultiplePrinters(printers: List<Printer>, htmlContent: String, documentTitle: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "printToMultiplePrinters - ${printers.size} yazıcıya yazdırma başlatılıyor")
        Log.d(TAG, "========================================")

        lifecycleScope.launch {
            var successCount = 0
            var failCount = 0

            printers.forEach { printer ->
                try {
                    Log.d(TAG, "Yazdırma başlatılıyor: ${printer.getDisplayName()} - ${printer.ipAddress}:${printer.port}")

                    val success = sendHtmlToPrinter(
                        htmlContent = htmlContent,
                        printer = printer
                    )

                    if (success) {
                        successCount++
                        Log.d(TAG, "✓ ${printer.getDisplayName()} - Yazdırma başarılı")
                    } else {
                        failCount++
                        Log.e(TAG, "✗ ${printer.getDisplayName()} - Yazdırma başarısız")
                    }

                    // Printerlar arasında kısa bir bekleme
                    kotlinx.coroutines.delay(500)
                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "✗ ${printer.getDisplayName()} - Yazdırma hatası", e)
                }
            }

            runOnUiThread {
                val message = when {
                    failCount == 0 -> {
                        callJavaScriptCallback("onPrintSuccess", documentTitle)
                        "Tüm yazıcılara başarıyla yazdırıldı! ($successCount yazıcı)"
                    }
                    successCount == 0 -> {
                        callJavaScriptCallback("onPrintError", "Hiçbir yazıcıya yazdırılamadı")
                        "Hiçbir yazıcıya yazdırılamadı! ($failCount hata)"
                    }
                    else -> {
                        callJavaScriptCallback("onPrintSuccess", "$successCount başarılı, $failCount başarısız")
                        "Kısmi başarı: $successCount başarılı, $failCount başarısız"
                    }
                }
                showToast(message)
                Log.d(TAG, "========================================")
                Log.d(TAG, "Yazdırma tamamlandı: $message")
                Log.d(TAG, "========================================")
            }
        }
    }

    /**
     * HTML içeriğini yazıcıya gönder
     * Logo ve footer ile birlikte formatlanmış fiş yazdırır
     */
    private suspend fun sendHtmlToPrinter(
        htmlContent: String, 
        printer: Printer
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "sendHtmlToPrinter - YAZDIRMA BAŞLATILIYOR")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Printer: ${printer.name} (${printer.ipAddress}:${printer.port})")
            Log.d(TAG, "Cut paper: ${printer.cutPaper}, Feed lines: ${printer.cutFeedLines}")
            Log.d(TAG, "CHARSET ENCODING: ${printer.charsetEncoding}") // ÖNEMLİ: Encoding log'u
            
            // HTML'den temiz metin çıkar
            var cleanText = TurkishCharacterEncoder.extractTextFromHtml(htmlContent)
            
            // Türkçe karakter iptal et (eğer ayar aktifse)
            if (printer.cancelTurkishChars) {
                cleanText = TurkishCharacterEncoder.cancelTurkishCharacters(cleanText)
                Log.d(TAG, "Turkish characters cancelled (İ→I, ı→i, Ö→O, ö→o, Ü→U, ü→u, Ş→S, ş→s, Ğ→G, ğ→g)")
            }
            
            // Fiş içeriğini oluştur (logo + içerik + footer)
            val receiptContent = buildReceiptContent(cleanText)
            
            // Socket bağlantısı kur
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(printer.ipAddress, printer.port), 5000)
            val outputStream = socket.getOutputStream()

            // 1. ÖNCELİKLE ÇİNCE MODU İPTAL ET ve PRINTER'I İNİTİALİZE ET!
            val initCommands = EscPosCommands.getInitCommands()
            Log.d(TAG, "ESC/POS Init Commands sent (Çince modu iptal + Initialize)")
            outputStream.write(initCommands)
            outputStream.flush()
            
            // Kısa bekleme - printer'ın hazırlanması için
            Thread.sleep(50)
            
            // 2. Encoding ayarla
            val (charsetCommand, charsetName) = EscPosCommands.parseEncoding(printer.charsetEncoding)
            if (charsetCommand.isNotEmpty()) {
                outputStream.write(charsetCommand)
                outputStream.flush()
                Log.d(TAG, "Charset command sent for encoding: ${printer.charsetEncoding}")
            } else {
                Log.d(TAG, "No charset command (NONE_ prefix or PC3846_CP3846)")
            }

            // İçeriği charsetEncoding'e göre encode et
            Log.d(TAG, "Encoding content with: ${printer.charsetEncoding}")
            val encodedContent = when (printer.charsetEncoding) {
                // PC3846_CP3846 eski encoding - artık NONE_CP857 kullanılıyor (karakter seti komutu göndermeden)
                "PC3846_CP3846" -> {
                    // PC3846 komutu Çince karakterlere neden oluyor, bu yüzden karakter seti komutu göndermeden sadece CP857 encoding kullanıyoruz
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        try {
                            receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                        } catch (e2: Exception) {
                            receiptContent.toByteArray(Charsets.UTF_8)
                        }
                    }
                }
                // PC857 kombinasyonları (ESC t 13 - 0x0D) - Standart PC857
                "PC857_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                "PC857_ISO88599" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_Windows1254" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                "PC857_CP852" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP852"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_CP853" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP853"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                // PC857_61 kombinasyonları (ESC t 61 - 0x3D) - Self-test: 61:PC857 Turkish
                "PC857_61_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                "PC857_61_ISO88599" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_61_Windows1254" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_61_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                "PC857_61_CP852" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP852"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC857_61_CP853" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP853"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                // PC850 kombinasyonları (ESC t 2 - 0x02)
                "PC850_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                "PC850_Windows1254" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    }
                }
                "PC850_ISO88599" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    }
                }
                "PC850_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                // PC852 kombinasyonları (ESC t 18 - 0x12) - Central Europe
                "PC852_CP852" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP852"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC852_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                "PC852_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                // PC853 kombinasyonları (ESC t 8 - 0x08) - Turkish
                "PC853_CP853" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP853"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "PC853_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                "PC853_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                // Karakter seti seçimi olmadan encoding'ler
                "NONE_CP857" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    }
                }
                "NONE_CP850" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    }
                }
                "NONE_Windows1254" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("Windows-1254"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP850"))
                    }
                }
                "NONE_ISO88599" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "NONE_CP852" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP852"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                "NONE_CP853" -> {
                    try {
                        receiptContent.toByteArray(Charset.forName("CP853"))
                    } catch (e: Exception) {
                        receiptContent.toByteArray(Charset.forName("CP857"))
                    }
                }
                else -> {
                    // NONE_ prefix'i ile başlayan encoding'ler için direkt encoding kullan
                    if (printer.charsetEncoding.startsWith("NONE_")) {
                        val encodingName = printer.charsetEncoding.substring(5) // "NONE_" kısmını çıkar
                        // ISO-8859-X formatını düzelt
                        val normalizedName = when {
                            encodingName.startsWith("ISO8859") -> {
                                val num = encodingName.substring(7)
                                "ISO-8859-$num"
                            }
                            encodingName.startsWith("IBM") -> {
                                when (encodingName) {
                                    "IBM437" -> "CP437"
                                    "IBM850" -> "CP850"
                                    "IBM857" -> "CP857"
                                    "IBM860" -> "CP860"
                                    "IBM861" -> "CP861"
                                    "IBM862" -> "CP862"
                                    "IBM863" -> "CP863"
                                    "IBM864" -> "CP864"
                                    "IBM865" -> "CP865"
                                    "IBM866" -> "CP866"
                                    "IBM869" -> "CP869"
                                    "IBM00858" -> "CP858"
                                    else -> encodingName
                                }
                            }
                            else -> encodingName
                        }
                        try {
                            receiptContent.toByteArray(Charset.forName(normalizedName))
                        } catch (e: Exception) {
                            receiptContent.toByteArray(Charsets.UTF_8)
                        }
                    } else {
                        // Varsayılan: NONE_CP857 (karakter seti komutu göndermeden sadece CP857 encoding)
                        // PC3846 komutu Çince karakterlere neden oluyor, bu yüzden karakter seti komutu göndermiyoruz
                        try {
                            receiptContent.toByteArray(Charset.forName("CP857"))
                        } catch (e: Exception) {
                            try {
                                receiptContent.toByteArray(Charset.forName("ISO-8859-9"))
                            } catch (e2: Exception) {
                                receiptContent.toByteArray(Charsets.UTF_8)
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Encoded content size: ${encodedContent.size} bytes")
            Log.d(TAG, "Content preview (first 100 chars): ${receiptContent.take(100)}")
            outputStream.write(encodedContent)

            // Kağıdı kes (printer ayarına göre)
            if (printer.cutPaper) {
                // Kesme öncesi boşluk ver
                if (printer.cutFeedLines > 0) {
                    // ESC d n - n satır besle
                    outputStream.write(byteArrayOf(0x1B, 0x64, printer.cutFeedLines.toByte()))
                    Log.d(TAG, "Feed ${printer.cutFeedLines} lines before cut")
                }

                // Tam kesim
                outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))  // GS V 0
                Log.d(TAG, "Cut paper command sent")
            } else {
                Log.d(TAG, "Cut paper disabled")
            }

            outputStream.flush()
            outputStream.close()
            socket.close()

            Log.d(TAG, "✓ Print successful with encoding: ${printer.charsetEncoding}")
            Log.d(TAG, "========================================")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Printer connection error", e)
            false
        }
    }
    
    /**
     * Fiş içeriğini oluştur (logo + içerik + footer)
     */
    private fun buildReceiptContent(content: String): String {
        val receipt = StringBuilder()
        
        // Logo ekle
        if (PrintConstants.SHOW_LOGO) {
            receipt.append(PrintConstants.getFormattedLogo())
        }
        
        // İçerik ekle
        receipt.append(content)
        
        // Footer ekle
        if (PrintConstants.SHOW_FOOTER) {
            receipt.append(PrintConstants.getFormattedFooter())
        }
        
        return receipt.toString()
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
     * TurkishCharacterEncoder kullanarak Türkçe karakterleri korur
     */
    private fun parseHtmlToText(html: String): String {
        return TurkishCharacterEncoder.extractTextFromHtml(html)
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
                    title = "PDF Makbuz",
                    cutPaper = selectedPrinter.cutPaper,
                    cutFeedLines = selectedPrinter.cutFeedLines,
                    charsetEncoding = selectedPrinter.charsetEncoding,
                    cancelTurkishChars = selectedPrinter.cancelTurkishChars
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
            // PDF'i string'e çevir - UTF-8 desteği için
            // PDF genelde Latin-1 encoding kullanır ama Türkçe karakterler için özel işlem gerekir
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
                    var text = textMatch.groupValues[1]
                        .replace("\\\\n", "\n")
                        .replace("\\\\r", "")
                        .replace("\\\\t", "\t")
                    
                    // PDF'deki Unicode escape sequence'leri decode et
                    text = decodePdfUnicode(text)
                    
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
     * PDF Unicode escape sequence'lerini decode et
     * TurkishCharacterEncoder kullanarak Türkçe karakterleri korur
     */
    private fun decodePdfUnicode(text: String): String {
        return TurkishCharacterEncoder.decodePdfUnicode(text)
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
            var text = match.groupValues[1]
                .replace("\\\\n", "\n")
                .replace("\\\\r", "")
            
            // Unicode decode
            text = decodePdfUnicode(text)
            
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

            // HTML içeriğini yükle - UTF-8 encoding ile
            Log.d(TAG, "Loading HTML content into temporary WebView...")
            printWebView.loadDataWithBaseURL(null, htmlContent, "text/html; charset=UTF-8", "UTF-8", null)

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

            // HTML içeriğini yükle - UTF-8 encoding ile
            Log.d(TAG, "Loading HTML into temporary WebView...")
            printWebView.loadDataWithBaseURL(null, htmlContent, "text/html; charset=UTF-8", "UTF-8", null)

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
        // PrintConstants'tan CSS stillerini al
        val defaultPrintCss = "<style>\n${PrintConstants.HTML_PRINT_CSS}\n</style>"

        val fullHtml = """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
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
            // Console mesajlarını yakala (debugging için)
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val level = when (msg.messageLevel()) {
                        android.webkit.ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                        android.webkit.ConsoleMessage.MessageLevel.WARNING -> "WARN"
                        android.webkit.ConsoleMessage.MessageLevel.LOG -> "LOG"
                        android.webkit.ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                        android.webkit.ConsoleMessage.MessageLevel.TIP -> "TIP"
                        else -> "INFO"
                    }
                    Log.d(TAG, "WebView Console [$level]: ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
                }
                return true
            }

            // Progress bar için (opsiyonel)
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress % 25 == 0) {
                    Log.d(TAG, "Page loading progress: $newProgress%")
                }
            }
        }

        Log.d(TAG, "Note: For window.print() support, use JavaScript interface triggerNativePrint()")
    }

    // Preview dialog state
    private val _showPreviewDialog = mutableStateOf(false)
    private val _previewContent = mutableStateOf("")
    private val _previewTitle = mutableStateOf("")

    /**
     * Önizleme göster - Dialog olarak mevcut ekranın üstünde
     */
    private fun showPrintPreview(htmlContent: String, title: String) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "showPrintPreview - Önizleme Gösteriliyor")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "HTML length: ${htmlContent.length}")
        
        try {
            // HTML içeriğini wrapper ile hazırla
            val fullHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$title</title>
                    <style>
                        body {
                            margin: ${PrintConstants.WEB_CONTENT_TOP_SPACING}em 0 ${PrintConstants.WEB_CONTENT_BOTTOM_SPACING}em 0;
                            padding: 0;
                            font-family: Arial, sans-serif;
                        }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
            """.trimIndent()
            
            // Dialog state'ini güncelle
            _previewContent.value = fullHtml
            _previewTitle.value = title
            _showPreviewDialog.value = true

            Log.d(TAG, "✓ Preview dialog opened")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing print preview", e)
            Toast.makeText(this, "Önizleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Printer yönetim ekranını aç
     */
    private fun openPrinterScreen() {
        Log.d(TAG, "Opening PrinterSelectionActivity from settings button")
        val intent = Intent(this, com.example.rawbtapp.printer.PrinterSelectionActivity::class.java)
        intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_HTML_CONTENT, "")
        intent.putExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.EXTRA_DOCUMENT_TITLE, "Printer Yönetimi")
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Sadece WebView history'de geri git, Activity'yi kapatma
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
            Log.d(TAG, "WebView navigated back")
        } else {
            Log.d(TAG, "WebView at first page, staying in WebView")
            // Activity'yi kapatma, WebView'de kal
        }
    }
}

/**
 * WebView Compose UI - Tam ekran, geri butonu yok
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String = "POS Web Sistemi",
    onWebViewCreated: (WebView) -> Unit,
    onSettingsClick: () -> Unit = {},
    showPreviewDialog: Boolean = false,
    previewContent: String = "",
    previewTitle: String = "",
    onPreviewDismiss: () -> Unit = {},
    onPreviewPrint: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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

            // Preview Dialog
            if (showPreviewDialog) {
                com.example.rawbtapp.ui.PrintPreviewDialog(
                    content = previewContent,
                    onDismiss = onPreviewDismiss,
                    onPrint = onPreviewPrint,
                    showRawData = false
                )
            }
        }
    }
}
