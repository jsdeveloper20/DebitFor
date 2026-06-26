package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.WalletRepository
import com.example.data.database.AppDatabase
import com.example.data.database.P2PAd
import com.example.data.database.P2PTrade
import com.example.data.database.TransactionEntity
import com.example.data.database.WalletAsset
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "crypto_exchange_db"
    )
        .fallbackToDestructiveMigration()
        .build()

    val repository = WalletRepository(database.walletDao())

    // Exposed Flows from database
    val assets: StateFlow<List<WalletAsset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val p2pAds: StateFlow<List<P2PAd>> = repository.allP2PAds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val p2pTrades: StateFlow<List<P2PTrade>> = repository.allP2PTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active trade handling
    private val _selectedTradeId = MutableStateFlow<Int?>(null)
    val selectedTradeId: StateFlow<Int?> = _selectedTradeId.asStateFlow()

    val activeTrade: StateFlow<P2PTrade?> = _selectedTradeId
        .flatMapLatest { id ->
            if (id != null) repository.getP2PTradeFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTradeMessages = _selectedTradeId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForTrade(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Temporary alert/notification message
    private val _uiNotification = MutableStateFlow<String?>(null)
    val uiNotification: StateFlow<String?> = _uiNotification.asStateFlow()

    private val _livePricesInUSD = MutableStateFlow<Map<String, Double>>(mapOf(
        "USD" to 1.0,
        "USDT" to 1.0,
        "EUR" to 1.08,  // 1 EUR = 1.08 USD (approx)
        "RUB" to 0.0108, // 1 RUB = 0.0108 USD (approx 92.5 RUB per USD)
        "BTC" to 68200.0,
        "ETH" to 3480.0
    ))
    val livePrices: StateFlow<Map<String, Double>> = _livePricesInUSD.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
        startLivePriceUpdates()
    }

    private fun startLivePriceUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val newPrices = fetchLivePrices()
                    if (newPrices != null) {
                        _livePricesInUSD.value = newPrices
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(30000) // update every 30 seconds
            }
        }
    }

    private fun fetchLivePrices(): Map<String, Double>? {
        return try {
            // Fetch live rates from Binance public tickers
            val usdtRub = fetchPriceFromBinance("USDTRUB") ?: 92.5
            val btcUsdt = fetchPriceFromBinance("BTCUSDT") ?: 68200.0
            val ethUsdt = fetchPriceFromBinance("ETHUSDT") ?: 3480.0
            val eurUsdt = fetchPriceFromBinance("EURUSDT") ?: 1.08

            val rubInUsd = 1.0 / usdtRub

            mapOf(
                "USD" to 1.0,
                "USDT" to 1.0,
                "EUR" to eurUsdt,
                "RUB" to rubInUsd,
                "BTC" to btcUsdt,
                "ETH" to ethUsdt
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchPriceFromBinance(symbol: String): Double? {
        return try {
            val url = java.net.URL("https://api.binance.com/api/v3/ticker/price?symbol=$symbol")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val matcher = java.util.regex.Pattern.compile("\"price\":\"([^\"]+)\"").matcher(response)
                if (matcher.find()) {
                    matcher.group(1)?.toDoubleOrNull()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun showNotification(message: String) {
        _uiNotification.value = message
    }

    fun clearNotification() {
        _uiNotification.value = null
    }

    fun selectTrade(tradeId: Int?) {
        _selectedTradeId.value = tradeId
    }

    // 1. Deposit Action
    fun deposit(currency: String, amount: Double, method: String) {
        viewModelScope.launch {
            val success = repository.deposit(currency, amount, method)
            if (success) {
                showNotification("Баланс пополнен на $amount $currency через $method!")
            } else {
                showNotification("Ошибка при пополнении.")
            }
        }
    }

    // 2. Withdraw Action
    fun withdraw(currency: String, amount: Double, details: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.withdraw(currency, amount, details)
            if (success) {
                showNotification("Вывод $amount $currency успешно оформлен!")
                onSuccess()
            } else {
                showNotification("Ошибка: недостаточно средств для вывода!")
            }
        }
    }

    // 3. Exchange Rates Calculator & Exchange Action
    fun getExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0

        val pricesInUSD = _livePricesInUSD.value
        val fromInUSD = pricesInUSD[from] ?: 1.0
        val toInUSD = pricesInUSD[to] ?: 1.0

        // Rate = Price of FROM in USD / Price of TO in USD
        return fromInUSD / toInUSD
    }

    fun executeExchange(fromCurrency: String, toCurrency: String, amount: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val rate = getExchangeRate(fromCurrency, toCurrency)
            val success = repository.exchange(fromCurrency, toCurrency, amount, rate)
            if (success) {
                val targetAmount = String.format("%.6f", amount * rate).trimEnd('0').trimEnd('.')
                showNotification("Обменено $amount $fromCurrency на $targetAmount $toCurrency!")
                onSuccess()
            } else {
                showNotification("Ошибка: недостаточно средств $fromCurrency для обмена!")
            }
        }
    }

    // 4. Create custom P2P Ad
    fun createAd(
        type: String, // "BUY" or "SELL"
        crypto: String,
        fiat: String,
        price: Double,
        minLim: Double,
        maxLim: Double,
        methods: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.createP2PAd(type, crypto, fiat, price, minLim, maxLim, methods)
            if (success) {
                showNotification("Ваше P2P объявление успешно опубликовано!")
                onSuccess()
            } else {
                showNotification("Ошибка публикации объявления.")
            }
        }
    }

    // 5. Start P2P Trade from Ad
    fun startP2PTrade(ad: P2PAd, amountCrypto: Double, amountFiat: Double, onTradeStarted: (Int) -> Unit) {
        viewModelScope.launch {
            val tradeId = repository.startP2PTrade(ad, amountCrypto, amountFiat)
            if (tradeId != -1) {
                _selectedTradeId.value = tradeId
                onTradeStarted(tradeId)
                showNotification("P2P сделка #${tradeId} открыта!")
            } else {
                showNotification("Ошибка: Недостаточно баланса для обеспечения сделки!")
            }
        }
    }

    // 6. Pay P2P Trade
    fun payTrade(tradeId: Int) {
        viewModelScope.launch {
            repository.payP2PTrade(tradeId)
            showNotification("Сделка отмечена как оплаченная. Ожидайте подтверждения.")
        }
    }

    // 7. Complete trade (Releasing Escrow)
    fun completeTrade(tradeId: Int) {
        viewModelScope.launch {
            val success = repository.completeP2PTrade(tradeId)
            if (success) {
                showNotification("Криптовалюта отпущена покупателю. Сделка завершена!")
            } else {
                showNotification("Не удалось завершить сделку.")
            }
        }
    }

    // 8. Cancel P2P Trade
    fun cancelTrade(tradeId: Int) {
        viewModelScope.launch {
            val success = repository.cancelP2PTrade(tradeId)
            if (success) {
                showNotification("Сделка отменена.")
            } else {
                showNotification("Не удалось отменить сделку.")
            }
        }
    }

    // 9. Send Chat Message
    fun sendMessage(tradeId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(tradeId, "USER", text)
        }
    }
}

class WalletViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
