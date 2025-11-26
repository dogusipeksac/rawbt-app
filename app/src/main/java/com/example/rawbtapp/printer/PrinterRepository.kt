package com.example.rawbtapp.printer

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
    
    private fun getCurrentDateTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
