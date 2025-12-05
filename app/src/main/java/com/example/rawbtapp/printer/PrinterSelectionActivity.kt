package com.example.rawbtapp.printer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
        const val RESULT_PRINTER_IDS = "printer_ids"  // Multiple printer IDs
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
                    onPrintersSelected = { printerIds ->
                        Log.d(TAG, "${printerIds.size} yazıcı seçildi")
                        returnPrinterResult(printerIds)
                    },
                    onPreview = {
                        Log.d(TAG, "Önizleme istendi")
                        returnPreviewResult()
                    },
                    onCancel = {
                        Log.d(TAG, "İptal edildi")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onTestPrinter = { printer, text, type ->
                        testPrinter(printer, text, type)
                    }
                )
            }
        }
    }

    private fun testPrinter(printer: Printer, customText: String = "", testType: String = "simple") {
        lifecycleScope.launch {
            try {
                val repository = com.example.rawbtapp.printer.PrinterRepository()
                val result = when (testType) {
                    "simple" -> {
                        repository.printCustomTest(
                            ipAddress = printer.ipAddress,
                            port = printer.port,
                            customText = customText.ifBlank { "TEST YAZDIR\nYazıcı Bağlantı Testi" },
                            cutPaper = printer.cutPaper,
                            cutFeedLines = printer.cutFeedLines,
                            charsetEncoding = printer.charsetEncoding
                        )
                    }
                    "full" -> {
                        repository.printFullTest(
                            ipAddress = printer.ipAddress,
                            port = printer.port,
                            customText = customText.ifBlank { "TEST YAZDIR\nYazıcı Bağlantı Testi" },
                            cutPaper = printer.cutPaper,
                            cutFeedLines = printer.cutFeedLines
                        )
                    }
                    "detailed" -> {
                        repository.printDetailedTest(
                            printer = printer,
                            customText = customText.ifBlank { "TEST YAZDIR\nYazıcı Bağlantı Testi" }
                        )
                    }
                    else -> {
                        repository.printTest(
                            ipAddress = printer.ipAddress,
                            port = printer.port,
                            cutPaper = printer.cutPaper,
                            cutFeedLines = printer.cutFeedLines,
                            charsetEncoding = printer.charsetEncoding
                        )
                    }
                }
                when (result) {
                    is com.example.rawbtapp.printer.PrintResult.Success -> {
                        runOnUiThread {
                            android.widget.Toast.makeText(
                                this@PrinterSelectionActivity,
                                "✓ Test yazdırma başarılı: ${printer.getDisplayName()}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is com.example.rawbtapp.printer.PrintResult.Error -> {
                        runOnUiThread {
                            android.widget.Toast.makeText(
                                this@PrinterSelectionActivity,
                                "✗ Test yazdırma hatası: ${result.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@PrinterSelectionActivity,
                        "✗ Hata: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun returnPrinterResult(printerIds: List<String>) {
        val resultIntent = Intent().apply {
            putStringArrayListExtra(RESULT_PRINTER_IDS, ArrayList(printerIds))
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
    onPrintersSelected: (List<String>) -> Unit,
    onPreview: () -> Unit,
    onCancel: () -> Unit,
    onTestPrinter: (Printer, String, String) -> Unit
) {
    var printers by remember { mutableStateOf(printerManager.getAllPrinters()) }
    var selectedPrinters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPrinter by remember { mutableStateOf<Printer?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var printerToDelete by remember { mutableStateOf<Printer?>(null) }
    
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
            if (selectedPrinters.isNotEmpty()) {
                val selectedPrintersList = printers.filter { it.id in selectedPrinters }
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        text = "${selectedPrinters.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedPrinters.size == 1)
                                        selectedPrintersList.firstOrNull()?.name ?: ""
                                    else
                                        "${selectedPrinters.size} Yazıcı Seçildi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (selectedPrinters.size == 1)
                                        "${selectedPrintersList.firstOrNull()?.ipAddress}:${selectedPrintersList.firstOrNull()?.port}"
                                    else
                                        selectedPrintersList.joinToString(", ") { "#${it.number}" },
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
                                "Önizleme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Yazdır butonu
                        Button(
                            onClick = {
                                if (selectedPrintersList.isNotEmpty()) {
                                    isLoading = true
                                    // Seçili tüm printer ID'lerini gönder
                                    onPrintersSelected(selectedPrintersList.map { it.id })
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
                                    if (selectedPrinters.size == 1) "Yazdır" else "${selectedPrinters.size} Yazıcıya Yazdır",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        isSelected = printer.id in selectedPrinters,
                        onSelect = {
                            selectedPrinters = if (printer.id in selectedPrinters) {
                                selectedPrinters - printer.id
                            } else {
                                selectedPrinters + printer.id
                            }
                        },
                        onDelete = { printerToDelete = it },
                        onTest = onTestPrinter,
                        onEdit = { p ->
                            editingPrinter = p
                            showAddDialog = true
                        }
                    )
                }
            }

            // Bottom padding for bottom bar - daha fazla boşluk
            Spacer(modifier = Modifier.height(250.dp)            )
        }
    }
    }

    // Silme onay dialog'u
    printerToDelete?.let { printer ->
        AlertDialog(
            onDismissRequest = { printerToDelete = null },
            title = {
                Text(
                    "Yazıcıyı Sil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "\"${printer.name}\" yazıcısını silmek istediğinizden emin misiniz?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        printerManager.deletePrinter(printer.id)
                        printers = printerManager.getAllPrinters()
                        if (printer.id in selectedPrinters) {
                            selectedPrinters = selectedPrinters - printer.id
                        }
                        printerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { printerToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Yazıcı ekleme/düzenleme dialog'u
    if (showAddDialog) {
        if (editingPrinter != null) {
            // Düzenleme modu
            EditPrinterDialogInSelection(
                printer = editingPrinter!!,
                onDismiss = {
                    showAddDialog = false
                    editingPrinter = null
                },
                onUpdate = { name, number, ip, port, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars ->
                    printerManager.updatePrinter(
                        editingPrinter!!.id,
                        name,
                        number,
                        ip,
                        port.toInt(),
                        cutPaper,
                        cutFeedLines,
                        charsetEncoding,
                        cancelTurkishChars
                    )
                    printers = printerManager.getAllPrinters()
                    showAddDialog = false
                    editingPrinter = null
                }
            )
        } else {
            // Ekleme modu
            AddPrinterDialogInSelection(
                onDismiss = {
                    showAddDialog = false
                },
                onAdd = { name, number, ip, port, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars ->
                    val printer = printerManager.addPrinter(name, number, ip, port.toInt(), cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars)
                    printers = printerManager.getAllPrinters()
                    selectedPrinters = selectedPrinters + printer.id
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun PrinterSelectionItem(
    printer: Printer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (Printer) -> Unit,
    onTest: (Printer, String, String) -> Unit,
    onEdit: (Printer) -> Unit
) {
    var showTestDialog by remember { mutableStateOf(false) }
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
            
            // Test, düzenle ve sil butonları
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Test butonu
                IconButton(onClick = { showTestDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Yazdır",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Düzenle butonu
                IconButton(onClick = { onEdit(printer) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Sil butonu
                IconButton(onClick = { onDelete(printer) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Seçili ikonu (sadece seçiliyse)
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Seçili",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(4.dp)
                    )
                }
            }
        }
        
        // Test Dialog
        if (showTestDialog) {
            com.example.rawbtapp.ui.PrinterTestDialog(
                printer = printer,
                onDismiss = { showTestDialog = false },
                onSimpleTest = { text ->
                    onTest(printer, text, "simple")
                    showTestDialog = false
                },
                onFullTest = { text ->
                    onTest(printer, text, "full")
                    showTestDialog = false
                },
                onDetailedTest = { text ->
                    onTest(printer, text, "detailed")
                    showTestDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrinterDialogInSelection(
    onDismiss: () -> Unit,
    onAdd: (name: String, number: String, ipAddress: String, port: String, cutPaper: Boolean, cutFeedLines: Int, charsetEncoding: String, cancelTurkishChars: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("192.168.1.") }
    var port by remember { mutableStateOf("9100") }
    var cutPaper by remember { mutableStateOf(true) }
    var cutFeedLines by remember { mutableStateOf(3) }
    var charsetEncoding by remember { mutableStateOf(com.example.rawbtapp.printer.CharsetEncodingOptions.getDefaultValue()) }
    var expandedCharsetDropdown by remember { mutableStateOf(false) }
    var cancelTurkishChars by remember { mutableStateOf(false) }
    
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

                // Kağıt kesme switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kağıt Kesme",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (cutPaper) "Aktif" else "Kapalı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cutPaper,
                        onCheckedChange = { cutPaper = it }
                    )
                }

                // Kesme boşluğu slider
                if (cutPaper) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kesme Öncesi Boşluk: $cutFeedLines satır",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = cutFeedLines.toFloat(),
                            onValueChange = { cutFeedLines = it.toInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "10",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Karakter seti ve encoding seçimi
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Karakter Seti / Encoding",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedCharsetDropdown,
                        onExpandedChange = { expandedCharsetDropdown = !expandedCharsetDropdown }
                    ) {
                        OutlinedTextField(
                            value = com.example.rawbtapp.printer.CharsetEncodingOptions.getOptionByValue(charsetEncoding)?.displayName ?: charsetEncoding,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCharsetDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCharsetDropdown,
                            onDismissRequest = { expandedCharsetDropdown = false }
                        ) {
                            com.example.rawbtapp.printer.CharsetEncodingOptions.options.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = option.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        charsetEncoding = option.value
                                        expandedCharsetDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Türkçe karakter iptal switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Türkçe Karakter İptal Et",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "İ→I, ı→i, Ö→O, ö→o, Ü→U, ü→u, Ş→S, ş→s, Ğ→G, ğ→g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cancelTurkishChars,
                        onCheckedChange = { cancelTurkishChars = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, number, ipAddress, port, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrinterDialogInSelection(
    printer: Printer,
    onDismiss: () -> Unit,
    onUpdate: (name: String, number: String, ipAddress: String, port: String, cutPaper: Boolean, cutFeedLines: Int, charsetEncoding: String, cancelTurkishChars: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(printer.name) }
    var number by remember { mutableStateOf(printer.number) }
    var ipAddress by remember { mutableStateOf(printer.ipAddress) }
    var port by remember { mutableStateOf(printer.port.toString()) }
    var cutPaper by remember { mutableStateOf(printer.cutPaper) }
    var cutFeedLines by remember { mutableStateOf(printer.cutFeedLines) }
    var charsetEncoding by remember { mutableStateOf(printer.charsetEncoding) }
    var expandedCharsetDropdown by remember { mutableStateOf(false) }
    var cancelTurkishChars by remember { mutableStateOf(printer.cancelTurkishChars) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 3.dp
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        title = {
            Text(
                "Yazıcı Düzenle",
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

                // Kağıt kesme switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kağıt Kesme",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (cutPaper) "Aktif" else "Kapalı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cutPaper,
                        onCheckedChange = { cutPaper = it }
                    )
                }

                // Kesme boşluğu slider
                if (cutPaper) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kesme Öncesi Boşluk: $cutFeedLines satır",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = cutFeedLines.toFloat(),
                            onValueChange = { cutFeedLines = it.toInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "10",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Karakter seti ve encoding seçimi
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Karakter Seti / Encoding",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedCharsetDropdown,
                        onExpandedChange = { expandedCharsetDropdown = !expandedCharsetDropdown }
                    ) {
                        OutlinedTextField(
                            value = com.example.rawbtapp.printer.CharsetEncodingOptions.getOptionByValue(charsetEncoding)?.displayName ?: charsetEncoding,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCharsetDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCharsetDropdown,
                            onDismissRequest = { expandedCharsetDropdown = false }
                        ) {
                            com.example.rawbtapp.printer.CharsetEncodingOptions.options.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = option.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        charsetEncoding = option.value
                                        expandedCharsetDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Türkçe karakter iptal switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Türkçe Karakter İptal Et",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "İ→I, ı→i, Ö→O, ö→o, Ü→U, ü→u, Ş→S, ş→s, Ğ→G, ğ→g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cancelTurkishChars,
                        onCheckedChange = { cancelTurkishChars = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(name, number, ipAddress, port, cutPaper, cutFeedLines, charsetEncoding, cancelTurkishChars) },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Güncelle", style = MaterialTheme.typography.labelLarge)
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
