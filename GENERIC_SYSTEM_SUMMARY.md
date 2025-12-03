# 🎯 Generic Multi-Site POS Yazdırma Sistemi

## ✅ Yapılan Değişiklikler

### 1. **Yeni Accordion Tabanlı Ana Sayfa**

Artık tek bir sayfadan hem siteleri hem yazıcıları yönetebilirsiniz!

```
┌─────────────────────────────────┐
│  POS Yazdırma Sistemi          │
├─────────────────────────────────┤
│  ℹ️ Hoş Geldiniz                │
│  Sitelerinizi ve yazıcılarınızı│
│  yönetin                        │
├─────────────────────────────────┤
│  ⭐ Web Siteleri (3 site) ▼    │
│  ├─ Örnek POS Template         │
│  ├─ Restoran Sistemi           │
│  ├─ Kafe POS                   │
│  └─ [+ Yeni Site Ekle]         │
├─────────────────────────────────┤
│  ✓ Yazıcılar (2 yazıcı) ▼     │
│  ├─ Yazıcı #1                  │
│  ├─ Yazıcı #2                  │
│  └─ [+ Yeni Ekle]              │
└─────────────────────────────────┘
```

---

### 2. **Yeni Model ve Manager Sınıfları**

#### **Website Model** (`model/Website.kt`):
```kotlin
data class Website(
    val id: String,
    val name: String,
    val url: String,
    val description: String = "",
    val isDefault: Boolean = false
)
```

#### **WebsiteManager** (`manager/WebsiteManager.kt`):
- Siteleri local storage'da saklar (SharedPreferences)
- CRUD işlemleri: Ekle, Sil, Güncelle, Listele
- Seçili site yönetimi
- Varsayılan template otomatik eklenir

---

### 3. **Basitleştirilmiş Yazdırma**

#### **Önceki Sistem** ❌:
```
Logo + Web İçeriği + Footer + Printer Bilgisi
```

#### **Yeni Sistem** ✅:
```
Üst Boşluk (2 satır)
    ↓
Web İçeriği (Olduğu Gibi)
    ↓
Alt Boşluk (3 satır)
```

**PrintConstants Güncellemeleri**:
```kotlin
const val SHOW_LOGO = false  // Logo kapalı
const val SHOW_FOOTER = false  // Footer kapalı
const val WEB_CONTENT_TOP_SPACING = 2  // Üst boşluk
const val WEB_CONTENT_BOTTOM_SPACING = 3  // Alt boşluk
```

---

## 🎨 Yeni Özellikler

### 1. **Site Yönetimi**

#### **Site Ekleme**:
```
1. "Web Siteleri" accordion'unu aç
2. "Yeni Site Ekle" butonuna tıkla
3. Bilgileri gir:
   - Site Adı: "Restoran POS"
   - Site URL: "https://restaurant.com/pos"
   - Açıklama: "Ana restoran sistemi"
4. "Ekle" butonuna tıkla
```

#### **Site Açma**:
```
1. Site listesinden bir site seç
2. ⭐ (Aç) ikonuna tıkla
3. WebView açılır ve site yüklenir
```

#### **Site Silme**:
```
1. Silinecek siteyi bul
2. 🗑️ (Sil) ikonuna tıkla
3. Site silinir (Varsayılan site silinemez)
```

---

### 2. **Accordion Yapısı**

#### **Web Siteleri Accordion**:
- **Başlık**: "Web Siteleri"
- **Alt Başlık**: Site sayısı (örn: "3 site")
- **İkon**: ⭐ (Star)
- **İçerik**: 
  - Site listesi
  - Her site için: Seç, Aç, Sil butonları
  - Yeni site ekle butonu

#### **Yazıcılar Accordion**:
- **Başlık**: "Yazıcılar"
- **Alt Başlık**: Yazıcı sayısı (örn: "2 yazıcı")
- **İkon**: ✓ (Check)
- **İçerik**:
  - Mevcut `PrinterManagementCard` bileşeni
  - Yazıcı ekleme, silme, seçme

---

### 3. **Varsayılan Template**

İlk açılışta otomatik olarak eklenir:

```kotlin
Website(
    id = "default_template",
    name = "Örnek POS Template",
    url = "file:///android_asset/pos-web.html",
    description = "Varsayılan POS sistemi template'i",
    isDefault = true
)
```

