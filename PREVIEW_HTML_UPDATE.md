# 🔍 Önizleme Dialog'u HTML Desteği

## ✅ Yapılan Değişiklikler

### **1. PrinterViewModel Güncellendi**

#### **Önceki Sistem** ❌:
```kotlin
fun getPreviewContent(): String {
    val content = StringBuilder()
    content.append("Caramel Latte\n")
    content.append("1.00 Ad x 55.00\n")
    // ... plain text
    return content.toString()
}
```

#### **Yeni Sistem** ✅:
```kotlin
fun getPreviewContent(): String {
    val builder = ThermalReceiptBuilder(getApplication())
    val sampleData = ThermalReceiptBuilder.getSampleReceipt()
    val html = builder.buildReceipt(sampleData)
    return html  // 80mm termal receipt HTML
}
```

---

### **2. PrintPreviewDialog HTML Render Desteği**

#### **PreviewTab Güncellendi**:
```kotlin
@Composable
private fun PreviewTab(content: String) {
    // HTML mi kontrol et
    val isHtml = content.trim().startsWith("<!DOCTYPE") || 
                 content.trim().startsWith("<html")
    
    if (isHtml) {
        // WebView ile HTML render et
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                }
            }
        )
    } else {
        // Plain text göster
        Text(text = content, fontFamily = FontFamily.Monospace)
    }
}
```

---

## 🎯 Özellikler

### **Önizleme Dialog'u Artık**:

✅ **HTML render** eder (WebView ile)  
✅ **80mm termal receipt** template gösterir  
✅ **Gerçek görünüm** (print ile aynı)  
✅ **Monospace font** (Courier New)  
✅ **Siyah-beyaz** optimizasyon  
✅ **Tüm detaylar** görünür  
✅ **Backward compatible** (plain text de destekler)  

---

## 📱 Kullanıcı Deneyimi

### **Önizleme Akışı**:

```
1. PrinterScreen'de "Önizleme" butonuna tıkla
   ↓
2. PrintPreviewDialog açılır
   ↓
3. "Önizleme" tab'ı seçili
   ↓
4. WebView içinde HTML render edilir
   ↓
5. 80mm termal receipt görünümü
   ↓
6. Kullanıcı içeriği görür
```

---

## 🎨 Dialog Görünümü

### **3 Tab Yapısı**:

#### **1. Önizleme Tab** (YENİ! HTML Render):
```
┌─────────────────────────────────┐
│  Önizleme | Ham Veri | Türkçe   │
├─────────────────────────────────┤
│  ┌───────────────────────────┐  │
│  │   7 DAYS HAVACILIK        │  │
│  │   Stok Yönetim Sistemi    │  │
│  │   ─────────────────────   │  │
│  │   Fiş No: #12345          │  │
│  │   Tarih: 03.12.2025       │  │
│  │   ─────────────────────   │  │
│  │   Ürünler                 │  │
│  │   Caramel Latte           │  │
│  │   1.00 Ad x 55.00  55.00  │  │
│  │   ─────────────────────   │  │
│  │   Ara Toplam:    150.00   │  │
│  │   KDV (18%):      27.00   │  │
│  │   ═══════════════════     │  │
│  │   GENEL TOPLAM:  177.00   │  │
│  │   ═══════════════════     │  │
│  │   Ödeme: Nakit            │  │
│  │   ─────────────────────   │  │
│  │   Teşekkürler!            │  │
│  └───────────────────────────┘  │
│                                 │
│  [İptal]          [Yazdır]      │
└─────────────────────────────────┘
```

#### **2. Ham Veri Tab** (Aynı):
- HTML source code
- Byte array
- Encoding bilgisi

#### **3. Türkçe Test Tab** (Aynı):
- Türkçe karakter kontrolü
- Karakter tablosu
- Test metni

---

## 🔧 Teknik Detaylar

### **HTML Algılama**:
```kotlin
val isHtml = content.trim().startsWith("<!DOCTYPE") || 
             content.trim().startsWith("<html")
```

### **WebView Ayarları**:
```kotlin
WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadWithOverviewMode = true
    settings.useWideViewPort = false  // 80mm için
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    
    loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
}
```

### **Boyut**:
```kotlin
AndroidView(
    modifier = Modifier
        .fillMaxWidth()
        .height(600.dp)  // Sabit yükseklik
)
```

---

## 📊 Karşılaştırma

