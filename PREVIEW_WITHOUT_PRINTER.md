# 🔍 Yazıcısız Önizleme Sistemi

## ✅ Yapılan Değişiklikler

### 1. **Default Site Güncellendi**

7 Days Havacılık Stok Sistemi artık varsayılan site:

```kotlin
// Website.kt
fun getDefaultTemplate(): Website {
    return Website(
        id = "default_7days",
        name = "7 Days Stok Sistemi",
        url = "https://stock.7dayshavacilik.com/",
        description = "7 Days Havacılık Stok Yönetim Sistemi",
        isDefault = true
    )
}
```

---

### 2. **Yazıcı Seçim Ekranına Önizleme Butonu Eklendi**

#### **Yeni UI**:
```
┌─────────────────────────────────┐
│  Yazıcı Seçin                   │
│  Belge: Fiş #12345              │
├─────────────────────────────────┤
│                                 │
│  [Yazıcı Listesi]               │
│                                 │
├─────────────────────────────────┤
│  Seçili Yazıcı: #1              │
│  192.168.1.100:9100             │
│                                 │
│  ┌───────────────────────────┐ │
│  │  🔍 Önizleme              │ │ ← YENİ!
│  └───────────────────────────┘ │
│  ┌───────────────────────────┐ │
│  │  ✓ Yazdır                 │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

#### **Kod** (`PrinterSelectionActivity.kt`):
```kotlin
// Önizleme butonu - Her zaman görünür
OutlinedButton(
    onClick = onPreview,
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
) {
    Icon(Icons.Default.Search, ...)
    Text("🔍 Önizleme", ...)
}
```

---

### 3. **Önizleme Akışı**

#### **Kullanıcı Akışı**:
```
1. Web sitesinde "Yazdır" butonuna tıkla
   ↓
2. Yazıcı seçim ekranı açılır
   ↓
3. "🔍 Önizleme" butonuna tıkla
   ↓
4. Android Print Dialog açılır
   ↓
5. PDF olarak kaydet veya yazdır
```

#### **Teknik Akış**:
```
WebViewActivity
    ↓
PrinterSelectionActivity
    ↓
onPreview() callback
    ↓
returnPreviewResult()
    ↓
WebViewActivity (result handler)
    ↓
showPrintPreview()
    ↓
