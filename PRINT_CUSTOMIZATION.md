# Fiş Yazdırma Özelleştirme Rehberi

Bu rehber, fiş yazdırma özelliklerini nasıl özelleştirebileceğinizi açıklar.

## 📋 İçindekiler
- [Logo Özelleştirme](#logo-özelleştirme)
- [Footer (Alt Bilgi) Özelleştirme](#footer-alt-bilgi-özelleştirme)
- [Genel Ayarlar](#genel-ayarlar)
- [Türkçe Karakter Desteği](#türkçe-karakter-desteği)

---

## 🎨 Logo Özelleştirme

### Logo'yu Değiştirme

`PrintConstants.kt` dosyasını açın ve `RECEIPT_LOGO` değişkenini düzenleyin:

```kotlin
const val RECEIPT_LOGO = """
    ╔═══════════════════════════╗
    ║   7 DAYS RESTAURANT & CAFE       ║
    ║      POS SİSTEMİ          ║
    ╚═══════════════════════════╝
"""
```

### Kendi Logo'nuzu Oluşturma

ASCII art kullanarak kendi logo'nuzu oluşturabilirsiniz:

```kotlin
const val RECEIPT_LOGO = """
    ********************************
    *                              *
    *      7 DAYS        *
    *      Tel: 0212 XXX XX XX     *
    *                              *
    ********************************
"""
```

veya daha basit:

```kotlin
const val RECEIPT_LOGO = """
    ============================
         RESTAURANT ADI
    ============================
"""
```

### Logo'yu Kapatma

Logo'yu göstermek istemiyorsanız:

```kotlin
const val SHOW_LOGO = false
```

### Logo Boşluğu

Logo'dan sonra kaç satır boşluk bırakılacağını ayarlayın:

```kotlin
const val LOGO_SPACING = 1  // 0-5 arası değer önerilir
```

---

## 📝 Footer (Alt Bilgi) Özelleştirme

### Teşekkür Mesajı

```kotlin
const val FOOTER_THANK_YOU = "Bizi tercih ettiğiniz için\nteşekkür ederiz!"
```

Çok satırlı mesaj için `\n` kullanın:

```kotlin
const val FOOTER_THANK_YOU = "Teşekkürler!\nTekrar bekleriz\nAfiyet olsun"
```

### Web Sitesi

```kotlin
const val FOOTER_WEBSITE = "www.ornekrestoran.com"
```

Göstermek istemiyorsanız boş bırakın:

```kotlin
const val FOOTER_WEBSITE = ""
```

### Telefon Numarası

```kotlin
const val FOOTER_PHONE = "Tel: 0212 123 45 67"
```

### Adres

```kotlin
const val FOOTER_ADDRESS = "İstanbul, Türkiye"
```

Daha detaylı adres:

```kotlin
const val FOOTER_ADDRESS = "Atatürk Cad. No:123\nKadıköy, İstanbul"
```

### Footer'ı Kapatma

Footer'ı göstermek istemiyorsanız:

```kotlin
const val SHOW_FOOTER = false
```

### Footer Boşlukları

Footer'dan önce ve sonra boşluk ayarları:

```kotlin
const val FOOTER_TOP_SPACING = 2      // Footer'dan önce kaç satır boşluk
const val FOOTER_BOTTOM_SPACING = 4   // Footer'dan sonra kaç satır boşluk (kağıt besleme)
```

---

## ⚙️ Genel Ayarlar

### Fiş Genişliği

Standart 80mm termal yazıcı için:

```kotlin
const val RECEIPT_WIDTH = 32  // 32-48 karakter arası önerilir
```

- **32 karakter**: Standart 80mm yazıcı
- **48 karakter**: Geniş yazıcılar için

### Yatay Çizgi Karakterleri

```kotlin
const val HORIZONTAL_LINE_CHAR = "-"        // Normal çizgi
const val HORIZONTAL_LINE_BOLD_CHAR = "="   // Kalın çizgi
```

Alternatif karakterler:
- `"*"` - Yıldız
- `"#"` - Diyez
- `"~"` - Tilda
- `"_"` - Alt çizgi

### Tarih Formatı

```kotlin
const val DATE_FORMAT = "dd/MM/yyyy HH:mm:ss"
```

Alternatif formatlar:
- `"dd.MM.yyyy HH:mm"` - 03.12.2025 16:30
- `"yyyy-MM-dd HH:mm:ss"` - 2025-12-03 16:30:45
- `"dd MMM yyyy"` - 03 Ara 2025

---

## 🇹🇷 Türkçe Karakter Desteği

### Encoding Ayarları

Türkçe karakterler için en uygun encoding:

```kotlin
const val TURKISH_CHARSET = "Windows-1254"
const val ESCPOS_CHARSET_TURKISH: Byte = 0x0D  // PC857 (Turkish)
```

### Desteklenen Türkçe Karakterler

Sistem şu Türkçe karakterleri tam olarak destekler:
- **Büyük harfler**: Ç, Ğ, İ, Ö, Ş, Ü
- **Küçük harfler**: ç, ğ, ı, ö, ş, ü

### Encoding Test

Türkçe karakterlerin doğru yazdırıldığını test etmek için:

```kotlin
TurkishCharacterEncoder.testEncoding("Çağrı, Şişli'de güzel bir öğle yemeği yedi.")
```

Bu test, logcat'te encoding detaylarını gösterir.

---

## 📖 Örnek Kullanımlar

### Örnek 1: Minimalist Fiş

```kotlin
// Logo'yu kapat
const val SHOW_LOGO = false

// Basit footer
const val FOOTER_THANK_YOU = "Teşekkürler!"
const val FOOTER_WEBSITE = ""
const val FOOTER_PHONE = ""
const val FOOTER_ADDRESS = ""

// Az boşluk
const val FOOTER_TOP_SPACING = 1
const val FOOTER_BOTTOM_SPACING = 2
```

### Örnek 2: Detaylı Fiş

```kotlin
// Büyük logo
const val RECEIPT_LOGO = """
    ╔════════════════════════════╗
    ║                            ║
    ║    LÜKS RESTAURANT         ║
    ║    Gourmet Mutfak          ║
    ║                            ║
    ╚════════════════════════════╝
"""

// Detaylı footer
const val FOOTER_THANK_YOU = "Bizi tercih ettiğiniz için\nçok teşekkür ederiz!\nTekrar bekleriz"
const val FOOTER_WEBSITE = "www.luksrestaurant.com"
const val FOOTER_PHONE = "Tel: 0212 123 45 67"
const val FOOTER_ADDRESS = "Nişantaşı Mah. Lüks Sok. No:1\nŞişli, İstanbul"

// Geniş boşluklar
const val FOOTER_TOP_SPACING = 3
const val FOOTER_BOTTOM_SPACING = 5
```

### Örnek 3: Cafe Fişi

```kotlin
const val RECEIPT_LOGO = """
    ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕
         COFFEE HOUSE
    ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕ ☕
"""

const val FOOTER_THANK_YOU = "Kahveniz için teşekkürler!\nİyi günler dileriz ☕"
const val FOOTER_WEBSITE = "instagram.com/coffeehouse"
const val FOOTER_PHONE = "Tel: 0212 XXX XX XX"
```

---

## 🔧 Gelişmiş Özelleştirme

### Özel Fonksiyonlar

`PrintConstants` sınıfında yardımcı fonksiyonlar:

```kotlin
// Metni ortala
PrintConstants.centerText("BAŞLIK", width = 32)

// İki sütunlu metin
PrintConstants.twoColumnText("Ürün", "Fiyat", width = 32)

// Yatay çizgi
PrintConstants.horizontalLine(char = "=", width = 32)
```

### Programatik Değişiklikler

Kod içinde dinamik olarak değiştirmek için:

```kotlin
val customLogo = """
    ╔═══════════════╗
    ║   ${restaurantName}   ║
    ╚═══════════════╝
"""

// Kullanımda
if (PrintConstants.SHOW_LOGO) {
    val logo = customLogo.trimIndent()
    // ... yazdırma işlemi
}
```

---

## 🐛 Sorun Giderme

### Türkçe Karakterler Düzgün Görünmüyor

1. `TURKISH_CHARSET = "Windows-1254"` olduğundan emin olun
2. `ESCPOS_CHARSET_TURKISH = 0x0D` değerini kontrol edin
3. Yazıcınızın PC857 karakter setini desteklediğinden emin olun

### Logo Bozuk Görünüyor

1. Logo genişliğinin `RECEIPT_WIDTH` değerini aşmadığından emin olun
2. ASCII karakterler kullanın (özel karakterler bazı yazıcılarda sorun çıkarabilir)
3. Her satırın aynı uzunlukta olmasına dikkat edin

### Çok Fazla/Az Boşluk

Boşluk ayarlarını değiştirin:
- `LOGO_SPACING`: Logo sonrası boşluk
- `FOOTER_TOP_SPACING`: Footer öncesi boşluk
- `FOOTER_BOTTOM_SPACING`: Footer sonrası boşluk (kağıt besleme)

---

## 📞 Destek

Sorunlarınız için:
1. Logcat'i kontrol edin: `TAG = "TurkishCharEncoder"`
2. Test fonksiyonunu kullanın: `TurkishCharacterEncoder.testEncoding()`
3. Yazıcı modelinizin ESC/POS komutlarını desteklediğinden emin olun

---

**Not**: Değişiklikleri yaptıktan sonra uygulamayı yeniden derleyin (Clean & Rebuild).
