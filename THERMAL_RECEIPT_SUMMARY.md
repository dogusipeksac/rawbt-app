# 🖨️ 80mm Termal Yazıcı Sistemi - Özet

## ✅ Oluşturulan Dosyalar

### **1. HTML Template**:
```
📄 app/src/main/assets/thermal-receipt-template.html
```
- 80mm termal yazıcı optimize
- Siyah-beyaz
- @media print kuralları
- Monospace font
- Tüm detaylar (ürün, tax, discount, shipping, payment)

---

### **2. Kotlin Builder**:
```
📄 app/src/main/java/com/example/rawbtapp/printer/ThermalReceiptBuilder.kt
```
- Dinamik fiş oluşturma
- Data class'lar
- Format helper'lar
- Sample receipt

---

### **3. Dokümantasyon**:
```
📄 THERMAL_RECEIPT_GUIDE.md - Detaylı kılavuz
📄 THERMAL_RECEIPT_EXAMPLE.kt - Kullanım örnekleri
📄 THERMAL_RECEIPT_SUMMARY.md - Bu dosya
```

---

## 🎯 Hızlı Başlangıç

### **1. Builder Oluştur**:
```kotlin
val builder = ThermalReceiptBuilder(context)
```

### **2. Fiş Verisi Hazırla**:
```kotlin
val items = listOf(
    ThermalReceiptBuilder.ReceiptItem("Kahve", 1.0, 25.0, 25.0)
)

val receiptData = ThermalReceiptBuilder.ReceiptData(
    receiptNumber = "12345",
    items = items,
    subtotal = 25.0,
    tax = 4.5,
    grandTotal = 29.5,
    amountPaid = 30.0,
    change = 0.5
)
```

### **3. HTML Oluştur**:
```kotlin
val html = builder.buildReceipt(receiptData)
```

### **4. WebView'de Göster**:
```kotlin
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

### **5. Print Et**:
```kotlin
webView.evaluateJavascript("window.print();", null)
```

---

## 📐 Teknik Özellikler

### **Boyutlar**:
- **Genişlik**: 80mm (384px @ 120dpi)
- **Yükseklik**: Otomatik (içeriğe göre)
- **Padding**: 3mm (print), 10px (screen)

### **Font**:
- **Family**: Courier New, monospace
- **Size**: 12px (body), 16px (logo), 14px (total)
- **Line Height**: 1.4

### **Renkler**:
- **Arka plan**: #fff (beyaz)
- **Metin**: #000 (siyah)
- **Border**: #000 (siyah)

---

## 🎨 Template Bölümleri

### **1. Header**:
```
┌─────────────────────────────┐
│     7 DAYS HAVACILIK        │
│  Stok Yönetim Sistemi       │
│  Tel: 0212 XXX XX XX        │
│  İstanbul, Türkiye          │
├─────────────────────────────┤
```

### **2. Receipt Info**:
```
Fiş No:     #12345
Tarih:      03.12.2025 18:30
Kasiyer:    Admin
```

### **3. Items**:
```
Ürünler
─────────────────────────────
Caramel Latte
1.00 Ad x 55.00 TL      55.00 TL

Espresso
2.00 Ad x 25.00 TL      50.00 TL
```

### **4. Totals**:
```
─────────────────────────────
Ara Toplam:           150.00 TL
KDV (%18):             27.00 TL
İndirim:              -10.00 TL
═════════════════════════════
GENEL TOPLAM:         167.00 TL
═════════════════════════════
```

### **5. Payment**:
```
Ödeme Bilgileri
─────────────────────────────
Ödeme Türü:              Nakit
Ödenen:              200.00 TL
Para Üstü:            33.00 TL
```

### **6. Footer**:
```
─────────────────────────────
Bizi tercih ettiğiniz için
teşekkür ederiz!

www.7dayshavacilik.com
İade ve değişim için 14 gün
içinde fişinizi saklayınız.
```

---

## 🖨️ Print Optimizasyonu

### **@media print Kuralları**:

```css
@media print {
    /* Sayfa boyutu */
    @page {
        size: 80mm auto;
        margin: 0;
    }

    /* Siyah-beyaz zorunlu */
    * {
        color: #000 !important;
        background: #fff !important;
        -webkit-print-color-adjust: exact;
    }

    /* Sayfa kırılmalarını önle */
    .receipt, .section, table {
        page-break-inside: avoid;
    }
}
```

---

## 💡 Kullanım Örnekleri

### **Örnek 1: Basit Fiş**:
```kotlin
val builder = ThermalReceiptBuilder(context)
val html = builder.buildReceipt(
    ThermalReceiptBuilder.getSampleReceipt()
)
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

