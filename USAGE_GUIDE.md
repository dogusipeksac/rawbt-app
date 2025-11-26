# Kullanım Kılavuzu

## 📱 Uygulama Kurulumu

### 1. Projeyi Android Studio'da Açma

```bash
# Projeyi klonlayın veya indirin
cd /path/to/RawBTApp

# Android Studio'da açın
# File > Open > RawBTApp klasörünü seçin
```

### 2. Gradle Sync

Android Studio otomatik olarak Gradle sync yapacaktır. Eğer yapmazsa:
- File > Sync Project with Gradle Files

### 3. Uygulamayı Çalıştırma

- Run > Run 'app'
- Veya Shift + F10

## 🖨️ Yazıcı Hazırlığı

### Yazıcı IP Adresini Öğrenme

#### Yöntem 1: Yazıcı Ayarları
```
1. Yazıcının ayarlar butonuna basın
2. Network/WiFi ayarlarına gidin
3. "Print Network Configuration" seçeneğini bulun
4. IP adresini not edin
```

#### Yöntem 2: Router Arayüzü
```
1. Router arayüzüne giriş yapın (genelde 192.168.1.1)
2. Bağlı cihazlar listesine bakın
3. Yazıcınızı bulun ve IP adresini not edin
```

#### Yöntem 3: Network Scanner Uygulaması
```
1. Play Store'dan "Fing" veya benzeri bir uygulama indirin
2. Network taraması yapın
3. Yazıcınızı bulun (genelde "Printer" veya marka adı ile görünür)
```

### Yazıcı Port Numarası

Çoğu termal yazıcı **9100** portunu kullanır. Bazı modeller için:
- **9100** - RAW TCP (en yaygın)
- **515** - LPD (Line Printer Daemon)
- **631** - IPP (Internet Printing Protocol)

## 🎯 Temel Kullanım

### 1. İlk Bağlantı

```
1. Uygulamayı açın
2. IP Adresi alanına yazıcının IP'sini girin (örn: 192.168.1.100)
3. Port alanına 9100 girin
4. "Test Yazdır" butonuna basın
5. Yazıcıdan test sayfası çıkmalı
```

**Beklenen Çıktı:**
```
        TEST YAZDIR

Yazıcı Bağlantı Testi
--------------------------------
IP Adresi: 192.168.1.100
Port: 9100
--------------------------------
Tarih: 26/11/2024 20:30:45


    Test Başarılı!
```

### 2. Özel Metin Yazdırma

```
1. "Yazdırılacak Metin" alanına metninizi girin
2. "Metin Yazdır" butonuna basın
```

**Örnek Metin:**
```
Merhaba Dünya!
Bu bir test mesajıdır.
Türkçe karakterler: ÇçĞğİıÖöŞşÜü
```

**Çıktı:**
```
        YAZDIRMA

--------------------------------
Merhaba Dünya!
Bu bir test mesajıdır.
Türkçe karakterler: ÇçĞğİıÖöŞşÜü
--------------------------------

Tarih: 26/11/2024 20:30:45
```

### 3. Örnek Fiş Yazdırma

```
1. "Örnek Fiş Yazdır" butonuna basın
2. Hazır fiş şablonu yazdırılır
```

**Çıktı:**
```
        ÖRNEK FİŞ
    Termal Yazıcı Test

ABC Şirketi Ltd. Şti.
Atatürk Cad. No:123
İstanbul / Türkiye
Tel: 0212 123 45 67

================================
Tarih:           26/11/2024 20:30
Fiş No:                 2024-001
================================

ÜRÜNLER
--------------------------------
Ürün 1
  2 x 10.00 TL           20.00 TL

Ürün 2
  1 x 15.50 TL           15.50 TL

Ürün 3
  3 x 8.00 TL            24.00 TL
--------------------------------

                 ARA TOPLAM: 59.50 TL
                  KDV (%18): 10.71 TL
================================
        TOPLAM: 70.21 TL
================================

Bizi tercih ettiğiniz için
teşekkür ederiz!

www.orneksite.com
```

