package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.P2PAd
import com.example.data.database.P2PMessage
import com.example.data.database.P2PTrade
import com.example.ui.WalletViewModel
import java.util.Locale

@Composable
fun P2PScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTradeId by viewModel.selectedTradeId.collectAsState()

    // Choose whether to display the marketplace listing OR the active trade simulation
    AnimatedContent(
        targetState = selectedTradeId,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "P2PContentTransition"
    ) { tradeId ->
        if (tradeId != null) {
            ActiveP2PTradeView(
                tradeId = tradeId,
                viewModel = viewModel,
                onBackClick = { viewModel.selectTrade(null) }
            )
        } else {
            P2PMarketplaceView(
                viewModel = viewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
fun P2PMarketplaceView(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val ads by viewModel.p2pAds.collectAsState()
    val activeTrades by viewModel.p2pTrades.collectAsState()

    var userActionType by remember { mutableStateOf("BUY") } // "BUY" = User wants to buy crypto; "SELL" = User wants to sell crypto
    var selectedCrypto by remember { mutableStateOf("USDT") }
    var selectedFiat by remember { mutableStateOf("RUB") }

    var showCreateAdDialog by remember { mutableStateOf(false) }
    var selectedAdForTrade by remember { mutableStateOf<P2PAd?>(null) }

    // Filter advertisements
    val filteredAds = remember(ads, userActionType, selectedCrypto, selectedFiat) {
        ads.filter { ad ->
            // Match currencies
            val isCurrencyMatch = ad.cryptoCurrency == selectedCrypto && ad.fiatCurrency == selectedFiat
            // Match merchant action type:
            // If User is clicking "BUY" (user buys crypto), we want merchant ads of type "SELL"
            // If User is clicking "SELL" (user sells crypto), we want merchant ads of type "BUY"
            val isActionMatch = if (userActionType == "BUY") ad.type == "SELL" else ad.type == "BUY"
            isCurrencyMatch && isActionMatch
        }
    }

    val pendingTradesCount = remember(activeTrades) {
        activeTrades.count { it.status == "CREATED" || it.status == "PAID" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Space for safe top layout
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Core Header & Quick stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "P2P Торговля",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                    Text(
                        text = "Покупайте и продавайте крипту напрямую у людей",
                        style = MaterialTheme.typography.bodySmall.copy(color = SoftGray)
                    )
                }

                IconButton(
                    onClick = { showCreateAdDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .testTag("create_ad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Создать объявление",
                        tint = PrimaryGold
                    )
                }
            }
        }

        // Active Trade Bar notification (if any active trades are running)
        if (pendingTradesCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val activeTrade = activeTrades.firstOrNull { it.status == "CREATED" || it.status == "PAID" }
                            if (activeTrade != null) {
                                viewModel.selectTrade(activeTrade.id)
                            }
                        }
                        .testTag("active_trades_notification"),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AccentTeal.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "У вас есть активные сделки ($pendingTradesCount)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Нажмите, чтобы вернуться к симуляции ордера и чату",
                                color = SoftGray,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SoftGray
                        )
                    }
                }
            }
        }

        // Buy / Sell Mode Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2930).copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (userActionType == "BUY") SuccessGreen.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { userActionType = "BUY" }
                        .padding(vertical = 10.dp)
                        .testTag("p2p_tab_buy"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "КУПИТЬ КРИПТУ",
                        color = if (userActionType == "BUY") SuccessGreen else SoftGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (userActionType == "SELL") DangerRed.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { userActionType = "SELL" }
                        .padding(vertical = 10.dp)
                        .testTag("p2p_tab_sell"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ПРОДАТЬ КРИПТУ",
                        color = if (userActionType == "SELL") DangerRed else SoftGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Currency filters (Crypto and Fiat selectors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crypto selectors
                listOf("USDT", "BTC", "ETH").forEach { crypto ->
                    FilterChip(
                        selected = (selectedCrypto == crypto),
                        onClick = { selectedCrypto = crypto },
                        label = { Text(crypto) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGold.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGold,
                            containerColor = Color(0xFF2B2930).copy(alpha = 0.40f),
                            labelColor = SoftGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = (selectedCrypto == crypto),
                            borderColor = Color.White.copy(alpha = 0.05f),
                            selectedBorderColor = PrimaryGold.copy(alpha = 0.35f),
                            borderWidth = 1.dp
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fiat selectors
                listOf("RUB", "USD").forEach { fiat ->
                    FilterChip(
                        selected = (selectedFiat == fiat),
                        onClick = { selectedFiat = fiat },
                        label = { Text(fiat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurple.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPurple,
                            containerColor = Color(0xFF2B2930).copy(alpha = 0.40f),
                            labelColor = SoftGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = (selectedFiat == fiat),
                            borderColor = Color.White.copy(alpha = 0.05f),
                            selectedBorderColor = AccentPurple.copy(alpha = 0.35f),
                            borderWidth = 1.dp
                        )
                    )
                }
            }
        }

        // List of ads or empty placeholder
        if (filteredAds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SoftGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Объявлений по данным фильтрам не найдено.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Вы можете создать свое собственное объявление, нажав на кнопку '+' в правом верхнем углу!",
                            color = SoftGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredAds) { ad ->
                AdItemCard(
                    ad = ad,
                    userActionType = userActionType,
                    onActionClick = { selectedAdForTrade = ad }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Modal Dialog for Creating Custom Ad
    if (showCreateAdDialog) {
        CreateAdDialog(
            onDismiss = { showCreateAdDialog = false },
            onConfirm = { type, crypto, fiat, price, minLim, maxLim, methods ->
                viewModel.createAd(type, crypto, fiat, price, minLim, maxLim, methods) {
                    showCreateAdDialog = false
                }
            }
        )
    }

    // Modal Dialog for Placing P2P order (Start trade)
    if (selectedAdForTrade != null) {
        selectedAdForTrade?.let { ad ->
            PlaceOrderDialog(
                ad = ad,
                userActionType = userActionType,
                viewModel = viewModel,
                onDismiss = { selectedAdForTrade = null },
                onConfirm = { cryptoAmount, fiatAmount ->
                    viewModel.startP2PTrade(ad, cryptoAmount, fiatAmount) {
                        selectedAdForTrade = null
                    }
                }
            )
        }
    }
}

@Composable
fun AdItemCard(
    ad: P2PAd,
    userActionType: String,
    onActionClick: () -> Unit
) {
    val buttonColor = if (userActionType == "BUY") SuccessGreen else DangerRed
    val buttonText = if (userActionType == "BUY") "Купить" else "Продать"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("p2p_ad_card_${ad.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Merchant Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PrimaryGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ad.merchantName.take(1).uppercase(),
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ad.merchantName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "${String.format("%.2f", ad.merchantRating * 100)}% (${ad.merchantTrades} сделок)",
                    color = SoftGray,
                    fontSize = 12.sp
                )
            }

            Divider(color = Color.White.copy(alpha = 0.05f))

            // Row 2: Price & Limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val priceFormatted = String.format(Locale.US, "%,.2f", ad.price)
                    Text(
                        text = "$priceFormatted ${ad.fiatCurrency}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )

                    Text(
                        text = "Лимиты: ${String.format(Locale.US, "%,.0f", ad.minLimit)} - ${String.format(Locale.US, "%,.0f", ad.maxLimit)} ${ad.fiatCurrency}",
                        color = SoftGray,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "Оплата: ${ad.paymentMethods}",
                        color = PrimaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("ad_action_button_${ad.id}")
                ) {
                    Text(
                        text = "$buttonText ${ad.cryptoCurrency}",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Dialog to place active order on Ad
@Composable
fun PlaceOrderDialog(
    ad: P2PAd,
    userActionType: String,
    viewModel: WalletViewModel,
    onDismiss: () -> Unit,
    onConfirm: (cryptoAmount: Double, fiatAmount: Double) -> Unit
) {
    val assets by viewModel.assets.collectAsState()
    var inputAmountStr by remember { mutableStateOf("") }
    var isInputInFiat by remember { mutableStateOf(true) } // If true, user types fiat, we calc crypto; else vice-versa

    val currentPrice = ad.price

    // Calculated amounts
    val inputAmount = inputAmountStr.toDoubleOrNull() ?: 0.0
    val fiatAmount = if (isInputInFiat) inputAmount else inputAmount * currentPrice
    val cryptoAmount = if (isInputInFiat) inputAmount / currentPrice else inputAmount

    // Error checks
    val validationError = when {
        inputAmount <= 0 -> null
        fiatAmount < ad.minLimit -> "Минимальная сумма сделки: ${ad.minLimit} ${ad.fiatCurrency}"
        fiatAmount > ad.maxLimit -> "Максимальная сумма сделки: ${ad.maxLimit} ${ad.fiatCurrency}"
        userActionType == "SELL" -> {
            // User is selling crypto to merchant, check user crypto balance
            val userCryptoAsset = assets.find { it.id == ad.cryptoCurrency }
            if (userCryptoAsset == null || userCryptoAsset.balance < cryptoAmount) {
                "Недостаточно ${ad.cryptoCurrency} на балансе кошелька!"
            } else null
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (userActionType == "BUY") "Покупка ${ad.cryptoCurrency}" else "Продажа ${ad.cryptoCurrency}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Мерчант:", color = SoftGray, fontSize = 12.sp)
                    Text(ad.merchantName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Цена:", color = SoftGray, fontSize = 12.sp)
                    Text("${String.format(Locale.US, "%,.2f", ad.price)} ${ad.fiatCurrency}/${ad.cryptoCurrency}", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Способы оплаты:", color = SoftGray, fontSize = 12.sp)
                    Text(ad.paymentMethods, color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selector: Enter in fiat vs crypto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161722), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isInputInFiat) Color(0xFF282B3D) else Color.Transparent)
                            .clickable {
                                isInputInFiat = true
                                inputAmountStr = ""
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("В фиате (${ad.fiatCurrency})", color = Color.White, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isInputInFiat) Color(0xFF282B3D) else Color.Transparent)
                            .clickable {
                                isInputInFiat = false
                                inputAmountStr = ""
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("В крипте (${ad.cryptoCurrency})", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Input Box
                OutlinedTextField(
                    value = inputAmountStr,
                    onValueChange = { inputAmountStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("p2p_order_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = SoftGray
                    ),
                    placeholder = {
                        Text(
                            text = if (isInputInFiat) "Введите сумму в ${ad.fiatCurrency}" else "Введите сумму в ${ad.cryptoCurrency}",
                            color = SoftGray,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true
                )

                // Results preview
                if (inputAmount > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1D2A), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Вы заплатите:", color = SoftGray, fontSize = 12.sp)
                            Text(
                                text = String.format(Locale.US, "%,.2f %s", fiatAmount, ad.fiatCurrency),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Вы получите:", color = SoftGray, fontSize = 12.sp)
                            Text(
                                text = String.format(Locale.US, "%,.6f %s", cryptoAmount, ad.cryptoCurrency).trimEnd('0').trimEnd('.'),
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Error message
                if (validationError != null) {
                    Text(
                        text = validationError,
                        color = DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validationError == null && cryptoAmount > 0) {
                        onConfirm(cryptoAmount, fiatAmount)
                    }
                },
                enabled = (validationError == null && cryptoAmount > 0),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, disabledContainerColor = Color.Transparent),
                modifier = Modifier.testTag("confirm_place_order_button")
            ) {
                Text("Открыть сделку", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = SoftGray)
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(24.dp)
    )
}

// Dialog for Custom Ad Creation
@Composable
fun CreateAdDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, crypto: String, fiat: String, price: Double, minLim: Double, maxLim: Double, methods: String) -> Unit
) {
    var type by remember { mutableStateOf("SELL") } // User selling crypto (ad.type is SELL means user sells, merchants buy) -> actually let's match Room entities: "BUY" (merchant buys, user sells) or "SELL" (merchant sells, user buys).
    // Let's offer options:
    // "Я хочу КУПИТЬ" (User buys from merchant -> Ad is SELL)
    // "Я хочу ПРОДАТЬ" (User sells to merchant -> Ad is BUY)
    var isUserBuying by remember { mutableStateOf(true) }

    var selectedCrypto by remember { mutableStateOf("USDT") }
    var selectedFiat by remember { mutableStateOf("RUB") }

    var priceStr by remember { mutableStateOf("") }
    var minLimitStr by remember { mutableStateOf("") }
    var maxLimitStr by remember { mutableStateOf("") }
    var methodsStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать объявление", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector: Want to Buy or Sell
                item {
                    Text("Направление объявления:", color = SoftGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161722), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isUserBuying) SuccessGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { isUserBuying = true }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Я хочу КУПИТЬ", color = if (isUserBuying) SuccessGreen else SoftGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isUserBuying) DangerRed.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { isUserBuying = false }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Я хочу ПРОДАТЬ", color = if (!isUserBuying) DangerRed else SoftGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Currencies row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Криптовалюта:", color = SoftGray, fontSize = 11.sp)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Button(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161722)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(selectedCrypto, color = Color.White)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("USDT", "BTC", "ETH").forEach { c ->
                                        DropdownMenuItem(text = { Text(c) }, onClick = { selectedCrypto = c; expanded = false })
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Фиатная валюта:", color = SoftGray, fontSize = 11.sp)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Button(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161722)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(selectedFiat, color = Color.White)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("RUB", "USD").forEach { f ->
                                        DropdownMenuItem(text = { Text(f) }, onClick = { selectedFiat = f; expanded = false })
                                    }
                                }
                            }
                        }
                    }
                }

                // Price input
                item {
                    Text("Установите цену (курс):", color = SoftGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("create_ad_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = SoftGray
                        ),
                        placeholder = { Text("Курс в $selectedFiat (например, 93.5)", color = SoftGray, fontSize = 12.sp) },
                        singleLine = true
                    )
                }

                // Limits inputs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Мин. Лимит ($selectedFiat):", color = SoftGray, fontSize = 11.sp)
                            OutlinedTextField(
                                value = minLimitStr,
                                onValueChange = { minLimitStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("create_ad_min_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = SoftGray
                                ),
                                placeholder = { Text("500", color = SoftGray, fontSize = 11.sp) },
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Макс. Лимит ($selectedFiat):", color = SoftGray, fontSize = 11.sp)
                            OutlinedTextField(
                                value = maxLimitStr,
                                onValueChange = { maxLimitStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("create_ad_max_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = SoftGray
                                ),
                                placeholder = { Text("15000", color = SoftGray, fontSize = 11.sp) },
                                singleLine = true
                            )
                        }
                    }
                }

                // Payment methods
                item {
                    Text("Способы оплаты:", color = SoftGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = methodsStr,
                        onValueChange = { methodsStr = it },
                        modifier = Modifier.fillMaxWidth().testTag("create_ad_methods_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = SoftGray
                        ),
                        placeholder = { Text("СБП, Сбербанк, Тинькофф", color = SoftGray, fontSize = 12.sp) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val minLim = minLimitStr.toDoubleOrNull() ?: 0.0
                    val maxLim = maxLimitStr.toDoubleOrNull() ?: 0.0
                    val paymentMethods = if (methodsStr.isNotBlank()) methodsStr else "СБП"

                    if (price > 0 && minLim > 0 && maxLim >= minLim) {
                        // User buying crypto means merchant sells to user. So merchant ad is SELL.
                        // User selling crypto means merchant buys from user. So merchant ad is BUY.
                        val adType = if (isUserBuying) "SELL" else "BUY"
                        onConfirm(adType, selectedCrypto, selectedFiat, price, minLim, maxLim, paymentMethods)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                modifier = Modifier.testTag("create_ad_submit_button")
            ) {
                Text("Опубликовать", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = SoftGray)
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(24.dp)
    )
}

// Full screen overlay for an active Escrow P2P Trade and counterparty chat!
@Composable
fun ActiveP2PTradeView(
    tradeId: Int,
    viewModel: WalletViewModel,
    onBackClick: () -> Unit
) {
    val tradeState by viewModel.activeTrade.collectAsState()
    val messages by viewModel.activeTradeMessages.collectAsState()
    var chatInput by remember { mutableStateOf("") }

    val trade = tradeState ?: return

    val statusLabel = when (trade.status) {
        "CREATED" -> "Сделка открыта"
        "PAID" -> "Оплачена"
        "COMPLETED" -> "Успешно завершена"
        "CANCELLED" -> "Сделка отменена"
        else -> "Обработка"
    }

    val statusColor = when (trade.status) {
        "CREATED" -> PrimaryGold
        "PAID" -> AccentTeal
        "COMPLETED" -> SuccessGreen
        "CANCELLED" -> DangerRed
        else -> SoftGray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B2930).copy(alpha = 0.85f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "P2P Ордер #$tradeId",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Мерчант: ${trade.merchantName}",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Направление:", color = SoftGray, fontSize = 12.sp)
                            val typeText = if (trade.type == "BUY") "Покупка криптовалюты" else "Продажа криптовалюты"
                            val typeColor = if (trade.type == "BUY") SuccessGreen else DangerRed
                            Text(typeText, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Сумма криптовалюты:", color = SoftGray, fontSize = 12.sp)
                            val cryptoFormatted = String.format(Locale.US, "%,.6f", trade.cryptoAmount).trimEnd('0').trimEnd('.')
                            Text("$cryptoFormatted ${trade.cryptoCurrency}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Сумма к оплате:", color = SoftGray, fontSize = 12.sp)
                            val fiatFormatted = String.format(Locale.US, "%,.2f", trade.fiatAmount)
                            Text("$fiatFormatted ${trade.fiatCurrency}", color = PrimaryGold, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Курс сделки:", color = SoftGray, fontSize = 12.sp)
                            val priceFormatted = String.format(Locale.US, "%,.2f", trade.price)
                            Text("1 ${trade.cryptoCurrency} = $priceFormatted ${trade.fiatCurrency}", color = Color.White, fontSize = 12.sp)
                        }

                        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                        // Payment Details
                        Text(
                            text = if (trade.type == "BUY") "Реквизиты для отправки оплаты:" else "Ваши реквизиты для получения оплаты от мерчанта:",
                            color = SoftGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161722), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = trade.paymentDetails,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Action section depending on status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (trade.status) {
                            "CREATED" -> {
                                if (trade.type == "BUY") {
                                    Text(
                                        text = "Пожалуйста, переведите точную сумму ${trade.fiatAmount} ${trade.fiatCurrency} по указанным выше реквизитам мерчанта в вашем банке/кошельке. После перевода ОБЯЗАТЕЛЬНО нажмите кнопку 'Я оплатил'.",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.payTrade(trade.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.5f).height(44.dp).testTag("p2p_action_pay")
                                        ) {
                                            Text("Я оплатил", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.cancelTrade(trade.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(44.dp).testTag("p2p_action_cancel")
                                        ) {
                                            Text("Отменить", fontWeight = FontWeight.Bold, color = DangerRed)
                                        }
                                    }
                                } else {
                                    // User is selling crypto. Waiting for merchant to pay user
                                    Text(
                                        text = "Ожидайте совершения платежа от мерчанта на ваши реквизиты.\nКогда мерчант совершит перевод, статус обновится на 'Оплачена'.",
                                        color = SoftGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.size(24.dp))

                                    OutlinedButton(
                                        onClick = { viewModel.cancelTrade(trade.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text("Отменить сделку", color = DangerRed)
                                    }
                                }
                            }

                            "PAID" -> {
                                if (trade.type == "SELL") {
                                    // User is selling crypto, user must release escrow
                                    Text(
                                        text = "ВНИМАНИЕ: Мерчант сообщил об отправке средств. Пожалуйста, откройте ваш банк и ПРОВЕРЬТЕ поступление в размере ${trade.fiatAmount} ${trade.fiatCurrency}.\nТолько после подтверждения баланса в банке нажмите кнопку ниже, чтобы отпустить криптовалюту.",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Button(
                                        onClick = { viewModel.completeTrade(trade.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("p2p_action_release")
                                    ) {
                                        Text("Подтвердить получение (Отпустить крипту)", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // User bought, waiting for merchant to release crypto
                                    Text(
                                        text = "Ожидание подтверждения платежа от мерчанта. Мерчант проверяет зачисление фиата на банковский счет.\nКак только оплата подтвердится, криптовалюта будет зачислена на ваш кошелек.",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(24.dp))
                                }
                            }

                            "COMPLETED" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Сделка успешно завершена! Активы распределены.",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            "CANCELLED" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Сделка отменена.",
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chat title
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Чат поддержки и сделки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Scrolling Chat Messages
            if (messages.isEmpty()) {
                item {
                    Text("История сообщений пуста.", color = SoftGray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            } else {
                items(messages) { msg ->
                    ChatBubbleItem(msg = msg)
                }
            }
        }

        // Chat Input Box (Only show if trade is not finalized)
        if (trade.status == "CREATED" || trade.status == "PAID") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2930).copy(alpha = 0.85f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.weight(1f).testTag("p2p_chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = SoftGray
                    ),
                    placeholder = { Text("Напишите сообщение мерчанту...", color = SoftGray, fontSize = 12.sp) },
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        viewModel.sendMessage(trade.id, chatInput)
                        chatInput = ""
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryGold, CircleShape)
                        .testTag("p2p_chat_send_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Отправить", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: P2PMessage) {
    val isSystem = msg.sender == "SYSTEM"
    val isUser = msg.sender == "USER"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F29)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = msg.text,
                    color = SoftGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
        val cardColor = if (isUser) Color(0xFF2E3B2E) else Color(0xFF232533)
        val titleColor = if (isUser) SuccessGreen else PrimaryGold
        val titleText = if (isUser) "Вы" else "Мерчант"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = alignment
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 0.dp,
                    bottomEnd = if (isUser) 0.dp else 12.dp
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = titleText,
                        color = titleColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = msg.text,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
