# 🌐 Otomatik WebView Açılışı

## ✅ Yapılan Değişiklikler

### 1. **Uygulama Açılışında Direkt WebView**

Artık uygulama açıldığında direkt olarak 7 Days Stok Sistemi web sitesi açılıyor.

#### **Önceki Akış** ❌:
```
Uygulama Aç
    ↓
Ana Sayfa (Accordion)
    ↓
Site Seç
    ↓
Aç Butonuna Tıkla
    ↓
WebView Açılır
```

#### **Yeni Akış** ✅:
```
Uygulama Aç
    ↓
WebView Direkt Açılır
    ↓
7 Days Stok Sistemi Yüklenir
```

---

### 2. **MainActivity Güncellemesi**

#### **Kod** (`MainActivity.kt`):
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // WebsiteManager'ı başlat
    websiteManager = WebsiteManager(this)
    
    // Deep link'i işle
    handleDeepLink(intent)
    
    // İlk açılışta direkt web sitesini aç
    Log.d(TAG, "Auto-opening default website: 7 Days Stock System")
    val defaultSite = Website.getDefaultTemplate()
    openWebsite(defaultSite)
}
```

**Özellikler**:
- ✅ MainScreen gösterilmez
- ✅ Direkt WebView açılır
- ✅ 7 Days Stok Sistemi otomatik yüklenir

---

### 3. **WebView URL Yönetimi**

#### **Intent Extras**:
```kotlin
// MainActivity → WebViewActivity
val intent = Intent(this, WebViewActivity::class.java).apply {
    putExtra("WEBSITE_NAME", website.name)
    putExtra("WEBSITE_URL", website.url)
}
startActivity(intent)
```

#### **WebViewActivity**:
```kotlin
// Intent'ten URL ve isim al
val websiteName = intent.getStringExtra(EXTRA_WEBSITE_NAME)
val websiteUrl = intent.getStringExtra(EXTRA_WEBSITE_URL)

// Fallback: Config dosyasından al
val configUrl = getString(R.string.webview_url)
val configTitle = getString(R.string.webview_title)

val url = websiteUrl ?: intent.getStringExtra(EXTRA_URL) ?: configUrl
val title = websiteName ?: configTitle
```

**Öncelik Sırası**:
1. `WEBSITE_URL` (Intent extra)
2. `EXTRA_URL` (Eski format)
3. `R.string.webview_url` (Config)

---

### 4. **Geri Butonu Kaldırıldı**

#### **Önceki UI** ❌:
```
┌─────────────────────────────────┐
│  ← 7 Days Stok Sistemi          │ ← Geri butonu vardı
├─────────────────────────────────┤
│                                 │
│  [Web İçeriği]                  │
│                                 │
└─────────────────────────────────┘
```

#### **Yeni UI** ✅:
```
┌─────────────────────────────────┐
│  7 Days Stok Sistemi            │ ← Geri butonu yok
├─────────────────────────────────┤
│                                 │
│  [Web İçeriği]                  │
│                                 │
└─────────────────────────────────┘
```

#### **Kod**:
```kotlin
// WebViewScreen - Geri butonu kaldırıldı
@Composable
fun WebViewScreen(
    url: String,
    title: String = "POS Web Sistemi",
    onWebViewCreated: (WebView) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                // navigationIcon kaldırıldı
                colors = TopAppBarDefaults.topAppBarColors(...)
            )
        }
    ) { ... }
}
```

---

### 5. **Back Button Davranışı**

#### **Yeni Davranış**:
```kotlin
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
```

**Davranış**:
- ✅ WebView'de sayfa geçmişi varsa → Geri git
- ✅ İlk sayfadaysa → WebView'de kal (Activity kapanmaz)
- ✅ Kullanıcı her zaman WebView'de kalır

---

## 🎯 Kullanıcı Deneyimi

### **Uygulama Açılışı**:
```
1. Uygulama ikonu tıklanır
   ↓
2. WebView direkt açılır
   ↓
3. https://stock.7dayshavacilik.com/ yüklenir
   ↓
4. Kullanıcı direkt çalışmaya başlar
```

### **Yazdırma Akışı**:
```
1. Web sitesinde "Yazdır" butonuna tıkla
   ↓
2. Yazıcı seçim ekranı açılır
   ↓
3. Seçenekler:
   - 🔍 Önizleme (yazıcı gerekmez)
   - ✓ Yazdır (yazıcı seç)
   ↓
4. İşlem tamamlanınca WebView'e dön
   ↓
5. Kullanıcı WebView'de kalmaya devam eder
```

### **Geri Tuşu**:
```
Senaryo 1: Web sitesinde birden fazla sayfa gezildi
- Geri tuşuna bas → Önceki sayfaya git
- Geri tuşuna bas → Daha önceki sayfaya git
- İlk sayfaya gelince → WebView'de kal

