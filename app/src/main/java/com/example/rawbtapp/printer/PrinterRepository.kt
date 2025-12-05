package com.example.rawbtapp.printer

import com.example.rawbtapp.model.ReceiptData

/**
 * Yazıcı işlemleri için Repository katmanı
 * PrinterClient'ı sarmallar ve iş mantığını yönetir
 */
class PrinterRepository {
    
    private val printerClient = PrinterClient()
    
    /**
     * Özel metin yazdır
     */
    suspend fun printCustomText(
        ipAddress: String,
        port: Int,
        text: String,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        if (text.isBlank()) {
            return PrintResult.Error("Yazdırılacak metin boş olamaz")
        }
        
        val printData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            alignCenter()
            boldTextLine("YAZDIRMA")
            newLine()
            alignLeft()
            horizontalLine()
            textLine(text)
            horizontalLine()
            newLine()
            alignCenter()
            textLine("Tarih: ${getCurrentDateTime()}")
            feedPaper(3)
            cutPaper()
        }
        
        return printerClient.print(ipAddress, port, printData)
    }
    
    /**
     * Test yazdırma
     */
    suspend fun printTest(
        ipAddress: String,
        port: Int,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        return printerClient.printTest(ipAddress, port, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars)
    }
    
    /**
     * Özel metin ile basit test
     */
    suspend fun printCustomTest(
        ipAddress: String,
        port: Int,
        customText: String,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val printData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            alignCenter()
            doubleTextLine("TEST YAZDIR")
            newLine()
            alignLeft()
            horizontalLine()
            textLine(customText)
            horizontalLine()
            newLine()
            alignCenter()
            textLine("Tarih: ${getCurrentDateTime()}")
            if (cutPaper) {
                feedPaper(cutFeedLines)
                cutPaper()
            } else {
                feedPaper(3)
            }
        }
        return printerClient.print(ipAddress, port, printData)
    }
    
    /**
     * Full test - Tüm encoding'leri dener (sadece kullanıcının metni + encoding adı, sonunda kesme)
     */
    suspend fun printFullTest(
        ipAddress: String,
        port: Int,
        customText: String,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val allEncodings = com.example.rawbtapp.printer.CharsetEncodingOptions.allEncodingsForFullTest
        val testResults = mutableListOf<String>()
        
        allEncodings.forEach { encoding ->
            try {
                // Encoding değerini direkt kullan (zaten doğru formatta)
                // PC3846_, PC857_, PC857_61_, PC850_, PC858_, PC437_ ile başlayanlar karakter seti komutu gönderir
                // NONE_ ile başlayanlar sadece encoding kullanır
                val charsetValue = encoding
                
                val printData = buildEscPosCommand(charsetValue, cancelTurkishChars) {
                    initialize() // ESC @ + karakter seti komutu (varsa) veya sadece ESC @
                    textLine("$customText - $encoding")
                    newLine(1) // Sadece bir satır boşluk, kesme yok (aralarında)
                }
                
                val result = printerClient.print(ipAddress, port, printData)
                when (result) {
                    is PrintResult.Success -> {
                        testResults.add("✓ $encoding: Başarılı")
                    }
                    is PrintResult.Error -> {
                        testResults.add("✗ $encoding: ${result.message}")
                    }
                }
                
                // Her test arasında kısa bir bekleme
                kotlinx.coroutines.delay(300)
            } catch (e: Exception) {
                testResults.add("✗ $encoding: ${e.message}")
            }
        }
        
        // Tüm testler bittikten sonra kesme yap
        if (cutPaper) {
            try {
                val cutData = buildEscPosCommand("NONE_UTF8") {
                    initialize()
                    feedPaper(cutFeedLines)
                    cutPaper()
                }
                printerClient.print(ipAddress, port, cutData)
            } catch (e: Exception) {
                // Kesme hatası önemli değil
            }
        }
        
        return PrintResult.Success("Full test tamamlandı. ${testResults.size} encoding test edildi.")
    }
    
    /**
     * Detaylı test - IP, port, yazıcı bilgileriyle
     */
    suspend fun printDetailedTest(
        printer: com.example.rawbtapp.model.Printer,
        customText: String
    ): PrintResult {
        val printData = buildEscPosCommand(printer.charsetEncoding, printer.cancelTurkishChars) {
            initialize()
            alignCenter()
            doubleTextLine("DETAYLI TEST")
            newLine()
            alignLeft()
            horizontalLine()
            boldTextLine("YAZICI BİLGİLERİ")
            horizontalLine()
            textLine("Ad: ${printer.name}")
            textLine("Numara: #${printer.number}")
            textLine("IP Adresi: ${printer.ipAddress}")
            textLine("Port: ${printer.port}")
            horizontalLine()
            boldTextLine("AYARLAR")
            horizontalLine()
            textLine("Kağıt Kesme: ${if (printer.cutPaper) "Aktif" else "Kapalı"}")
            textLine("Kesme Öncesi Boşluk: ${printer.cutFeedLines} satır")
            textLine("Karakter Seti: ${com.example.rawbtapp.printer.CharsetEncodingOptions.getOptionByValue(printer.charsetEncoding)?.displayName ?: printer.charsetEncoding}")
            horizontalLine()
            boldTextLine("TEST METNİ")
            horizontalLine()
            textLine(customText)
            horizontalLine()
            newLine()
            alignCenter()
            textLine("Tarih: ${getCurrentDateTime()}")
            newLine(2)
            alignCenter()
            textLine("Test Başarılı!")
            if (printer.cutPaper) {
                feedPaper(printer.cutFeedLines)
                cutPaper()
            } else {
                feedPaper(3)
            }
        }
        return printerClient.print(printer.ipAddress, printer.port, printData)
    }
    
    /**
     * Örnek fiş yazdır
     * Sadece logo ve footer ile, printer bilgisi olmadan
     */
    suspend fun printSampleReceipt(
        ipAddress: String,
        port: Int,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val receiptData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            
            // Logo ekle (eğer aktifse)
            if (PrintConstants.SHOW_LOGO) {
                alignCenter()
                val logoLines = PrintConstants.RECEIPT_LOGO.trimIndent().split("\n")
                logoLines.forEach { line ->
                    textLine(line)
                }
                newLine(PrintConstants.LOGO_SPACING)
            }
            
            // Başlık
            alignCenter()
            doubleTextLine("ÖRNEK FİŞ")
            newLine()
            
            // Tarih
            alignLeft()
            horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            twoColumnText("Tarih:", getCurrentDateTime())
            horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            newLine()
            
            // Ürünler (Web'den gelecek içerik burada olacak)
            boldTextLine("ÜRÜNLER")
            horizontalLine()
            twoColumnText("Çay", "15.00 TL")
            twoColumnText("Kahve", "25.00 TL")
            twoColumnText("Börek", "30.00 TL")
            twoColumnText("Çiğ Köfte", "40.00 TL")
            horizontalLine()
            
            // Toplam
            newLine()
            alignRight()
            boldTextLine("TOPLAM: 110.00 TL")
            horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            
            // Footer ekle (eğer aktifse)
            if (PrintConstants.SHOW_FOOTER) {
                newLine(PrintConstants.FOOTER_TOP_SPACING)
                horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
                alignCenter()
                
                val footerLines = PrintConstants.FOOTER_THANK_YOU.split("\n")
                footerLines.forEach { line ->
                    textLine(line)
                }
                
                if (PrintConstants.FOOTER_WEBSITE.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_WEBSITE)
                }
                if (PrintConstants.FOOTER_PHONE.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_PHONE)
                }
                if (PrintConstants.FOOTER_ADDRESS.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_ADDRESS)
                }
                
                horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            }
            
            // Kağıt besle ve kes
            if (cutPaper) {
                feedPaper(cutFeedLines)
                cutPaper()
            } else {
                feedPaper(PrintConstants.FOOTER_BOTTOM_SPACING)
            }
        }
        
        return printerClient.print(ipAddress, port, receiptData)
    }
    
    /**
     * Tüm ESC/POS özelliklerini gösteren demo yazdırma
     */
    suspend fun printDemo(
        ipAddress: String,
        port: Int,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val demoData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            
            // Başlık
            alignCenter()
            doubleTextLine("ESC/POS DEMO")
            newLine()
            
            // Normal metin
            alignLeft()
            textLine("1. Normal Metin")
            textLine("Bu normal boyutta bir metindir.")
            newLine()
            
            // Kalın metin
            textLine("2. Kalın Metin")
            boldTextLine("Bu kalın (bold) bir metindir.")
            newLine()
            
            // Çift boyut
            textLine("3. Çift Boyut")
            doubleTextLine("Çift Boyut")
            newLine()
            
            // Hizalama
            textLine("4. Hizalama")
            alignLeft()
            textLine("Sola hizalı")
            alignCenter()
            textLine("Ortaya hizalı")
            alignRight()
            textLine("Sağa hizalı")
            alignLeft()
            newLine()
            
            // Çizgiler
            textLine("5. Yatay Çizgiler")
            horizontalLine("-")
            horizontalLine("=")
            horizontalLine("*")
            newLine()
            
            // İki sütun
            textLine("6. İki Sütunlu Metin")
            twoColumnText("Sol Taraf", "Sağ Taraf")
            twoColumnText("Ürün", "Fiyat")
            twoColumnText("Toplam", "100.00 TL")
            newLine()
            
            // Türkçe karakterler
            textLine("7. Türkçe Karakter Testi")
            textLine("ÇçĞğİıÖöŞşÜü")
            newLine()
            
            // Bitiş
            alignCenter()
            horizontalLine("=")
            textLine("DEMO TAMAMLANDI")
            horizontalLine("=")
            
            feedPaper(3)
            cutPaper()
        }
        
        return printerClient.print(ipAddress, port, demoData)
    }
    
    /**
     * Web'den gelen fiş verisini yazdır
     * Deep link ile gelen ReceiptData'yı ESC/POS formatına çevirir
     */
    suspend fun printReceiptFromWeb(
        ipAddress: String,
        port: Int,
        receiptData: ReceiptData,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val printData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            
            // Başlık - İşletme bilgileri
            alignCenter()
            doubleTextLine(receiptData.merchant.name)
            textLine(receiptData.merchant.address)
            textLine(receiptData.merchant.city)
            textLine("Tel: ${receiptData.merchant.phone}")
            
            // Vergi numarası varsa
            receiptData.merchant.taxNumber?.let {
                textLine("Vergi No: $it")
            }
            newLine()
            
            // Fiş bilgileri
            alignLeft()
            horizontalLine("=")
            twoColumnText("Fiş No:", receiptData.receiptId)
            twoColumnText("Tarih:", receiptData.getFormattedDate())
            receiptData.paymentMethod?.let {
                twoColumnText("Ödeme:", it)
            }
            horizontalLine("=")
            newLine()
            
            // Ürünler başlığı
            boldTextLine("ÜRÜNLER")
            horizontalLine()
            
            // Ürün listesi
            for (item in receiptData.items) {
                textLine(item.name)
                val itemDetail = "${item.quantity} x ${receiptData.formatPrice(item.unitPrice)}"
                val itemTotal = receiptData.formatPrice(item.totalPrice)
                twoColumnText("  $itemDetail", itemTotal)
            }
            
            horizontalLine()
            newLine()
            
            // Toplam bilgileri
            alignRight()
            textLine("Ara Toplam: ${receiptData.formatPrice(receiptData.subtotal)}")
            textLine("KDV (%${receiptData.taxRate}): ${receiptData.formatPrice(receiptData.tax)}")
            horizontalLine("=")
            doubleTextLine("TOPLAM: ${receiptData.formatPrice(receiptData.totalAmount)}")
            horizontalLine("=")
            newLine()
            
            // Alt bilgi
            alignCenter()
            textLine("Bizi tercih ettiğiniz için")
            textLine("teşekkür ederiz!")
            newLine()
            
            // Kağıt besle ve kes
            if (cutPaper) {
                feedPaper(cutFeedLines)
                cutPaper()
            } else {
                feedPaper(4)
            }
        }
        
        return printerClient.print(ipAddress, port, printData)
    }
    
    /**
     * Fiş yazdırma (retry mekanizması ile)
     * Başarısız olursa belirtilen sayıda tekrar dener
     */
    suspend fun printReceiptWithRetry(
        ipAddress: String,
        port: Int,
        receiptData: ReceiptData,
        maxRetries: Int = 3,
        delayMillis: Long = 1000,
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        var lastError: String? = null
        
        repeat(maxRetries) { attempt ->
            val result = printReceiptFromWeb(ipAddress, port, receiptData, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars)
            
            when (result) {
                is PrintResult.Success -> return result
                is PrintResult.Error -> {
                    lastError = result.message
                    if (attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay(delayMillis)
                    }
                }
            }
        }
        
        return PrintResult.Error("Yazdırma başarısız (${maxRetries} deneme): $lastError")
    }
    
    /**
     * HTML içeriğini direkt yazdır
     * WebView'dan gelen HTML içeriğini ESC/POS formatına çevirir
     * Logo ve footer ile birlikte formatlanmış fiş yazdırır
     */
    suspend fun printHtmlContent(
        ipAddress: String,
        port: Int,
        htmlContent: String,
        title: String = "POS Fiş",
        cutPaper: Boolean = true,
        cutFeedLines: Int = 3,
        charsetEncoding: String = "PC857_CP857",
        cancelTurkishChars: Boolean = false
    ): PrintResult {
        val printData = buildEscPosCommand(charsetEncoding, cancelTurkishChars) {
            initialize()
            
            // Logo ekle (eğer aktifse)
            if (PrintConstants.SHOW_LOGO) {
                alignCenter()
                val logoLines = PrintConstants.RECEIPT_LOGO.trimIndent().split("\n")
                logoLines.forEach { line ->
                    textLine(line)
                }
                newLine(PrintConstants.LOGO_SPACING)
            }
            
            // Başlık
            alignCenter()
            doubleTextLine(title)
            newLine()
            horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            
            // HTML içeriğini satır satır yazdır
            alignLeft()
            val lines = htmlContent.split("\n")
            for (line in lines) {
                if (line.isNotBlank()) {
                    // Uzun satırları böl
                    if (line.length > PrintConstants.RECEIPT_WIDTH) {
                        val chunks = line.chunked(PrintConstants.RECEIPT_WIDTH)
                        chunks.forEach { chunk ->
                            textLine(chunk)
                        }
                    } else {
                        textLine(line)
                    }
                }
            }
            
            // Footer ekle (eğer aktifse)
            if (PrintConstants.SHOW_FOOTER) {
                newLine(PrintConstants.FOOTER_TOP_SPACING)
                horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
                alignCenter()
                
                val footerLines = PrintConstants.FOOTER_THANK_YOU.split("\n")
                footerLines.forEach { line ->
                    textLine(line)
                }
                
                if (PrintConstants.FOOTER_WEBSITE.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_WEBSITE)
                }
                if (PrintConstants.FOOTER_PHONE.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_PHONE)
                }
                if (PrintConstants.FOOTER_ADDRESS.isNotEmpty()) {
                    textLine(PrintConstants.FOOTER_ADDRESS)
                }
                
                horizontalLine(PrintConstants.HORIZONTAL_LINE_BOLD_CHAR)
            }
            
            // Kağıt besle ve kes
            if (cutPaper) {
                feedPaper(cutFeedLines)
                cutPaper()
            } else {
                feedPaper(PrintConstants.FOOTER_BOTTOM_SPACING)
            }
        }
        
        return printerClient.print(ipAddress, port, printData)
    }
    
    private fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
