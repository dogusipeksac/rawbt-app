# WiFi Termal POS Yazıcı Uygulaması

WiFi üzerinden termal POS yazıcılara ESC/POS komutları gönderebilen Android uygulaması. Jetpack Compose ve Kotlin ile geliştirilmiştir.

## 📋 Özellikler

- ✅ WiFi üzerinden TCP/IP bağlantısı
- ✅ ESC/POS komut desteği
- ✅ MVVM mimarisi
- ✅ Jetpack Compose UI
- ✅ Kotlin Coroutines ile asenkron işlemler
- ✅ Türkçe karakter desteği
- ✅ **Tablet desteği** - Responsive layout (telefon ve tablet)
- ✅ Test yazdırma
- ✅ Özel metin yazdırma
- ✅ Örnek fiş yazdırma
- ✅ ESC/POS demo yazdırma

## 🏗️ Mimari

Uygulama **MVVM (Model-View-ViewModel)** mimarisi kullanır:

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  ┌─────────────────────────────────┐   │
│  │      PrinterScreen.kt           │   │
│  └─────────────────────────────────┘   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         ViewModel Layer                 │
│  ┌─────────────────────────────────┐   │
│  │    PrinterViewModel.kt          │   │
│  │  - State Management             │   │
│  │  - Business Logic               │   │
│  └─────────────────────────────────┘   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       Repository Layer                  │
│  ┌─────────────────────────────────┐   │
│  │   PrinterRepository.kt          │   │
│  │  - Data Operations              │   │
│  └─────────────────────────────────┘   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Data Layer                      │
│  ┌─────────────────────────────────┐   │
│  │     PrinterClient.kt            │   │
│  │  - TCP Socket Connection        │   │
│  │  - ESC/POS Commands             │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │    EscPosCommands.kt            │   │
│  │  - Command Builder              │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

## 📁 Proje Yapısı

```
app/src/main/java/com/example/rawbtapp/
├── MainActivity.kt                 # Ana aktivite
├── printer/
│   ├── EscPosCommands.kt          # ESC/POS komut builder
│   ├── PrinterClient.kt           # TCP socket bağlantısı
│   └── PrinterRepository.kt       # Repository katmanı
└── ui/
    ├── PrinterViewModel.kt        # ViewModel
    ├── PrinterScreen.kt           # Compose UI
    └── theme/
        └── Theme.kt               # Tema ayarları
```

## 🔧 Teknik Detaylar

### 1. ESC/POS Komutları (EscPosCommands.kt)

ESC/POS komutlarını oluşturmak için builder pattern kullanır:

```kotlin
val printData = buildEscPosCommand {
    initialize()                    // Yazıcıyı başlat
    alignCenter()                   // Ortaya hizala
    doubleTextLine("BAŞLIK")       // Çift boyut metin
    alignLeft()                     // Sola hizala
    textLine("Normal metin")       // Normal metin
    boldTextLine("Kalın metin")    // Kalın metin
    horizontalLine()               // Yatay çizgi
    feedPaper(3)                   // Kağıt besle
    cutPaper()                     // Kağıdı kes
}
```

**Desteklenen Komutlar:**
- `initialize()` - Yazıcı başlatma
- `text(String)` - Metin ekleme
- `textLine(String)` - Metin + satır atlama
- `boldText(String)` - Kalın metin
- `doubleText(String)` - Çift boyut metin
- `alignLeft/Center/Right()` - Hizalama
- `horizontalLine()` - Yatay çizgi
- `newLine(Int)` - Satır atlama
- `feedPaper(Int)` - Kağıt besleme
- `cutPaper()` - Kağıt kesme
- `twoColumnText()` - İki sütunlu metin

### 2. TCP Socket Bağlantısı (PrinterClient.kt)

TCP socket üzerinden yazıcıya bağlanır ve veri gönderir:

```kotlin
suspend fun print(ipAddress: String, port: Int, data: ByteArray): PrintResult
```

**Özellikler:**
- Timeout yönetimi (5 saniye bağlantı, 3 saniye okuma)
- IP adresi validasyonu
- Port validasyonu
- Detaylı hata mesajları
- Otomatik kaynak temizleme

**Hata Türleri:**
- `SocketTimeoutException` - Bağlantı zaman aşımı
- `UnknownHostException` - Yazıcı bulunamadı
- `ConnectException` - Bağlantı hatası
- `IOException` - Veri gönderme hatası

### 3. Repository Katmanı (PrinterRepository.kt)

İş mantığını yönetir ve farklı yazdırma türlerini sağlar:

```kotlin
suspend fun printCustomText(ipAddress: String, port: Int, text: String): PrintResult
suspend fun printTest(ipAddress: String, port: Int): PrintResult
suspend fun printSampleReceipt(ipAddress: String, port: Int): PrintResult
suspend fun printDemo(ipAddress: String, port: Int): PrintResult
```

### 4. ViewModel (PrinterViewModel.kt)

UI state'ini yönetir ve Coroutines ile asenkron işlemleri koordine eder:

```kotlin
data class PrinterUiState(
    val ipAddress: String,
    val port: String,
    val printText: String,
    val isLoading: Boolean,
    val message: String?,
    val isError: Boolean
)
```

**Fonksiyonlar:**
- `updateIpAddress(String)` - IP güncelle
- `updatePort(String)` - Port güncelle
- `updatePrintText(String)` - Metin güncelle
- `printTest()` - Test yazdır
- `printCustomText()` - Özel metin yazdır
- `printSampleReceipt()` - Örnek fiş yazdır
- `printDemo()` - Demo yazdır

