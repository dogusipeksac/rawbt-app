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
import com.example.rawbtapp.printer.PrintConstants
import com.example.rawbtapp.printer.TurkishCharacterEncoder
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
    }

    // Yazıcı seçim activity launcher
    private val printerSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val printerName = data.getStringExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.RESULT_PRINTER_NAME) ?: ""
                val printerNumber = data.getStringExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.RESULT_PRINTER_NUMBER) ?: "1"
                val printerIp = data.getStringExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.RESULT_PRINTER_IP) ?: ""
                val printerPort = data.getIntExtra(com.example.rawbtapp.printer.PrinterSelectionActivity.RESULT_PRINTER_PORT, 9100)

                Log.d(TAG, "Yazıcı seçildi: #$printerNumber - $printerName")

                // Printer objesi oluştur
                val printer = Printer(
                    id = "",
                    name = printerName,
                    number = printerNumber,
                    ipAddress = printerIp,
                    port = printerPort
                )

                // Pending data varsa yazdır
                if (pendingHtmlContent != null && pendingDocumentTitle != null) {
                    printWithSelectedPrinter(printer, pendingHtmlContent!!, pendingDocumentTitle!!)
                    pendingHtmlContent = null
                    pendingDocumentTitle = null
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

                Log.d(TAG, "WebView settings: JS=$jsEnabled, DOM=$domStorageConfig, FileAccess=$fileAccessEnabled")
                Log.d(TAG, "Viewport: useWideViewPort=true, loadWithOverviewMode=true, initialScale=100")
                Log.d(TAG, "Original User Agent: $originalUA")
                Log.d(TAG, "Modified User Agent: $userAgentString")
                Log.d(TAG, "Device Type: ${if (originalUA.contains("Mobile")) "Mobile" else "Tablet"}")
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")

                    // Tablet için mobil mod zorla + CSS/Layout fix
                    view?.evaluateJavascript("""
                        (function() {
                            console.log('=== WebView Tablet Fix Starting ===');
                            console.log('Window dimensions:', window.innerWidth, 'x', window.innerHeight);
                            console.log('Screen dimensions:', screen.width, 'x', screen.height);
                            console.log('Device pixel ratio:', window.devicePixelRatio);
                            
                            // Viewport meta tag ekle/güncelle
                            var viewport = document.querySelector('meta[name=viewport]');
                            if (!viewport) {
                                viewport = document.createElement('meta');
                                viewport.name = 'viewport';
                                document.head.appendChild(viewport);
                            }
                            viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                            console.log('✓ Viewport updated');
                            
                            // Touch event desteğini zorla
                            if (!('ontouchstart' in window)) {
                                window.ontouchstart = function() {};
                            }
                            console.log('✓ Touch events enabled');
                            
                            // Mobil cihaz olduğumuzu belirt
                            window.isMobile = true;
                            window.isTablet = true;
                            
                            // Screen size override (tablet'i telefon gibi göster)
                            Object.defineProperty(window.screen, 'width', { 
                                get: function() { return 412; } 
                            });
                            Object.defineProperty(window.screen, 'height', { 
                                get: function() { return 915; } 
                            });
                            console.log('✓ Screen size overridden to mobile');
                            
                            // CSS Media Query Override - Tablet için mobil stilleri zorla
                            var style = document.createElement('style');
                            style.id = 'webview-tablet-fix';
                            style.innerHTML = `
                                /* Tablet için mobil layout zorla */
                                @media screen and (min-width: 600px) {
                                    /* Tüm grid/flex layout'ları tek sütun yap */
                                    [class*="grid"],
                                    [class*="Grid"],
                                    [class*="row"],
                                    [class*="Row"],
                                    [class*="col"],
                                    [class*="Col"],
                                    [class*="container"],
                                    [class*="Container"] {
                                        display: block !important;
                                        width: 100% !important;
                                        max-width: 100% !important;
                                        grid-template-columns: 1fr !important;
                                        flex-direction: column !important;
                                    }
                                    
                                    /* Ürün listesi görünür olsun */
                                    [class*="product"],
                                    [class*="Product"],
                                    [class*="item"],
                                    [class*="Item"],
                                    [class*="list"],
                                    [class*="List"] {
                                        display: block !important;
                                        visibility: visible !important;
                                        opacity: 1 !important;
                                        height: auto !important;
                                        overflow: visible !important;
                                    }
                                    
                                    /* Hidden sınıflarını override et */
                                    .hidden,
                                    .d-none,
                                    [hidden] {
                                        display: block !important;
                                    }
                                    
                                    /* Mobil için gizlenmiş öğeleri göster */
                                    .mobile-only,
                                    .show-mobile {
                                        display: block !important;
                                    }
                                    
                                    /* Desktop için gösterilenleri gizleme */
                                    .desktop-only,
                                    .hide-mobile {
                                        display: none !important;
                                    }
                                }
                                
                                /* Tüm breakpoint'lerde geçerli */
                                body {
                                    max-width: 100vw !important;
                                    overflow-x: hidden !important;
                                }
                            `;
                            document.head.appendChild(style);
                            console.log('✓ CSS media query override injected');
                            
                            // JavaScript conditional rendering fix
                            // matchMedia'yı override et (tablet'i mobil gibi göster)
                            var originalMatchMedia = window.matchMedia;
                            window.matchMedia = function(query) {
                                console.log('matchMedia called with:', query);
                                
                                // Tablet breakpoint'lerini mobil olarak döndür
                                if (query.includes('min-width') && 
                                    (query.includes('768px') || query.includes('600px') || 
                                     query.includes('1024px') || query.includes('900px'))) {
                                    console.log('→ Overriding to mobile (false)');
                                    return { matches: false, media: query };
                                }
                                
                                // Mobil breakpoint'leri true döndür
                                if (query.includes('max-width') && 
                                    (query.includes('767px') || query.includes('599px'))) {
                                    console.log('→ Overriding to mobile (true)');
                                    return { matches: true, media: query };
                                }
                                
                                return originalMatchMedia.call(window, query);
                            };
                            console.log('✓ matchMedia overridden');
                            
                            // DOM'da gizli ürünleri bul ve göster
                            setTimeout(function() {
                                console.log('--- Searching for hidden products ---');
                                
                                // Yaygın ürün selector'ları
                                var selectors = [
                                    '[class*="product"]',
                                    '[class*="Product"]',
                                    '[class*="item"]',
                                    '[class*="Item"]',
                                    '[class*="card"]',
                                    '[class*="Card"]',
                                    '[data-product]',
                                    '[data-item]'
                                ];
                                
                                selectors.forEach(function(selector) {
                                    var elements = document.querySelectorAll(selector);
                                    console.log('Found', elements.length, 'elements for:', selector);
                                    
                                    elements.forEach(function(el) {
                                        var computed = window.getComputedStyle(el);
                                        if (computed.display === 'none' || 
                                            computed.visibility === 'hidden' ||
                                            computed.opacity === '0') {
                                            console.log('→ Showing hidden element:', el.className);
                                            el.style.display = 'block';
                                            el.style.visibility = 'visible';
                                            el.style.opacity = '1';
                                        }
                                    });
                                });
                                
                                console.log('✓ Hidden products revealed');
                            }, 500);
                            
                            console.log('=== WebView Tablet Fix Complete ===');
                        })();
                    """.trimIndent(), null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.e(TAG, "WebView error: ${error?.description} (${error?.errorCode})")
                        Log.e(TAG, "Failed URL: ${request?.url}")
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e(TAG, "HTTP error: ${errorResponse?.statusCode} - ${request?.url}")
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

        // HTML'e yazıcı bilgisini ekle
        val htmlWithPrinterInfo = addPrinterInfoToHtml(htmlContent, printer)

        // Direkt WiFi yazıcıya gönder (native dialog yok)
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Yazdırma başlatılıyor: ${printer.ipAddress}:${printer.port}")

                // HTML'i ESC/POS komutlarına çevir ve gönder
                val success = sendHtmlToPrinter(
                    htmlContent = htmlWithPrinterInfo,
                    ipAddress = printer.ipAddress,
                    port = printer.port
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
     * HTML içeriğini yazıcıya gönder
     * Logo ve footer ile birlikte formatlanmış fiş yazdırır
     */
    private suspend fun sendHtmlToPrinter(
        htmlContent: String, 
        ipAddress: String, 
        port: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending HTML to printer: $ipAddress:$port")
            
            // HTML'den temiz metin çıkar
            val cleanText = TurkishCharacterEncoder.extractTextFromHtml(htmlContent)
            
            // Fiş içeriğini oluştur (logo + içerik + footer)
            val receiptContent = buildReceiptContent(cleanText)
            
            // Socket bağlantısı kur
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ipAddress, port), 5000)
            val outputStream = socket.getOutputStream()

            // ESC/POS başlatma komutları (Türkçe karakter desteği ile)
            outputStream.write(TurkishCharacterEncoder.getEscPosInitCommands())
            
            // İçeriği Türkçe karakter desteği ile gönder
            outputStream.write(TurkishCharacterEncoder.encodeForPrinter(receiptContent))
            
            // Kağıdı kes
            outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))

            outputStream.flush()
            outputStream.close()
            socket.close()

            Log.d(TAG, "✓ Print successful")
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
