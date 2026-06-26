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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.TransactionEntity
import com.example.data.database.WalletAsset
import com.example.ui.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Beautiful colors matching Frosted Glass premium design
val DarkBG = Color(0xFF1C1B1F)
val CardBG = Color(0xFF2B2930)
val PrimaryGold = Color(0xFFD0BCFF) // Accent/primary lavender color in Frosted Glass
val SuccessGreen = Color(0xFF48E18E) // Vivid glass green
val AccentTeal = Color(0xFF26A17B) // Tether green
val AccentPurple = Color(0xFFD0BCFF) // Purple accent
val DangerRed = Color(0xFFF2B8B5) // Pastel red
val SoftGray = Color(0xFF938F99) // Medium gray

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBG)
    ) {
        // Top-left glowing lavender blob
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFD0BCFF).copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(-w * 0.1f, -h * 0.1f),
                    radius = w * 0.8f
                ),
                radius = w * 0.8f,
                center = androidx.compose.ui.geometry.Offset(-w * 0.1f, -h * 0.1f)
            )
            // Bottom-right glowing red/pink blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB3261E).copy(alpha = 0.04f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * 1.1f, h * 1.1f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = androidx.compose.ui.geometry.Offset(w * 1.1f, h * 1.1f)
            )
        }
        content()
    }
}

