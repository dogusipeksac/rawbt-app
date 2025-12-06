package com.example.rawbtapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Türkçe karakter sorunu için yardım dialog'u
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurkishCharacterHelpDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Türkçe Karakter Sorunu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Çözüm Rehberi",
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Problem
                    SectionCard(
                        title = "❌ Sorun",
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(
                            text = "Yazıcı Türkçe karakterleri (ÇĞİÖŞÜ) doğru yazdıramıyor veya garip karakterler çıkıyor.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Çözüm Adımları
                    SectionCard(
                        title = "✅ Çözüm Adımları",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StepItem(
                                number = "1",
                                title = "Karakter Setini Değiştirin",
                                description = "Yazıcı ayarlarında 'Karakter Seti' bölümünden farklı seçenekleri deneyin."
                            )
                            
                            StepItem(
                                number = "2",
                                title = "Önerilen Ayarlar (Sırayla Deneyin)",
                                description = """
                                    • PC857 + CP857 (Turkish) - ÖNERİLEN
                                    • CP857 (Karakter seti yok)
                                    • PC3846 + CP857 (Turkish)
                                    • PC857 (61) + CP857
                                """.trimIndent()
                            )
                            
                            StepItem(
                                number = "3",
                                title = "Test Yazdırma Yapın",
                                description = "Her ayar değişikliğinden sonra 'Detaylı Test' ile Türkçe karakterleri test edin."
                            )
                            
                            StepItem(
                                number = "4",
                                title = "Full Test Kullanın",
                                description = "Hiçbir ayar çalışmazsa 'Full Test' ile tüm kombinasyonları otomatik deneyin."
                            )
                        }
                    }
                    
                    // Alternatif Çözüm
                    SectionCard(
                        title = "⚙️ Alternatif Çözüm",
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Eğer hiçbir ayar çalışmazsa:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "'Türkçe Karakter İptal' seçeneğini aktif edin. Bu durumda:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "ÇĞİÖŞÜ → CGIOSU olarak yazdırılır",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Test Metni
                    SectionCard(
                        title = "📝 Test Metni",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Test yazdırma için bu metni kullanın:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "ÇĞİÖŞÜ çğıöşü",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Çağrı, Şişli'de güzel bir öğle yemeği yedi.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Teknik Bilgi
                    SectionCard(
                        title = "ℹ️ Teknik Bilgi",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = """
                                Yazıcılar farklı karakter setleri kullanır. Türkçe karakterler için:
                                
                                • PC857: Türkçe için standart
                                • CP857: Kod sayfası 857
                                • ISO-8859-9: Latin-5 (Türkçe)
                                • Windows-1254: Windows Türkçe
                                
                                Her yazıcı modeli farklı kombinasyonları destekler.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
