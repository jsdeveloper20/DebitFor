package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WalletViewModel
import java.util.Locale

@Composable
fun ExchangeScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsState()

    var fromCurrency by remember { mutableStateOf("RUB") }
    var toCurrency by remember { mutableStateOf("USDT") }

    var fromAmountStr by remember { mutableStateOf("") }

    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    // Find the currently selected assets to show available balances
    val fromAsset = assets.find { it.id == fromCurrency }
    val toAsset = assets.find { it.id == toCurrency }

    // Live exchange rate
    val currentRate = viewModel.getExchangeRate(fromCurrency, toCurrency)

    // Calculate dynamic "To" amount
    val fromAmount = fromAmountStr.toDoubleOrNull() ?: 0.0
    val calculatedToAmount = fromAmount * currentRate

    val errorMessage = when {
        fromAmount <= 0 -> null
        fromAsset != null && fromAmount > fromAsset.balance -> "Недостаточно средств на балансе!"
        fromCurrency == toCurrency -> "Выберите разные валюты для обмена!"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Быстрый Обмен Валют",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Конвертируйте рубли, доллары и криптовалюту за долю секунды по лучшему рыночному курсу.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = SoftGray,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // "FROM" Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Отдаете", color = SoftGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Доступно: ${fromAsset?.let { String.format(Locale.US, "%,.4f", it.balance).trimEnd('0').trimEnd('.') } ?: "0.0"} ${fromAsset?.symbol ?: ""}",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            fromAsset?.let {
                                fromAmountStr = if (it.type == "CRYPTO") {
                                    String.format(Locale.US, "%.6f", it.balance).trimEnd('0').trimEnd('.')
                                } else {
                                    String.format(Locale.US, "%.2f", it.balance)
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // From Currency Dropdown Trigger
                    Box(modifier = Modifier.width(110.dp)) {
                        Button(
                            onClick = { fromDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("exchange_from_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fromCurrency, color = Color.White, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = fromDropdownExpanded,
                            onDismissRequest = { fromDropdownExpanded = false },
                            modifier = Modifier.background(CardBG)
                        ) {
                            assets.forEach { asset ->
                                DropdownMenuItem(
                                    text = { Text(asset.id, color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        fromCurrency = asset.id
                                        fromDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Input Field
                    OutlinedTextField(
                        value = fromAmountStr,
                        onValueChange = { fromAmountStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("exchange_from_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        placeholder = { Text("0.00", color = SoftGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Swap Direction Button
        IconButton(
            onClick = {
                val temp = fromCurrency
                fromCurrency = toCurrency
                toCurrency = temp
                fromAmountStr = ""
            },
            modifier = Modifier
                .size(44.dp)
                .background(PrimaryGold, RoundedCornerShape(12.dp))
                .testTag("swap_direction_button")
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "Поменять местами",
                tint = Color(0xFF381E72),
                modifier = Modifier.size(24.dp)
            )
        }

        // "TO" Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Получаете", color = SoftGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Доступно: ${toAsset?.let { String.format(Locale.US, "%,.4f", it.balance).trimEnd('0').trimEnd('.') } ?: "0.0"} ${toAsset?.symbol ?: ""}",
                        color = SoftGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // To Currency Dropdown Trigger
                    Box(modifier = Modifier.width(110.dp)) {
                        Button(
                            onClick = { toDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("exchange_to_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(toCurrency, color = Color.White, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = toDropdownExpanded,
                            onDismissRequest = { toDropdownExpanded = false },
                            modifier = Modifier.background(CardBG)
                        ) {
                            assets.forEach { asset ->
                                DropdownMenuItem(
                                    text = { Text(asset.id, color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        toCurrency = asset.id
                                        toDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Simulated output amount box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val formattedOutput = if (calculatedToAmount > 0) {
                            String.format(Locale.US, "%,.6f", calculatedToAmount).trimEnd('0').trimEnd('.')
                        } else {
                            "0.00"
                        }
                        Text(
                            text = formattedOutput,
                            color = if (calculatedToAmount > 0) Color.White else SoftGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Live Rate Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SoftGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val rateString = String.format(Locale.US, "%,.6f", currentRate).trimEnd('0').trimEnd('.')
            val revRateString = String.format(Locale.US, "%,.6f", 1.0 / currentRate).trimEnd('0').trimEnd('.')
            Text(
                text = "Текущий курс: 1 $fromCurrency = $rateString $toCurrency (1 $toCurrency = $revRateString $fromCurrency)",
                color = SoftGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Warning message label (if any error)
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.20f))
            ) {
                Text(
                    text = errorMessage,
                    color = DangerRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }

        // Core Exchange Action Button
        Button(
            onClick = {
                viewModel.executeExchange(fromCurrency, toCurrency, fromAmount) {
                    fromAmountStr = ""
                }
            },
            enabled = (errorMessage == null && fromAmount > 0),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("exchange_submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold,
                contentColor = Color(0xFF381E72),
                disabledContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Обменять сейчас",
                color = if (fromAmount > 0 && errorMessage == null) Color(0xFF381E72) else SoftGray,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
