package com.example.rawbtapp.preview

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rawbtapp.ui.theme.RawBTAppTheme

/**
 * HTML önizleme Activity'si
 * WebView'de HTML içeriğini gösterir
 */
class PreviewActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PreviewActivity"
        const val EXTRA_HTML_CONTENT = "html_content"
        const val EXTRA_TITLE = "title"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Önizleme"
        
        Log.d(TAG, "PreviewActivity opened")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "HTML length: ${htmlContent.length}")
        
        setContent {
            RawBTAppTheme {
                PreviewScreen(
                    htmlContent = htmlContent,
                    title = title,
                    onBack = { finish() },
                    onPrint = {
                        // Print işlemi
                        printContent(htmlContent, title)
                    }
                )
            }
        }
    }
    
    private fun printContent(htmlContent: String, title: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            try {
                // Geçici WebView oluştur
                val printWebView = WebView(this).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                }
                
                // HTML yükle
                printWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                
                // Print dialog aç
                printWebView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val printAdapter = printWebView.createPrintDocumentAdapter(title)
                        
                        printManager.print(
                            title,
                            printAdapter,
                            android.print.PrintAttributes.Builder()
                                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                .build()
                        )
                        
                        Log.d(TAG, "Print dialog opened")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing", e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    htmlContent: String,
    title: String,
    onBack: () -> Unit,
    onPrint: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Önizleme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        // WebView ile HTML göster
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = false // 80mm için
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }
                        
                        // HTML yükle
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}
