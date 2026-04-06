package com.madhwanikritika.sportsmeet.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.ui.screens.AdminDashboardScreen
import com.madhwanikritika.sportsmeet.ui.screens.AdminSettingsScreen
import com.madhwanikritika.sportsmeet.ui.screens.AdminWalletManagerScreen
import com.madhwanikritika.sportsmeet.ui.screens.CreateEventScreen
import com.madhwanikritika.sportsmeet.ui.screens.EventDetailScreen
import com.madhwanikritika.sportsmeet.ui.screens.EventListScreen
import com.madhwanikritika.sportsmeet.ui.screens.LoginScreen
import com.madhwanikritika.sportsmeet.ui.screens.MyTicketsScreen
import com.madhwanikritika.sportsmeet.ui.screens.PlayerWalletScreen
import com.madhwanikritika.sportsmeet.ui.screens.SignUpScreen
import com.madhwanikritika.sportsmeet.ui.screens.SplashScreen
import com.madhwanikritika.sportsmeet.ui.viewmodel.AuthViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.EventViewModel
import com.madhwanikritika.sportsmeet.ui.viewmodel.PlayerWalletViewModel
import com.madhwanikritika.sportsmeet.R
import com.madhwanikritika.sportsmeet.ui.util.formatUsd
import com.madhwanikritika.sportsmeet.ui.viewmodel.WalletViewModel

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val ADMIN_HOME = "admin_home"
    const val PLAYER_HOME = "player_home"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val CREATE_EVENT = "create_event"

    fun eventDetail(eventId: String) = "event_detail/$eventId"
}

@Composable
fun SportsMeetNavGraph() {
    val activity = LocalContext.current as ComponentActivity
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel(activity)
    val eventViewModel: EventViewModel = hiltViewModel(activity)
    val playerWalletViewModel: PlayerWalletViewModel = hiltViewModel(activity)

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                authViewModel = authViewModel,
                onUserReady = { user ->
                    navigateAfterAuth(
                        navController,
                        user,
                        eventViewModel,
                        playerWalletViewModel,
                        popUpToRoute = Routes.SPLASH
                    )
                },
                onNeedLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onSignedIn = { user ->
                    navigateAfterAuth(
                        navController,
                        user,
                        eventViewModel,
                        playerWalletViewModel,
                        popUpToRoute = Routes.LOGIN
                    )
                },
                onNavigateSignUp = { navController.navigate(Routes.SIGNUP) }
            )
        }
        composable(
            route = Routes.SIGNUP,
            enterTransition = {
                slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(280))
            },
            exitTransition = {
                slideOutHorizontally(tween(260)) { -it / 8 } + fadeOut(tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(tween(260)) { -it / 8 } + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(tween(280)) { it / 6 } + fadeOut(tween(220))
            }
        ) {
            SignUpScreen(
                authViewModel = authViewModel,
                onSuccess = { user ->
                    navigateAfterAuth(
                        navController,
                        user,
                        eventViewModel,
                        playerWalletViewModel,
                        popUpToRoute = Routes.LOGIN
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADMIN_HOME) {
            val walletVm: WalletViewModel = hiltViewModel(activity)
            AdminTabs(
                navController = navController,
                eventViewModel = eventViewModel,
                walletViewModel = walletVm,
                authViewModel = authViewModel
            )
        }
        composable(Routes.PLAYER_HOME) {
            PlayerTabs(
                navController = navController,
                eventViewModel = eventViewModel,
                playerWalletViewModel = playerWalletViewModel,
                authViewModel = authViewModel
            )
        }
        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            enterTransition = {
                slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } +
                    fadeIn(tween(300))
            },
            exitTransition = {
                fadeOut(tween(240))
            },
            popEnterTransition = {
                fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(260))
            }
        ) {
            EventDetailScreen(
                authViewModel = authViewModel,
                eventViewModel = eventViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.CREATE_EVENT,
            enterTransition = {
                slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(240))
            }
        ) {
            val walletVm: WalletViewModel = hiltViewModel(activity)
            CreateEventScreen(
                authViewModel = authViewModel,
                eventViewModel = eventViewModel,
                walletViewModel = walletVm,
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
        }
        PlayerLowBalanceNavAlert(
            navController = navController,
            authViewModel = authViewModel,
            playerWalletViewModel = playerWalletViewModel
        )
    }
}

