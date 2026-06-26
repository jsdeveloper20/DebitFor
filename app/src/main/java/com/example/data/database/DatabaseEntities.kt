package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// 1. Wallet Asset Entity
@Entity(tableName = "wallet_assets")
data class WalletAsset(
    @PrimaryKey val id: String, // e.g., "RUB", "USD", "BTC", "ETH", "USDT"
    val name: String,           // e.g., "Российский Рубль", "Доллар США", "Bitcoin", "Ethereum", "Tether"
    val balance: Double,
    val type: String,           // "FIAT", "CRYPTO"
    val symbol: String          // "₽", "$", "₿", "Ξ", "₮"
)

// 2. Transaction Entity
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,           // "DEPOSIT" (Пополнение), "WITHDRAW" (Вывод), "EXCHANGE" (Обмен), "P2P_BUY" (Покупка P2P), "P2P_SELL" (Продажа P2P)
    val amount: Double,
    val currency: String,
    val targetAmount: Double? = null,
    val targetCurrency: String? = null,
    val fee: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,         // "SUCCESS", "PENDING", "CANCELLED"
    val details: String
)

// 3. P2P Ad Entity (Объявления)
@Entity(tableName = "p2p_ads")
data class P2PAd(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val merchantRating: Double,
    val merchantTrades: Int,
    val type: String,           // "BUY" (Мерчант покупает, юзер продает) or "SELL" (Мерчант продает, юзер покупает)
    val cryptoCurrency: String, // "USDT", "BTC", "ETH"
    val fiatCurrency: String,   // "RUB", "USD"
    val price: Double,          // Курс сделки (например, 94.2)
    val minLimit: Double,
    val maxLimit: Double,
    val paymentMethods: String, // Комма-разделенные методы, например: "СБП, Тинькофф"
    val isUserCreated: Boolean = false
)

// 4. Active P2P Trade Entity (Активные сделки)
@Entity(tableName = "p2p_trades")
data class P2PTrade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val adId: Int,
    val merchantName: String,
    val type: String,           // "BUY" (юзер покупает крипту за фиат) or "SELL" (юзер продает крипту за фиат)
    val cryptoAmount: Double,
    val fiatAmount: Double,
    val cryptoCurrency: String,
    val fiatCurrency: String,
    val price: Double,
    val paymentMethod: String,
    val status: String,         // "CREATED" (Создана), "PAID" (Оплачена юзером / мерчантом), "COMPLETED" (Завершена / Эскроу отпущен), "CANCELLED" (Отменена)
    val timestamp: Long = System.currentTimeMillis(),
    val paymentDetails: String,  // Реквизиты оплаты (например, номер телефона или карты)
    val isMerchantPaid: Boolean = false // Оплатил ли мерчант (в случае если юзер продает крипту)
)

// 5. P2P Chat Messages
@Entity(tableName = "p2p_messages")
data class P2PMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tradeId: Int,
    val sender: String,         // "USER", "MERCHANT", "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WalletDao {
    // Assets
    @Query("SELECT * FROM wallet_assets")
    fun getAllAssetsFlow(): Flow<List<WalletAsset>>

    @Query("SELECT * FROM wallet_assets WHERE id = :id")
    suspend fun getAssetById(id: String): WalletAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<WalletAsset>)

    @Query("UPDATE wallet_assets SET balance = :newBalance WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Double)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // P2P Ads
    @Query("SELECT * FROM p2p_ads ORDER BY price ASC, id DESC")
    fun getAllP2PAdsFlow(): Flow<List<P2PAd>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertP2PAd(ad: P2PAd)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertP2PAds(ads: List<P2PAd>)

    @Query("DELETE FROM p2p_ads WHERE id = :id")
    suspend fun deleteP2PAdById(id: Int)

    // P2P Trades
    @Query("SELECT * FROM p2p_trades ORDER BY timestamp DESC")
    fun getAllP2PTradesFlow(): Flow<List<P2PTrade>>

    @Query("SELECT * FROM p2p_trades WHERE id = :id")
    fun getP2PTradeFlow(id: Int): Flow<P2PTrade?>

    @Query("SELECT * FROM p2p_trades WHERE id = :id")
    suspend fun getP2PTradeById(id: Int): P2PTrade?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertP2PTrade(trade: P2PTrade): Long

    @Query("UPDATE p2p_trades SET status = :status WHERE id = :id")
    suspend fun updateTradeStatus(id: Int, status: String)

    @Query("UPDATE p2p_trades SET isMerchantPaid = :paid WHERE id = :id")
    suspend fun updateTradeMerchantPaid(id: Int, paid: Boolean)

    // P2P Chat Messages
    @Query("SELECT * FROM p2p_messages WHERE tradeId = :tradeId ORDER BY timestamp ASC")
    fun getMessagesForTradeFlow(tradeId: Int): Flow<List<P2PMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertP2PMessage(message: P2PMessage)
}

@Database(
    entities = [
        WalletAsset::class,
        TransactionEntity::class,
        P2PAd::class,
        P2PTrade::class,
        P2PMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
}
