// ============================================
// 80mm TERMAL YAZICI - HIZLI KULLANIM ÖRNEĞİ
// ============================================

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.example.rawbtapp.printer.ThermalReceiptBuilder

/**
 * Örnek 1: Basit Fiş Yazdırma
 */
fun printSimpleReceipt(context: Context, webView: WebView) {
    val builder = ThermalReceiptBuilder(context)
    
    // Ürünler
    val items = listOf(
        ThermalReceiptBuilder.ReceiptItem(
            name = "Caramel Latte",
            quantity = 1.0,
            unitPrice = 55.0,
            total = 55.0
        ),
        ThermalReceiptBuilder.ReceiptItem(
            name = "Espresso",
            quantity = 2.0,
            unitPrice = 25.0,
            total = 50.0
        )
    )
    
    // Hesaplamalar
    val subtotal = items.sumOf { it.total }
    val tax = subtotal * 0.18
    val grandTotal = subtotal + tax
    
    // Fiş verisi
    val receiptData = ThermalReceiptBuilder.ReceiptData(
        receiptNumber = "12345",
        cashier = "Admin",
        items = items,
        subtotal = subtotal,
        tax = tax,
        taxRate = 18.0,
        discount = 0.0,
        shipping = 0.0,
        grandTotal = grandTotal,
        paymentType = "Nakit",
        amountPaid = 120.0,
        change = 120.0 - grandTotal
    )
    
    // HTML oluştur
    val html = builder.buildReceipt(receiptData)
    
    // WebView'de göster
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    
    // Print et (WebView yüklendikten sonra)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            // window.print() çağır
            view?.evaluateJavascript("window.print();", null)
        }
    }
}

/**
 * Örnek 2: İndirimli Fiş
 */
fun printDiscountedReceipt(context: Context, webView: WebView) {
    val builder = ThermalReceiptBuilder(context)
    
    val items = listOf(
        ThermalReceiptBuilder.ReceiptItem("Kahve", 3.0, 25.0, 75.0),
        ThermalReceiptBuilder.ReceiptItem("Kek", 2.0, 15.0, 30.0)
    )
    
    val subtotal = 105.0
    val tax = subtotal * 0.18
    val discount = 10.0 // %10 indirim
    val grandTotal = subtotal + tax - discount
    
    val receiptData = ThermalReceiptBuilder.ReceiptData(
        receiptNumber = "12346",
        items = items,
        subtotal = subtotal,
        tax = tax,
        discount = discount,
        grandTotal = grandTotal,
        amountPaid = 120.0,
        change = 120.0 - grandTotal
    )
    
    val html = builder.buildReceipt(receiptData)
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

/**
 * Örnek 3: Android PrintManager ile Print
 */
fun printWithPrintManager(context: Context, webView: WebView, receiptNumber: String) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter("Fiş #$receiptNumber")
        
        printManager.print(
            "Fiş #$receiptNumber",
            printAdapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
    }
}

/**
 * Örnek 4: Hazır Sample Fiş
 */
fun printSampleReceipt(context: Context, webView: WebView) {
    val builder = ThermalReceiptBuilder(context)
    
    // Hazır örnek fiş verisi
    val sampleData = ThermalReceiptBuilder.getSampleReceipt()
    
    // HTML oluştur ve göster
    val html = builder.buildReceipt(sampleData)
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

/**
 * Örnek 5: Dinamik Veri ile Fiş
 */
fun printDynamicReceipt(
    context: Context,
    webView: WebView,
    orderItems: List<OrderItem>,
    paymentInfo: PaymentInfo
) {
    val builder = ThermalReceiptBuilder(context)
    
    // OrderItem'ları ReceiptItem'a çevir
    val receiptItems = orderItems.map { item ->
        ThermalReceiptBuilder.ReceiptItem(
            name = item.productName,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            total = item.quantity * item.unitPrice
        )
    }
    
    // Hesaplamalar
    val subtotal = receiptItems.sumOf { it.total }
    val tax = subtotal * (paymentInfo.taxRate / 100.0)
    val grandTotal = subtotal + tax - paymentInfo.discount + paymentInfo.shipping
    
    val receiptData = ThermalReceiptBuilder.ReceiptData(
        receiptNumber = paymentInfo.receiptNumber,
        cashier = paymentInfo.cashierName,
        items = receiptItems,
        subtotal = subtotal,
        tax = tax,
        taxRate = paymentInfo.taxRate,
        discount = paymentInfo.discount,
        shipping = paymentInfo.shipping,
        grandTotal = grandTotal,
        paymentType = paymentInfo.paymentType,
        amountPaid = paymentInfo.amountPaid,
        change = paymentInfo.amountPaid - grandTotal
    )
    
    val html = builder.buildReceipt(receiptData)
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

// ============================================
// YARDIMCI DATA CLASSLAR
// ============================================

data class OrderItem(
    val productName: String,
    val quantity: Double,
    val unitPrice: Double
)

data class PaymentInfo(
    val receiptNumber: String,
    val cashierName: String = "Admin",
    val taxRate: Double = 18.0,
    val discount: Double = 0.0,
    val shipping: Double = 0.0,
    val paymentType: String = "Nakit",
    val amountPaid: Double
)

// ============================================
// WEBVIEW SETUP HELPER
// ============================================

fun setupWebViewForReceipt(webView: WebView) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = false // 80mm için false
        builtInZoomControls = false
        displayZoomControls = false
    }
}

// ============================================
// KULLANIM ÖRNEĞİ - ACTIVITY'DE
// ============================================

/*
class ReceiptActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this)
        setContentView(webView)
        
        // WebView setup
        setupWebViewForReceipt(webView)
        
        // Fiş yazdır
        printSimpleReceipt(this, webView)
        
        // Veya dinamik veri ile
        val items = listOf(
            OrderItem("Kahve", 2.0, 25.0),
            OrderItem("Kek", 1.0, 15.0)
        )
        
        val payment = PaymentInfo(
            receiptNumber = "001",
            cashierName = "Ahmet",
            discount = 5.0,
            amountPaid = 100.0
        )
        
        printDynamicReceipt(this, webView, items, payment)
    }
}
*/