### **Önceki Önizleme** ❌:
```
Plain Text
─────────────────
Caramel Latte
1.00 Ad x 55.00
55.00
─────────────────
Toplam: 55.00
```

### **Yeni Önizleme** ✅:
```
HTML Render (WebView)
┌─────────────────────┐
│  7 DAYS HAVACILIK   │
│  ─────────────────  │
│  Fiş No: #12345     │
│  Tarih: 03.12.2025  │
│  ─────────────────  │
│  Ürünler            │
│  Caramel Latte      │
│  1.00 x 55.00 55.00 │
│  ─────────────────  │
│  Toplam:     55.00  │
│  ═════════════════  │
│  Teşekkürler!       │
└─────────────────────┘
```

---

## 🎯 Avantajlar

### **1. Gerçek Görünüm**:
- ✅ Print çıktısı ile aynı
- ✅ Font, boyut, spacing doğru
- ✅ Border'lar görünür

### **2. Profesyonel**:
- ✅ 80mm termal receipt formatı
- ✅ Tüm detaylar
- ✅ Barcode desteği

### **3. Kullanıcı Dostu**:
- ✅ Görsel önizleme
- ✅ Kolay kontrol
- ✅ Hata önleme

### **4. Backward Compatible**:
- ✅ Plain text de destekler
- ✅ Eski içerik çalışır
- ✅ Otomatik algılama

---

## 🚀 Kullanım

### **PrinterScreen'den**:
```kotlin
// Önizleme butonu
OutlinedButton(
    onClick = { showPreview = true }
) {
    Icon(Icons.Default.Search, ...)
    Text("🔍 Önizleme")
}

// Dialog
if (showPreview) {
    PrintPreviewDialog(
        content = viewModel.getPreviewContent(),  // HTML döner
        onDismiss = { showPreview = false },
        onPrint = { viewModel.printSampleReceipt() }
    )
}
```

---

## 📝 Örnek Çıktı

### **getPreviewContent() Dönen HTML**:
```html
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: 'Courier New', monospace;
            width: 384px;
            margin: 0 auto;
        }
        .header {
            text-align: center;
            border-bottom: 2px dashed #000;
        }
        /* ... */
    </style>
</head>
<body>
    <div class="receipt">
        <div class="header">
            <div class="logo">7 DAYS HAVACILIK</div>
            <!-- ... -->
        </div>
        <!-- ... -->
    </div>
</body>
</html>
```

---

## ✅ Test Senaryoları

### **Test 1: HTML Önizleme**:
```
1. PrinterScreen aç
2. "Önizleme" butonuna tıkla
3. Dialog açılır
4. "Önizleme" tab'ı seçili
5. Beklenen: HTML render edilmiş görünüm
6. Beklenen: 80mm termal receipt formatı
```

### **Test 2: Tab Geçişi**:
```
1. "Önizleme" tab → HTML görünümü
2. "Ham Veri" tab → HTML source code
3. "Türkçe Test" tab → Karakter kontrolü
4. Tekrar "Önizleme" tab → HTML görünümü
```

### **Test 3: Yazdırma**:
```
1. Önizlemeyi kontrol et
2. "Yazdır" butonuna tıkla
3. Beklenen: Önizleme ile aynı çıktı
```

---

## 🔍 Debug

### **HTML Kontrolü**:
```kotlin
Log.d("Preview", "Content type: ${if (isHtml) "HTML" else "Plain Text"}")
Log.d("Preview", "Content length: ${content.length}")
```

### **WebView Debug**:
```kotlin
WebView.setWebContentsDebuggingEnabled(true)
// Chrome DevTools ile inspect edilebilir
```

---

## 📞 Özet

✅ **PrinterViewModel**: ThermalReceiptBuilder kullanıyor  
✅ **getPreviewContent()**: HTML döndürüyor  
✅ **PrintPreviewDialog**: HTML render ediyor  
✅ **WebView**: 80mm termal receipt gösteriyor  
✅ **Önizleme Tab**: Gerçek görünüm  
✅ **Ham Veri Tab**: HTML source  
✅ **Türkçe Test Tab**: Karakter kontrolü  
✅ **Build**: Başarılı ✅  

**Kullanım**:
```kotlin
// Önizleme aç
showPreview = true

// Dialog içinde
PrintPreviewDialog(
    content = viewModel.getPreviewContent(),  // HTML
    onDismiss = { showPreview = false },
    onPrint = { /* yazdır */ }
)
```

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 3.3 (HTML Preview Support)
