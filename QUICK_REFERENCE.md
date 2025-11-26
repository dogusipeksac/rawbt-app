# Hızlı Referans Kartı

## 🚀 Hızlı Başlangıç

### 1. Projeyi Çalıştır
```bash
# Android Studio'da aç
File > Open > RawBTApp

# Gradle sync
File > Sync Project with Gradle Files

# Çalıştır
Run > Run 'app' (Shift + F10)
```

### 2. Yazıcıyı Hazırla
```
1. Yazıcıyı WiFi'ye bağla
2. IP adresini öğren (yazıcı ayarlarından)
3. Port: 9100 (varsayılan)
```

### 3. İlk Test
```
1. Uygulamayı aç
2. IP ve Port gir
3. "Test Yazdır" butonuna bas
```

## 📁 Dosya Yapısı

```
RawBTApp/
├── app/src/main/java/com/example/rawbtapp/
│   ├── MainActivity.kt              # Ana aktivite
│   ├── printer/
│   │   ├── EscPosCommands.kt       # ESC/POS komutları
│   │   ├── PrinterClient.kt        # TCP socket
│   │   └── PrinterRepository.kt    # Repository
│   └── ui/
│       ├── PrinterViewModel.kt     # ViewModel
│       └── PrinterScreen.kt        # Compose UI
├── README.md                        # Genel dokümantasyon
├── ARCHITECTURE.md                  # Mimari detayları
├── USAGE_GUIDE.md                   # Kullanım kılavuzu
└── PROJECT_SUMMARY.md               # Proje özeti
```

## 🔧 Temel Kullanım

### ViewModel'den Yazdırma
```kotlin
// Test yazdır
viewModel.printTest()

// Özel metin yazdır
viewModel.updatePrintText("Merhaba!")
viewModel.printCustomText()

// Örnek fiş
viewModel.printSampleReceipt()

// Demo
viewModel.printDemo()
```

### Repository'den Yazdırma
```kotlin
val repository = PrinterRepository()

lifecycleScope.launch {
    val result = repository.printCustomText(
        ipAddress = "192.168.1.100",
        port = 9100,
        text = "Test"
    )
    
    when (result) {
        is PrintResult.Success -> Log.d("Print", "OK")
        is PrintResult.Error -> Log.e("Print", result.message)
    }
}
```

### ESC/POS Komutları
```kotlin
val data = buildEscPosCommand {
    initialize()              // Başlat
    alignCenter()            // Ortala
    doubleTextLine("BAŞLIK") // Çift boyut
    alignLeft()              // Sola hizala
    textLine("Normal metin") // Normal metin
    boldTextLine("Kalın")    // Kalın metin
    horizontalLine()         // Çizgi
    newLine(2)               // 2 satır atla
    feedPaper(3)             // Kağıt besle
    cutPaper()               // Kes
}
```

## 📊 ESC/POS Komut Listesi

| Fonksiyon | Açıklama | Örnek |
|-----------|----------|-------|
| `initialize()` | Yazıcıyı başlat | `initialize()` |
| `text(String)` | Metin ekle | `text("Merhaba")` |
| `textLine(String)` | Metin + satır | `textLine("Merhaba")` |
| `boldText(String)` | Kalın metin | `boldText("Kalın")` |
| `boldTextLine(String)` | Kalın + satır | `boldTextLine("Kalın")` |
| `doubleText(String)` | Çift boyut | `doubleText("Büyük")` |
| `doubleTextLine(String)` | Çift + satır | `doubleTextLine("Büyük")` |
| `alignLeft()` | Sola hizala | `alignLeft()` |
| `alignCenter()` | Ortala | `alignCenter()` |
| `alignRight()` | Sağa hizala | `alignRight()` |
| `newLine(Int)` | Satır atla | `newLine(2)` |
| `horizontalLine()` | Yatay çizgi | `horizontalLine()` |
| `twoColumnText()` | İki sütun | `twoColumnText("Sol", "Sağ")` |
| `feedPaper(Int)` | Kağıt besle | `feedPaper(3)` |
| `cutPaper()` | Kağıt kes | `cutPaper()` |

## 🎯 Hızlı Örnekler

### Basit Metin
```kotlin
buildEscPosCommand {
    initialize()
    textLine("Merhaba Dünya!")
    cutPaper()
}
```

### Başlıklı Metin
```kotlin
buildEscPosCommand {
    initialize()
    alignCenter()
    doubleTextLine("BAŞLIK")
    newLine()
    alignLeft()
    textLine("İçerik buraya gelir")
    feedPaper(3)
    cutPaper()
}
```

### Fiş Formatı
```kotlin
buildEscPosCommand {
    initialize()
    alignCenter()
    boldTextLine("ŞIRKET ADI")
    textLine("Adres bilgisi")
    newLine()
    alignLeft()
    horizontalLine("=")
    twoColumnText("Ürün 1", "10.00 TL")
    twoColumnText("Ürün 2", "15.00 TL")
    horizontalLine("=")
    alignRight()
    boldTextLine("TOPLAM: 25.00 TL")
    feedPaper(3)
    cutPaper()
}
```

### İki Sütunlu Liste
```kotlin
buildEscPosCommand {
    initialize()
    twoColumnText("Ürün", "Fiyat")
    horizontalLine()
    twoColumnText("Kahve", "15.00 TL")
    twoColumnText("Çay", "10.00 TL")
    twoColumnText("Su", "5.00 TL")
    horizontalLine()
    twoColumnText("TOPLAM", "30.00 TL")
    cutPaper()
}
```