### 4. ESC/POS Demo Yazdırma

```
1. "ESC/POS Demo Yazdır" butonuna basın
2. Tüm ESC/POS özellikleri gösterilir
```

## 💻 Programatik Kullanım

### Temel Örnek

```kotlin
// ViewModel'i al
val viewModel: PrinterViewModel by viewModels()

// IP ve Port ayarla
viewModel.updateIpAddress("192.168.1.100")
viewModel.updatePort("9100")

// Test yazdır
viewModel.printTest()

// Özel metin yazdır
viewModel.updatePrintText("Merhaba Dünya!")
viewModel.printCustomText()
```

### Repository Kullanımı

```kotlin
// Repository instance
val repository = PrinterRepository()

// Coroutine scope içinde
lifecycleScope.launch {
    val result = repository.printCustomText(
        ipAddress = "192.168.1.100",
        port = 9100,
        text = "Test mesajı"
    )
    
    when (result) {
        is PrintResult.Success -> {
            Log.d("Print", "Başarılı: ${result.message}")
        }
        is PrintResult.Error -> {
            Log.e("Print", "Hata: ${result.message}")
        }
    }
}
```

### Özel ESC/POS Komutları

```kotlin
// ESC/POS komutları oluştur
val customData = buildEscPosCommand {
    initialize()
    
    // Başlık
    alignCenter()
    doubleTextLine("ÖZEL FİŞ")
    newLine()
    
    // İçerik
    alignLeft()
    boldTextLine("Müşteri Bilgileri:")
    textLine("Ad: Ahmet Yılmaz")
    textLine("Tel: 0555 123 45 67")
    newLine()
    
    // Tablo
    horizontalLine("=")
    twoColumnText("Ürün", "Fiyat")
    horizontalLine("-")
    twoColumnText("Kahve", "15.00 TL")
    twoColumnText("Çay", "10.00 TL")
    horizontalLine("=")
    
    // Toplam
    alignRight()
    boldTextLine("TOPLAM: 25.00 TL")
    
    // Bitiş
    feedPaper(3)
    cutPaper()
}

// Yazdır
val printerClient = PrinterClient()
lifecycleScope.launch {
    val result = printerClient.print(
        ipAddress = "192.168.1.100",
        port = 9100,
        data = customData
    )
}
```

## 🎨 Gelişmiş Örnekler

### 1. Restoran Fişi

```kotlin
fun printRestaurantReceipt(
    tableNumber: String,
    items: List<MenuItem>,
    total: Double
) {
    val receiptData = buildEscPosCommand {
        initialize()
        
        // Logo/Başlık
        alignCenter()
        doubleTextLine("LEZZET RESTAURANT")
        textLine("Gourmet Yemek Deneyimi")
        textLine("Tel: 0212 555 12 34")
        newLine()
        
        // Masa ve tarih
        alignLeft()
        horizontalLine("=")
        twoColumnText("Masa:", tableNumber)
        twoColumnText("Tarih:", getCurrentDateTime())
        twoColumnText("Garson:", "Mehmet")
        horizontalLine("=")
        newLine()
        
        // Siparişler
        boldTextLine("SİPARİŞLER")
        horizontalLine()
        
        items.forEach { item ->
            textLine(item.name)
            twoColumnText(
                "  ${item.quantity} x ${item.price} TL",
                "${item.total} TL"
            )
        }
        
        horizontalLine()
        newLine()
        
        // Toplam
        alignRight()
        textLine("Ara Toplam: ${total} TL")
        textLine("KDV (%18): ${total * 0.18} TL")
        horizontalLine("=")
        doubleTextLine("TOPLAM: ${total * 1.18} TL")
        horizontalLine("=")
        
        // Alt bilgi
        newLine()
        alignCenter()
        textLine("Afiyet olsun!")
        textLine("Tekrar bekleriz")
        
        feedPaper(4)
        cutPaper()
    }
    
    // Yazdır
    viewModel.printCustomData(receiptData)
}

data class MenuItem(
    val name: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)
```