### 5. Compose UI (PrinterScreen.kt)

Modern Material 3 tasarımı ile kullanıcı arayüzü:

**Bileşenler:**
- `ConnectionSettingsCard` - IP ve port ayarları
- `PrintTextCard` - Yazdırılacak metin girişi
- `PrintButtonsCard` - Yazdırma butonları
- `LoadingIndicator` - Yükleme göstergesi
- `InfoCard` - Kullanım bilgileri

## 🚀 Kullanım

### Gereksinimler

1. **Yazıcı Ayarları:**
   - Yazıcı WiFi ağına bağlı olmalı
   - Yazıcının IP adresi bilinmeli
   - Port numarası (genelde 9100)

2. **Telefon Ayarları:**
   - Yazıcı ile aynı WiFi ağında olmalı
   - İnternet izni verilmeli

### Adım Adım Kullanım

1. **Yazıcı IP Adresini Öğrenme:**
   - Yazıcı ayarlarından network bilgilerini yazdırın
   - Veya router arayüzünden bağlı cihazlara bakın

2. **Uygulamayı Açın:**
   - IP adresi ve port numarasını girin
   - Varsayılan port: 9100

3. **Test Yazdırma:**
   - "Test Yazdır" butonuna basın
   - Bağlantıyı kontrol edin

4. **Özel Metin Yazdırma:**
   - Metin alanına yazdırmak istediğiniz metni girin
   - "Metin Yazdır" butonuna basın

5. **Örnek Fiş:**
   - "Örnek Fiş Yazdır" ile hazır fiş şablonu yazdırın

6. **ESC/POS Demo:**
   - "ESC/POS Demo Yazdır" ile tüm özellikleri test edin

## 🔐 İzinler

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📦 Bağımlılıklar

```kotlin
// ViewModel and Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## 🎨 UI Özellikleri

- Material 3 Design
- Dark/Light tema desteği
- Responsive tasarım
- Snackbar bildirimleri
- Loading states
- Error handling

## 🔍 Hata Ayıklama

### Yaygın Sorunlar ve Çözümleri

1. **"Bağlantı zaman aşımına uğradı"**
   - Yazıcının açık olduğundan emin olun
   - Aynı WiFi ağında olduğunuzu kontrol edin
   - IP adresini kontrol edin

2. **"Yazıcı bulunamadı"**
   - IP adresini doğru girdiğinizden emin olun
   - Yazıcının network ayarlarını kontrol edin

3. **"Yazıcıya bağlanılamadı"**
   - Port numarasını kontrol edin (genelde 9100)
   - Yazıcının RAW TCP modunda olduğundan emin olun

4. **Türkçe karakterler yanlış çıkıyor**
   - Yazıcı PC857 karakter setini desteklemelidir
   - EscPosCommands sınıfı otomatik olarak ayarlar

## 🧪 Test Etme

1. **Test Yazdırma:**
   ```kotlin
   viewModel.printTest()
   ```

2. **Özel Metin:**
   ```kotlin
   viewModel.updatePrintText("Test metni")
   viewModel.printCustomText()
   ```

3. **Programatik Kullanım:**
   ```kotlin
   val repository = PrinterRepository()
   val result = repository.printCustomText("192.168.1.100", 9100, "Test")
   ```

## 📱 Desteklenen Yazıcılar

Bu uygulama ESC/POS protokolünü destekleyen tüm termal yazıcılarla çalışır:

- Epson TM serisi
- Star Micronics
- Bixolon
- Citizen
- Custom
- Ve diğer ESC/POS uyumlu yazıcılar

## 🔄 Geliştirme Önerileri

1. **Yazıcı Keşfi:**
   - Network tarama ile yazıcıları otomatik bulma
   - Bonjour/mDNS desteği

2. **Ayarlar:**
   - Yazıcı profilleri kaydetme
   - Son kullanılan IP'yi hatırlama

3. **Şablonlar:**
   - Özelleştirilebilir fiş şablonları
   - Logo yazdırma desteği

4. **QR Kod:**
   - QR kod oluşturma ve yazdırma

5. **Barkod:**
   - Barkod yazdırma desteği

## 📄 Lisans

Bu proje eğitim amaçlı geliştirilmiştir.

## 👨‍💻 Geliştirici Notları

### Coroutines Kullanımı

Tüm network işlemleri `Dispatchers.IO` üzerinde çalışır:

```kotlin
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        // Network işlemi
    }
}
```

### State Management

StateFlow ile reactive state yönetimi:

```kotlin
val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()
```

### Error Handling

Sealed class ile tip güvenli hata yönetimi:

```kotlin
sealed class PrintResult {
    data class Success(val message: String) : PrintResult()
    data class Error(val message: String) : PrintResult()
}
```

## 🎯 Sonuç

Bu uygulama, WiFi üzerinden termal yazıcılara ESC/POS komutları göndermek için eksiksiz bir çözüm sunar. Modern Android geliştirme pratiklerini (Jetpack Compose, MVVM, Coroutines) kullanarak temiz ve sürdürülebilir bir kod tabanı oluşturulmuştur.

Uygulamayı Android Studio'da açıp direkt çalıştırabilirsiniz. Herhangi bir ek konfigürasyon gerekmez.
# rawbt-app
