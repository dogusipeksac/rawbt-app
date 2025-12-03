package com.example.rawbtapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawbtapp.manager.WebsiteManager
import com.example.rawbtapp.model.Printer
import com.example.rawbtapp.model.Website

/**
 * Ana ekran - Accordion yapısıyla site ve yazıcı yönetimi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    websiteManager: WebsiteManager,
    printerViewModel: PrinterViewModel,
    onOpenWebsite: (Website) -> Unit,
    modifier: Modifier = Modifier
) {
    var websites by remember { mutableStateOf(websiteManager.getAllWebsites()) }
    var selectedWebsite by remember { mutableStateOf(websiteManager.getSelectedWebsite()) }
    
    val printerUiState by printerViewModel.uiState.collectAsStateWithLifecycle()
    
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var expandedWebsites by remember { mutableStateOf(true) }
    var expandedPrinters by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "POS Yazdırma Sistemi",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bilgi kartı
            InfoBanner()
            
            // Web Siteleri Accordion
            AccordionCard(
                title = "Web Siteleri",
                subtitle = "${websites.size} site",
                icon = Icons.Default.Star,
                expanded = expandedWebsites,
                onExpandChange = { expandedWebsites = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Site listesi
                    websites.forEach { website ->
                        WebsiteListItem(
                            website = website,
                            isSelected = website.id == selectedWebsite?.id,
                            onSelect = {
                                selectedWebsite = website
                                websiteManager.selectWebsite(website)
                            },
                            onOpen = { onOpenWebsite(website) },
                            onDelete = {
                                if (!website.isDefault) {
                                    websiteManager.deleteWebsite(website.id)
                                    websites = websiteManager.getAllWebsites()
                                }
                            }
                        )
                    }
                    
                    // Yeni site ekle butonu
                    OutlinedButton(
                        onClick = { showAddWebsiteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Site Ekle")
                    }
                }
            }
            
            // Yazıcılar Accordion
            AccordionCard(
                title = "Yazıcılar",
                subtitle = "${printerUiState.savedPrinters.size} yazıcı",
                icon = Icons.Default.Check,
                expanded = expandedPrinters,
                onExpandChange = { expandedPrinters = it }
            ) {
                PrinterManagementCard(
                    savedPrinters = printerUiState.savedPrinters,
                    selectedPrinter = printerUiState.selectedPrinter,
                    onAddPrinter = printerViewModel::addPrinter,
                    onDeletePrinter = printerViewModel::deletePrinter,
                    onSelectPrinter = printerViewModel::selectPrinter,
                    isEnabled = !printerUiState.isLoading
                )
            }
        }
    }
    
    // Yeni site ekleme dialog'u
    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onDismiss = { showAddWebsiteDialog = false },
            onAdd = { name, url, description ->
                val success = websiteManager.addWebsite(name, url, description)
                if (success) {
                    websites = websiteManager.getAllWebsites()
                    showAddWebsiteDialog = false
                }
            }
        )
    }
}

/**
 * Bilgi banner'ı
 */
@Composable
fun InfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column {
                Text(
                    "Hoş Geldiniz",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Sitelerinizi ve yazıcılarınızı yönetin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Accordion kartı
 */
@Composable
fun AccordionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 3.dp
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Daralt" else "Genişlet"
                )
            }
            
            // Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Web sitesi liste öğesi
 */
@Composable
fun WebsiteListItem(
    website: Website,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
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
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = website.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = website.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (website.description.isNotEmpty()) {
                    Text(
                        text = website.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (website.isDefault) {
                    Text(
                        text = "Varsayılan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Aç butonu
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Aç",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Sil butonu (varsayılan site silinemez)
                if (!website.isDefault) {
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
}

/**
 * Yeni site ekleme dialog'u
 */
@Composable
fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Site Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Site Adı") },
                    placeholder = { Text("Örn: Restoran POS") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Site URL") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama (Opsiyonel)") },
                    placeholder = { Text("Site hakkında kısa bilgi") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAdd(name, url, description)
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