### 2. Kargo Etiketi

```kotlin
fun printShippingLabel(
    orderNumber: String,
    sender: Address,
    receiver: Address
) {
    val labelData = buildEscPosCommand {
        initialize()
        
        // Başlık
        alignCenter()
        boldTextLine("KARGO ETİKETİ")
        textLine("Sipariş No: $orderNumber")
        newLine()
        
        // Gönderici
        alignLeft()
        boldTextLine("GÖNDERİCİ:")
        horizontalLine()
        textLine(sender.name)
        textLine(sender.address)
        textLine("${sender.city} / ${sender.country}")
        textLine("Tel: ${sender.phone}")
        newLine()
        
        // Alıcı
        boldTextLine("ALICI:")
        horizontalLine()
        textLine(receiver.name)
        textLine(receiver.address)
        textLine("${receiver.city} / ${receiver.country}")
        textLine("Tel: ${receiver.phone}")
        newLine()
        
        // Barkod (metin olarak)
        alignCenter()
        textLine("* $orderNumber *")
        
        feedPaper(3)
        cutPaper()
    }
    
    // Yazdır
    viewModel.printCustomData(labelData)
}

data class Address(
    val name: String,
    val address: String,
    val city: String,
    val country: String,
    val phone: String
)
```

### 3. Etkinlik Bileti

```kotlin
fun printEventTicket(
    eventName: String,
    date: String,
    time: String,
    venue: String,
    seatNumber: String,
    ticketNumber: String
) {
    val ticketData = buildEscPosCommand {
        initialize()
        
        // Başlık
        alignCenter()
        doubleTextLine("ETKİNLİK BİLETİ")
        newLine()
        
        // Etkinlik bilgileri
        boldTextLine(eventName)
        newLine()
        
        alignLeft()
        horizontalLine("=")
        twoColumnText("Tarih:", date)
        twoColumnText("Saat:", time)
        twoColumnText("Mekan:", venue)
        twoColumnText("Koltuk:", seatNumber)
        horizontalLine("=")
        newLine()
        
        // Bilet numarası
        alignCenter()
        textLine("Bilet No:")
        boldTextLine(ticketNumber)
        newLine()
        
        // QR kod placeholder
        textLine("[QR KOD]")
        textLine(ticketNumber)
        newLine()
        
        // Uyarılar
        alignLeft()
        textLine("* Biletinizi saklayin")
        textLine("* Giris sirasinda gosteriniz")
        textLine("* Iade ve degisim yoktur")
        
        feedPaper(3)
        cutPaper()
    }
    
    // Yazdır
    viewModel.printCustomData(ticketData)
}
```

## 🔧 Sorun Giderme

### Yaygın Hatalar ve Çözümleri

#### 1. "Bağlantı zaman aşımına uğradı"

**Nedenleri:**
- Yazıcı kapalı
- Farklı WiFi ağında
- Yanlış IP adresi

**Çözümler:**
```
✓ Yazıcının açık olduğunu kontrol edin
✓ Yazıcının WiFi ağına bağlı olduğunu kontrol edin
✓ Telefonun aynı WiFi ağında olduğunu kontrol edin
✓ IP adresini yazıcıdan tekrar yazdırın
✓ Yazıcıyı yeniden başlatın
```

#### 2. "Yazıcı bulunamadı"

**Nedenleri:**
- Yanlış IP adresi
- Network problemi

**Çözümler:**
```
✓ IP adresini kontrol edin
✓ Ping testi yapın: ping 192.168.1.100
✓ Network scanner ile yazıcıyı bulun
✓ Yazıcının network ayarlarını kontrol edin
```

#### 3. "Yazıcıya bağlanılamadı"

**Nedenleri:**
- Yanlış port numarası
- Yazıcı RAW TCP modunda değil

**Çözümler:**
```
✓ Port numarasını kontrol edin (genelde 9100)
✓ Yazıcının RAW TCP modunu etkinleştirin
✓ Yazıcı ayarlarından port numarasını kontrol edin
✓ Farklı portları deneyin (9100, 515, 631)
```

