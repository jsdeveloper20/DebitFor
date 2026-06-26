package com.example.data

import com.example.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletRepository(private val dao: WalletDao) {

    val allAssets: Flow<List<WalletAsset>> = dao.getAllAssetsFlow()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
    val allP2PAds: Flow<List<P2PAd>> = dao.getAllP2PAdsFlow()
    val allP2PTrades: Flow<List<P2PTrade>> = dao.getAllP2PTradesFlow()

    fun getP2PTradeFlow(tradeId: Int): Flow<P2PTrade?> = dao.getP2PTradeFlow(tradeId)
    fun getMessagesForTrade(tradeId: Int): Flow<List<P2PMessage>> = dao.getMessagesForTradeFlow(tradeId)

    // Seed initial data if database is empty
    suspend fun seedInitialDataIfNeeded() {
        withContext(Dispatchers.IO) {
            val assets = allAssets.first()
            if (assets.isEmpty()) {
                // Seed Wallet Assets
                val initialAssets = listOf(
                    WalletAsset("RUB", "Российский Рубль", 25000.0, "FIAT", "₽"),
                    WalletAsset("USD", "Доллар США", 500.0, "FIAT", "$"),
                    WalletAsset("EUR", "Евро", 450.0, "FIAT", "€"),
                    WalletAsset("USDT", "Tether ERC-20", 120.0, "CRYPTO", "₮"),
                    WalletAsset("BTC", "Bitcoin", 0.0012, "CRYPTO", "₿"),
                    WalletAsset("ETH", "Ethereum", 0.045, "CRYPTO", "Ξ")
                )
                dao.insertAssets(initialAssets)

                // Seed some initial Transactions
                val initialTx = listOf(
                    TransactionEntity(
                        type = "DEPOSIT",
                        amount = 25000.0,
                        currency = "RUB",
                        status = "SUCCESS",
                        details = "Пополнение через СБП",
                        timestamp = System.currentTimeMillis() - 86400000 * 3
                    ),
                    TransactionEntity(
                        type = "DEPOSIT",
                        amount = 500.0,
                        currency = "USD",
                        status = "SUCCESS",
                        details = "Пополнение картой Visa",
                        timestamp = System.currentTimeMillis() - 86400000 * 2
                    ),
                    TransactionEntity(
                        type = "EXCHANGE",
                        amount = 50.0,
                        currency = "USD",
                        targetAmount = 50.0,
                        targetCurrency = "USDT",
                        status = "SUCCESS",
                        details = "Обмен по курсу 1.00 USD/USDT",
                        timestamp = System.currentTimeMillis() - 3600000 * 5
                    )
                )
                for (tx in initialTx) {
                    dao.insertTransaction(tx)
                }

                // Seed some P2P advertisements
                val initialAds = listOf(
                    P2PAd(
                        merchantName = "CryptoBogatyr",
                        merchantRating = 4.95,
                        merchantTrades = 1240,
                        type = "SELL", // User buys from merchant
                        cryptoCurrency = "USDT",
                        fiatCurrency = "RUB",
                        price = 93.40,
                        minLimit = 500.0,
                        maxLimit = 15000.0,
                        paymentMethods = "СБП, Тинькофф"
                    ),
                    P2PAd(
                        merchantName = "SiberianBull",
                        merchantRating = 4.88,
                        merchantTrades = 620,
                        type = "SELL", // User buys from merchant
                        cryptoCurrency = "BTC",
                        fiatCurrency = "RUB",
                        price = 6380000.0,
                        minLimit = 1000.0,
                        maxLimit = 100000.0,
                        paymentMethods = "СБП, Сбербанк"
                    ),
                    P2PAd(
                        merchantName = "AlphaCapital",
                        merchantRating = 4.98,
                        merchantTrades = 3120,
                        type = "SELL", // User buys from merchant
                        cryptoCurrency = "USDT",
                        fiatCurrency = "USD",
                        price = 1.006,
                        minLimit = 50.0,
                        maxLimit = 5000.0,
                        paymentMethods = "Revolut, Bank Transfer"
                    ),
                    P2PAd(
                        merchantName = "TinkoffVip",
                        merchantRating = 4.91,
                        merchantTrades = 480,
                        type = "BUY", // User sells to merchant
                        cryptoCurrency = "USDT",
                        fiatCurrency = "RUB",
                        price = 92.85,
                        minLimit = 500.0,
                        maxLimit = 20000.0,
                        paymentMethods = "Тинькофф, СБП"
                    ),
                    P2PAd(
                        merchantName = "SwiftTrader",
                        merchantRating = 4.79,
                        merchantTrades = 150,
                        type = "BUY", // User sells to merchant
                        cryptoCurrency = "BTC",
                        fiatCurrency = "USD",
                        price = 68400.0,
                        minLimit = 100.0,
                        maxLimit = 10000.0,
                        paymentMethods = "Bank Transfer"
                    )
                )
                dao.insertP2PAds(initialAds)
            } else {
                // Ensure EUR is present in case database was already seeded without EUR
                val hasEur = assets.any { it.id == "EUR" }
                if (!hasEur) {
                    dao.insertAssets(listOf(WalletAsset("EUR", "Евро", 450.0, "FIAT", "€")))
                }
            }
        }
    }

    // 1. Local Deposit
    suspend fun deposit(currency: String, amount: Double, method: String): Boolean {
        return withContext(Dispatchers.IO) {
            val asset = dao.getAssetById(currency) ?: return@withContext false
            val newBalance = asset.balance + amount
            dao.updateBalance(currency, newBalance)

            val transaction = TransactionEntity(
                type = "DEPOSIT",
                amount = amount,
                currency = currency,
                status = "SUCCESS",
                details = "Пополнение кошелька через $method"
            )
            dao.insertTransaction(transaction)
            true
        }
    }

    // 2. Local Withdraw
    suspend fun withdraw(currency: String, amount: Double, details: String): Boolean {
        return withContext(Dispatchers.IO) {
            val asset = dao.getAssetById(currency) ?: return@withContext false
            if (asset.balance < amount) return@withContext false

            val newBalance = asset.balance - amount
            dao.updateBalance(currency, newBalance)

            val transaction = TransactionEntity(
                type = "WITHDRAW",
                amount = amount,
                currency = currency,
                status = "SUCCESS",
                details = "Вывод средств. Получатель: $details"
            )
            dao.insertTransaction(transaction)
            true
        }
    }

    // 3. Instant Swap Exchange
    suspend fun exchange(fromCurrency: String, toCurrency: String, amount: Double, rate: Double): Boolean {
        return withContext(Dispatchers.IO) {
            val fromAsset = dao.getAssetById(fromCurrency) ?: return@withContext false
            val toAsset = dao.getAssetById(toCurrency) ?: return@withContext false

            if (fromAsset.balance < amount) return@withContext false

            val newFromBalance = fromAsset.balance - amount
            val targetAmount = amount * rate
            val newToBalance = toAsset.balance + targetAmount

            dao.updateBalance(fromCurrency, newFromBalance)
            dao.updateBalance(toCurrency, newToBalance)

            val transaction = TransactionEntity(
                type = "EXCHANGE",
                amount = amount,
                currency = fromCurrency,
                targetAmount = targetAmount,
                targetCurrency = toCurrency,
                status = "SUCCESS",
                details = "Обмен по курсу $rate $fromCurrency/$toCurrency"
            )
            dao.insertTransaction(transaction)
            true
        }
    }

    // 4. Create P2P Ad
    suspend fun createP2PAd(
        type: String,
        crypto: String,
        fiat: String,
        price: Double,
        minLim: Double,
        maxLim: Double,
        methods: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val ad = P2PAd(
                merchantName = "Я (Вы)",
                merchantRating = 5.0,
                merchantTrades = 0,
                type = type,
                cryptoCurrency = crypto,
                fiatCurrency = fiat,
                price = price,
                minLimit = minLim,
                maxLimit = maxLim,
                paymentMethods = methods,
                isUserCreated = true
            )
            dao.insertP2PAd(ad)
            true
        }
    }

    // 5. Start P2P Trade
    suspend fun startP2PTrade(ad: P2PAd, amountCrypto: Double, amountFiat: Double): Int {
        return withContext(Dispatchers.IO) {
            // If selling crypto, we lock user balance right away
            if (ad.type == "BUY") { // ad.type is BUY -> User sells to merchant. Merchant is BUY, user is SELL.
                val userAsset = dao.getAssetById(ad.cryptoCurrency)
                if (userAsset == null || userAsset.balance < amountCrypto) {
                    return@withContext -1
                }
                // Lock crypto balance
                dao.updateBalance(ad.cryptoCurrency, userAsset.balance - amountCrypto)
            }

            val paymentDetails = if (ad.type == "SELL") {
                // User is buying. Merchant will provide bank details to pay.
                if (ad.fiatCurrency == "RUB") {
                    "СБП / Тинькофф: +7 (999) 123-45-67 (Мерчант ${ad.merchantName})"
                } else {
                    "Revolut USD: merchant_rev_tag_99 (${ad.merchantName})"
                }
            } else {
                // User is selling. User must provide details where to receive fiat.
                if (ad.fiatCurrency == "RUB") {
                    "СБП / Ваша карта: +7 (900) 555-55-55 (Получатель: Вы)"
                } else {
                    "Ваш Revolut ID: user_rev_tag"
                }
            }

            val trade = P2PTrade(
                adId = ad.id,
                merchantName = ad.merchantName,
                type = if (ad.type == "SELL") "BUY" else "SELL", // User's side of the trade
                cryptoAmount = amountCrypto,
                fiatAmount = amountFiat,
                cryptoCurrency = ad.cryptoCurrency,
                fiatCurrency = ad.fiatCurrency,
                price = ad.price,
                paymentMethod = ad.paymentMethods.split(",").firstOrNull()?.trim() ?: "СБП",
                status = "CREATED",
                paymentDetails = paymentDetails
            )

            val tradeId = dao.insertP2PTrade(trade).toInt()

            // Insert initial messages
            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "SYSTEM",
                    text = "Сделка #$tradeId успешно создана. Сумма: $amountFiat ${ad.fiatCurrency} за $amountCrypto ${ad.cryptoCurrency}."
                )
            )

            if (ad.type == "SELL") {
                // User is buying. Merchant greets user and waits for payment
                dao.insertP2PMessage(
                    P2PMessage(
                        tradeId = tradeId,
                        sender = "MERCHANT",
                        text = "Здравствуйте! Жду оплату по указанным реквизитам. Отправьте точную сумму и подтвердите платеж."
                    )
                )
            } else {
                // User is selling. Merchant greets user and says they will pay shortly
                dao.insertP2PMessage(
                    P2PMessage(
                        tradeId = tradeId,
                        sender = "MERCHANT",
                        text = "Приветствую! Пожалуйста, отправьте реквизиты. Я уже совершаю платеж, это займет 1-2 минуты."
                    )
                )
                // Schedule merchant auto-pay simulator
                simulateMerchantPayment(tradeId, ad.merchantName)
            }

            tradeId
        }
    }

    // 6. User marks trade as PAID
    suspend fun payP2PTrade(tradeId: Int) {
        withContext(Dispatchers.IO) {
            val trade = dao.getP2PTradeById(tradeId) ?: return@withContext
            if (trade.status != "CREATED") return@withContext

            dao.updateTradeStatus(tradeId, "PAID")
            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "SYSTEM",
                    text = "Пользователь подтвердил отправку оплаты. Криптовалюта заморожена на эскроу-балансе биржи."
                )
            )
            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "MERCHANT",
                    text = "Отлично, вижу уведомление! Сейчас проверю поступление на счет и отпущу монеты. Буквально минутку..."
                )
            )

            // Simulate merchant auto-escrow-release
            simulateMerchantEscrowRelease(tradeId)
        }
    }

    // 7. Complete trade (Escrow release)
    suspend fun completeP2PTrade(tradeId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val trade = dao.getP2PTradeById(tradeId) ?: return@withContext false
            if (trade.status != "PAID") return@withContext false

            dao.updateTradeStatus(tradeId, "COMPLETED")

            if (trade.type == "BUY") {
                // User bought crypto. Add crypto to user wallet
                val asset = dao.getAssetById(trade.cryptoCurrency)
                if (asset != null) {
                    dao.updateBalance(trade.cryptoCurrency, asset.balance + trade.cryptoAmount)
                }
            } else {
                // User sold crypto. The crypto was already locked (subtracted) at the start of trade.
                // We add fiat to user wallet since they sold crypto for cash.
                val asset = dao.getAssetById(trade.fiatCurrency)
                if (asset != null) {
                    dao.updateBalance(trade.fiatCurrency, asset.balance + trade.fiatAmount)
                }
            }

            // Create Transaction Log
            val transaction = TransactionEntity(
                type = if (trade.type == "BUY") "P2P_BUY" else "P2P_SELL",
                amount = trade.cryptoAmount,
                currency = trade.cryptoCurrency,
                targetAmount = trade.fiatAmount,
                targetCurrency = trade.fiatCurrency,
                status = "SUCCESS",
                details = "P2P сделка #$tradeId с мерчантом ${trade.merchantName}"
            )
            dao.insertTransaction(transaction)

            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "SYSTEM",
                    text = "Сделка завершена! Активы зачислены на кошельки участников."
                )
            )
            true
        }
    }

    // 8. Cancel trade
    suspend fun cancelP2PTrade(tradeId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val trade = dao.getP2PTradeById(tradeId) ?: return@withContext false
            if (trade.status == "COMPLETED" || trade.status == "CANCELLED") return@withContext false

            dao.updateTradeStatus(tradeId, "CANCELLED")

            // If user was selling, refund their locked crypto
            if (trade.type == "SELL") {
                val asset = dao.getAssetById(trade.cryptoCurrency)
                if (asset != null) {
                    dao.updateBalance(trade.cryptoCurrency, asset.balance + trade.cryptoAmount)
                }
            }

            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "SYSTEM",
                    text = "Сделка отменена. Активы возвращены на кошелек продавца."
                )
            )
            true
        }
    }

    // 9. Send Chat Message
    suspend fun sendChatMessage(tradeId: Int, sender: String, text: String) {
        withContext(Dispatchers.IO) {
            dao.insertP2PMessage(P2PMessage(tradeId = tradeId, sender = sender, text = text))

            // Trigger a quick automated reaction from merchant to simulate realism!
            if (sender == "USER") {
                delay(1200)
                val trade = dao.getP2PTradeById(tradeId) ?: return@withContext
                if (trade.status == "COMPLETED" || trade.status == "CANCELLED") return@withContext

                val reply = when {
                    text.contains("привет", ignoreCase = true) || text.contains("здравствуй", ignoreCase = true) -> {
                        "Здравствуйте! Все в силе, жду выполнения условий сделки."
                    }
                    text.contains("оплатил", ignoreCase = true) || text.contains("перевел", ignoreCase = true) || text.contains("готово", ignoreCase = true) -> {
                        if (trade.type == "BUY") {
                            "Отлично, спасибо! Проверяю баланс банка."
                        } else {
                            "Я уже перевел рубли, обновите ваш банковский клиент."
                        }
                    }
                    text.contains("рекв", ignoreCase = true) || text.contains("куда", ignoreCase = true) -> {
                        "Реквизиты указаны в деталях сделки. Пожалуйста, отправляйте точно по ним."
                    }
                    else -> {
                        "Понял вас. Всё выполняю по регламенту сделки."
                    }
                }
                dao.insertP2PMessage(P2PMessage(tradeId = tradeId, sender = "MERCHANT", text = reply))
            }
        }
    }

    // Simulator: Merchant pays user (for User Selling Crypto)
    private fun simulateMerchantPayment(tradeId: Int, merchantName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000) // Simulates merchant taking 5 seconds to transfer money to user's card
            val trade = dao.getP2PTradeById(tradeId) ?: return@launch
            if (trade.status != "CREATED") return@launch

            dao.updateTradeMerchantPaid(tradeId, true)
            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "MERCHANT",
                    text = "Я перевел $${trade.fiatAmount} ${trade.fiatCurrency} по вашим реквизитам. Пожалуйста, проверьте баланс карты и отпустите монеты (нажмите кнопку 'Подтвердить получение')."
                )
            )
            dao.insertP2PMessage(
                P2PMessage(
                    tradeId = tradeId,
                    sender = "SYSTEM",
                    text = "Мерчант отметил сделку как оплаченную. Проверьте зачисление средств на свои реквизиты перед подтверждением."
                )
            )
        }
    }

    // Simulator: Merchant verifies payment and releases crypto (for User Buying Crypto)
    private fun simulateMerchantEscrowRelease(tradeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(6000) // Simulates merchant checking bank and releasing crypto
            val trade = dao.getP2PTradeById(tradeId) ?: return@launch
            if (trade.status != "PAID") return@launch

            // Auto complete!
            completeP2PTrade(tradeId)
        }
    }
}
