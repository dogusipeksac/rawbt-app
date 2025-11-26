# Mimari Dokümantasyonu

## 📐 Genel Mimari

Bu uygulama **MVVM (Model-View-ViewModel)** mimarisi ve **Clean Architecture** prensiplerine göre tasarlanmıştır.

## 🏛️ Katmanlar

### 1. UI Layer (Presentation)

**Sorumluluklar:**
- Kullanıcı arayüzünü gösterme
- Kullanıcı etkileşimlerini yakalama
- ViewModel'den gelen state'i render etme

**Bileşenler:**

#### MainActivity.kt
```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: PrinterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RawBTAppTheme {
                PrinterScreen(viewModel = viewModel)
            }
        }
    }
}
```

**Görevler:**
- Activity lifecycle yönetimi
- ViewModel instance oluşturma
- Compose content ayarlama

#### PrinterScreen.kt

**Composable Fonksiyonlar:**

1. **PrinterScreen** - Ana ekran container
   - Scaffold yapısı
   - TopAppBar
   - SnackbarHost
   - State collection

2. **ConnectionSettingsCard** - Bağlantı ayarları
   - IP adresi input
   - Port input
   - Validation

3. **PrintTextCard** - Metin girişi
   - Multi-line text field
   - Character limit

4. **PrintButtonsCard** - Yazdırma butonları
   - Test print
   - Custom text print
   - Sample receipt
   - Demo print

5. **LoadingIndicator** - Yükleme durumu
   - Progress indicator
   - Status message

6. **InfoCard** - Bilgilendirme
   - Usage instructions
   - Tips

**State Management:**
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

**Side Effects:**
```kotlin
LaunchedEffect(uiState.message) {
    uiState.message?.let { message ->
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }
}
```

### 2. ViewModel Layer

**Sorumluluklar:**
- UI state yönetimi
- Business logic
- Repository ile iletişim
- Coroutine scope yönetimi

#### PrinterViewModel.kt

**State Definition:**
```kotlin
data class PrinterUiState(
    val ipAddress: String = "192.168.1.100",
    val port: String = "9100",
    val printText: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)
```

**State Flow:**
```kotlin
private val _uiState = MutableStateFlow(PrinterUiState())
val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()
```

**State Updates:**
```kotlin
fun updateIpAddress(ip: String) {
    _uiState.update { it.copy(ipAddress = ip) }
}
```

**Business Logic:**
```kotlin
fun printTest() {
    // 1. Validation
    if (currentState.ipAddress.isBlank()) {
        showMessage("IP adresi boş olamaz", isError = true)
        return
    }
    
    // 2. Start loading
    _uiState.update { it.copy(isLoading = true) }
    
    // 3. Execute operation
    viewModelScope.launch {
        val result = repository.printTest(ipAddress, port)
        handlePrintResult(result)
    }
}
```

**Coroutine Scope:**
- `viewModelScope` kullanımı
- Otomatik lifecycle yönetimi
- Cancellation support

### 3. Repository Layer

**Sorumluluklar:**
- Data source koordinasyonu
- Business logic
- Data transformation

#### PrinterRepository.kt

**Fonksiyonlar:**

1. **printCustomText** - Özel metin yazdırma
```kotlin
suspend fun printCustomText(
    ipAddress: String,
    port: Int,
    text: String
): PrintResult {
    // Validation
    if (text.isBlank()) {
        return PrintResult.Error("Metin boş olamaz")
    }
    
    // Build ESC/POS commands
    val printData = buildEscPosCommand {
        initialize()
        // ... commands
    }
    
    // Send to printer
    return printerClient.print(ipAddress, port, printData)
}
```

2. **printTest** - Test yazdırma
3. **printSampleReceipt** - Örnek fiş
4. **printDemo** - Demo yazdırma

**ESC/POS Command Building:**
```kotlin
val printData = buildEscPosCommand {
    initialize()
    alignCenter()
    doubleTextLine("BAŞLIK")
    alignLeft()
    textLine("İçerik")
    feedPaper(3)
    cutPaper()
}
```

### 4. Data Layer

**Sorumluluklar:**
- Network communication
- ESC/POS command generation
- Error handling

#### PrinterClient.kt

**TCP Socket Connection:**