@Composable
fun LivePriceTickerRow(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val btcRate = viewModel.getExchangeRate("BTC", "RUB")
    val ethRate = viewModel.getExchangeRate("ETH", "RUB")
    val usdtRate = viewModel.getExchangeRate("USDT", "RUB")
    val usdRate = viewModel.getExchangeRate("USD", "RUB")

    val tickers = remember(btcRate, ethRate, usdtRate, usdRate) {
        listOf(
            TickerData("BTC", btcRate, "+3.45%", true),
            TickerData("ETH", ethRate, "-1.20%", false),
            TickerData("USDT", usdtRate, "+0.05%", true),
            TickerData("USD", usdRate, "-0.15%", false)
        )
    }

    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(tickers) { ticker ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (ticker.isUp) SuccessGreen else DangerRed,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = ticker.symbol,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%,.1f ₽", ticker.price),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = ticker.change,
                        color = if (ticker.isUp) SuccessGreen else DangerRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class TickerData(
    val symbol: String,
    val price: Double,
    val change: String,
    val isUp: Boolean
)

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onNavigateToExchange: () -> Unit,
    onNavigateToP2P: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val livePrices by viewModel.livePrices.collectAsState()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedTxFilter by remember { mutableStateOf("Все") }

    // Calculate total balance in RUB dynamically based on exchange rates
    val totalBalanceRUB = remember(assets, livePrices) {
        assets.fold(0.0) { sum, asset ->
            val rateToRUB = if (asset.id == "RUB") 1.0 else {
                viewModel.getExchangeRate(asset.id, "RUB")
            }
            sum + (asset.balance * rateToRUB)
        }
    }

    // Total balance in USD and EUR
    val totalBalanceUSD = totalBalanceRUB * viewModel.getExchangeRate("RUB", "USD")

    val filteredTransactions = remember(transactions, selectedTxFilter) {
        when (selectedTxFilter) {
            "Пополнения" -> transactions.filter { it.type == "DEPOSIT" }
            "Выводы" -> transactions.filter { it.type == "WITHDRAW" }
            "Обмены" -> transactions.filter { it.type == "EXCHANGE" }
            "P2P" -> transactions.filter { it.type in listOf("P2P_BUY", "P2P_SELL") }
            else -> transactions
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spacer for safe edge-to-edge layout top inset
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Live Price Tickers (Crypto stats ticker row)
        item {
            LivePriceTickerRow(viewModel = viewModel)
        }

        // Hero Card - Total Balance
        item {
            TotalBalanceCard(
                balanceRUB = totalBalanceRUB,
                assets = assets,
                viewModel = viewModel,
                onDepositClick = { showDepositDialog = true },
                onWithdrawClick = { showWithdrawDialog = true }
            )
        }

        // Quick action buttons row
        item {
            QuickActionsRow(
                onExchangeClick = onNavigateToExchange,
                onP2PClick = onNavigateToP2P
            )
        }

        // Wallet Assets title
        item {
            Text(
                text = "Мои Активы",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // List of custom styled Wallet Assets
        if (assets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
            }
        } else {
            items(assets) { asset ->
                AssetCardItem(asset = asset, viewModel = viewModel)
            }
        }

        // Transaction History title & Filters
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "История операций",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )

                // Transaction Filters
                val filters = listOf("Все", "Пополнения", "Выводы", "Обмены", "P2P")
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filters) { filterName ->
                        val isSelected = selectedTxFilter == filterName
                        val containerColor = if (isSelected) PrimaryGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
                        val borderColor = if (isSelected) PrimaryGold.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.05f)
                        val textColor = if (isSelected) PrimaryGold else SoftGray
                        Box(
                            modifier = Modifier
                                .background(containerColor, RoundedCornerShape(100.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(100.dp))
                                .clickable { selectedTxFilter = filterName }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filterName,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // List of transaction entities
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBG)
                ) {
                    Text(
                        text = if (selectedTxFilter == "Все") {
                            "У вас пока нет транзакций.\nПополните кошелек или обменяйте валюту!"
                        } else {
                            "Нет операций в категории \"$selectedTxFilter\""
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SoftGray,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredTransactions) { tx ->
                TransactionCardItem(tx = tx)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Modal dialog for Depositing Funds
    if (showDepositDialog) {
        DepositDialog(
            availableCurrencies = assets.map { it.id },
            onDismiss = { showDepositDialog = false },
            onConfirm = { currency, amount, method ->
                viewModel.deposit(currency, amount, method)
                showDepositDialog = false
            }
        )
    }

    // Modal dialog for Withdrawing Funds
    if (showWithdrawDialog) {
        WithdrawDialog(
            assets = assets,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { currency, amount, details ->
                viewModel.withdraw(currency, amount, details) {
                    showWithdrawDialog = false
                }
            }
        )
    }
}

@Composable
fun TotalBalanceCard(
    balanceRUB: Double,
    assets: List<WalletAsset>,
    viewModel: WalletViewModel,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    val livePrices by viewModel.livePrices.collectAsState()
    val totalValueRUB = remember(assets, livePrices) {
        assets.fold(0.0) { sum, asset ->
            val rateToRUB = if (asset.id == "RUB") 1.0 else {
                viewModel.getExchangeRate(asset.id, "RUB")
            }
            sum + (asset.balance * rateToRUB)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("total_balance_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing dynamic Bezier Line Background inside Card
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path()

                path.moveTo(0f, height * 0.8f)
                path.cubicTo(
                    width * 0.25f, height * 0.75f,
                    width * 0.45f, height * 0.45f,
                    width * 0.65f, height * 0.55f
                )
                path.cubicTo(
                    width * 0.85f, height * 0.65f,
                    width * 0.95f, height * 0.25f,
                    width, height * 0.15f
                )

                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD0BCFF).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF48E18E),
                            Color(0xFFD0BCFF)
                        )
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Баланс кошелька",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SoftGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // PRO Badge
                    Box(
                        modifier = Modifier
                            .background(PrimaryGold.copy(alpha = 0.20f), RoundedCornerShape(100.dp))
                            .border(1.dp, PrimaryGold.copy(alpha = 0.30f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PrimaryGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PRO",
                                color = PrimaryGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format(Locale.US, "%,.2f ₽", balanceRUB),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 32.sp
                    ),
                    modifier = Modifier.testTag("total_balance_text")
                )

                val balanceUSD = balanceRUB * viewModel.getExchangeRate("RUB", "USD")
                val balanceEUR = balanceRUB * viewModel.getExchangeRate("RUB", "EUR")

                Text(
                    text = String.format(Locale.US, "≈ %,.2f $  •  ≈ %,.2f €", balanceUSD, balanceEUR),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful Segmented Asset Allocation Bar
                if (totalValueRUB > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            assets.forEach { asset ->
                                val rate = if (asset.id == "RUB") 1.0 else viewModel.getExchangeRate(asset.id, "RUB")
                                val rubVal = asset.balance * rate
                                val weight = if (totalValueRUB > 0) (rubVal / totalValueRUB).toFloat() else 0f
                                if (weight > 0.01f) {
                                    val color = when (asset.id) {
                                        "RUB" -> SuccessGreen
                                        "USD" -> AccentTeal
                                        "EUR" -> Color(0xFF2196F3) // Dynamic blue for EUR
                                        "USDT" -> Color(0xFF00BFA5)
                                        "BTC" -> Color(0xFFFFC107)
                                        "ETH" -> AccentPurple
                                        else -> SoftGray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(weight)
                                            .background(color)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            assets.take(5).forEach { asset ->
                                val rate = if (asset.id == "RUB") 1.0 else viewModel.getExchangeRate(asset.id, "RUB")
                                val rubVal = asset.balance * rate
                                val percentage = if (totalValueRUB > 0) (rubVal / totalValueRUB * 100) else 0.0
                                if (percentage > 2.0) {
                                    val color = when (asset.id) {
                                        "RUB" -> SuccessGreen
                                        "USD" -> AccentTeal
                                        "EUR" -> Color(0xFF2196F3) // Dynamic blue for EUR
                                        "USDT" -> Color(0xFF00BFA5)
                                        "BTC" -> Color(0xFFFFC107)
                                        "ETH" -> AccentPurple
                                        else -> SoftGray
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(color, CircleShape)
                                        )
                                        Text(
                                            text = "${asset.id} ${String.format(Locale.US, "%.0f%%", percentage)}",
                                            color = SoftGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDepositClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("deposit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color(0xFF381E72)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Пополнить",
                            tint = Color(0xFF381E72)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Пополнить", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onWithdrawClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("withdraw_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Вывести",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Вывести", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onExchangeClick: () -> Unit,
    onP2PClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onExchangeClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentPurple.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Быстрый Обмен",
                        tint = AccentPurple,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Обменник", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Моментальный обмен", color = SoftGray, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onP2PClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "P2P Торговля",
                        tint = AccentTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("P2P Сделки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Покупка без комиссий", color = SoftGray, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun AssetCardItem(asset: WalletAsset, viewModel: WalletViewModel) {
    // Elegant custom icon backgrounds
    val iconColor = when (asset.id) {
        "RUB" -> SuccessGreen
        "USD" -> AccentTeal
        "EUR" -> Color(0xFF2196F3) // Dynamic blue for EUR
        "USDT" -> AccentTeal
        "BTC" -> PrimaryGold
        "ETH" -> AccentPurple
        else -> SoftGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_card_${asset.id.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.40f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle with custom dynamic logo drawing
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = asset.symbol,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = asset.id,
                    color = SoftGray,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedBalance = if (asset.type == "CRYPTO") {
                    String.format(Locale.US, "%,.6f", asset.balance).trimEnd('0').trimEnd('.')
                } else {
                    String.format(Locale.US, "%,.2f", asset.balance)
                }
                Text(
                    text = "$formattedBalance ${asset.symbol}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End
                )

                // Show value in RUB as secondary text
                if (asset.id != "RUB") {
                    val rubRate = viewModel.getExchangeRate(asset.id, "RUB")
                    val valueInRUB = asset.balance * rubRate
                    Text(
                        text = String.format(Locale.US, "≈ %,.2f ₽", valueInRUB),
                        color = SoftGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCardItem(tx: TransactionEntity) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val timeString = dateFormat.format(Date(tx.timestamp))

    val icon = when (tx.type) {
        "DEPOSIT" -> Icons.Default.ArrowDownward
        "WITHDRAW" -> Icons.Default.ArrowUpward
        "EXCHANGE" -> Icons.Default.SwapHoriz
        else -> Icons.Default.People
    }

    val iconColor = when (tx.type) {
        "DEPOSIT" -> SuccessGreen
        "WITHDRAW" -> DangerRed
        "EXCHANGE" -> AccentPurple
        else -> AccentTeal
    }

    val typeLabel = when (tx.type) {
        "DEPOSIT" -> "Пополнение"
        "WITHDRAW" -> "Вывод средств"
        "EXCHANGE" -> "Обмен монет"
        "P2P_BUY" -> "Покупка P2P"
        "P2P_SELL" -> "Продажа P2P"
        else -> "Сделка"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.25f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = tx.details,
                    color = SoftGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    text = timeString,
                    color = SoftGray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = if (tx.type == "DEPOSIT" || tx.type == "P2P_BUY") "+" else if (tx.type == "WITHDRAW") "-" else ""
                val color = if (tx.type == "DEPOSIT" || tx.type == "P2P_BUY") SuccessGreen else if (tx.type == "WITHDRAW") DangerRed else Color.White

                val amountFormatted = String.format(Locale.US, "%,.4f", tx.amount).trimEnd('0').trimEnd('.')
                Text(
                    text = "$sign$amountFormatted ${tx.currency}",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (tx.targetAmount != null && tx.targetCurrency != null) {
                    val targetFormatted = String.format(Locale.US, "%,.4f", tx.targetAmount).trimEnd('0').trimEnd('.')
                    Text(
                        text = "Получено: $targetFormatted ${tx.targetCurrency}",
                        color = SuccessGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoQRCodeCanvas(modifier: Modifier = Modifier, color: Color = Color.White) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.width
        val cellSize = sizePx / 15f

        // Finder patterns (Corner Squares)
        fun drawFinderPattern(x: Float, y: Float) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(cellSize * 5, cellSize * 5),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = cellSize)
            )
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x + cellSize * 1.5f, y + cellSize * 1.5f),
                size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2)
            )
        }

        drawFinderPattern(0f, 0f) // Top Left
        drawFinderPattern(sizePx - cellSize * 5, 0f) // Top Right
        drawFinderPattern(0f, sizePx - cellSize * 5) // Bottom Left

        val random = java.util.Random(42)
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if ((row < 6 && col < 6) || (row < 6 && col > 8) || (row > 8 && col < 6)) {
                    continue
                }
                if (random.nextBoolean()) {
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize * 0.9f, cellSize * 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun DepositDialog(
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (currency: String, amount: Double, method: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) } // 0: Input, 1: Payment Info/QR, 2: Simulating Progress, 3: Success
    var selectedCurrency by remember { mutableStateOf(availableCurrencies.firstOrNull() ?: "RUB") }
    var amountStr by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val depositMethods = when (selectedCurrency) {
        "RUB" -> listOf("СБП (Система Быстрых Платежей)", "Банковская карта (РФ)", "QIWI Кошелек")
        "USD", "EUR" -> listOf("Международная карта Visa/MC", "Банковский перевод (Swift)")
        else -> listOf("Кошелек криптовалюты ($selectedCurrency Network)")
    }
    var selectedMethod by remember { mutableStateOf(depositMethods.first()) }

    // Reset method when currency changes
    LaunchedEffect(selectedCurrency) {
        selectedMethod = depositMethods.first()
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showCopiedNotification by remember { mutableStateOf(false) }

    // Generated address or instructions reference
    val isCrypto = selectedCurrency in listOf("BTC", "ETH", "USDT")
    val depositAddress = remember(selectedCurrency) {
        when (selectedCurrency) {
            "BTC" -> "bc1q8c8mms49kh7ux28uwyvux5k56976px7m3m8pqg"
            "ETH" -> "0x71C7656EC7ab88b098defB751B7401B5f6d1476B"
            "USDT" -> "TXv8Uf8NAsvA4uY6e4VwYQ52zQ9yBqW8mN"
            else -> "N/A"
        }
    }

    val transactionCode = remember { "TX-${(100000 + java.util.Random().nextInt(900000))}" }

    // Simulating block confirmations or payment validations
    var verificationText by remember { mutableStateOf("Отправка запроса в платежный шлюз...") }
    var verificationProgress by remember { mutableStateOf(0.1f) }

    if (currentStep == 2) {
        LaunchedEffect(Unit) {
            verificationProgress = 0.15f
            verificationText = "Ожидание транзакции в сети..."
            kotlinx.coroutines.delay(1200)

            verificationProgress = 0.45f
            verificationText = if (isCrypto) {
                "Транзакция обнаружена. Получение подтверждений блокчейна (1/3)..."
            } else {
                "Обработка перевода банком. Проверка по СБП..."
            }
            kotlinx.coroutines.delay(1500)

            verificationProgress = 0.80f
            verificationText = if (isCrypto) {
                "Синхронизация смарт-контракта. Подтверждения (2/3)..."
            } else {
                "Сверка подписей СБП. Зачисление на баланс..."
            }
            kotlinx.coroutines.delay(1200)

            verificationProgress = 1.0f
            verificationText = "Активы успешно зачислены!"
            kotlinx.coroutines.delay(800)

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            onConfirm(selectedCurrency, amount, selectedMethod)
            currentStep = 3
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (currentStep == 0 || currentStep == 3) onDismiss()
        },
        title = {
            val titleText = when (currentStep) {
                0 -> "Депозит"
                1 -> "Оплата"
                2 -> "Верификация"
                else -> "Успешно!"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (currentStep) {
                        0 -> Icons.Default.Add
                        1 -> Icons.Default.QrCodeScanner
                        2 -> Icons.Default.Sync
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = if (currentStep == 3) SuccessGreen else PrimaryGold
                )
                Text(titleText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentStep) {
                    0 -> {
                        // Choose currency
                        Text("Валюта пополнения:", color = SoftGray, fontSize = 12.sp)
                        Box {
                            Button(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deposit_currency_dropdown"),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBG),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedCurrency, color = Color.White)
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.background(CardBG)
                            ) {
                                availableCurrencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr, color = Color.White) },
                                        onClick = {
                                            selectedCurrency = curr
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Amount input
                        Text("Сумма пополнения:", color = SoftGray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deposit_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = SoftGray
                            ),
                            placeholder = { Text("0.00", color = SoftGray) },
                            singleLine = true
                        )

                        // Method selection
                        Text("Способ пополнения:", color = SoftGray, fontSize = 12.sp)
                        depositMethods.forEach { method ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedMethod == method) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { selectedMethod = method }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedMethod == method),
                                    onClick = { selectedMethod = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(method, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    1 -> {
                        // Payment Details
                        if (isCrypto) {
                            Text(
                                "Отправьте ТОЛЬКО $selectedCurrency на указанный ниже адрес. Перевод других активов может привести к их утере.",
                                color = DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Render dynamic custom canvas QR code
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CryptoQRCodeCanvas(
                                    modifier = Modifier.size(120.dp),
                                    color = Color.White
                                )
                            }

                            // Address copy container
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Адрес пополнения ($selectedCurrency):", color = SoftGray, fontSize = 10.sp)
                                        Text(
                                            depositAddress,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(depositAddress) }
                                            clipboardManager.setText(annotatedString)
                                            showCopiedNotification = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy address",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Fiat Details
                            Text("Инструкция по переводу средств:", color = SoftGray, fontSize = 12.sp)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val currencySymbol = when (selectedCurrency) {
                                        "RUB" -> "₽"
                                        "EUR" -> "€"
                                        else -> "$"
                                    }
                                    Text(
                                        text = "Сумма к отправке: ${amountStr} $currencySymbol",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    if (selectedCurrency == "RUB") {
                                        if (selectedMethod.contains("СБП")) {
                                            Text("• Получатель: Вячеслав Т.\n• Телефон: +7 (925) 555-43-21\n• Банк: Т-Банк", color = Color.White, fontSize = 12.sp)
                                        } else if (selectedMethod.contains("карта")) {
                                            Text("• Получатель: Slava T.\n• Карта: 2202 2011 3344 5566", color = Color.White, fontSize = 12.sp)
                                        } else {
                                            Text("• Qiwi кошелек: +7 (925) 555-43-21", color = Color.White, fontSize = 12.sp)
                                        }
                                    } else {
                                        if (selectedMethod.contains("Swift")) {
                                            Text("• Банк: Deutsche Bank AG\n• Получатель: CryptoEX Ltd\n• IBAN: DE89 5001 0517 3802 9182 00\n• SWIFT/BIC: DEUTDEDDXXX", color = Color.White, fontSize = 12.sp)
                                        } else {
                                            Text("• Нажмите Перейти к оплате для проведения карточного списания по безопасному эквайрингу 3D-Secure.", color = Color.White, fontSize = 12.sp)
                                        }
                                    }

                                    // Ref code copy
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Код примечания: $transactionCode", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = {
                                                val annotated = androidx.compose.ui.text.buildAnnotatedString { append(transactionCode) }
                                                clipboardManager.setText(annotated)
                                                showCopiedNotification = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (showCopiedNotification) {
                            Text(
                                "Скопировано в буфер обмена!",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                    2 -> {
                        // Verification in progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { verificationProgress },
                                    color = PrimaryGold,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = verificationText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {
                        // Success confirmation screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "Баланс успешно пополнен!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "+$amountStr $selectedCurrency",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentStep) {
                0 -> {
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                currentStep = 1
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                        modifier = Modifier.testTag("confirm_deposit_button")
                    ) {
                        Text("Далее", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> {
                    Button(
                        onClick = { currentStep = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Я оплатил", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    // Hidden or disabled in progress
                }
                else -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("Закрыть", color = Color.Black)
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep < 2) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SoftGray)
                }
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun WithdrawDialog(
    assets: List<WalletAsset>,
    onDismiss: () -> Unit,
    onConfirm: (currency: String, amount: Double, details: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) } // 0: Input, 1: Processing, 2: Success
    var selectedAsset by remember { mutableStateOf(assets.firstOrNull() ?: WalletAsset("RUB", "", 0.0, "", "")) }
    var amountStr by remember { mutableStateOf("") }
    var detailsStr by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val detailsPlaceholder = when (selectedAsset.id) {
        "RUB" -> "Номер телефона для СБП или номер банковской карты РФ"
        "USD" -> "Реквизиты SWIFT перевода или номер карты Visa/MC"
        else -> "Адрес получателя в сети ${selectedAsset.id} (например, TRC-20)"
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showCopiedNotification by remember { mutableStateOf(false) }

    // Generate a realistic TxID/receipt ID upon success
    val generatedTxID = remember(selectedAsset.id) {
        if (selectedAsset.type == "CRYPTO") {
            val hex = "0123456789abcdef"
            val sb = StringBuilder("0x")
            val random = java.util.Random()
            for (i in 0 until 40) {
                sb.append(hex[random.nextInt(16)])
            }
            sb.toString()
        } else {
            "BANK-REF-" + (10000000 + java.util.Random().nextInt(90000000))
        }
    }

    var processingProgress by remember { mutableStateOf(0.1f) }
    var processingText by remember { mutableStateOf("Генерация транзакции...") }

    if (currentStep == 1) {
        LaunchedEffect(Unit) {
            processingProgress = 0.2f
            processingText = "Подписание транзакции приватными ключами в холодном кошельке..."
            kotlinx.coroutines.delay(1200)

            processingProgress = 0.6f
            processingText = if (selectedAsset.type == "CRYPTO") {
                "Трансляция транзакции в распределенную сеть ${selectedAsset.id}..."
            } else {
                "Отправка запроса авторизации платежа в клиринговый шлюз..."
            }
            kotlinx.coroutines.delay(1400)

            processingProgress = 0.9f
            processingText = "Ожидание включения в следующий блок..."
            kotlinx.coroutines.delay(1000)

            processingProgress = 1.0f
            processingText = "Запрос успешно исполнен!"
            kotlinx.coroutines.delay(600)

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            onConfirm(selectedAsset.id, amount, detailsStr)
            currentStep = 2
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (currentStep == 0 || currentStep == 2) onDismiss()
        },
        title = {
            val titleText = when (currentStep) {
                0 -> "Вывод средств"
                1 -> "Трансляция в сеть"
                else -> "Транзакция отправлена"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (currentStep) {
                        0 -> Icons.Default.ArrowUpward
                        1 -> Icons.Default.Sync
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = if (currentStep == 2) SuccessGreen else PrimaryGold
                )
                Text(titleText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentStep) {
                    0 -> {
                        // Currency dropdown
                        Text("Выберите актив для вывода:", color = SoftGray, fontSize = 12.sp)
                        Box {
                            Button(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_currency_dropdown"),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBG),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${selectedAsset.id} (Доступно: ${String.format("%.4f", selectedAsset.balance).trimEnd('0').trimEnd('.')} ${selectedAsset.symbol})",
                                        color = Color.White
                                    )
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.background(CardBG)
                            ) {
                                assets.forEach { asset ->
                                    DropdownMenuItem(
                                        text = { Text("${asset.id} (${String.format("%.4f", asset.balance).trimEnd('0').trimEnd('.')} ${asset.symbol})", color = Color.White) },
                                        onClick = {
                                            selectedAsset = asset
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Amount input
                        Text("Сумма вывода:", color = SoftGray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = SoftGray
                            ),
                            placeholder = { Text("0.00", color = SoftGray) },
                            singleLine = true
                        )

                        // Details input
                        Text("Реквизиты получателя:", color = SoftGray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = detailsStr,
                            onValueChange = { detailsStr = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_details_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = SoftGray
                            ),
                            placeholder = { Text(detailsPlaceholder, color = SoftGray, fontSize = 11.sp) },
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                    1 -> {
                        // Processing progress screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { processingProgress },
                                    color = PrimaryGold,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = processingText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {
                        // Success details receipt
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Text(
                                text = "Средства успешно отправлены!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Детали выплаты:", color = SoftGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Сумма: $amountStr ${selectedAsset.symbol}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Получатель: $detailsStr", color = Color.White, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (selectedAsset.type == "CRYPTO") "Хэш транзакции (TxID):" else "Референс платежа:",
                                        color = SoftGray,
                                        fontSize = 10.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            generatedTxID,
                                            color = PrimaryGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                val annotated = androidx.compose.ui.text.buildAnnotatedString { append(generatedTxID) }
                                                clipboardManager.setText(annotated)
                                                showCopiedNotification = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            if (showCopiedNotification) {
                                Text(
                                    "Хэш скопирован в буфер обмена!",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (currentStep) {
                0 -> {
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && amount <= selectedAsset.balance && detailsStr.isNotBlank()) {
                                currentStep = 1
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                        modifier = Modifier.testTag("confirm_withdraw_button")
                    ) {
                        Text("Далее", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> {
                    // Disabled while processing
                }
                else -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("Закрыть", color = Color.Black)
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep == 0) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SoftGray)
                }
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(24.dp)
    )
}