Android Print Dialog
```

---

### 4. **showPrintPreview Metodu**

#### **Özellikler**:
- ✅ Geçici WebView oluşturur
- ✅ HTML içeriğini yükler
- ✅ Android Print Dialog'u açar
- ✅ Yazıcı bağlantısı gerektirmez
- ✅ PDF olarak kaydetme seçeneği

#### **Kod** (`WebViewActivity.kt`):
```kotlin
@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun showPrintPreview(htmlContent: String, title: String) {
    // Geçici WebView oluştur
    val previewWebView = WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
    }
    
    // HTML içeriğini yükle - Sadece üst/alt boşluk
    val fullHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    margin: ${PrintConstants.WEB_CONTENT_TOP_SPACING}em 0 
                            ${PrintConstants.WEB_CONTENT_BOTTOM_SPACING}em 0;
                }
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()
    
    previewWebView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
    
    // Print dialog'u aç
    previewWebView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = previewWebView.createPrintDocumentAdapter(title)
            
            printManager.print(title, printAdapter, 
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )
        }
    }
}
```

---

## 🎯 Avantajlar

### **1. Yazıcısız Önizleme**:
- ✅ Fiziksel yazıcı gerekmez
- ✅ Yazıcı bağlantısı kontrol edilmez
- ✅ Her zaman çalışır

### **2. Android Print Dialog**:
- ✅ Native Android UI
- ✅ PDF olarak kaydetme
- ✅ Önizleme gösterimi
- ✅ Sayfa ayarları

### **3. Esnek Kullanım**:
- ✅ Önizle → PDF kaydet
- ✅ Önizle → Yazıcı seç → Yazdır
- ✅ Direkt yazdır (yazıcı seçerek)

---

## 📱 Kullanım Senaryoları

### **Senaryo 1: Sadece Önizleme**
```
1. Web sitesinde "Yazdır" tıkla
2. Yazıcı seçim ekranında "🔍 Önizleme" tıkla
3. Android Print Dialog açılır
4. "PDF olarak kaydet" seç
5. Dosya kaydedilir
```

### **Senaryo 2: Önizle ve Yazdır**
```
1. Web sitesinde "Yazdır" tıkla
2. "🔍 Önizleme" tıkla → İçeriği kontrol et
3. Geri dön
4. Yazıcı seç
5. "Yazdır" tıkla
```

### **Senaryo 3: Direkt Yazdır**
```
1. Web sitesinde "Yazdır" tıkla
2. Yazıcı seç
3. "Yazdır" tıkla
```

---

## 🔧 Teknik Detaylar

### **Intent Flow**:
```kotlin
// PrinterSelectionActivity → WebViewActivity
val resultIntent = Intent().apply {
    putExtra(ACTION_PREVIEW, true)  // Önizleme flag'i
}
setResult(Activity.RESULT_OK, resultIntent)
```

### **Result Handler**:
```kotlin
// WebViewActivity
printerSelectionLauncher = registerForActivityResult(...) { result ->
    if (result.resultCode == RESULT_OK) {
        val isPreview = data.getBooleanExtra(ACTION_PREVIEW, false)
        
        if (isPreview) {
            showPrintPreview(pendingHtmlContent!!, pendingDocumentTitle!!)
        } else {
            // Normal yazdırma
            printWithSelectedPrinter(...)
        }
    }
}
```

---

## 🎨 UI Özellikleri

### **Önizleme Butonu**:
- **Renk**: Outlined (Primary border)
- **Yükseklik**: 56dp
- **İkon**: 🔍 Search
- **Konum**: Yazdır butonunun üstünde
- **Görünürlük**: Her zaman (yazıcı seçili olmasa da)

### **Android Print Dialog**:
- **Boyut**: A4
- **Margin**: No margins
- **Özellikler**:
  - Önizleme
  - PDF kaydetme
  - Yazıcı seçimi
  - Sayfa ayarları
  - Kopya sayısı

---

## 📊 Karşılaştırma

### **Önceki Sistem** ❌:
```
- Önizleme için yazıcı gerekli
- PrintPreviewDialog (custom)
- Sadece metin önizleme
- PDF kaydetme yok
```

### **Yeni Sistem** ✅:
```
- Önizleme için yazıcı gerekmez
- Android Print Dialog (native)
- Tam HTML önizleme
- PDF kaydetme var
```

---

## 🚀 Kullanım Adımları

### **1. Uygulamayı Aç**:
```
- Ana sayfa açılır
- "7 Days Stok Sistemi" varsayılan site olarak görünür
```

### **2. Siteyi Aç**:
```
- "7 Days Stok Sistemi" → ⭐ (Aç) tıkla
- Site yüklenir
```

### **3. Yazdırma Başlat**:
```
- Web sitesinde "Yazdır" butonuna tıkla
- Yazıcı seçim ekranı açılır
```

### **4. Önizleme**:
```
- "🔍 Önizleme" butonuna tıkla
- Android Print Dialog açılır
- İçeriği kontrol et
- PDF olarak kaydet veya yazdır
```

---

## 🔍 Debug İpuçları

### **Logcat Filtreleri**:
```bash
# Önizleme
adb logcat | grep "showPrintPreview"

# Yazıcı seçimi
adb logcat | grep "PrinterSelectionActivity"

# Result handling
adb logcat | grep "ACTION_PREVIEW"
```

### **Test Adımları**:
```
1. Uygulamayı aç
2. 7 Days sitesini aç
3. Yazdır butonuna tıkla
4. Önizleme butonuna tıkla
5. Print dialog açılmalı ✅
6. PDF kaydet seçeneği görünmeli ✅
```

---

## ✅ Özet

✅ **Default site**: `https://stock.7dayshavacilik.com/`  
✅ **Önizleme butonu**: Yazıcı seçim ekranında  
✅ **Yazıcısız çalışır**: Fiziksel yazıcı gerekmez  
✅ **Android Print Dialog**: Native önizleme  
✅ **PDF kaydetme**: Mümkün  
✅ **Basit HTML**: Sadece üst/alt boşluk  
✅ **Build başarılı**: Hatasız derleme  

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 3.1 (Yazıcısız Önizleme)
