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
        text: String
    ): PrintResult {
        if (text.isBlank()) {
            return PrintResult.Error("Yazdırılacak metin boş olamaz")
        }
        
        val printData = buildEscPosCommand {
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
        port: Int
    ): PrintResult {
        return printerClient.printTest(ipAddress, port)
    }
    
    /**
     * Örnek fiş yazdır
     */
    suspend fun printSampleReceipt(
        ipAddress: String,
        port: Int
    ): PrintResult {
        val receiptData = buildEscPosCommand {
            initialize()
            
            // Başlık
            alignCenter()
            doubleTextLine("ÖRNEK FİŞ")
            textLine("Termal Yazıcı Test")
            newLine()
            
            // Firma bilgileri
            textLine("ABC Şirketi Ltd. Şti.")
            textLine("Atatürk Cad. No:123")
            textLine("İstanbul / Türkiye")
            textLine("Tel: 0212 123 45 67")
            newLine()
            
            // Tarih ve fiş no
            alignLeft()
            horizontalLine("=")
            twoColumnText("Tarih:", getCurrentDateTime())
            twoColumnText("Fiş No:", "2024-001")
            horizontalLine("=")
            newLine()
            
            // Ürünler
            boldTextLine("ÜRÜNLER")
            horizontalLine()
            textLine("Ürün 1")
            twoColumnText("  2 x 10.00 TL", "20.00 TL")
            newLine()
            textLine("Ürün 2")
            twoColumnText("  1 x 15.50 TL", "15.50 TL")
            newLine()
            textLine("Ürün 3")
            twoColumnText("  3 x 8.00 TL", "24.00 TL")
            horizontalLine()
            
            // Toplam
            newLine()
            alignRight()
            boldTextLine("ARA TOPLAM: 59.50 TL")
            textLine("KDV (%18): 10.71 TL")
            horizontalLine("=")
            doubleTextLine("TOPLAM: 70.21 TL")
            horizontalLine("=")
            
            // Alt bilgi
            newLine()
            alignCenter()
            textLine("Bizi tercih ettiğiniz için")
            textLine("teşekkür ederiz!")
            newLine()
            textLine("www.orneksite.com")
            
            // Kağıt besle ve kes
            feedPaper(4)
            cutPaper()
        }
        
        return printerClient.print(ipAddress, port, receiptData)
    }
    
    /**
     * Tüm ESC/POS özelliklerini gösteren demo yazdırma
     */
    suspend fun printDemo(
        ipAddress: String,
        port: Int
    ): PrintResult {
        val demoData = buildEscPosCommand {
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
        receiptData: ReceiptData
    ): PrintResult {
        val printData = buildEscPosCommand {
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
            feedPaper(4)
            cutPaper()
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
        delayMillis: Long = 1000
    ): PrintResult {
        var lastError: String? = null
        
        repeat(maxRetries) { attempt ->
            val result = printReceiptFromWeb(ipAddress, port, receiptData)
            
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
        title: String = "POS Fiş"
    ): PrintResult {
        val printData = buildEscPosCommand {
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
            feedPaper(PrintConstants.FOOTER_BOTTOM_SPACING)
            cutPaper()
        }
        
        return printerClient.print(ipAddress, port, printData)
    }
    
    private fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
