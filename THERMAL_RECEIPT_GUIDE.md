# 🖨️ 80mm Termal Yazıcı HTML Template Kılavuzu

## 📋 Genel Bakış

Bu template, 80mm termal yazıcılar için optimize edilmiş, siyah-beyaz HTML fiş sistemidir.

### ✅ Özellikler:
- **80mm kağıt genişliği** (384px @ 120dpi)
- **Monospace font** (Courier New)
- **Siyah-beyaz** optimizasyon
- **@media print** kuralları ile print optimizasyonu
- **WebView render** = **Print çıktısı** (aynı görünüm)
- **Tüm detaylar**: Ürünler, KDV, İndirim, Kargo, Toplam, Ödeme

---

## 📐 Teknik Spesifikasyonlar

### **Kağıt Boyutları**:
```
Genişlik: 80mm = 384px (120dpi)
Yükseklik: Otomatik (içeriğe göre)
Margin: 0
Padding: 3mm (print), 10px (screen)
```

### **Font Ayarları**:
```css
font-family: 'Courier New', Courier, monospace;
font-size: 12px (body)
line-height: 1.4
```

### **Renkler**:
```css
Arka plan: #fff (beyaz)
Metin: #000 (siyah)
Border: #000 (siyah)
```

---

## 🎨 Template Yapısı

### **1. Header (Başlık)**:
```html
<div class="header">
    <div class="logo">7 DAYS HAVACILIK</div>
    <div class="store-info">
        Stok Yönetim Sistemi<br>
        Tel: 0212 XXX XX XX<br>
        İstanbul, Türkiye
    </div>
</div>
```

**Stil**:
- Logo: 16px, bold, letter-spacing: 2px
- Store info: 10px, line-height: 1.3
- Border-bottom: 2px dashed

---

### **2. Receipt Info (Fiş Bilgileri)**:
```html
<table>
    <tr>
        <td><strong>Fiş No:</strong></td>
        <td class="align-right">#12345</td>
    </tr>
    <tr>
        <td><strong>Tarih:</strong></td>
        <td class="align-right">03.12.2025 18:30</td>
    </tr>
    <tr>
        <td><strong>Kasiyer:</strong></td>
        <td class="align-right">Admin</td>
    </tr>
</table>
```

---

### **3. Items (Ürünler)**:
```html
<div class="section">
    <div class="section-title">Ürünler</div>
    <table>
        <tr>
            <td colspan="3" class="item-name">Caramel Latte</td>
        </tr>
        <tr>
            <td class="item-details">1.00 Ad x 55.00 TL</td>
            <td></td>
            <td class="align-right"><strong>55.00 TL</strong></td>
        </tr>
    </table>
</div>
```

**Format**:
- Ürün adı: Bold, 12px
- Detay: 11px (miktar x fiyat)
- Toplam: Bold, sağa hizalı

---

### **4. Totals (Toplamlar)**:
```html
<div class="totals">
    <table>
        <tr>
            <td class="label">Ara Toplam:</td>
            <td class="value">150.00 TL</td>
        </tr>
        <tr>
            <td class="label">KDV (%18):</td>
            <td class="value">27.00 TL</td>
        </tr>
        <tr>
            <td class="label">İndirim:</td>
            <td class="value">-10.00 TL</td>
        </tr>
        <tr class="grand-total">
            <td class="label">GENEL TOPLAM:</td>
            <td class="value">167.00 TL</td>
        </tr>
    </table>
</div>
```

**Grand Total**:
- Font: 14px, bold
- Border: 2px solid (üst ve alt)
- Padding: 5px

---

### **5. Payment (Ödeme)**:
```html
<div class="payment">
    <div class="section-title">Ödeme Bilgileri</div>
    <table>
        <tr>
            <td><strong>Ödeme Türü:</strong></td>
            <td class="align-right">Nakit</td>
        </tr>
        <tr>
            <td><strong>Ödenen:</strong></td>
            <td class="align-right">200.00 TL</td>
        </tr>
        <tr>
            <td><strong>Para Üstü:</strong></td>
            <td class="align-right">33.00 TL</td>
        </tr>
    </table>
</div>
```

---

### **6. Footer (Alt Bilgi)**:
```html
<div class="footer">
    <div>
        <strong>Bizi tercih ettiğiniz için<br>teşekkür ederiz!</strong>
    </div>
    <div style="font-size: 9px;">
        www.7dayshavacilik.com<br>
        İade ve değişim için 14 gün içinde<br>
        fişinizi saklayınız.
    </div>
</div>
```

---

## 🖨️ Print Media Queries

### **Kritik CSS**:
```css
@media print {
    /* Sayfa boyutu */
    @page {
        size: 80mm auto;
        margin: 0;
    }

    /* Body ayarları */
    body {
        width: 80mm;
        max-width: 80mm;
        margin: 0;
        padding: 3mm;
    }

    /* Siyah-beyaz zorunlu */
    * {
        color: #000 !important;
        background: #fff !important;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
    }

    /* Sayfa kırılmalarını önle */
    .receipt,
    .section,
    table,
    tr,
    td {
        page-break-inside: avoid;
    }
}
```

---

## 💻 Kotlin Kullanımı

### **1. ThermalReceiptBuilder Kullanımı**:

```kotlin
// Receipt builder oluştur
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

// Fiş verisi
val receiptData = ThermalReceiptBuilder.ReceiptData(
    receiptNumber = "12345",
    cashier = "Admin",
    items = items,
    subtotal = 105.0,
    tax = 18.9,
    taxRate = 18.0,
    discount = 5.0,
    shipping = 0.0,
    grandTotal = 118.9,
    paymentType = "Nakit",
    amountPaid = 120.0,
    change = 1.1
)

// HTML oluştur
val html = builder.buildReceipt(receiptData)

// WebView'de göster
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

---

### **2. Örnek Fiş**:

```kotlin
// Hazır örnek fiş
val sampleData = ThermalReceiptBuilder.getSampleReceipt()
val html = builder.buildReceipt(sampleData)
```

---

## 🌐 WebView Entegrasyonu

### **WebView Setup**:

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = false // 80mm için false
}

// HTML yükle
val html = builder.buildReceipt(receiptData)
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

---

### **Print Trigger**:

```kotlin
// JavaScript'ten print
webView.evaluateJavascript("window.print();", null)

// Veya Android PrintManager
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter("Fiş")
    
    printManager.print(
        "Fiş #12345",
        printAdapter,
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    )
}
```

---

## 📱 Ekran vs Print Karşılaştırma

### **Ekranda (Screen)**:
```
Width: 384px
Padding: 10px
Background: #fff
Print button: Görünür
```

### **Print'te**:
```
Width: 80mm
Padding: 3mm
Background: #fff (forced)
Print button: Gizli (.no-print)
```

### **Aynı Görünüm Garantisi**:
- ✅ Font boyutları aynı
- ✅ Layout aynı
- ✅ Border'lar aynı
- ✅ Spacing aynı
- ✅ Sadece padding değişir (10px → 3mm)

---

## 🎯 Kullanım Senaryoları

### **Senaryo 1: Basit Fiş**:
```kotlin
val items = listOf(
    ReceiptItem("Kahve", 1.0, 25.0, 25.0)
)

val data = ReceiptData(
    receiptNumber = "001",
    items = items,
    subtotal = 25.0,
    tax = 4.5,
    grandTotal = 29.5,
    amountPaid = 30.0,
    change = 0.5
)
```

---

### **Senaryo 2: İndirimli Fiş**:
```kotlin
val data = ReceiptData(
    receiptNumber = "002",
    items = items,
    subtotal = 100.0,
    tax = 18.0,
    discount = 10.0, // İndirim
    grandTotal = 108.0,
    amountPaid = 110.0,
    change = 2.0
)
```

---

### **Senaryo 3: Kargolu Fiş**:
```kotlin
val data = ReceiptData(
    receiptNumber = "003",
    items = items,
    subtotal = 100.0,
    tax = 18.0,
    shipping = 15.0, // Kargo
    grandTotal = 133.0,
    amountPaid = 150.0,
    change = 17.0
)
```

---

## 🔧 Özelleştirme

### **1. Logo Değiştirme**:
```html
<div class="logo">KENDI LOGONUZ</div>
```

### **2. Store Info**:
```html
<div class="store-info">
    Mağaza Adı<br>
    Tel: 0XXX XXX XX XX<br>
    Adres Bilgisi
</div>
```

### **3. Footer Mesajı**:
```html
<div class="footer">
    <strong>Özel mesajınız</strong><br>
    İletişim bilgileri
</div>
```

### **4. Barcode**:
```html
<div class="barcode">
    <div style="font-size: 24px;">||||| |||| |||||</div>
    <div class="barcode-number">*12345*</div>
</div>
```

---

## 📊 Boyut Referansı

### **Element Boyutları**:
```
Logo: 16px
Section Title: 13px
Body Text: 12px
Item Details: 11px
Store Info: 10px
Footer: 10px
Footer Small: 9px
```

### **Spacing**:
```
Section margin: 10px 0
Divider margin: 8px 0
Divider solid margin: 10px 0
Table padding: 2-3px
```

### **Borders**:
```
Divider: 1px dashed
Divider solid: 2px solid
Grand total: 2px solid (top & bottom)
Header border: 2px dashed
Footer border: 2px dashed
```

---

## ✅ Checklist

### **Print Öncesi Kontrol**:
- [ ] Width: 80mm (384px)
- [ ] Font: Monospace
- [ ] Colors: Black & white only
- [ ] @media print kuralları var
- [ ] page-break-inside: avoid
- [ ] Tüm detaylar görünür
- [ ] WebView'de test edildi
- [ ] Print'te test edildi

---

## 🚀 Hızlı Başlangıç

### **1. Template'i Kullan**:
```kotlin
val builder = ThermalReceiptBuilder(context)
val html = builder.buildReceipt(ThermalReceiptBuilder.getSampleReceipt())
```

### **2. WebView'de Göster**:
```kotlin
webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
```

### **3. Print Et**:
```kotlin
webView.evaluateJavascript("window.print();", null)
```

---

## 📞 Özet

✅ **80mm termal yazıcı** optimize  
✅ **384px genişlik** (120dpi)  
✅ **Monospace font** (Courier New)  
✅ **Siyah-beyaz** sadece  
✅ **@media print** kuralları  
✅ **WebView = Print** (aynı görünüm)  
✅ **Tüm detaylar** görünür  
✅ **Kotlin builder** sınıfı  
✅ **Kolay özelleştirme**  

**Dosyalar**:
- `thermal-receipt-template.html` - Statik template
- `ThermalReceiptBuilder.kt` - Kotlin builder sınıfı
- `THERMAL_RECEIPT_GUIDE.md` - Bu kılavuz

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 1.0 (Thermal Receipt System)