#### 4. Türkçe Karakterler Yanlış Çıkıyor

**Nedenleri:**
- Karakter seti uyumsuzluğu

**Çözümler:**
```
✓ EscPosCommands otomatik PC857 kullanır
✓ Yazıcının karakter seti ayarlarını kontrol edin
✓ Yazıcı dokümantasyonuna bakın
```

#### 5. Kağıt Kesilmiyor

**Nedenleri:**
- Yazıcı kesme özelliğini desteklemiyor
- Kesme komutu uyumsuz

**Çözümler:**
```
✓ cutPaper() yerine cutPaper(partial = true) deneyin
✓ Yazıcı dokümantasyonuna bakın
✓ Manuel kesim yapın
```

### Debug Modu

Detaylı log için:

```kotlin
// PrinterClient.kt içinde
private const val DEBUG = true

if (DEBUG) {
    Log.d("PrinterClient", "Connecting to $ipAddress:$port")
    Log.d("PrinterClient", "Data size: ${data.size} bytes")
}
```

## 📊 Test Senaryoları

### Test 1: Temel Bağlantı
```
1. IP: 192.168.1.100
2. Port: 9100
3. Test Yazdır
4. Beklenen: Test sayfası çıkmalı
```

### Test 2: Özel Metin
```
1. Metin: "Test 123 ÇçĞğİıÖöŞşÜü"
2. Metin Yazdır
3. Beklenen: Metin doğru çıkmalı
```

### Test 3: Uzun Metin
```
1. 500 karakterlik metin girin
2. Metin Yazdır
3. Beklenen: Tüm metin yazdırılmalı
```

### Test 4: Hızlı Ardışık Yazdırma
```
1. Test Yazdır butonuna 3 kez hızlıca basın
2. Beklenen: 3 sayfa çıkmalı
```

### Test 5: Hatalı IP
```
1. IP: 999.999.999.999
2. Test Yazdır
3. Beklenen: "Geçersiz IP adresi" hatası
```

## 🎓 İpuçları

### 1. IP Adresini Kaydetme

```kotlin
// SharedPreferences kullanarak
val prefs = getSharedPreferences("printer_prefs", MODE_PRIVATE)

// Kaydet
prefs.edit().putString("last_ip", "192.168.1.100").apply()

// Oku
val lastIp = prefs.getString("last_ip", "192.168.1.100")
viewModel.updateIpAddress(lastIp ?: "")
```

### 2. Çoklu Yazıcı Desteği

```kotlin
data class PrinterProfile(
    val name: String,
    val ipAddress: String,
    val port: Int
)

val printers = listOf(
    PrinterProfile("Mutfak", "192.168.1.100", 9100),
    PrinterProfile("Kasa", "192.168.1.101", 9100),
    PrinterProfile("Bar", "192.168.1.102", 9100)
)
```

### 3. Offline Yazdırma Kuyruğu

```kotlin
// Bağlantı yoksa kuyruğa ekle
if (!isConnected) {
    printQueue.add(printData)
}

// Bağlantı geldiğinde yazdır
if (isConnected) {
    printQueue.forEach { data ->
        printerClient.print(ip, port, data)
    }
    printQueue.clear()
}
```

## 📚 Ek Kaynaklar

### ESC/POS Komut Referansı
- [Epson ESC/POS Documentation](https://reference.epson-biz.com/modules/ref_escpos/index.php)
- [Star Micronics Command Reference](https://www.starmicronics.com/support/Mannualfolder/escpos_cm_en.pdf)

### Android Geliştirme
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [MVVM Architecture](https://developer.android.com/topic/architecture)

## 🎯 Sonuç

Bu kılavuz, WiFi termal yazıcı uygulamasının tüm özelliklerini kullanmanız için gereken bilgileri içerir. Herhangi bir sorunla karşılaşırsanız, sorun giderme bölümüne bakın veya yazıcı dokümantasyonunu kontrol edin.

Başarılı yazdırmalar! 🖨️✨
