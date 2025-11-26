package com.example.rawbtapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Ana yazıcı ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    viewModel: PrinterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mesaj gösterimi için effect
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("WiFi Termal Yazıcı") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bağlantı ayarları kartı
            ConnectionSettingsCard(
                ipAddress = uiState.ipAddress,
                port = uiState.port,
                onIpAddressChange = viewModel::updateIpAddress,
                onPortChange = viewModel::updatePort,
                isEnabled = !uiState.isLoading
            )
            
            // Yazdırma metni kartı
            PrintTextCard(
                printText = uiState.printText,
                onPrintTextChange = viewModel::updatePrintText,
                isEnabled = !uiState.isLoading
            )
            
            // Yazdırma butonları kartı
            PrintButtonsCard(
                onTestPrint = viewModel::printTest,
                onPrintCustomText = viewModel::printCustomText,
                onPrintSampleReceipt = viewModel::printSampleReceipt,
                onPrintDemo = viewModel::printDemo,
                isLoading = uiState.isLoading
            )
            
            // Yükleme göstergesi
            if (uiState.isLoading) {
                LoadingIndicator()
            }
            
            // Bilgi kartı
            InfoCard()
        }
    }
}

/**
 * Bağlantı ayarları kartı
 */
@Composable
fun ConnectionSettingsCard(
    ipAddress: String,
    port: String,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bağlantı Ayarları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = ipAddress,
                onValueChange = onIpAddressChange,
                label = { Text("IP Adresi") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                placeholder = { Text("9100") },
                singleLine = true,
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Yazdırma metni kartı
 */
@Composable
fun PrintTextCard(
    printText: String,
    onPrintTextChange: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yazdırılacak Metin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = printText,
                onValueChange = onPrintTextChange,
                label = { Text("Metin") },
                placeholder = { Text("Yazdırılacak metni girin...") },
                enabled = isEnabled,
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Yazdırma butonları kartı
 */
@Composable
fun PrintButtonsCard(
    onTestPrint: () -> Unit,
    onPrintCustomText: () -> Unit,
    onPrintSampleReceipt: () -> Unit,
    onPrintDemo: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yazdırma İşlemleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Test yazdırma butonu
            Button(
                onClick = onTestPrint,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Yazdır")
            }
            
            // Özel metin yazdırma butonu
            Button(
                onClick = onPrintCustomText,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Metin Yazdır")
            }
            
            // Örnek fiş yazdırma butonu
            OutlinedButton(
                onClick = onPrintSampleReceipt,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Örnek Fiş Yazdır")
            }
            
            // Demo yazdırma butonu
            OutlinedButton(
                onClick = onPrintDemo,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ESC/POS Demo Yazdır")
            }
        }
    }
}

/**
 * Yükleme göstergesi
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Yazıcıya bağlanılıyor...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Bilgi kartı
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ℹ️ Kullanım Bilgisi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "• Yazıcı ve telefon aynı WiFi ağında olmalı",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "• Yazıcının IP adresini yazıcı ayarlarından öğrenebilirsiniz",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "• Çoğu termal yazıcı 9100 portunu kullanır",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "• Test yazdırma ile bağlantıyı kontrol edin",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
