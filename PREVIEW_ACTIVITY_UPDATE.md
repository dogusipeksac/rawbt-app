# 📱 PreviewActivity - Kendi Önizleme Ekranımız

## ✅ Yapılan Değişiklikler

### **Önceki Sistem** ❌:
```
Önizleme Butonu Tıkla
    ↓
Native Android Print Dialog Açılır
    ↓
Kullanıcı print dialog'unda önizleme görür
```

### **Yeni Sistem** ✅:
```
Önizleme Butonu Tıkla
    ↓
PreviewActivity Açılır
    ↓
WebView'de HTML render edilir
    ↓
Kullanıcı tam ekran önizleme görür
    ↓
"Yazdır" butonuna tıklarsa → Native Print Dialog
```

---

## 📦 Yeni Dosyalar

### **1. PreviewActivity.kt**:
```
📄 app/src/main/java/com/example/rawbtapp/preview/PreviewActivity.kt
```

**Özellikler**:
- Tam ekran WebView
- HTML render
- Zoom desteği
- Yazdır butonu
- Geri butonu

---

## 🎨 PreviewActivity UI

### **Layout**:
```
┌─────────────────────────────────┐
│  ← Fiş #12345                   │ ← TopAppBar
│     Önizleme                     │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │   7 DAYS HAVACILIK        │  │
│  │   Stok Yönetim Sistemi    │  │
│  │   ─────────────────────   │  │
│  │   Fiş No: #12345          │  │ ← WebView
│  │   Tarih: 03.12.2025       │  │   (HTML Render)
│  │   ─────────────────────   │  │
│  │   Ürünler                 │  │
│  │   Caramel Latte           │  │
│  │   1.00 Ad x 55.00  55.00  │  │
│  │   ...                     │  │
│  └───────────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│  [İptal]          [✓ Yazdır]    │ ← BottomBar
└─────────────────────────────────┘
```

---

## 💻 Kod Yapısı

### **PreviewActivity.kt**:

```kotlin
class PreviewActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_HTML_CONTENT = "html_content"
        const val EXTRA_TITLE = "title"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Önizleme"
        
        setContent {
            RawBTAppTheme {
                PreviewScreen(
                    htmlContent = htmlContent,
                    title = title,
                    onBack = { finish() },
                    onPrint = { printContent(htmlContent, title) }
                )
            }
        }
    }
    
    private fun printContent(htmlContent: String, title: String) {
        // Native Print Dialog aç
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        // ...
    }
}
```

---

### **PreviewScreen Composable**:

```kotlin
@Composable
fun PreviewScreen(
    htmlContent: String,
    title: String,
    onBack: () -> Unit,
    onPrint: () -> Unit
) {
    Scaffold(
        topBar = { /* TopAppBar */ },
        bottomBar = { /* İptal ve Yazdır butonları */ }
    ) { paddingValues ->
        // WebView ile HTML göster
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
```

---

## 🔧 WebViewActivity Güncellemesi

### **showPrintPreview() Metodu**:

```kotlin
private fun showPrintPreview(htmlContent: String, title: String) {
    // HTML wrapper ile hazırla
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
    
    // PreviewActivity'yi aç
    val intent = Intent(this, PreviewActivity::class.java).apply {
        putExtra(PreviewActivity.EXTRA_HTML_CONTENT, fullHtml)
        putExtra(PreviewActivity.EXTRA_TITLE, title)
    }
    startActivity(intent)
}
```

---

## 📱 Kullanıcı Akışı

### **Senaryo 1: Önizleme ve Yazdırma**:
```
1. Web sitesinde "Yazdır" tıkla
   ↓
2. Yazıcı seçim ekranı açılır
   ↓
3. "🔍 Önizleme" butonuna tıkla
   ↓
4. PreviewActivity açılır
   ↓
5. HTML içeriği WebView'de render edilir
   ↓
6. Kullanıcı içeriği kontrol eder
   ↓
7. "Yazdır" butonuna tıklar
   ↓
8. Native Print Dialog açılır
   ↓
9. Kullanıcı yazıcı seçer veya PDF kaydeder
```

### **Senaryo 2: Önizleme ve İptal**:
```
1. Önizleme aç
   ↓
2. İçeriği kontrol et
   ↓
3. "İptal" veya "←" (geri) butonuna tıkla
   ↓
4. PreviewActivity kapanır
   ↓
5. Yazıcı seçim ekranına dön
```

---

## 🎯 Avantajlar

### **1. Tam Ekran Önizleme**:
- ✅ Daha büyük görünüm alanı
- ✅ Zoom desteği
- ✅ Detaylı inceleme