---

### **Örnek 2: Özel Fiş**:
```kotlin
val items = listOf(
    ReceiptItem("Kahve", 2.0, 25.0, 50.0),
    ReceiptItem("Kek", 1.0, 15.0, 15.0)
)

val data = ReceiptData(
    receiptNumber = "001",
    items = items,
    subtotal = 65.0,
    tax = 11.7,
    discount = 5.0,
    grandTotal = 71.7,
    amountPaid = 75.0,
    change = 3.3
)

val html = builder.buildReceipt(data)
```

---

### **Örnek 3: Print Manager**:
```kotlin
val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
val printAdapter = webView.createPrintDocumentAdapter("Fiş")

printManager.print(
    "Fiş #12345",
    printAdapter,
    PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
)
```

---

## ✅ Özellikler

### **Template**:
- ✅ 80mm kağıt genişliği
- ✅ Monospace font
- ✅ Siyah-beyaz optimizasyon
- ✅ @media print kuralları
- ✅ WebView = Print (aynı görünüm)
- ✅ Tüm detaylar görünür
- ✅ Barcode desteği
- ✅ Özelleştirilebilir

### **Builder**:
- ✅ Kotlin data class
- ✅ Dinamik içerik
- ✅ Format helper'lar
- ✅ Sample data
- ✅ Kolay kullanım
- ✅ Type-safe

### **Print**:
- ✅ window.print() desteği
- ✅ Android PrintManager
- ✅ Sayfa kırılması yok
- ✅ Siyah-beyaz zorunlu
- ✅ Optimize edilmiş

---

## 📊 Veri Yapısı

### **ReceiptItem**:
```kotlin
data class ReceiptItem(
    val name: String,        // Ürün adı
    val quantity: Double,    // Miktar
    val unitPrice: Double,   // Birim fiyat
    val total: Double        // Toplam
)
```

### **ReceiptData**:
```kotlin
data class ReceiptData(
    val receiptNumber: String,  // Fiş no
    val cashier: String,        // Kasiyer
    val items: List<ReceiptItem>, // Ürünler
    val subtotal: Double,       // Ara toplam
    val tax: Double,            // KDV
    val taxRate: Double,        // KDV oranı
    val discount: Double,       // İndirim
    val shipping: Double,       // Kargo
    val grandTotal: Double,     // Genel toplam
    val paymentType: String,    // Ödeme türü
    val amountPaid: Double,     // Ödenen
    val change: Double          // Para üstü
)
```

---

## 🔧 WebView Setup

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = false  // 80mm için false
}
```

---

## 📱 Test Adımları

### **1. Ekranda Görüntüle**:
```kotlin
val html = builder.buildReceipt(data)
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

### **2. Print Preview**:
```kotlin
webView.evaluateJavascript("window.print();", null)
```

### **3. Kontrol Et**:
- [ ] Genişlik 80mm
- [ ] Tüm detaylar görünür
- [ ] Font monospace
- [ ] Siyah-beyaz
- [ ] Border'lar düzgün
- [ ] Spacing doğru

---

## 🎯 Avantajlar

### **1. Optimize Edilmiş**:
- ✅ 80mm termal yazıcı için özel
- ✅ Minimum kağıt israfı
- ✅ Hızlı yazdırma

### **2. Esnek**:
- ✅ Dinamik içerik
- ✅ Özelleştirilebilir
- ✅ Kolay entegrasyon

### **3. Profesyonel**:
- ✅ Temiz tasarım
- ✅ Tüm detaylar
- ✅ Barcode desteği

### **4. Güvenilir**:
- ✅ @media print kuralları
- ✅ WebView = Print
- ✅ Sayfa kırılması yok

---

## 📞 Özet

✅ **Template**: `thermal-receipt-template.html`  
✅ **Builder**: `ThermalReceiptBuilder.kt`  
✅ **Genişlik**: 80mm (384px)  
✅ **Font**: Monospace  
✅ **Renk**: Siyah-beyaz  
✅ **Print**: @media print optimize  
✅ **WebView**: Render = Print  
✅ **Detaylar**: Tam  
✅ **Build**: Başarılı ✅  

**Kullanım**:
```kotlin
val builder = ThermalReceiptBuilder(context)
val html = builder.buildReceipt(data)
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
webView.evaluateJavascript("window.print();", null)
```

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 1.0 (Thermal Receipt System)