## 🔍 Hata Kodları

| Hata | Neden | Çözüm |
|------|-------|-------|
| Bağlantı zaman aşımı | Yazıcı kapalı/farklı ağda | Yazıcıyı kontrol et |
| Yazıcı bulunamadı | Yanlış IP | IP'yi kontrol et |
| Bağlanılamadı | Yanlış port | Port'u kontrol et (9100) |
| Geçersiz IP | Format hatası | IP formatını kontrol et |
| Geçersiz port | Aralık dışı | 1-65535 arası olmalı |

## 🎨 UI State Yönetimi

### State Okuma
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// Kullanım
Text(uiState.ipAddress)
if (uiState.isLoading) { CircularProgressIndicator() }
```

### State Güncelleme
```kotlin
viewModel.updateIpAddress("192.168.1.100")
viewModel.updatePort("9100")
viewModel.updatePrintText("Test")
```

### State Yapısı
```kotlin
data class PrinterUiState(
    val ipAddress: String,      // IP adresi
    val port: String,           // Port numarası
    val printText: String,      // Yazdırılacak metin
    val isLoading: Boolean,     // Yükleniyor mu?
    val message: String?,       // Mesaj (başarı/hata)
    val isError: Boolean        // Hata mı?
)
```

## 🧪 Test Komutları

### Unit Test
```kotlin
// ViewModel test
@Test
fun `test ip address update`() {
    viewModel.updateIpAddress("192.168.1.1")
    assertEquals("192.168.1.1", viewModel.uiState.value.ipAddress)
}
```

### Integration Test
```kotlin
// PrinterClient test
@Test
fun `test invalid ip returns error`() = runTest {
    val result = printerClient.print("invalid", 9100, byteArrayOf())
    assertTrue(result is PrintResult.Error)
}
```

## 📱 Desteklenen Yazıcılar

### Marka Listesi
- ✅ Epson (TM-T20, TM-T82, TM-T88)
- ✅ Star Micronics (TSP100, TSP650)
- ✅ Bixolon (SRP-350, SRP-275)
- ✅ Citizen (CT-S310, CT-S601)
- ✅ Tüm ESC/POS uyumlu yazıcılar

### Port Numaraları
- **9100** - RAW TCP (en yaygın) ⭐
- **515** - LPD
- **631** - IPP

## 🔐 Gerekli İzinler

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📦 Bağımlılıklar

```kotlin
// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## 🎯 Sık Kullanılan Kod Parçaları

### Tarih/Saat Ekleme
```kotlin
val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
val dateTime = sdf.format(Date())
textLine("Tarih: $dateTime")
```

### Türkçe Karakterler
```kotlin
// Otomatik olarak PC857 kullanılır
textLine("ÇçĞğİıÖöŞşÜü") // Doğru çıkar
```

### Çizgi Çeşitleri
```kotlin
horizontalLine("-")  // --------------------------------
horizontalLine("=")  // ================================
horizontalLine("*")  // ********************************
```

### Fiyat Formatlama
```kotlin
val price = 123.45
val formatted = String.format("%.2f TL", price)
twoColumnText("Ürün", formatted)
```

## 🚨 Önemli Notlar

1. **Aynı WiFi Ağı**: Yazıcı ve telefon aynı ağda olmalı
2. **Port 9100**: Çoğu yazıcı bu portu kullanır
3. **Timeout**: 5 saniye bağlantı, 3 saniye okuma
4. **Karakter Seti**: PC857 (Türkçe) otomatik ayarlanır
5. **Kağıt Kesme**: Bazı yazıcılar desteklemeyebilir

## 📞 Destek

### Dokümantasyon
- `README.md` - Genel bilgi
- `ARCHITECTURE.md` - Mimari detayları
- `USAGE_GUIDE.md` - Detaylı kullanım
- `PROJECT_SUMMARY.md` - Proje özeti

### Kaynaklar
- [Epson ESC/POS](https://reference.epson-biz.com/modules/ref_escpos/index.php)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)

## ⚡ Performans İpuçları

1. **Coroutines**: Her zaman `Dispatchers.IO` kullan
2. **State**: Gereksiz state güncellemelerinden kaçın
3. **Socket**: Her yazdırmadan sonra kapat
4. **Memory**: ByteArrayOutputStream kullan
5. **UI**: Compose recomposition'ı minimize et

## 🎓 Best Practices

```kotlin
// ✅ DOĞRU
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        printerClient.print(ip, port, data)
    }
}

// ❌ YANLIŞ
GlobalScope.launch {
    printerClient.print(ip, port, data)
}
```

```kotlin
// ✅ DOĞRU
_uiState.update { it.copy(ipAddress = newIp) }

// ❌ YANLIŞ
_uiState.value.ipAddress = newIp
```

## 🎯 Sonuç

Bu referans kartı, en sık kullanılan işlemleri ve komutları içerir. Detaylı bilgi için diğer dokümantasyon dosyalarına bakın.

**Hızlı Erişim:**
- Genel Bilgi → `README.md`
- Mimari → `ARCHITECTURE.md`
- Detaylı Kullanım → `USAGE_GUIDE.md`
- Proje Özeti → `PROJECT_SUMMARY.md`

---

**İyi Yazdırmalar! 🖨️✨**
