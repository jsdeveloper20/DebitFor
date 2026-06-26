package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.WalletViewModel
import com.example.ui.WalletViewModelFactory
import com.example.ui.screens.CardBG
import com.example.ui.screens.DarkBG
import com.example.ui.screens.ExchangeScreen
import com.example.ui.screens.FrostedGlassBackground
import com.example.ui.screens.P2PScreen
import com.example.ui.screens.PrimaryGold
import com.example.ui.screens.SoftGray
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class ScreenTab {
    WALLET,
    EXCHANGE,
    P2P
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate WalletViewModel with simple Factory
        val viewModel = ViewModelProvider(
            this,
            WalletViewModelFactory(application)
        )[WalletViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: WalletViewModel) {
    var activeTab by remember { mutableStateOf(ScreenTab.WALLET) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notificationMessage by viewModel.uiNotification.collectAsState()

    // Handle Snackbar Alerts
    LaunchedEffect(notificationMessage) {
        notificationMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearNotification()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CardBG.copy(alpha = 0.85f),
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = (activeTab == ScreenTab.WALLET),
                    onClick = { activeTab = ScreenTab.WALLET },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Кошелек",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Кошелек",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = PrimaryGold,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_wallet")
                )

                NavigationBarItem(
                    selected = (activeTab == ScreenTab.EXCHANGE),
                    onClick = { activeTab = ScreenTab.EXCHANGE },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Обмен",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Обменник",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = PrimaryGold,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_exchange")
                )

                NavigationBarItem(
                    selected = (activeTab == ScreenTab.P2P),
                    onClick = { activeTab = ScreenTab.P2P },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "P2P Торговля",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "P2P Сделки",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = PrimaryGold,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    ),
                    modifier = Modifier.testTag("nav_p2p")
                )
            }
        }
    ) { innerPadding ->
        FrostedGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                ScreenTab.WALLET -> {
                    WalletScreen(
                        viewModel = viewModel,
                        onNavigateToExchange = { activeTab = ScreenTab.EXCHANGE },
                        onNavigateToP2P = { activeTab = ScreenTab.P2P }
                    )
                }
                ScreenTab.EXCHANGE -> {
                    ExchangeScreen(
                        viewModel = viewModel
                    )
                }
                ScreenTab.P2P -> {
                    P2PScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
