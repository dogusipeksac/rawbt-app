package com.example.rawbtapp.printer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rawbtapp.model.Printer
import com.example.rawbtapp.ui.theme.RawBTAppTheme

/**
 * Yazıcı seçim ve yönetim Activity'si
 * WebView üzerinde açılır, yazıcı seçimi ve ekleme yapılabilir
 */
class PrinterSelectionActivity : ComponentActivity() {
    
    private lateinit var printerManager: PrinterManager
    
    companion object {
        private const val TAG = "PrinterSelectionActivity"
        const val EXTRA_HTML_CONTENT = "html_content"
        const val EXTRA_DOCUMENT_TITLE = "document_title"
        const val RESULT_PRINTER_ID = "printer_id"
        const val RESULT_PRINTER_NAME = "printer_name"
        const val RESULT_PRINTER_NUMBER = "printer_number"
        const val RESULT_PRINTER_IP = "printer_ip"
        const val RESULT_PRINTER_PORT = "printer_port"
        const val ACTION_PREVIEW = "action_preview"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        printerManager = PrinterManager(this)
        
        val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT) ?: ""
        val documentTitle = intent.getStringExtra(EXTRA_DOCUMENT_TITLE) ?: "Belge"
        
        Log.d(TAG, "PrinterSelectionActivity açıldı")
        Log.d(TAG, "Document: $documentTitle")
        Log.d(TAG, "HTML length: ${htmlContent.length}")
        
        setContent {
            RawBTAppTheme {
                PrinterSelectionScreen(
                    documentTitle = documentTitle,
                    htmlContent = htmlContent,
                    printerManager = printerManager,
                    onPrinterSelected = { printer ->
                        Log.d(TAG, "Yazıcı seçildi: ${printer.getDisplayName()}")
                        returnPrinterResult(printer)
                    },
                    onPreview = {
                        Log.d(TAG, "Önizleme istendi")
                        returnPreviewResult()
                    },
                    onCancel = {
                        Log.d(TAG, "İptal edildi")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
    
    private fun returnPrinterResult(printer: Printer) {
        val resultIntent = Intent().apply {
            putExtra(RESULT_PRINTER_ID, printer.id)
            putExtra(RESULT_PRINTER_NAME, printer.name)
            putExtra(RESULT_PRINTER_NUMBER, printer.number)
            putExtra(RESULT_PRINTER_IP, printer.ipAddress)
            putExtra(RESULT_PRINTER_PORT, printer.port)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    private fun returnPreviewResult() {
        val resultIntent = Intent().apply {
            putExtra(ACTION_PREVIEW, true)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSelectionScreen(
    documentTitle: String,
    htmlContent: String,
    printerManager: PrinterManager,
    onPrinterSelected: (Printer) -> Unit,
    onPreview: () -> Unit,
    onCancel: () -> Unit
) {
    var printers by remember { mutableStateOf(printerManager.getAllPrinters()) }
    var selectedPrinter by remember { mutableStateOf<Printer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Yazıcı Seçin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            documentTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "İptal"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Yazıcı Ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        bottomBar = {
            if (selectedPrinter != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Seçili yazıcı bilgisi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${selectedPrinter?.number}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPrinter?.name ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${selectedPrinter?.ipAddress}:${selectedPrinter?.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                        
                        // Önizleme butonu - Her zaman görünür
                        OutlinedButton(
                            onClick = onPreview,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "🔍 Önizleme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Yazdır butonu
                        Button(
                            onClick = { 
                                selectedPrinter?.let { 
                                    isLoading = true
                                    onPrinterSelected(it) 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large,
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Yazdırılıyor...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Yazdır",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (printers.isEmpty()) {
                // Boş durum
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
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
                            text = "Başlamak için sağ üstteki '+' butonuna tıklayın",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Yazıcı listesi
                printers.forEach { printer ->
                    PrinterSelectionItem(
                        printer = printer,
                        isSelected = selectedPrinter?.id == printer.id,
                        onSelect = { selectedPrinter = printer },
                        onDelete = {
                            printerManager.deletePrinter(printer.id)
                            printers = printerManager.getAllPrinters()
                            if (selectedPrinter?.id == printer.id) {
                                selectedPrinter = null
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Yazıcı ekleme dialog'u
    if (showAddDialog) {
        AddPrinterDialogInSelection(
            onDismiss = { showAddDialog = false },
            onAdd = { name, number, ip, port ->
                val printer = printerManager.addPrinter(name, number, ip, port.toInt())
                printers = printerManager.getAllPrinters()
                selectedPrinter = printer
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PrinterSelectionItem(
    printer: Printer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium
        )
    } else Modifier
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable { onSelect() },
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yazıcı numarası
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = if (isSelected) 3.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
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
            
            // Yazıcı bilgileri
            Column(
                modifier = Modifier.weight(1f),
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
            
            // Seçili ikonu veya sil butonu
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddPrinterDialogInSelection(
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