```kotlin
suspend fun print(
    ipAddress: String,
    port: Int,
    data: ByteArray
): PrintResult = withContext(Dispatchers.IO) {
    var socket: Socket? = null
    var outputStream: OutputStream? = null
    
    try {
        // Validation
        if (!isValidIpAddress(ipAddress)) {
            return@withContext PrintResult.Error("Geçersiz IP")
        }
        
        // Connect
        socket = Socket()
        socket.connect(
            InetSocketAddress(ipAddress, port),
            CONNECTION_TIMEOUT
        )
        socket.soTimeout = READ_TIMEOUT
        
        // Send data
        outputStream = socket.getOutputStream()
        outputStream.write(data)
        outputStream.flush()
        
        PrintResult.Success("Başarılı")
        
    } catch (e: Exception) {
        // Error handling
        PrintResult.Error(e.message)
    } finally {
        // Cleanup
        outputStream?.close()
        socket?.close()
    }
}
```

**Features:**
- Timeout management
- IP validation
- Port validation
- Detailed error messages
- Resource cleanup

#### EscPosCommands.kt

**Builder Pattern:**

```kotlin
class EscPosCommands {
    private val buffer = ByteArrayOutputStream()
    
    fun initialize(): EscPosCommands {
        buffer.write(INIT)
        buffer.write(CHARSET_PC857)
        return this
    }
    
    fun text(text: String): EscPosCommands {
        buffer.write(text.toByteArray(Charset.forName("Windows-1254")))
        return this
    }
    
    fun build(): ByteArray {
        return buffer.toByteArray()
    }
}
```

**DSL Support:**
```kotlin
fun buildEscPosCommand(block: EscPosCommands.() -> Unit): ByteArray {
    return EscPosCommands().apply(block).build()
}
```

**ESC/POS Commands:**

| Komut | Byte Dizisi | Açıklama |
|-------|-------------|----------|
| INIT | 0x1B, 0x40 | Yazıcı başlatma |
| ALIGN_LEFT | 0x1B, 0x61, 0x00 | Sola hizala |
| ALIGN_CENTER | 0x1B, 0x61, 0x01 | Ortaya hizala |
| ALIGN_RIGHT | 0x1B, 0x61, 0x02 | Sağa hizala |
| BOLD_ON | 0x1B, 0x45, 0x01 | Kalın açık |
| BOLD_OFF | 0x1B, 0x45, 0x00 | Kalın kapalı |
| DOUBLE_SIZE | 0x1B, 0x21, 0x30 | Çift boyut |
| NORMAL_SIZE | 0x1B, 0x21, 0x00 | Normal boyut |
| LINE_FEED | 0x0A | Satır atlama |
| CUT_PAPER | 0x1D, 0x56, 0x00 | Kağıt kesme |

## 🔄 Veri Akışı

### 1. Kullanıcı Etkileşimi

```
User Action → Composable → ViewModel → Repository → PrinterClient → Printer
                                                                        ↓
User Feedback ← Composable ← ViewModel ← Repository ← PrintResult ←────┘
```

### 2. State Akışı

```
ViewModel State Change
        ↓
StateFlow Emission
        ↓
Compose Recomposition
        ↓
UI Update
```

### 3. Yazdırma Akışı

```
1. User clicks "Print"
        ↓
2. ViewModel validates input
        ↓
3. ViewModel updates state (isLoading = true)
        ↓
4. ViewModel calls Repository
        ↓
5. Repository builds ESC/POS commands
        ↓
6. Repository calls PrinterClient
        ↓
7. PrinterClient opens TCP socket
        ↓
8. PrinterClient sends data
        ↓
9. PrinterClient returns PrintResult
        ↓
10. Repository returns result
        ↓
11. ViewModel handles result
        ↓
12. ViewModel updates state (isLoading = false, message)
        ↓
13. UI shows result (Snackbar)
```

## 🎯 Design Patterns

### 1. MVVM Pattern

**Avantajları:**
- Separation of concerns
- Testability
- Reactive programming
- Lifecycle awareness

### 2. Repository Pattern

**Avantajları:**
- Data source abstraction
- Centralized data logic
- Easy to test
- Easy to swap implementations

### 3. Builder Pattern

**Kullanım:** ESC/POS command building

**Avantajları:**
- Fluent API
- Readable code
- Flexible command composition

### 4. Sealed Class

**Kullanım:** PrintResult

```kotlin
sealed class PrintResult {
    data class Success(val message: String) : PrintResult()
    data class Error(val message: String) : PrintResult()
}
```

**Avantajları:**
- Type safety
- Exhaustive when
- Clear intent

## 🧪 Testing Strategy

### Unit Tests

**ViewModel Tests:**
```kotlin
@Test
fun `updateIpAddress updates state correctly`() {
    viewModel.updateIpAddress("192.168.1.1")
    assertEquals("192.168.1.1", viewModel.uiState.value.ipAddress)
}
```

