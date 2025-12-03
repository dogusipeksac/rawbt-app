package com.example.rawbtapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rawbtapp.printer.TurkishCharacterEncoder

/**
 * Yazdırma önizleme dialog'u
 * Yazıcıya gönderilecek veriyi gösterir
 */
@Composable
fun PrintPreviewDialog(
    content: String,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    showRawData: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                PrintPreviewHeader(
                    onDismiss = onDismiss,
                    contentLength = content.length
                )
                
                HorizontalDivider()
                
                // Content
                PrintPreviewContent(
                    content = content,
                    showRawData = showRawData,
                    modifier = Modifier.weight(1f)
                )
                
                HorizontalDivider()
                
                // Footer
                PrintPreviewFooter(
                    onPrint = onPrint,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrintPreviewHeader(
    onDismiss: () -> Unit,
    contentLength: Int
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Yazdırma Önizleme",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "$contentLength karakter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun PrintPreviewContent(
    content: String,
    showRawData: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Önizleme", "Ham Veri", "Türkçe Test")
    
    Column(modifier = modifier) {
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Tab Content
        when (selectedTab) {
            0 -> PreviewTab(content)
            1 -> RawDataTab(content)
            2 -> TurkishTestTab(content)
        }
    }
}

@Composable
private fun PreviewTab(content: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun RawDataTab(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Byte array bilgisi
        val byteArray = TurkishCharacterEncoder.encodeForPrinter(content)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Encoding Bilgisi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toplam Byte: ${byteArray.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Karakter Sayısı: ${content.length}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Encoding: Windows-1254 (Turkish)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Ham metin
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF263238)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ham Metin",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Byte array (ilk 200 byte)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF263238)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Byte Array (İlk 200 byte)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = byteArray.take(200).joinToString(" ") { 
                        String.format("%02X", it) 
                    },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun TurkishTestTab(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Türkçe karakter testi
        val turkishChars = "ÇçĞğİıÖöŞşÜü"
        val foundChars = content.filter { it in turkishChars }
        val hasTurkish = foundChars.isNotEmpty()
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasTurkish) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (hasTurkish) "✓ Türkçe Karakter Bulundu" else "✗ Türkçe Karakter Yok",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasTurkish) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                
                if (hasTurkish) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bulunan karakterler: $foundChars",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Toplam: ${foundChars.length} adet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Türkçe karakter tablosu
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Türkçe Karakter Tablosu",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val turkishCharList = listOf(
                    "Ç" to "\\u00C7", "ç" to "\\u00E7",
                    "Ğ" to "\\u011E", "ğ" to "\\u011F",
                    "İ" to "\\u0130", "ı" to "\\u0131",
                    "Ö" to "\\u00D6", "ö" to "\\u00F6",
                    "Ş" to "\\u015E", "ş" to "\\u015F",
                    "Ü" to "\\u00DC", "ü" to "\\u00FC"
                )
                
                turkishCharList.forEach { (char, unicode) ->
                    val exists = char in content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$char ($unicode)",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (exists) "✓" else "—",
                            color = if (exists) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test metni
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Test Metni",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Çağrı, Şişli'de güzel bir öğle yemeği yedi.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ÇAĞRI, ŞİŞLİ'DE GÜZEL BİR ÖĞLE YEMEĞİ YEDİ.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PrintPreviewFooter(
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        ) {
            Text("İptal")
        }
        
        Button(
            onClick = onPrint,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yazdır")
        }
    }
}