**Özellikler**:
- ✅ Silinemez
- ✅ Otomatik yüklenir
- ✅ Local asset dosyası

---

## 📱 Kullanıcı Akışı

### **Senaryo 1: İlk Kullanım**
```
1. Uygulama açılır
2. Ana sayfa gösterilir
3. Varsayılan template otomatik eklenir
4. Kullanıcı yazıcı ekler
5. Kullanıcı siteyi açar
6. Yazdırma yapar
```

### **Senaryo 2: Yeni Site Ekleme**
```
1. Ana sayfada "Web Siteleri" accordion'unu aç
2. "Yeni Site Ekle" butonuna tıkla
3. Site bilgilerini gir
4. "Ekle" butonuna tıkla
5. Site listeye eklenir
6. Site'yi aç ve kullan
```

### **Senaryo 3: Çoklu Site Kullanımı**
```
1. Restoran POS sitesini aç → Yazdır
2. Geri dön
3. Kafe POS sitesini aç → Yazdır
4. Geri dön
5. Farklı bir site aç → Yazdır
```

---

## 🔧 Teknik Detaylar

### **Dosya Yapısı**:
```
app/src/main/java/com/example/rawbtapp/
├── model/
│   ├── Printer.kt
│   └── Website.kt  ← YENİ!
├── manager/
│   ├── PrinterManager.kt
│   └── WebsiteManager.kt  ← YENİ!
├── ui/
│   ├── MainScreen.kt  ← YENİ!
│   ├── PrinterScreen.kt
│   └── PrinterViewModel.kt
├── MainActivity.kt  ← GÜNCELLEND İ
└── webview/
    └── WebViewActivity.kt
```

### **Data Flow**:
```
MainActivity
    ↓
MainScreen (Accordion)
    ↓
WebsiteManager + PrinterViewModel
    ↓
SharedPreferences (Local Storage)
```

### **Intent Flow**:
```
MainActivity → WebViewActivity
    ↓
Extra: WEBSITE_NAME
Extra: WEBSITE_URL
    ↓
WebView yükler
    ↓
JavaScript Bridge
    ↓
Yazdırma
```

---

## 🎯 Avantajlar

### **1. Esneklik**:
- ✅ İstediğiniz kadar site ekleyin
- ✅ Her site için farklı URL
- ✅ Tek uygulamadan çoklu sistem yönetimi

### **2. Basitlik**:
- ✅ Tek sayfa, tüm yönetim
- ✅ Accordion ile düzenli görünüm
- ✅ Kolay site ekleme/silme

### **3. Generic Yazdırma**:
- ✅ Logo/Footer yok
- ✅ Sadece web içeriği
- ✅ Üst/alt boşluk ile temiz çıktı

### **4. Local Storage**:
- ✅ Siteler cihazda saklanır
- ✅ Offline çalışır
- ✅ Hızlı erişim

---

## 📊 Karşılaştırma

### **Önceki Sistem** ❌:
```
- Tek site (hardcoded)
- Logo + Footer + Printer bilgisi
- Değiştirmek için kod gerekli
- Esnek değil
```

### **Yeni Sistem** ✅:
```
- Çoklu site (dinamik)
- Sadece web içeriği
- UI'dan yönetim
- Tamamen esnek
```

---

## 🚀 Sonraki Adımlar

1. ✅ Website model oluşturuldu
2. ✅ WebsiteManager oluşturuldu
3. ✅ MainScreen accordion yapısı eklendi
4. ✅ PrintConstants basitleştirildi
5. ⏳ WebViewActivity'yi güncelle (URL intent'ten alsın)
6. ⏳ Printer selection'a preview butonu ekle
7. ⏳ WebView yazdırmayı basitleştir

---

## 📞 Özet

✅ **Accordion ana sayfa** - Site ve yazıcı yönetimi tek yerde  
✅ **Çoklu site desteği** - İstediğiniz kadar site ekleyin  
✅ **Generic yazdırma** - Sadece web içeriği, logo/footer yok  
✅ **Local storage** - Siteler cihazda saklanır  
✅ **Kolay yönetim** - UI'dan ekle/sil/seç  
✅ **Build başarılı** - Hatasız derleme  

**Geliştirme Tarihi**: 3 Aralık 2025  
**Versiyon**: 3.0 (Generic Multi-Site System)