**Repository Tests:**
```kotlin
@Test
fun `printCustomText returns error for empty text`() = runTest {
    val result = repository.printCustomText("192.168.1.1", 9100, "")
    assertTrue(result is PrintResult.Error)
}
```

**EscPosCommands Tests:**
```kotlin
@Test
fun `buildEscPosCommand creates correct byte array`() {
    val data = buildEscPosCommand {
        initialize()
        text("Test")
    }
    assertNotNull(data)
    assertTrue(data.isNotEmpty())
}
```

### Integration Tests

**PrinterClient Tests:**
```kotlin
@Test
fun `print returns error for invalid IP`() = runTest {
    val result = printerClient.print("invalid", 9100, byteArrayOf())
    assertTrue(result is PrintResult.Error)
}
```

### UI Tests

**Compose Tests:**
```kotlin
@Test
fun `PrinterScreen displays correctly`() {
    composeTestRule.setContent {
        PrinterScreen(viewModel = viewModel)
    }
    
    composeTestRule.onNodeWithText("WiFi Termal Yazıcı").assertExists()
    composeTestRule.onNodeWithText("Test Yazdır").assertExists()
}
```

## 🔒 Error Handling

### Katmanlara Göre Hata Yönetimi

**1. UI Layer:**
- Snackbar ile kullanıcıya bildirim
- Loading state gösterimi
- Error state gösterimi

**2. ViewModel Layer:**
- Input validation
- State update
- User-friendly error messages

**3. Repository Layer:**
- Business logic validation
- Data transformation errors

**4. Data Layer:**
- Network errors
- Socket errors
- Timeout errors

### Hata Türleri

```kotlin
try {
    // Operation
} catch (e: SocketTimeoutException) {
    PrintResult.Error("Bağlantı zaman aşımı")
} catch (e: UnknownHostException) {
    PrintResult.Error("Yazıcı bulunamadı")
} catch (e: ConnectException) {
    PrintResult.Error("Bağlantı hatası")
} catch (e: IOException) {
    PrintResult.Error("Veri gönderme hatası")
} catch (e: Exception) {
    PrintResult.Error("Beklenmeyen hata")
}
```

## 🚀 Performance Considerations

### 1. Coroutines

- `Dispatchers.IO` for network operations
- `viewModelScope` for automatic cancellation
- Structured concurrency

### 2. State Management

- StateFlow for reactive updates
- Minimal recomposition
- Efficient state updates

### 3. Memory Management

- Proper resource cleanup
- Socket closure in finally block
- ByteArrayOutputStream usage

### 4. UI Performance

- LazyColumn for lists (if needed)
- Remember for expensive computations
- Avoid unnecessary recomposition

## 📊 Dependency Graph

```
MainActivity
    ↓
PrinterViewModel
    ↓
PrinterRepository
    ↓
PrinterClient + EscPosCommands
```

## 🔐 Security Considerations

1. **Network Security:**
   - Local network only
   - No sensitive data transmission
   - No authentication required (printer-dependent)

2. **Input Validation:**
   - IP address validation
   - Port range validation
   - Text sanitization

3. **Permissions:**
   - INTERNET permission
   - ACCESS_NETWORK_STATE permission

## 🎨 UI/UX Principles

1. **Material Design 3:**
   - Modern UI components
   - Consistent styling
   - Accessibility support

2. **User Feedback:**
   - Loading indicators
   - Success/error messages
   - Clear button states

3. **Error Prevention:**
   - Input validation
   - Disabled states
   - Clear instructions

## 📈 Scalability

### Gelecek Geliştirmeler İçin Hazırlık

1. **Dependency Injection:**
   - Hilt/Koin eklenebilir
   - Repository interface'i oluşturulabilir

2. **Database:**
   - Room ile yazıcı profilleri
   - Yazdırma geçmişi

3. **Multiple Printers:**
   - Yazıcı listesi
   - Profil yönetimi

4. **Advanced Features:**
   - Logo yazdırma
   - QR kod
   - Barkod
   - Şablonlar

## 🎯 Best Practices

1. **Kotlin:**
   - Null safety
   - Extension functions
   - Data classes
   - Sealed classes

2. **Coroutines:**
   - Structured concurrency
   - Exception handling
   - Cancellation support

3. **Compose:**
   - State hoisting
   - Unidirectional data flow
   - Side effects

4. **Architecture:**
   - Single responsibility
   - Dependency inversion
   - Clean code

## 📝 Sonuç

Bu mimari, modern Android geliştirme pratiklerini kullanarak ölçeklenebilir, test edilebilir ve sürdürülebilir bir uygulama oluşturur. Her katman kendi sorumluluğuna sahiptir ve bağımlılıklar tek yönlüdür (yukarıdan aşağıya).
