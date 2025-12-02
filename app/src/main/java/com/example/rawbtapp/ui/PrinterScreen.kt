package com.example.rawbtapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawbtapp.model.Printer

/**
 * Ana yazıcı ekranı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    viewModel: PrinterViewModel,
    onOpenWebView: () -> Unit = {},
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
    
    // Ekran boyutunu tespit et
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "WiFi Termal Yazıcı",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) { paddingValues ->
        if (isTablet) {
            // Tablet layout - İki sütunlu
            TabletLayout(
                uiState = uiState,
                viewModel = viewModel,
                onOpenWebView = onOpenWebView,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Telefon layout - Tek sütunlu
            PhoneLayout(
                uiState = uiState,
                viewModel = viewModel,
                onOpenWebView = onOpenWebView,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

/**
 * Tablet için iki sütunlu layout
 */
@Composable
fun TabletLayout(
    uiState: PrinterUiState,
    viewModel: PrinterViewModel,
    onOpenWebView: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Sol sütun - Yazıcı yönetimi
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrinterManagementCard(
                savedPrinters = uiState.savedPrinters,
                selectedPrinter = uiState.selectedPrinter,
                onAddPrinter = viewModel::addPrinter,
                onDeletePrinter = viewModel::deletePrinter,
                onSelectPrinter = viewModel::selectPrinter,
                isEnabled = !uiState.isLoading
            )
            
            InfoCard()
        }
        
        // Sağ sütun - Butonlar ve durum
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrintButtonsCard(
                onTestPrint = viewModel::printTest,
                onPrintCustomText = viewModel::printCustomText,
                onPrintSampleReceipt = viewModel::printSampleReceipt,
                onPrintDemo = viewModel::printDemo,
                onOpenWebView = onOpenWebView,
                isLoading = uiState.isLoading
            )
            
            if (uiState.isLoading) {
                LoadingIndicator()
            }
        }
    }
}

/**
 * Telefon için tek sütunlu layout
 */
@Composable
fun PhoneLayout(
    uiState: PrinterUiState,
    viewModel: PrinterViewModel,
    onOpenWebView: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrinterManagementCard(
            savedPrinters = uiState.savedPrinters,
            selectedPrinter = uiState.selectedPrinter,
            onAddPrinter = viewModel::addPrinter,
            onDeletePrinter = viewModel::deletePrinter,
            onSelectPrinter = viewModel::selectPrinter,
            isEnabled = !uiState.isLoading
        )
        
        PrintButtonsCard(
            onTestPrint = viewModel::printTest,
            onPrintCustomText = viewModel::printCustomText,
            onPrintSampleReceipt = viewModel::printSampleReceipt,
            onPrintDemo = viewModel::printDemo,
            onOpenWebView = onOpenWebView,
            isLoading = uiState.isLoading
        )
        
        if (uiState.isLoading) {
            LoadingIndicator()
        }
        
        InfoCard()
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
    onOpenWebView: () -> Unit = {},
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Web POS Sistemi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // WebView butonu - Tek buton
            Button(
                onClick = onOpenWebView,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "🌐 Web POS Aç",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
 * Yazıcı yönetim kartı
 */
@Composable
fun PrinterManagementCard(
    savedPrinters: List<Printer>,
    selectedPrinter: Printer?,
    onAddPrinter: (String, String, String, String) -> Boolean,
    onDeletePrinter: (String) -> Unit,
    onSelectPrinter: (Printer) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kayıtlı Yazıcılar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${savedPrinters.size} yazıcı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Yazıcı Ekle",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Yeni Ekle",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            if (savedPrinters.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Henüz yazıcı eklenmemiş",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Başlamak için 'Yeni Ekle' butonuna tıklayın",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    savedPrinters.forEach { printer ->
                        PrinterListItem(
                            printer = printer,
                            isSelected = printer.id == selectedPrinter?.id,
                            onSelect = { onSelectPrinter(printer) },
                            onDelete = { onDeletePrinter(printer.id) },
                            isEnabled = isEnabled
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddPrinterDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, number, ip, port ->
                val success = onAddPrinter(name, number, ip, port)
                if (success) {
                    showAddDialog = false
                }
            }
        )
    }
}

/**
 * Yazıcı liste öğesi
 */
@Composable
fun PrinterListItem(
    printer: Printer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium
        )
    } else Modifier
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable(enabled = isEnabled) { onSelect() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yazıcı ikonu
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = if (isSelected) 3.dp else 0.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${printer.number}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = printer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 3.dp
                            ) {
                                Text(
                                    text = "SEÇİLİ",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = "${printer.ipAddress}:${printer.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            IconButton(
                onClick = onDelete,
                enabled = isEnabled
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Yazıcı ekleme dialog'u
 */
@Composable
fun AddPrinterDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, number: String, ipAddress: String, port: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("192.168.1.") }
    var port by remember { mutableStateOf("9100") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = { 
            Text(
                "Yeni Yazıcı Ekle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Yazıcı Adı") },
                    placeholder = { Text("Örn: Mutfak Yazıcısı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Yazıcı Numarası") },
                    placeholder = { Text("Örn: 1, 2, 3...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Adresi") },
                    placeholder = { Text("192.168.1.100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    placeholder = { Text("9100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, number, ipAddress, port) },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Ekle", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("İptal", style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = MaterialTheme.shapes.large
    )
}

/**
 * Bilgi kartı
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiary,
                    tonalElevation = 3.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                Text(
                    text = "Kullanım Bilgisi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoItem("Yazıcı ve telefon aynı WiFi ağında olmalı")
                InfoItem("Yazıcının IP adresini yazıcı ayarlarından öğrenebilirsiniz")
                InfoItem("Çoğu termal yazıcı 9100 portunu kullanır")
                InfoItem("Test yazdırma ile bağlantıyı kontrol edin")
            }
        }
    }
}

@Composable
fun InfoItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

