/**
 * POS Printer Bridge - Web to Mobile Deep Linking
 * 
 * Bu modül web uygulamasından mobil uygulamaya deep link ile
 * yazıcı tetikleme işlemlerini yönetir.
 * 
 * @author RawBT Team
 * @version 1.0.0
 */

const POSPrinterBridge = (function() {
    'use strict';

    // Konfigürasyon
    const CONFIG = {
        scheme: 'rawbtapp',
        host: 'print',
        timeout: 3000, // App yüklü değilse fallback için timeout (ms)
        retryAttempts: 3,
        retryDelay: 1000
    };

    // Logger
    const Logger = {
        info: (message, data) => {
            console.log(`[POSPrinterBridge] ${message}`, data || '');
        },
        error: (message, error) => {
            console.error(`[POSPrinterBridge] ${message}`, error || '');
        },
        warn: (message, data) => {
            console.warn(`[POSPrinterBridge] ${message}`, data || '');
        }
    };

    /**
     * Fiş verisini validate eder
     */
    function validateReceiptData(data) {
        const required = ['receiptId', 'items', 'totalAmount', 'merchant'];
        
        for (const field of required) {
            if (!data[field]) {
                throw new Error(`Gerekli alan eksik: ${field}`);
            }
        }

        if (!Array.isArray(data.items) || data.items.length === 0) {
            throw new Error('En az bir ürün olmalı');
        }

        if (typeof data.totalAmount !== 'number' || data.totalAmount <= 0) {
            throw new Error('Geçersiz toplam tutar');
        }

        return true;
    }

    /**
     * JSON verisini URL-safe formata encode eder
     */
    function encodeReceiptData(data) {
        try {
            const jsonString = JSON.stringify(data);
            return encodeURIComponent(jsonString);
        } catch (error) {
            Logger.error('JSON encode hatası:', error);
            throw new Error('Veri encode edilemedi');
        }
    }

    /**
     * Deep link URL'i oluşturur
     */
    function buildDeepLink(receiptData) {
        const encodedData = encodeReceiptData(receiptData);
        return `${CONFIG.scheme}://${CONFIG.host}?data=${encodedData}`;
    }

    /**
     * Mobil uygulamanın yüklü olup olmadığını kontrol eder
     */
    function checkAppInstalled(callback) {
        let timeout;
        let blurred = false;

        const onBlur = () => {
            blurred = true;
            clearTimeout(timeout);
            callback(true);
        };

        const onFocus = () => {
            if (!blurred) {
                clearTimeout(timeout);
                callback(false);
            }
        };

        // Sayfa blur olursa uygulama açıldı demektir
        window.addEventListener('blur', onBlur);
        window.addEventListener('focus', onFocus);

        timeout = setTimeout(() => {
            window.removeEventListener('blur', onBlur);
            window.removeEventListener('focus', onFocus);
            if (!blurred) {
                callback(false);
            }
        }, CONFIG.timeout);
    }

    /**
     * Deep link'i tetikler
     */
    function triggerDeepLink(url) {
        Logger.info('Deep link tetikleniyor:', url);

        // iOS için
        if (isIOS()) {
            window.location.href = url;
        }
        // Android için
        else if (isAndroid()) {
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            document.body.appendChild(iframe);
            
            setTimeout(() => {
                document.body.removeChild(iframe);
            }, 1000);
        }
        // Diğer platformlar
        else {
            window.location.href = url;
        }
    }

    /**
     * Platform tespit fonksiyonları
     */
    function isIOS() {
        return /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
    }

    function isAndroid() {
        return /Android/.test(navigator.userAgent);
    }

    function isMobile() {
        return isIOS() || isAndroid();
    }

    /**
     * Fallback: Web print dialog'u açar
     */
    function fallbackToPrint(receiptData) {
        Logger.warn('Mobil uygulama bulunamadı, web print kullanılıyor');
        
        showMessage('Mobil uygulama yüklü değil. Web yazdırma kullanılıyor...', 'info');
        
        // Web print için HTML oluştur
        const printWindow = window.open('', '_blank');
        if (printWindow) {
            printWindow.document.write(generatePrintHTML(receiptData));
            printWindow.document.close();
            
            setTimeout(() => {
                printWindow.print();
            }, 500);
        } else {
            showMessage('Pop-up engellendi. Lütfen pop-up engelleyiciyi kapatın.', 'error');
        }
    }

    /**
     * Yazdırma için HTML oluşturur
     */
    function generatePrintHTML(data) {
        const items = data.items.map(item => `
            <tr>
                <td>${item.name}</td>
                <td style="text-align: center;">${item.quantity}</td>
                <td style="text-align: right;">${formatCurrency(item.unitPrice, data.currency)}</td>
                <td style="text-align: right;">${formatCurrency(item.totalPrice, data.currency)}</td>
            </tr>
        `).join('');

        return `
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Fiş - ${data.receiptId}</title>
                <style>
                    @media print {
                        body { margin: 0; padding: 20px; }
                    }
                    body {
                        font-family: 'Courier New', monospace;
                        max-width: 80mm;
                        margin: 0 auto;
                    }
                    h1 { text-align: center; font-size: 18px; }
                    .merchant { text-align: center; margin-bottom: 20px; }
                    .info { margin: 10px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 5px; border-bottom: 1px solid #ddd; }
                    .total { font-weight: bold; font-size: 16px; }
                    hr { border: 1px dashed #000; }
                </style>
            </head>
            <body>
                <h1>${data.merchant.name}</h1>
                <div class="merchant">
                    ${data.merchant.address}<br>
                    ${data.merchant.city}<br>
                    Tel: ${data.merchant.phone}<br>
                    ${data.merchant.taxNumber ? `Vergi No: ${data.merchant.taxNumber}` : ''}
                </div>
                <hr>
                <div class="info">
                    <strong>Fiş No:</strong> ${data.receiptId}<br>
                    <strong>Tarih:</strong> ${formatDate(data.timestamp)}
                </div>
                <hr>
                <table>
                    <thead>
                        <tr>
                            <th>Ürün</th>
                            <th>Adet</th>
                            <th>Fiyat</th>
                            <th>Toplam</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${items}
                    </tbody>
                </table>
                <hr>
                <table>
                    <tr>
                        <td>Ara Toplam:</td>
                        <td style="text-align: right;">${formatCurrency(data.subtotal, data.currency)}</td>
                    </tr>
                    <tr>
                        <td>KDV (%${data.taxRate}):</td>
                        <td style="text-align: right;">${formatCurrency(data.tax, data.currency)}</td>
                    </tr>
                    <tr class="total">
                        <td>TOPLAM:</td>
                        <td style="text-align: right;">${formatCurrency(data.totalAmount, data.currency)}</td>
                    </tr>
                </table>
                <hr>
                <div style="text-align: center; margin-top: 20px;">
                    <p>Teşekkür ederiz!</p>
                </div>
            </body>
            </html>
        `;
    }

    /**
     * Para birimi formatlar
     */
    function formatCurrency(amount, currency = 'TRY') {
        const symbols = {
            'TRY': '₺',
            'USD': '$',
            'EUR': '€'
        };
        
        return `${symbols[currency] || '₺'}${amount.toFixed(2)}`;
    }

    /**
     * Tarih formatlar
     */
    function formatDate(timestamp) {
        const date = new Date(timestamp);
        return date.toLocaleString('tr-TR');
    }

    /**
     * Kullanıcıya mesaj gösterir
     */
    function showMessage(message, type = 'info') {
        const statusDiv = document.getElementById('statusMessage');
        if (statusDiv) {
            statusDiv.textContent = message;
            statusDiv.className = `status-message status-${type}`;
            statusDiv.style.display = 'block';

            setTimeout(() => {
                statusDiv.style.display = 'none';
            }, 5000);
        }
    }

    /**
     * Retry mekanizması ile yazdırma
     */
    async function printWithRetry(receiptData, attempt = 1) {
        try {
            Logger.info(`Yazdırma denemesi ${attempt}/${CONFIG.retryAttempts}`);
            
            const deepLink = buildDeepLink(receiptData);
            triggerDeepLink(deepLink);

            return new Promise((resolve, reject) => {
                checkAppInstalled((isInstalled) => {
                    if (isInstalled) {
                        showMessage('Fiş mobil uygulamaya gönderildi!', 'success');
                        resolve(true);
                    } else {
                        if (attempt < CONFIG.retryAttempts) {
                            setTimeout(() => {
                                printWithRetry(receiptData, attempt + 1)
                                    .then(resolve)
                                    .catch(reject);
                            }, CONFIG.retryDelay);
                        } else {
                            fallbackToPrint(receiptData);
                            resolve(false);
                        }
                    }
                });
            });
        } catch (error) {
            Logger.error('Yazdırma hatası:', error);
            showMessage(`Hata: ${error.message}`, 'error');
            throw error;
        }
    }

    /**
     * Ana yazdırma fonksiyonu
     */
    async function print(receiptData) {
        try {
            // Mobil cihaz kontrolü
            if (!isMobile()) {
                Logger.warn('Mobil cihaz değil, direkt web print kullanılıyor');
                fallbackToPrint(receiptData);
                return;
            }

            // Veri validasyonu
            validateReceiptData(receiptData);

            // Timestamp ekle
            if (!receiptData.timestamp) {
                receiptData.timestamp = new Date().toISOString();
            }

            // Yazdırmayı başlat
            showMessage('Fiş yazdırılıyor...', 'info');
            await printWithRetry(receiptData);

        } catch (error) {
            Logger.error('Print fonksiyonu hatası:', error);
            showMessage(`Hata: ${error.message}`, 'error');
            throw error;
        }
    }

    /**
     * App Store / Play Store'a yönlendir
     */
    function redirectToStore() {
        if (isIOS()) {
            window.location.href = 'https://apps.apple.com/app/your-app-id';
        } else if (isAndroid()) {
            window.location.href = 'https://play.google.com/store/apps/details?id=com.example.rawbtapp';
        }
    }

    /**
     * Konfigürasyonu güncelle
     */
    function configure(options) {
        Object.assign(CONFIG, options);
        Logger.info('Konfigürasyon güncellendi:', CONFIG);
    }

    // Public API
    return {
        print,
        configure,
        redirectToStore,
        isIOS,
        isAndroid,
        isMobile,
        version: '1.0.0'
    };
})();

// Global scope'a ekle
window.POSPrinterBridge = POSPrinterBridge;

// Module export (ES6)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = POSPrinterBridge;
}