Senaryo 2: İlk sayfadayken
- Geri tuşuna bas → Hiçbir şey olmaz
- WebView'de kal
```

---

## 📱 Akış Diyagramı

### **Tam Akış**:
```
┌─────────────────────────────────┐
│  Uygulama Başlat                │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  MainActivity.onCreate()        │
│  - WebsiteManager başlat        │
│  - Default site al              │
│  - openWebsite() çağır          │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  WebViewActivity.onCreate()     │
│  - Intent'ten URL al            │
│  - WebView oluştur              │
│  - Site yükle                   │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  WebView Gösteriliyor           │
│  https://stock.7dayshavacilik   │
│  .com/                          │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  Kullanıcı "Yazdır" Tıklar      │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│  PrinterSelectionActivity       │
│  - Yazıcı listesi               │
│  - 🔍 Önizleme butonu           │
│  - ✓ Yazdır butonu              │
└────────────┬────────────────────┘
             ↓
      ┌──────┴──────┐
      ↓             ↓
┌──────────┐  ┌──────────┐
│ Önizleme │  │ Yazdır   │
└────┬─────┘  └────┬─────┘
     ↓             ↓
┌──────────┐  ┌──────────┐
│ Android  │  │ Yazıcıya │
│ Print    │  │ Gönder   │
│ Dialog   │  │          │
└────┬─────┘  └────┬─────┘
     ↓             ↓
┌─────────────────────────────────┐
│  WebView'e Dön                  │
│  (Her zaman WebView'de kal)     │
└─────────────────────────────────┘
```

---

## 🔧 Teknik Detayler

### **Otomatik Açılış**:
```kotlin
// MainActivity.kt
private fun openWebsite(website: Website) {
    val intent = Intent(this, WebViewActivity::class.java).apply {
        putExtra("WEBSITE_NAME", website.name)
        putExtra("WEBSITE_URL", website.url)
    }
    startActivity(intent)
}
```

### **URL Yükleme**:
```kotlin
// WebViewActivity.kt
val url = websiteUrl ?: intent.getStringExtra(EXTRA_URL) ?: configUrl
val title = websiteName ?: configTitle

WebViewScreen(
    url = url,
    title = title,
    onWebViewCreated = { wv ->
        webView = wv
        setupWebView(wv)
    }
)
```

### **WebView Setup**:
```kotlin
private fun setupWebView(webView: WebView) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        // ... diğer ayarlar
    }
    
    // JavaScript interface ekle
    webView.addJavascriptInterface(
        PrintBridge(this),
        "AndroidPrintBridge"
    )
}
```

---

## 📊 Karşılaştırma

### **Önceki Sistem** ❌:
```
- Ana sayfa gösterilir
- Kullanıcı site seçer
- Aç butonuna tıklar
- WebView açılır
- Geri butonu var
- Geri basınca ana sayfaya döner
```

### **Yeni Sistem** ✅:
```
- Direkt WebView açılır
- 7 Days otomatik yüklenir
- Kullanıcı direkt çalışır
- Geri butonu yok
- Geri basınca WebView'de kalır
- Hiç ana sayfaya dönmez
```

---

## 🎯 Avantajlar

### **1. Hızlı Başlangıç**:
- ✅ Tek tıkla çalışmaya başla
- ✅ Ara adım yok
- ✅ Direkt web sitesi

### **2. Basit UX**:
- ✅ Karmaşık menü yok
- ✅ Tek ekran
- ✅ Kolay kullanım

### **3. Kesintisiz Çalışma**:
- ✅ WebView'den çıkma yok
- ✅ Yazdırma sonrası geri dön
- ✅ Sürekli aynı ekran

### **4. Kiosk Mode**:
- ✅ Uygulama kiosk gibi çalışır
- ✅ Kullanıcı sadece web sitesini görür
- ✅ Diğer ekranlara erişim yok

---

## 🚀 Test Senaryoları

### **Test 1: İlk Açılış**:
```
1. Uygulamayı tamamen kapat
2. Uygulamayı aç
3. Beklenen: WebView direkt açılır
4. Beklenen: 7 Days sitesi yüklenir
5. Beklenen: Ana sayfa gösterilmez
```

### **Test 2: Yazdırma**:
```
1. Web sitesinde "Yazdır" tıkla
2. Beklenen: Yazıcı seçim ekranı açılır
3. "Önizleme" tıkla
4. Beklenen: Android Print Dialog açılır
5. Dialog'u kapat
6. Beklenen: WebView'e dön
```

### **Test 3: Geri Tuşu**:
```
1. Web sitesinde birkaç sayfa gez
2. Geri tuşuna bas
3. Beklenen: Önceki sayfaya git
4. İlk sayfaya gelene kadar geri bas
5. İlk sayfada geri bas
6. Beklenen: WebView'de kal (kapanma)
```

---

## ✅ Özet

✅ **Otomatik açılış**: Uygulama açılınca direkt WebView  
✅ **Default site**: `https://stock.7dayshavacilik.com/`  
✅ **Geri butonu**: Kaldırıldı (UI'dan)  
✅ **Back tuşu**: Sadece WebView history  
✅ **Yazdırma**: Selection sayfası açılır  
✅ **Önizleme**: Yazıcısız çalışır  
✅ **Geri dönüş**: Her zaman WebView'e  
✅ **Build**: Başarılı ✅  

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 3.2 (Otomatik WebView Açılışı)
