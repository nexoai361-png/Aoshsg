package com.example.calculator.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import com.example.ui.theme.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.viewmodel.CalculatorViewModel
import com.example.calculator.viewmodel.HistoryItem
import com.example.calculator.viewmodel.AppMode
import com.example.calculator.engine.ConverterCategory
import com.example.calculator.engine.ConverterEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val previewResult by viewModel.previewResult.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isAdvancedMode by viewModel.isAdvancedMode.collectAsStateWithLifecycle()

    // Converter States
    val converterCategory by viewModel.converterCategory.collectAsStateWithLifecycle()
    val converterInputValue by viewModel.converterInputValue.collectAsStateWithLifecycle()
    val converterFromUnit by viewModel.converterFromUnit.collectAsStateWithLifecycle()
    val converterToUnit by viewModel.converterToUnit.collectAsStateWithLifecycle()
    val converterResult by viewModel.converterResult.collectAsStateWithLifecycle()

    var showHistoryPopup by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Wiggle/Shake state for syntax errors
    var shakeTrigger by remember { mutableStateOf(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            shakeTrigger = 15f
            delay(50)
            shakeTrigger = -15f
            delay(50)
            shakeTrigger = 10f
            delay(50)
            shakeTrigger = -10f
            delay(50)
            shakeTrigger = 5f
            delay(50)
            shakeTrigger = -5f
            delay(50)
            shakeTrigger = 0f
        }
    }
    val shakeOffset by animateFloatAsState(
        targetValue = shakeTrigger,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Detect screen width for responsive layouts
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // VS Code default dark theme solid background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isWideScreen) {
            // Adaptive Two-Column Layout (Canonical Supporting-Pane Layout)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Increased padding for generous whitespace
                horizontalArrangement = Arrangement.spacedBy(24.dp) // More whitespace between columns
            ) {
                // Calculator Main Pane (Keyboard & Displays)
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, VsCodeActiveBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp) // Increased padding for generous internal whitespace
                    ) {
                        CalculatorHeader(
                            onToggleAdvanced = { viewModel.toggleAdvancedMode() },
                            onToggleHistory = { showHistoryPopup = !showHistoryPopup },
                            onShowInfo = { showInfoDialog = true },
                            isAdvanced = isAdvancedMode,
                            isWide = true,
                            appMode = appMode,
                            onModeSelected = { viewModel.setAppMode(it) }
                        )
                        Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                        ModeSwitcher(
                            currentMode = appMode,
                            onModeSelected = { viewModel.setAppMode(it) }
                        )
                        Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                        
                        if (appMode == AppMode.CALCULATOR) {
                            DisplayArea(
                                expression = expression,
                                preview = previewResult,
                                shakeOffset = shakeOffset,
                                isError = isError,
                                modifier = Modifier.weight(0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                            KeypadGrid(
                                onKeyPress = { viewModel.onKeyPress(it) },
                                isAdvanced = isAdvancedMode,
                                modifier = Modifier.weight(2.2f)
                            )
                        } else {
                            // Converter layout main area
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ConverterCategoryRow(
                                    selectedCategory = converterCategory,
                                    onCategorySelected = { viewModel.setConverterCategory(it) }
                                )
                                ConverterInputCard(
                                    label = "FROM INPUT VALUE",
                                    value = converterInputValue,
                                    selectedUnit = converterFromUnit,
                                    availableUnits = converterCategory.units,
                                    onUnitSelected = { viewModel.setConverterFromUnit(it) },
                                    isActive = true,
                                    onValueChange = { viewModel.setConverterInputValue(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.swapConverterUnits() },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(VsCodeButtonBg)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Swap Units",
                                        tint = VsCodeBlueLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                ConverterInputCard(
                                    label = "CONVERTED RESULT",
                                    value = converterResult,
                                    selectedUnit = converterToUnit,
                                    availableUnits = converterCategory.units,
                                    onUnitSelected = { viewModel.setConverterToUnit(it) },
                                    isActive = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                            ConverterKeypadGrid(
                                onKeyPress = { viewModel.onKeyPress(it) },
                                onSwap = { viewModel.swapConverterUnits() },
                                modifier = Modifier.weight(2.2f)
                            )
                        }
                    }
                }

                // Supporting Pane (Permanent History Log and Advanced functions quick sheet)
                Card(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, VsCodeActiveBorder)
                ) {
                    if (appMode == AppMode.CALCULATOR) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp) // More whitespace
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Calculation History",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VsCodeTextPrimary
                                )
                                if (history.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearHistory() },
                                        modifier = Modifier.testTag("clear_history_wide_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear all history",
                                            tint = VsCodeRed
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = VsCodeActiveBorder)

                            if (history.isEmpty()) {
                                EmptyHistoryState()
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(history, key = { it.id }) { item ->
                                        HistoryRow(
                                            item = item,
                                            onClick = { viewModel.selectHistoryItem(item) }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VsCodeBackground),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, VsCodeActiveBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Quick help",
                                        tint = VsCodeTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Full operators (sin, cos, log, %, ^) auto-balance standard parenthesis groupings on calculation.",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = VsCodeTextPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // Multi unit converter fast reference side card
                        ConverterQuickReferencePane(
                            valueStr = converterInputValue,
                            fromUnit = converterFromUnit,
                            category = converterCategory
                        )
                    }
                }
            }
        } else {
            // Standard Compact / Mobile Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp) // More whitespace at edges
            ) {
                CalculatorHeader(
                    onToggleAdvanced = { viewModel.toggleAdvancedMode() },
                    onToggleHistory = { showHistoryPopup = true },
                    onShowInfo = { showInfoDialog = true },
                    isAdvanced = isAdvancedMode,
                    isWide = false,
                    appMode = appMode,
                    onModeSelected = { viewModel.setAppMode(it) }
                )
                Spacer(modifier = Modifier.height(20.dp)) // More whitespace
                ModeSwitcher(
                    currentMode = appMode,
                    onModeSelected = { viewModel.setAppMode(it) }
                )
                Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                
                if (appMode == AppMode.CALCULATOR) {
                    DisplayArea(
                        expression = expression,
                        preview = previewResult,
                        shakeOffset = shakeOffset,
                        isError = isError,
                        modifier = Modifier.weight(1.2f)
                    )
                    Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                    KeypadGrid(
                        onKeyPress = { viewModel.onKeyPress(it) },
                        isAdvanced = isAdvancedMode,
                        modifier = Modifier.weight(3f)
                    )
                } else {
                    // Converter Compact view
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ConverterCategoryRow(
                            selectedCategory = converterCategory,
                            onCategorySelected = { viewModel.setConverterCategory(it) }
                        )
                        ConverterInputCard(
                            label = "FROM",
                            value = converterInputValue,
                            selectedUnit = converterFromUnit,
                            availableUnits = converterCategory.units,
                            onUnitSelected = { viewModel.setConverterFromUnit(it) },
                            isActive = true,
                            onValueChange = { viewModel.setConverterInputValue(it) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.swapConverterUnits() },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(VsCodeButtonBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Units",
                                tint = VsCodeBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ConverterInputCard(
                            label = "TO",
                            value = converterResult,
                            selectedUnit = converterToUnit,
                            availableUnits = converterCategory.units,
                            onUnitSelected = { viewModel.setConverterToUnit(it) },
                            isActive = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp)) // More whitespace
                    ConverterKeypadGrid(
                        onKeyPress = { viewModel.onKeyPress(it) },
                        onSwap = { viewModel.swapConverterUnits() },
                        modifier = Modifier.weight(3f)
                    )
                }
            }

            // Compact History Bottom Sheet
            if (showHistoryPopup) {
                ModalBottomSheet(
                    onDismissRequest = { showHistoryPopup = false },
                    containerColor = VsCodeSidebar,
                    scrimColor = Color(0x99000000)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(0.6f)
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "History",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = VsCodeTextPrimary
                            )
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearHistory() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = VsCodeRed),
                                    modifier = Modifier.testTag("clear_history_compact_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear History",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (history.isEmpty()) {
                            EmptyHistoryState()
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(history, key = { it.id }) { item ->
                                    HistoryRow(
                                        item = item,
                                        onClick = {
                                            viewModel.selectHistoryItem(item)
                                            showHistoryPopup = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature / Operator Info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Supported Calculations") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoLabel("Basic Math", "Addition (+), Subtraction (-), Multiplication (×), Division (÷)")
                        InfoLabel("Grouping", "Parentheses ( and ) let you override standard operator priority precedence.")
                        InfoLabel("Powers & Roots", "Exponentiate using ^ (e.g., 2^3) and square root √ (e.g., √25)")
                        InfoLabel("Percentage", "% resolves postfix percentages easily (e.g., 50×10% = 5)")
                        InfoLabel("Trigonometry in Radian", "Trig ratios (sin, cos, tan) can be type-scaled automatically.")
                        InfoLabel("Logarithms", "ln(x) for natural logarithms and log(x) for base-10 calculations.")
                        InfoLabel("Constants", "Supports standard constants e and π (Pi value ~3.14).")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VsCodeStatusBar),
                        modifier = Modifier.testTag("info_dialog_ok")
                    ) {
                        Text("Close")
                    }
                },
                containerColor = VsCodeSidebar,
                titleContentColor = VsCodeTextPrimary,
                textContentColor = VsCodeTextSecondary
            )
        }
    }
}