### **2. Kullanıcı Kontrolü**:
- ✅ İptal edebilir
- ✅ Geri dönebilir
- ✅ Yazdırmadan önce kontrol

### **3. Native Print Dialog Ayrı**:
- ✅ Önizleme ≠ Print Dialog
- ✅ İki aşamalı süreç
- ✅ Daha iyi UX

### **4. Özelleştirilebilir**:
- ✅ Kendi UI'mız
- ✅ Ek özellikler eklenebilir
- ✅ Branding yapılabilir

---

## 📊 Karşılaştırma

### **Önceki (Native Print Dialog)** ❌:
```
Önizleme Butonu
    ↓
Print Dialog (Sistem)
    ↓
Önizleme + Yazdırma aynı ekranda
    ↓
Kullanıcı karışabilir
```

### **Yeni (PreviewActivity)** ✅:
```
Önizleme Butonu
    ↓
PreviewActivity (Bizim)
    ↓
Sadece önizleme
    ↓
Yazdır butonu → Print Dialog
    ↓
İki aşamalı, net süreç
```

---

## 🔧 WebView Ayarları

### **PreviewActivity'de**:
```kotlin
WebView(context).apply {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = false  // 80mm için
        builtInZoomControls = true  // Zoom aktif
        displayZoomControls = false  // Zoom butonları gizli
        setSupportZoom(true)
    }
    
    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}
```

---

## 📝 AndroidManifest.xml

### **PreviewActivity Tanımı**:
```xml
<activity
    android:name=".preview.PreviewActivity"
    android:exported="false"
    android:label="Önizleme"
    android:theme="@style/Theme.RawBTApp"
    android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
    android:windowSoftInputMode="adjustResize" />
```

---

## 🚀 Kullanım

### **WebViewActivity'den**:
```kotlin
// Önizleme göster
showPrintPreview(htmlContent, "Fiş #12345")
```

### **Intent ile**:
```kotlin
val intent = Intent(context, PreviewActivity::class.java).apply {
    putExtra(PreviewActivity.EXTRA_HTML_CONTENT, htmlContent)
    putExtra(PreviewActivity.EXTRA_TITLE, "Fiş #12345")
}
startActivity(intent)
```

---

## ✅ Test Senaryoları

### **Test 1: Önizleme Açma**:
```
1. Yazıcı seçim ekranında "Önizleme" tıkla
2. Beklenen: PreviewActivity açılır
3. Beklenen: HTML render edilir
4. Beklenen: TopAppBar'da başlık görünür
```

### **Test 2: Zoom**:
```
1. PreviewActivity'de içeriği pinch-to-zoom yap
2. Beklenen: Zoom çalışır
3. Beklenen: İçerik büyür/küçülür
```

### **Test 3: Yazdırma**:
```
1. PreviewActivity'de "Yazdır" tıkla
2. Beklenen: Native Print Dialog açılır
3. Beklenen: Aynı içerik görünür
```

### **Test 4: İptal**:
```
1. PreviewActivity'de "İptal" tıkla
2. Beklenen: Activity kapanır
3. Beklenen: Yazıcı seçim ekranına dön
```

### **Test 5: Geri Tuşu**:
```
1. PreviewActivity'de geri tuşuna bas
2. Beklenen: Activity kapanır
3. Beklenen: Yazıcı seçim ekranına dön
```

---

## 🎨 UI Özellikleri

### **TopAppBar**:
- **Başlık**: Fiş numarası veya belge adı
- **Alt Başlık**: "Önizleme"
- **Navigation Icon**: Geri butonu (←)

### **WebView**:
- **Tam ekran**: fillMaxSize()
- **Zoom**: Aktif
- **JavaScript**: Aktif
- **Encoding**: UTF-8

### **BottomBar**:
- **İptal Butonu**: OutlinedButton, yarım genişlik
- **Yazdır Butonu**: Button (filled), yarım genişlik, ✓ ikonu

---

## 📞 Özet

✅ **PreviewActivity**: Yeni önizleme ekranı  
✅ **Tam ekran**: WebView ile HTML render  
✅ **Zoom desteği**: Pinch-to-zoom aktif  
✅ **İki aşamalı**: Önizleme → Yazdır  
✅ **Native Print Dialog**: Sadece yazdırırken  
✅ **Kullanıcı kontrolü**: İptal edebilir  
✅ **AndroidManifest**: Tanımlandı  
✅ **Build**: Başarılı ✅  

**Akış**:
```
Önizleme Butonu
    ↓
PreviewActivity (Bizim UI)
    ↓
WebView (HTML Render)
    ↓
Yazdır Butonu
    ↓
Native Print Dialog
```

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 3.4 (Custom Preview Activity)