@Composable
private fun PlayerLowBalanceNavAlert(
    navController: androidx.navigation.NavController,
    authViewModel: AuthViewModel,
    playerWalletViewModel: PlayerWalletViewModel
) {
    val user by authViewModel.currentUser.collectAsState()
    val balance by playerWalletViewModel.balance.collectAsState()
    val config by playerWalletViewModel.appConfig.collectAsState()
    val tab by playerWalletViewModel.playerTabIndex.collectAsState()
    val navEntry by navController.currentBackStackEntryAsState()
    val route = navEntry?.destination?.route.orEmpty()

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(route, tab, user?.userId) {
        if (user?.role != "user") return@LaunchedEffect
        val bal = playerWalletViewModel.balance.value
        val threshold = playerWalletViewModel.appConfig.value.lowBalanceThreshold
        if (bal >= threshold) return@LaunchedEffect
        val onPlayerFlow =
            route == Routes.PLAYER_HOME || route.startsWith("event_detail")
        if (!onPlayerFlow) return@LaunchedEffect
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Low balance") },
            text = {
                Text(
                    "Your wallet balance (${formatUsd(balance)}) is below the threshold of ${formatUsd(config.lowBalanceThreshold)}. Add funds in Wallet."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        if (route.startsWith("event_detail")) {
                            navController.popBackStack()
                        }
                        playerWalletViewModel.setPlayerTabIndex(2)
                    }
                ) { Text("Go to Wallet") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            }
        )
    }
}

private fun navigateAfterAuth(
    navController: androidx.navigation.NavController,
    user: User,
    eventViewModel: EventViewModel,
    playerWalletViewModel: PlayerWalletViewModel,
    popUpToRoute: String
) {
    eventViewModel.setUserId(user.userId)
    eventViewModel.setSportFilter(null)
    playerWalletViewModel.setUserId(user.userId)
    if (user.role == "user") {
        playerWalletViewModel.setPlayerTabIndex(0)
    }
    if (user.role == "admin") {
        navController.navigate(Routes.ADMIN_HOME) {
            popUpTo(popUpToRoute) { inclusive = true }
        }
    } else {
        navController.navigate(Routes.PLAYER_HOME) {
            popUpTo(popUpToRoute) { inclusive = true }
        }
    }
}

@Composable
private fun AdminTabs(
    navController: androidx.navigation.NavController,
    eventViewModel: EventViewModel,
    walletViewModel: WalletViewModel,
    authViewModel: AuthViewModel
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Events", "Players", "Settings")
    val icons = listOf(Icons.Default.Event, Icons.Default.People, Icons.Default.Settings)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInHorizontally { w -> w / 12 }).togetherWith(
                    fadeOut(tween(180)) + slideOutHorizontally { w -> -w / 12 }
                )
            },
            label = "adminTabs"
        ) { t ->
            when (t) {
                0 -> AdminDashboardScreen(
                    modifier = mod,
                    eventViewModel = eventViewModel,
                    walletViewModel = walletViewModel,
                    authViewModel = authViewModel,
                    onCreateEvent = { navController.navigate(Routes.CREATE_EVENT) },
                    onOpenEvent = { id -> navController.navigate(Routes.eventDetail(id)) },
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                        }
                    }
                )
                1 -> AdminWalletManagerScreen(
                    modifier = mod,
                    walletViewModel = walletViewModel,
                    authViewModel = authViewModel
                )
                else -> AdminSettingsScreen(
                    modifier = mod,
                    walletViewModel = walletViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTabs(
    navController: androidx.navigation.NavController,
    eventViewModel: EventViewModel,
    playerWalletViewModel: PlayerWalletViewModel,
    authViewModel: AuthViewModel
) {
    val tab by playerWalletViewModel.playerTabIndex.collectAsState()
    var profileMenuExpanded by remember { mutableStateOf(false) }
    val titles = listOf("Explore", "Tickets", "Wallet")
    val icons = listOf(Icons.Default.Event, Icons.Default.ConfirmationNumber, Icons.Default.AccountBalanceWallet)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Box {
                        IconButton(onClick = { profileMenuExpanded = true }) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile"
                            )
                        }
                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                onClick = {
                                    profileMenuExpanded = false
                                    authViewModel.signOut()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.PLAYER_HOME) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { playerWalletViewModel.setPlayerTabIndex(index) },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInHorizontally { w -> w / 12 }).togetherWith(
                    fadeOut(tween(180)) + slideOutHorizontally { w -> -w / 12 }
                )
            },
            label = "playerTabs"
        ) { t ->
            when (t) {
                0 -> EventListScreen(
                    modifier = mod,
                    eventViewModel = eventViewModel,
                    authViewModel = authViewModel,
                    onOpenEvent = { id -> navController.navigate(Routes.eventDetail(id)) },
                    onWallet = { playerWalletViewModel.setPlayerTabIndex(2) }
                )
                1 -> MyTicketsScreen(
                    modifier = mod,
                    eventViewModel = eventViewModel,
                    authViewModel = authViewModel
                )
                else -> PlayerWalletScreen(
                    modifier = mod,
                    playerWalletViewModel = playerWalletViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