@Composable
fun InfoLabel(title: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = VsCodeTeal)
        Text(desc, fontSize = 12.sp, color = VsCodeTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun CalculatorHeader(
    onToggleAdvanced: () -> Unit,
    onToggleHistory: () -> Unit,
    onShowInfo: () -> Unit,
    isAdvanced: Boolean,
    isWide: Boolean,
    appMode: AppMode,
    onModeSelected: (AppMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (appMode == AppMode.CALCULATOR) "CALCULATOR" else "CONVERTER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = VsCodeStatusBar
            )
            Text(
                text = "VS Code Precision Widget",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = VsCodeTextSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // About/Syntax guidelines info trigger
            IconButton(
                onClick = onShowInfo,
                modifier = Modifier.testTag("info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Formula syntax help",
                    tint = VsCodeTextSecondary
                )
            }

            if (appMode == AppMode.CALCULATOR) {
                // Advanced keys drawer trigger
                TextButton(
                    onClick = onToggleAdvanced,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isAdvanced) VsCodeStatusBar else VsCodeTextSecondary
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("toggle_advanced_button")
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isAdvanced) "Basic" else "Scientific",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Compact History button
            if (!isWide && appMode == AppMode.CALCULATOR) {
                IconButton(
                    onClick = onToggleHistory,
                    modifier = Modifier.testTag("compact_history_trigger")
                ) {
                    Text(
                        text = "History",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VsCodeTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayArea(
    expression: String,
    preview: String,
    shakeOffset: Float,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Drag standard scrolls dynamically aligned to bottom right
    LaunchedEffect(expression) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VsCodeKeypadBg) // VS Code integrated container background
            .border(BorderStroke(1.dp, VsCodeActiveBorder), RoundedCornerShape(16.dp))
            .padding(24.dp) // More whitespace internally
            .offset(x = shakeOffset.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Main Formula Output Row
            Text(
                text = if (expression.isEmpty()) "0" else expression,
                fontSize = if (expression.length > 14) 28.sp else 38.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (isError) VsCodeRed else VsCodeTextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .testTag("expression_display")
            )

            Spacer(modifier = Modifier.height(12.dp)) // More whitespace

            // Sub Live Formula Outcome preview
            AnimatedVisibility(
                visible = preview.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "= $preview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VsCodeTeal, // VS Code Teal outcome highlights
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_preview")
                )
            }
        }
    }
}

@Composable
fun KeypadGrid(
    onKeyPress: (String) -> Unit,
    isAdvanced: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expandable Scientific Keypad drawer with beautiful soft reveal animations
        AnimatedVisibility(
            visible = isAdvanced,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Trigonometry scientific row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScientificKey("sin(", "sin", onKeyPress, Modifier.weight(1f))
                    ScientificKey("cos(", "cos", onKeyPress, Modifier.weight(1f))
                    ScientificKey("tan(", "tan", onKeyPress, Modifier.weight(1f))
                    ScientificKey("π", "π", onKeyPress, Modifier.weight(1f))
                }
                // Logs and other row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScientificKey("log(", "log", onKeyPress, Modifier.weight(1f))
                    ScientificKey("ln(", "ln", onKeyPress, Modifier.weight(1f))
                    ScientificKey("e", "e", onKeyPress, Modifier.weight(1f))
                    ScientificKey("^", "xʸ", onKeyPress, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Standard Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionKey("C", "C", onKeyPress, Modifier.weight(1f))
            SpecialKey("(", "(", onKeyPress, Modifier.weight(1.0f))
            SpecialKey(")", ")", onKeyPress, Modifier.weight(1.0f))
            ActionKey("⌫", "delete", onKeyPress, Modifier.weight(1f))
        }

        // Standard Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecialKey("√", "√", onKeyPress, Modifier.weight(1f))
            SpecialKey("^", "^", onKeyPress, Modifier.weight(1f))
            SpecialKey("%", "%", onKeyPress, Modifier.weight(1f))
            OperatorKey("÷", "÷", onKeyPress, Modifier.weight(1f))
        }

        // Standard Row 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("7", onKeyPress, Modifier.weight(1f))
            NumberKey("8", onKeyPress, Modifier.weight(1f))
            NumberKey("9", onKeyPress, Modifier.weight(1f))
            OperatorKey("×", "×", onKeyPress, Modifier.weight(1f))
        }

        // Standard Row 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("4", onKeyPress, Modifier.weight(1f))
            NumberKey("5", onKeyPress, Modifier.weight(1f))
            NumberKey("6", onKeyPress, Modifier.weight(1f))
            OperatorKey("-", "-", onKeyPress, Modifier.weight(1f))
        }

        // Standard Row 5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("1", onKeyPress, Modifier.weight(1f))
            NumberKey("2", onKeyPress, Modifier.weight(1f))
            NumberKey("3", onKeyPress, Modifier.weight(1f))
            OperatorKey("+", "+", onKeyPress, Modifier.weight(1f))
        }

        // Standard Row 6
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecialKey("±", "±", onKeyPress, Modifier.weight(1.0f))
            NumberKey("0", onKeyPress, Modifier.weight(1.0f))
            NumberKey(".", onKeyPress, Modifier.weight(1.0f))
            EqualKey("=", onKeyPress, Modifier.weight(1.0f))
        }
    }
}

