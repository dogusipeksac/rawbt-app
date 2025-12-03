package com.example.rawbtapp.printer

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

/**
 * 80mm termal yazıcı için HTML fiş oluşturucu
 */
class ThermalReceiptBuilder(private val context: Context) {
    
    data class ReceiptItem(
        val name: String,
        val quantity: Double,
        val unitPrice: Double,
        val total: Double
    )
    
    data class ReceiptData(
        val receiptNumber: String,
        val cashier: String = "Admin",
        val items: List<ReceiptItem>,
        val subtotal: Double,
        val tax: Double,
        val taxRate: Double = 18.0,
        val discount: Double = 0.0,
        val shipping: Double = 0.0,
        val grandTotal: Double,
        val paymentType: String = "Nakit",
        val amountPaid: Double,
        val change: Double
    )
    
    /**
     * Fiş HTML'i oluştur
     */
    fun buildReceipt(data: ReceiptData): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val currentDate = dateFormat.format(Date())
        
        return """
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fiş #${data.receiptNumber}</title>
    <style>
        ${getStyles()}
    </style>
</head>
<body>
    <div class="receipt">
        <!-- HEADER -->
        <div class="header">
            <div class="logo">7 DAYS HAVACILIK</div>
            <div class="store-info">
                Stok Yönetim Sistemi<br>
                Tel: 0212 XXX XX XX<br>
                İstanbul, Türkiye
            </div>
        </div>

        <!-- RECEIPT INFO -->
        <div class="section">
            <table>
                <tr>
                    <td><strong>Fiş No:</strong></td>
                    <td class="align-right">#${data.receiptNumber}</td>
                </tr>
                <tr>
                    <td><strong>Tarih:</strong></td>
                    <td class="align-right">$currentDate</td>
                </tr>
                <tr>
                    <td><strong>Kasiyer:</strong></td>
                    <td class="align-right">${data.cashier}</td>
                </tr>
            </table>
        </div>

        <div class="divider-solid"></div>

        <!-- ITEMS -->
        <div class="section">
            <div class="section-title">Ürünler</div>
            <table>
                ${buildItemsHtml(data.items)}
            </table>
        </div>

        <div class="divider"></div>

        <!-- TOTALS -->
        <div class="totals">
            <table>
                <tr>
                    <td class="label">Ara Toplam:</td>
                    <td class="value">${formatPrice(data.subtotal)}</td>
                </tr>
                <tr>
                    <td class="label">KDV (${data.taxRate.toInt()}%):</td>
                    <td class="value">${formatPrice(data.tax)}</td>
                </tr>
                ${if (data.discount > 0) """
                <tr>
                    <td class="label">İndirim:</td>
                    <td class="value">-${formatPrice(data.discount)}</td>
                </tr>
                """ else ""}
                ${if (data.shipping > 0) """
                <tr>
                    <td class="label">Kargo:</td>
                    <td class="value">${formatPrice(data.shipping)}</td>
                </tr>
                """ else ""}
                <tr class="grand-total">
                    <td class="label">GENEL TOPLAM:</td>
                    <td class="value">${formatPrice(data.grandTotal)}</td>
                </tr>
            </table>
        </div>

        <div class="divider-solid"></div>

        <!-- PAYMENT -->
        <div class="payment">
            <div class="section-title">Ödeme Bilgileri</div>
            <table>
                <tr>
                    <td><strong>Ödeme Türü:</strong></td>
                    <td class="align-right">${data.paymentType}</td>
                </tr>
                <tr>
                    <td><strong>Ödenen:</strong></td>
                    <td class="align-right">${formatPrice(data.amountPaid)}</td>
                </tr>
                <tr>
                    <td><strong>Para Üstü:</strong></td>
                    <td class="align-right">${formatPrice(data.change)}</td>
                </tr>
            </table>
        </div>

        <div class="divider"></div>

        <!-- BARCODE -->
        <div class="barcode">
            <div style="font-size: 24px; letter-spacing: 1px;">||||| |||| |||||</div>
            <div class="barcode-number">*${data.receiptNumber}*</div>
        </div>

        <!-- FOOTER -->
        <div class="footer">
            <div style="margin-bottom: 5px;">
                <strong>Bizi tercih ettiğiniz için<br>teşekkür ederiz!</strong>
            </div>
            <div style="font-size: 9px; margin-top: 5px;">
                www.7dayshavacilik.com<br>
                İade ve değişim için 14 gün içinde<br>
                fişinizi saklayınız.
            </div>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * Ürünler HTML'i oluştur
     */
    private fun buildItemsHtml(items: List<ReceiptItem>): String {
        return items.joinToString("\n") { item ->
            """
                <tr>
                    <td colspan="3" class="item-name">${item.name}</td>
                </tr>
                <tr>
                    <td class="item-details">${formatQuantity(item.quantity)} Ad x ${formatPrice(item.unitPrice)}</td>
                    <td></td>
                    <td class="align-right"><strong>${formatPrice(item.total)}</strong></td>
                </tr>
                <tr><td colspan="3" style="height: 5px;"></td></tr>
            """.trimIndent()
        }
    }
    
    /**
     * Fiyat formatla
     */
    private fun formatPrice(price: Double): String {
        return String.format(Locale("tr", "TR"), "%.2f TL", price)
    }
    
    /**
     * Miktar formatla
     */
    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format(Locale("tr", "TR"), "%.2f", quantity)
        }
    }
    
    /**
     * CSS stilleri
     */
    private fun getStyles(): String {
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Courier New', Courier, monospace;
            font-size: 12px;
            line-height: 1.4;
            color: #000;
            background: #fff;
            width: 384px;
            margin: 0 auto;
            padding: 10px;
        }

        .receipt {
            width: 100%;
            max-width: 384px;
        }

        .header {
            text-align: center;
            margin-bottom: 15px;
            border-bottom: 2px dashed #000;
            padding-bottom: 10px;
        }

        .logo {
            font-size: 16px;
            font-weight: bold;
            margin-bottom: 5px;
            letter-spacing: 2px;
        }

        .store-info {
            font-size: 10px;
            line-height: 1.3;
        }

        .section {
            margin: 10px 0;
            padding: 5px 0;
        }

        .section-title {
            font-weight: bold;
            font-size: 13px;
            margin-bottom: 5px;
            text-transform: uppercase;
        }

        .divider {
            border-top: 1px dashed #000;
            margin: 8px 0;
        }

        .divider-solid {
            border-top: 2px solid #000;
            margin: 10px 0;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 5px 0;
        }

        td {
            padding: 2px 0;
            vertical-align: top;
        }

        .item-name {
            font-weight: bold;
        }

        .item-details {
            font-size: 11px;
        }

        .align-right {
            text-align: right;
        }

        .align-center {
            text-align: center;
        }

        .totals {
            margin-top: 10px;
        }

        .totals table td {
            padding: 3px 0;
        }

        .totals .label {
            width: 60%;
        }

        .totals .value {
            width: 40%;
            text-align: right;
        }

        .grand-total {
            font-weight: bold;
            font-size: 14px;
            border-top: 2px solid #000;
            border-bottom: 2px solid #000;
            padding: 5px 0 !important;
            margin-top: 5px;
        }

        .payment {
            margin-top: 10px;
        }

        .payment table td {
            padding: 2px 0;
        }

        .footer {
            text-align: center;
            margin-top: 15px;
            padding-top: 10px;
            border-top: 2px dashed #000;
            font-size: 10px;
        }

        .barcode {
            margin: 10px 0;
            text-align: center;
        }

        .barcode-number {
            font-size: 10px;
            letter-spacing: 2px;
            margin-top: 5px;
        }

        @media print {
            @page {
                size: 80mm auto;
                margin: 0;
            }

            body {
                width: 80mm;
                max-width: 80mm;
                margin: 0;
                padding: 3mm;
                background: #fff;
                color: #000;
            }

            .receipt {
                width: 100%;
                max-width: 100%;
            }

            * {
                color: #000 !important;
                background: #fff !important;
                -webkit-print-color-adjust: exact;
                print-color-adjust: exact;
            }

            .no-print {
                display: none !important;
            }

            .receipt,
            .section,
            table,
            tr,
            td {
                page-break-inside: avoid;
            }

            body {
                font-size: 12px;
            }

            .logo {
                font-size: 16px;
            }

            .section-title {
                font-size: 13px;
            }

            .grand-total {
                font-size: 14px;
            }

            .divider {
                border-top: 1px dashed #000 !important;
            }

            .divider-solid {
                border-top: 2px solid #000 !important;
            }

            .grand-total {
                border-top: 2px solid #000 !important;
                border-bottom: 2px solid #000 !important;
            }
        }

        @media print and (-webkit-min-device-pixel-ratio:0) {
            body {
                -webkit-print-color-adjust: exact;
            }
        }
        """.trimIndent()
    }
    
    companion object {
        /**
         * Örnek fiş verisi
         */
        fun getSampleReceipt(): ReceiptData {
            val items = listOf(
                ReceiptItem("Caramel Latte", 1.0, 55.0, 55.0),
                ReceiptItem("Espresso", 2.0, 25.0, 50.0),
                ReceiptItem("Croissant", 3.0, 15.0, 45.0)
            )
            
            val subtotal = 150.0
            val tax = subtotal * 0.18
            val discount = 10.0
            val grandTotal = subtotal + tax - discount
            val amountPaid = 200.0
            val change = amountPaid - grandTotal
            
            return ReceiptData(
                receiptNumber = "12345",
                cashier = "Admin",
                items = items,
                subtotal = subtotal,
                tax = tax,
                taxRate = 18.0,
                discount = discount,
                shipping = 0.0,
                grandTotal = grandTotal,
                paymentType = "Nakit",
                amountPaid = amountPaid,
                change = change
            )
        }
    }
}
