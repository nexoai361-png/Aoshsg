package com.example.calculator.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.engine.ConverterCategory
import com.example.calculator.engine.ConverterEngine
import com.example.calculator.viewmodel.AppMode
import com.example.calculator.viewmodel.CalculatorViewModel
import com.example.calculator.viewmodel.HistoryItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AppSidebar(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    showHistoryPopup: Boolean,
    onToggleHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .background(VsCodeSidebar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab 1: Calculator Icon
        val isCalcSelected = currentMode == AppMode.CALCULATOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onModeSelected(AppMode.CALCULATOR) }
                .testTag("sidebar_tab_calculator"),
            contentAlignment = Alignment.Center
        ) {
            if (isCalcSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight(0.6f)
                        .background(VsCodeStatusBar)
                )
            }
            Icon(
                imageVector = Icons.Default.Terminal, // VS Code console look
                contentDescription = "Calculator Drawer",
                tint = if (isCalcSelected) Color.White else VsCodeTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Tab 2: History Icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onToggleHistory() }
                .testTag("sidebar_tab_history"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History Toggle",
                tint = if (showHistoryPopup) Color.White else VsCodeTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Tab 3: Converter Icon
        val isConverterSelected = currentMode == AppMode.CONVERTER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onModeSelected(AppMode.CONVERTER) }
                .testTag("sidebar_tab_converter"),
            contentAlignment = Alignment.Center
        ) {
            if (isConverterSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight(0.6f)
                        .background(VsCodeStatusBar)
                )
            }
            Icon(
                imageVector = Icons.Default.Autorenew, // circular arrows loop exactly like images
                contentDescription = "Converter Tools list",
                tint = if (isConverterSelected) Color.White else VsCodeTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

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

    // GST States
    val gstAmountInput by viewModel.gstAmountInput.collectAsStateWithLifecycle()
    val gstRateInput by viewModel.gstRateInput.collectAsStateWithLifecycle()
    val gstIsAddMode by viewModel.gstIsAddMode.collectAsStateWithLifecycle()

    var showHistoryPopup by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Active Category Selection local state - null indicates Grid Overview screen
    var activeCategory by rememberSaveable { mutableStateOf<ConverterCategory?>(null) }

    // Sound / Visual shake on syntax error triggers
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

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Authentic VS Code Sidebar (displays across both orientations)
        AppSidebar(
            currentMode = appMode,
            onModeSelected = { viewModel.setAppMode(it) },
            showHistoryPopup = showHistoryPopup,
            onToggleHistory = { showHistoryPopup = !showHistoryPopup }
        )

        // Sidebar divider line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(VsCodeActiveBorder)
        )

        // Main Workspace Screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (appMode == AppMode.CALCULATOR) {
                // CALCULATOR WORKSPACE
                if (isWideScreen) {
                    // Wide Desktop Split View
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VsCodeActiveBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                CalculatorHeader(
                                    onToggleAdvanced = { viewModel.toggleAdvancedMode() },
                                    onShowInfo = { showInfoDialog = true },
                                    isAdvanced = isAdvancedMode,
                                    appMode = appMode
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                DisplayArea(
                                    expression = expression,
                                    preview = previewResult,
                                    shakeOffset = shakeOffset,
                                    isError = isError,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                KeypadGrid(
                                    onKeyPress = { viewModel.onKeyPress(it) },
                                    isAdvanced = isAdvancedMode,
                                    modifier = Modifier.weight(2.2f)
                                )
                            }
                        }

                        // Wide History Block
                        Card(
                            modifier = Modifier
                                .weight(0.8f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VsCodeActiveBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Calculation History",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VsCodeTextPrimary
                                    )
                                    if (history.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.clearHistory() }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear",
                                                tint = VsCodeRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = VsCodeActiveBorder)
                                if (history.isEmpty()) {
                                    EmptyHistoryState()
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(history, key = { it.id }) { item ->
                                            HistoryRow(
                                                item = item,
                                                onClick = { viewModel.selectHistoryItem(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Compact Mobile calculator screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        CalculatorHeader(
                            onToggleAdvanced = { viewModel.toggleAdvancedMode() },
                            onShowInfo = { showInfoDialog = true },
                            isAdvanced = isAdvancedMode,
                            appMode = appMode
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DisplayArea(
                            expression = expression,
                            preview = previewResult,
                            shakeOffset = shakeOffset,
                            isError = isError,
                            modifier = Modifier.weight(1.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        KeypadGrid(
                            onKeyPress = { viewModel.onKeyPress(it) },
                            isAdvanced = isAdvancedMode,
                            modifier = Modifier.weight(3f)
                        )
                    }
                }
            } else {
                // CONVERTER WORKSPACE
                if (activeCategory == null) {
                    // MAIN CONVERTER GRID OVERVIEW (matching Image 2)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Bar Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew, // loop sync circular icon
                                contentDescription = "Converter Logo",
                                tint = VsCodeTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ReversX-Converter",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VsCodeTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        HorizontalDivider(
                            color = VsCodeActiveBorder,
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. STANDARD TOOLS
                        Text(
                            text = "STANDARD TOOLS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = VsCodeTextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        val standardTools = listOf(
                            ConverterCategory.LENGTH,
                            ConverterCategory.WEIGHT,
                            ConverterCategory.TEMPERATURE,
                            ConverterCategory.DATA,
                            ConverterCategory.AREA,
                            ConverterCategory.VOLUME,
                            ConverterCategory.SPEED,
                            ConverterCategory.TIME,
                            ConverterCategory.ANGLE,
                            ConverterCategory.PRESSURE
                        )

                        // 2-column list layout mapping Image 2
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (i in standardTools.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val leftTool = standardTools[i]
                                    val rightTool = if (i + 1 < standardTools.size) standardTools[i + 1] else null

                                    ConverterToolCard(
                                        title = leftTool.displayName,
                                        category = leftTool,
                                        onClick = {
                                            viewModel.setConverterCategory(leftTool)
                                            activeCategory = leftTool
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (rightTool != null) {
                                        ConverterToolCard(
                                            title = rightTool.displayName,
                                            category = rightTool,
                                            onClick = {
                                                viewModel.setConverterCategory(rightTool)
                                                activeCategory = rightTool
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. BUSINESS AND FINANCIAL
                        Text(
                            text = "BUSINESS AND FINANCIAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = VsCodeTextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ConverterToolCard(
                                title = "Currency",
                                category = ConverterCategory.CURRENCY,
                                onClick = {
                                    viewModel.setConverterCategory(ConverterCategory.CURRENCY)
                                    activeCategory = ConverterCategory.CURRENCY
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ConverterToolCard(
                                title = "GST",
                                category = ConverterCategory.GST,
                                onClick = {
                                    viewModel.setConverterCategory(ConverterCategory.GST)
                                    activeCategory = ConverterCategory.GST
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                } else if (activeCategory == ConverterCategory.GST) {
                    // DETAIL WORKSPACE: GST CALCULATOR VIEW (matching Image 1)
                    val amountDouble = gstAmountInput.toDoubleOrNull() ?: 0.0
                    val rateDouble = gstRateInput.toDoubleOrNull() ?: 0.0

                    val (computedGst, computedNet, computedTotal) = if (gstIsAddMode) {
                        val gst = amountDouble * (rateDouble / 100.0)
                        val total = amountDouble + gst
                        Triple(gst, amountDouble, total)
                    } else {
                        val total = amountDouble
                        val net = amountDouble / (1.0 + (rateDouble / 100.0))
                        val gst = total - net
                        Triple(gst, net, total)
                    }

                    val formattedGst = if (gstAmountInput.isEmpty()) "0.00" else String.format(java.util.Locale.US, "%.2f", computedGst)
                    val formattedNet = if (gstAmountInput.isEmpty()) "0.00" else String.format(java.util.Locale.US, "%.2f", computedNet)
                    val formattedTotal = if (gstAmountInput.isEmpty()) "0.00" else String.format(java.util.Locale.US, "%.2f", computedTotal)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Detail view header back control
                        DetailHeader(
                            title = "GST Calculator",
                            onBack = { activeCategory = null }
                        )

                        // Input Amount Label
                        Text(
                            text = "Amount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VsCodeTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // BIG SIZE AMOUNT BOX supporting system-keyboard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VsCodeSidebar)
                                .border(BorderStroke(1.dp, VsCodeActiveBorder))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = gstAmountInput,
                                onValueChange = { viewModel.setGstAmountInput(it) },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(VsCodeStatusBar),
                                modifier = Modifier.fillMaxWidth().testTag("gst_amount_input"),
                                decorationBox = { innerTextField ->
                                    if (gstAmountInput.isEmpty()) {
                                        Text(
                                            text = "100",
                                            color = VsCodeTextSecondary,
                                            fontSize = 18.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        // GST Rate Label
                        Text(
                            text = "GST Rate (%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VsCodeTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // BIG SIZE GST RATE BOX supporting system-keyboard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VsCodeSidebar)
                                .border(BorderStroke(1.dp, VsCodeActiveBorder))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = gstRateInput,
                                onValueChange = { viewModel.setGstRateInput(it) },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(VsCodeStatusBar),
                                modifier = Modifier.fillMaxWidth().testTag("gst_rate_input"),
                                decorationBox = { innerTextField ->
                                    if (gstRateInput.isEmpty()) {
                                        Text(
                                            text = "18",
                                            color = VsCodeTextSecondary,
                                            fontSize = 18.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        // Buttons Grid of ADD / REMOVE GST (styled identically to Image 1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Add GST Blue Filled Button
                            Button(
                                onClick = { viewModel.setGstAddMode(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("gst_add_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (gstIsAddMode) VsCodeStatusBar else VsCodeButtonBg,
                                    contentColor = if (gstIsAddMode) Color.White else VsCodeTextPrimary
                                ),
                                border = if (!gstIsAddMode) BorderStroke(1.dp, VsCodeActiveBorder) else null
                            ) {
                                Text(
                                    text = "Add GST",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Remove GST Outlined Border Button
                            Button(
                                onClick = { viewModel.setGstAddMode(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("gst_remove_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!gstIsAddMode) VsCodeStatusBar else VsCodeButtonBg,
                                    contentColor = if (!gstIsAddMode) Color.White else VsCodeTextPrimary
                                ),
                                border = if (gstIsAddMode) BorderStroke(1.dp, VsCodeActiveBorder) else null
                            ) {
                                Text(
                                    text = "Remove GST",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Outcome Summary Calculation Card (exactly like Image 1)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, VsCodeActiveBorder), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GST Amount:",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = VsCodeTextSecondary
                                    )
                                    Text(
                                        text = formattedGst,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Net Amount:",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = VsCodeTextSecondary
                                    )
                                    Text(
                                        text = formattedNet,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                HorizontalDivider(
                                    color = VsCodeActiveBorder,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Amount:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = formattedTotal,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // DETAIL WORKSPACE: STANDARD UNIT CONVERTERS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailHeader(
                            title = "${activeCategory?.displayName ?: ""} Converter",
                            onBack = { activeCategory = null }
                        )

                        // FROM Card - ENLARGED SIZE as requested ("box size cross boro kor")
                        ConverterInputCard(
                            label = "FROM INPUT VALUE",
                            value = converterInputValue,
                            selectedUnit = converterFromUnit,
                            availableUnits = activeCategory?.units ?: emptyList(),
                            onUnitSelected = { viewModel.setConverterFromUnit(it) },
                            isActive = true,
                            onValueChange = { viewModel.setConverterInputValue(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp) // Large Size!
                        )

                        // Swap Units Icon button
                        IconButton(
                            onClick = { viewModel.swapConverterUnits() },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VsCodeButtonBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Units",
                                tint = VsCodeBlueLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // TO Card - ENLARGED SIZE as requested
                        ConverterInputCard(
                            label = "CONVERTED RESULT",
                            value = converterResult,
                            selectedUnit = converterToUnit,
                            availableUnits = activeCategory?.units ?: emptyList(),
                            onUnitSelected = { viewModel.setConverterToUnit(it) },
                            isActive = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp) // Large Size!
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Full alternative equivalent list
                        ConverterQuickReferencePane(
                            valueStr = converterInputValue,
                            fromUnit = converterFromUnit,
                            category = activeCategory ?: ConverterCategory.LENGTH,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp, max = 500.dp)
                        )
                    }
                }
            }
        }
    }

    // Compact history list bottom panel sheet
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
                        text = "Calculation History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VsCodeTextPrimary
                    )
                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = VsCodeRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (history.isEmpty()) {
                    EmptyHistoryState()
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
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

    // Guidelines instruction popup
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Supported Operations") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoLabel("Basic calculations", "Addition (+), Subtraction (-), Multiplication (×), Division (÷)")
                    InfoLabel("Grouping", "Parentheses ( and ) logic standard resolution on equals trigger.")
                    InfoLabel("Trig, Logs, Powers", "Supports sin, cos, tan, log, ln, with custom base-10 functions.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VsCodeStatusBar)
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

@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBack() }
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // nice back arrow icon
                contentDescription = "Back",
                tint = VsCodeStatusBar,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeStatusBar
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VsCodeTextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ConverterToolCard(
    title: String,
    category: ConverterCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (category) {
        ConverterCategory.LENGTH -> Icons.Default.Straighten
        ConverterCategory.WEIGHT -> Icons.Default.Balance
        ConverterCategory.TEMPERATURE -> Icons.Default.Thermostat
        ConverterCategory.DATA -> Icons.Default.Storage
        ConverterCategory.AREA -> Icons.Default.GridView
        ConverterCategory.VOLUME -> Icons.Default.Science
        ConverterCategory.SPEED -> Icons.Default.Speed
        ConverterCategory.TIME -> Icons.Default.History
        ConverterCategory.ANGLE -> Icons.Default.Explore
        ConverterCategory.PRESSURE -> Icons.Default.Adjust
        ConverterCategory.CURRENCY -> Icons.Default.Language
        ConverterCategory.GST -> Icons.Default.ReceiptLong
    }

    val iconColor = when (category) {
        ConverterCategory.GST -> VsCodeOrange
        ConverterCategory.CURRENCY -> VsCodeBlueLight
        else -> VsCodeTeal
    }

    Card(
        modifier = modifier
            .height(105.dp) // Matching Image 2 vertical dimensions
            .clickable { onClick() }
            .testTag("tool_card_${category.name.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = VsCodeKeypadBg),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VsCodeTextPrimary,
                fontFamily = FontFamily.Monospace
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
    onShowInfo: () -> Unit,
    isAdvanced: Boolean,
    appMode: AppMode
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = VsCodeStatusBar
            )
            Text(
                text = "VS Code Precision Widget",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = VsCodeTextSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onShowInfo) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Help",
                    tint = VsCodeTextSecondary
                )
            }
            if (appMode == AppMode.CALCULATOR) {
                TextButton(
                    onClick = onToggleAdvanced,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isAdvanced) VsCodeStatusBar else VsCodeTextSecondary
                    ),
                    shape = CircleShape,
                ) {
                    Text(
                        text = if (isAdvanced) "Basic" else "Scientific",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
    LaunchedEffect(expression) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VsCodeKeypadBg)
            .border(BorderStroke(1.dp, VsCodeActiveBorder), RoundedCornerShape(12.dp))
            .padding(20.dp)
            .offset(x = shakeOffset.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = if (expression.isEmpty()) "0" else expression,
                fontSize = if (expression.length > 14) 26.sp else 36.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (isError) VsCodeRed else VsCodeTextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = preview.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "= $preview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VsCodeTeal,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
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
        AnimatedVisibility(
            visible = isAdvanced,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScientificKey("sin(", "sin", onKeyPress, Modifier.weight(1f))
                    ScientificKey("cos(", "cos", onKeyPress, Modifier.weight(1f))
                    ScientificKey("tan(", "tan", onKeyPress, Modifier.weight(1f))
                    ScientificKey("π", "π", onKeyPress, Modifier.weight(1f))
                }
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

        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionKey("C", "clear", onKeyPress, Modifier.weight(1f))
            ActionKey("(", "paren_open", onKeyPress, Modifier.weight(1f))
            ActionKey(")", "paren_close", onKeyPress, Modifier.weight(1f))
            OperatorKey("÷", "div", onKeyPress, Modifier.weight(1f))
        }

        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("7", onKeyPress, Modifier.weight(1f))
            NumberKey("8", onKeyPress, Modifier.weight(1f))
            NumberKey("9", onKeyPress, Modifier.weight(1f))
            OperatorKey("×", "mul", onKeyPress, Modifier.weight(1f))
        }

        // Row 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("4", onKeyPress, Modifier.weight(1f))
            NumberKey("5", onKeyPress, Modifier.weight(1f))
            NumberKey("6", onKeyPress, Modifier.weight(1f))
            OperatorKey("-", "sub", onKeyPress, Modifier.weight(1f))
        }

        // Row 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberKey("1", onKeyPress, Modifier.weight(1f))
            NumberKey("2", onKeyPress, Modifier.weight(1f))
            NumberKey("3", onKeyPress, Modifier.weight(1f))
            OperatorKey("+", "add", onKeyPress, Modifier.weight(1f))
        }

        // Row 5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecialKey("±", "sign", onKeyPress, Modifier.weight(1f))
            NumberKey("0", onKeyPress, Modifier.weight(1f))
            SpecialKey(".", "decimal", onKeyPress, Modifier.weight(1f))
            
            // Delete key
            Card(
                colors = CardDefaults.cardColors(containerColor = VsCodeButtonBg),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, VsCodeActiveBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onKeyPress("⌫") }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "backspace",
                        tint = VsCodeBlueLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Equal key (blue selection highlights)
            Card(
                colors = CardDefaults.cardColors(containerColor = VsCodeStatusBar),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clickable { onKeyPress("=") }
                    .testTag("key_equal")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "=",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .height(38.dp)
            .clickable { onKeyPress(symbol) }
            .testTag("key_scientific_$label")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeTeal
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
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_op_$label")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeTeal
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
    Card(
        colors = CardDefaults.cardColors(containerColor = VsCodeButtonBg),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_action_$label")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeYellow
            )
        }
    }
}

@Composable
fun NumberKey(
    number: String,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(number) }
            .testTag("key_num_$number")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = number,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodeTextPrimary
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
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxHeight()
            .clickable { onKeyPress(symbol) }
            .testTag("key_special_$label")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VsCodePurple
            )
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VsCodeKeypadBg),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, VsCodeActiveBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expression,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = VsCodeTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "= ${item.result}",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = VsCodeTeal
                )
            }
            Icon(
                imageVector = Icons.Default.VerticalAlignBottom,
                contentDescription = "Inject",
                tint = VsCodeBlueLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = VsCodeActiveBorder,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No calculations yet",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VsCodeTextSecondary
        )
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
        modifier = modifier.fillMaxWidth()
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
                Spacer(modifier = Modifier.height(8.dp))
                
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
                    BasicTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == '.' || it == '-' }) {
                                onValueChange(newValue)
                            }
                        },
                        textStyle = TextStyle(
                            color = VsCodeTextPrimary,
                            fontSize = if (value.length > 10) (if (value.length > 15) 16.sp else 20.sp) else 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(VsCodeStatusBar),
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
                                        textAlign = TextAlign.End
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
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Full Unit Reference",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = VsCodeTextPrimary
        )
        Text(
            text = "Equivalent conversions across all units simultaneously",
            fontSize = 11.sp,
            color = VsCodeTextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, VsCodeActiveBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = unit,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSame) VsCodeStatusBar else VsCodeTextSecondary
                        )
                        Text(
                            text = formatted,
                            fontSize = 14.sp,
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