@Composable
fun NumberKey(
    value: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VsCodeButtonBg
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(value) }
            .testTag("key_num_$value")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeBlueLight
            )
        }
    }
}

@Composable
fun OperatorKey(
    symbol: String,
    label: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VsCodeSidebar
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_op_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodePurple
            )
        }
    }
}

@Composable
fun SpecialKey(
    symbol: String,
    label: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VsCodeButtonBg
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_spec_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VsCodeYellow
            )
        }
    }
}

@Composable
fun ScientificKey(
    symbol: String,
    label: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VsCodeKeypadBg
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .height(36.dp)
            .clickable { onKeyPress(symbol) }
            .testTag("key_scientific_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodePurple
            )
        }
    }
}

@Composable
fun ActionKey(
    symbol: String,
    label: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (label == "delete" || symbol == "C") VsCodeRed else VsCodeOrange
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_action_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (symbol == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Backspace,
                    contentDescription = "delete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = symbol,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EqualKey(
    symbol: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VsCodeStatusBar
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_equal")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VsCodeKeypadBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp) // Added more whitespace
        ) {
            Text(
                text = item.expression,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = VsCodeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp)) // Added more whitespace
            Text(
                text = "= ${item.result}",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = VsCodeTeal,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = VsCodeActiveBorder,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No calculations yet",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VsCodeTextSecondary
        )
        Text(
            text = "Your computation history displays here.",
            fontSize = 11.sp,
            color = VsCodeTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ModeSwitcher(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VsCodeKeypadBg) // VS Code Keypad Background
            .border(BorderStroke(1.dp, VsCodeActiveBorder), RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val modes = listOf(AppMode.CALCULATOR, AppMode.CONVERTER)
        modes.forEach { mode ->
            val isSelected = currentMode == mode
            val isSelectedColor = if (isSelected) VsCodeStatusBar else Color.Transparent
            val textColor = if (isSelected) Color.White else VsCodeTextSecondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(isSelectedColor)
                    .clickable { onModeSelected(mode) }
                    .testTag("mode_switch_${mode.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        AppMode.CALCULATOR -> "Calculator"
                        AppMode.CONVERTER -> "Converter Tools"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ConverterCategoryRow(
    selectedCategory: ConverterCategory,
    onCategorySelected: (ConverterCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp), // More spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConverterCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val containerColor = if (isSelected) VsCodeSidebar else VsCodeBackground
            val borderStroke = if (isSelected) BorderStroke(1.5.dp, VsCodeStatusBar) else BorderStroke(1.dp, VsCodeActiveBorder)
            val textColor = if (isSelected) Color.White else VsCodeTextSecondary
            
            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = borderStroke,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
                    .testTag("category_pill_${category.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), // Spaced out / padding whitespace
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categoryIcon = when (category) {
                        ConverterCategory.LENGTH -> Icons.Default.Straighten
                        ConverterCategory.WEIGHT -> Icons.Default.Balance
                        ConverterCategory.TEMPERATURE -> Icons.Default.Thermostat
                        ConverterCategory.VOLUME -> Icons.Default.Science
                    }
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category.displayName,
                        tint = if (isSelected) VsCodeTeal else VsCodeTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun ConverterInputCard(
    label: String,
    value: String,
    selectedUnit: String,
    availableUnits: List<String>,
    onUnitSelected: (String) -> Unit,
    isActive: Boolean,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) VsCodeSidebar else VsCodeKeypadBg
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isActive) BorderStroke(1.5.dp, VsCodeStatusBar) else BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 105.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = VsCodeTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VsCodeBackground)
                            .clickable { expanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedUnit,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VsCodeTeal
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown icon",
                            tint = VsCodeBlueLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
 
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(VsCodeSidebar)
                    ) {
                        availableUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit, color = VsCodeTextPrimary) },
                                onClick = {
                                    onUnitSelected(unit)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
 
            Box(
                modifier = Modifier.weight(1.9f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isActive) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == '.' || it == '-' }) {
                                onValueChange(newValue)
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = VsCodeTextPrimary,
                            fontSize = if (value.length > 10) (if (value.length > 15) 16.sp else 20.sp) else 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(VsCodeStatusBar),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("converter_input_text_field"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = "0",
                                        color = VsCodeTextSecondary,
                                        fontSize = 26.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Text(
                        text = if (value.isEmpty()) "0" else value,
                        fontSize = if (value.length > 10) (if (value.length > 15) 16.sp else 20.sp) else 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = VsCodeTeal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ConverterKeypadGrid(
    onKeyPress: (String) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("7", onKeyPress, Modifier.weight(1f))
            NumberKey("8", onKeyPress, Modifier.weight(1f))
            NumberKey("9", onKeyPress, Modifier.weight(1f))
            ActionKey("⌫", "delete", onKeyPress, Modifier.weight(1f))
        }

        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("4", onKeyPress, Modifier.weight(1f))
            NumberKey("5", onKeyPress, Modifier.weight(1f))
            NumberKey("6", onKeyPress, Modifier.weight(1f))
            ActionKey("C", "C", onKeyPress, Modifier.weight(1f))
        }

        // Row 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("1", onKeyPress, Modifier.weight(1f))
            NumberKey("2", onKeyPress, Modifier.weight(1f))
            NumberKey("3", onKeyPress, Modifier.weight(1f))
            SpecialKey("±", "±", onKeyPress, Modifier.weight(1f))
        }

        // Row 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = VsCodeTeal
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, VsCodeActiveBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSwap() }
                    .testTag("key_converter_swap")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap icon",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Swap",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            NumberKey("0", onKeyPress, Modifier.weight(1f))
            NumberKey(".", onKeyPress, Modifier.weight(1f))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ConverterQuickReferencePane(
    valueStr: String,
    fromUnit: String,
    category: ConverterCategory,
    modifier: Modifier = Modifier
) {
    val value = valueStr.toDoubleOrNull() ?: 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            text = "Full Unit Reference",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VsCodeTextPrimary
        )
        Text(
            text = "Simultaneous equivalents for current input",
            fontSize = 11.sp,
            color = VsCodeTextSecondary
        )
        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = VsCodeKeypadBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, VsCodeActiveBorder),
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(category.units) { unit ->
                    val isSame = unit == fromUnit
                    val converted = if (isSame) value else ConverterEngine.convert(value, fromUnit, unit, category)
                    val formatted = ConverterEngine.formatResult(converted)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSame) VsCodeBackground else Color.Transparent)
                            .border(
                                width = if (isSame) 1.dp else 0.dp,
                                color = if (isSame) VsCodeStatusBar else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = unit,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSame) VsCodeStatusBar else VsCodeTextSecondary
                        )
                        Text(
                            text = formatted,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSame) VsCodeTeal else VsCodeTextPrimary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
