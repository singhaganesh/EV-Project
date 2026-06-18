package com.ganesh.ev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import com.ganesh.ev.data.notifications.DeviceTokenRegistrar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ganesh.ev.data.model.User
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.ganesh.ev.ui.screens.*
import com.ganesh.ev.ui.theme.ClayBottomBar
import com.ganesh.ev.ui.theme.EvTheme
import com.ganesh.ev.ui.viewmodel.BookingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import android.widget.Toast
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    // Injected by Hilt (singleton from AppModule) instead of hand-constructed (I4).
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    // Activity-scoped so the same instance is shared by the Compose charging/payment
    // screens and the Razorpay result callback below — no manual construction (I4).
    private val chargingViewModel: com.ganesh.ev.ui.viewmodel.ChargingViewModel by viewModels()

    private var navController: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore persisted auth tokens into the network client up front, so entry
        // points that bypass the splash screen — e.g. a notification deeplink straight
        // to the payment screen — are authenticated before their first API call.
        runBlocking {
            try {
                userPreferencesRepository.authToken.first()?.takeIf { it.isNotEmpty() }
                        ?.let { RetrofitClient.setAuthToken(it) }
                userPreferencesRepository.refreshToken.first()?.takeIf { it.isNotEmpty() }
                        ?.let { RetrofitClient.setRefreshToken(it) }
            } catch (_: Exception) { /* no persisted session yet */ }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesRepository.themeMode.collectAsState(initial = "SYSTEM")
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            EvTheme(darkTheme = darkTheme) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    EVChargingApp(userPreferencesRepository, chargingViewModel) { controller ->
                        navController = controller
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId
        val signature = paymentData?.signature
        
        val sessionIdString = paymentData?.data?.optString("session_id") ?: 
                             paymentData?.data?.optJSONObject("notes")?.optString("session_id")
        val sessionId = sessionIdString?.toLongOrNull() ?: -1L

        if (orderId != null && razorpayPaymentId != null && signature != null) {
            // This will refresh the state in the shared ViewModel
            chargingViewModel.verifyPayment(orderId, razorpayPaymentId, signature, sessionId)
            Toast.makeText(this, "Payment Verified!", Toast.LENGTH_SHORT).show()
            
            // REDIRECT REMOVED: We stay on the summary screen to show the success state
        } else {
            Toast.makeText(this, "Payment Successful, but details missing.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object StationDetail : Screen("station/{stationId}?tab={tab}") {
        fun createRoute(stationId: Long, tab: Int = 0) = "station/$stationId?tab=$tab"
    }
    object SlotBooking : Screen("booking/station/{stationId}") {
        fun createRoute(stationId: Long) = "booking/station/$stationId"
    }
    object BookingConfirmation :
            Screen(
                    "booking/confirm/user/{userId}/station/{stationId}/connector/{connectorType}/vehicle/{vehicleType}"
            ) {
        fun createRoute(userId: Long, stationId: Long, connectorType: String, vehicleType: String) =
                "booking/confirm/user/$userId/station/$stationId/connector/$connectorType/vehicle/$vehicleType"
    }
    object MyBookings : Screen("bookings/{userId}") {
        fun createRoute(userId: Long) = "bookings/$userId"
    }
    object BookingDetail : Screen("booking/{bookingId}/user/{userId}") {
        fun createRoute(bookingId: Long, userId: Long) = "booking/$bookingId/user/$userId"
    }
    object Charging : Screen("charging/booking/{bookingId}?isNewSession={isNewSession}") {
        fun createRoute(bookingId: Long, isNewSession: Boolean = true) = 
            "charging/booking/$bookingId?isNewSession=$isNewSession"
    }
    object ChargingSession : Screen("charging/session/{sessionId}") {
        fun createRoute(sessionId: Long) = "charging/session/$sessionId"
    }
    object PaymentSummary : Screen("payment/summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "payment/summary/$sessionId"
    }
    object ChargingHistory : Screen("history/{userId}") {
        fun createRoute(userId: Long) = "history/$userId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Saved : Screen("saved")
    object Vehicles : Screen("vehicles")
    object Recurring : Screen("recurring")
    object Onboarding : Screen("onboarding")
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Bookings : BottomNavItem("bottom_bookings", Icons.Default.List, "Bookings")
    object History : BottomNavItem("bottom_history", Icons.Default.Refresh, "History")
    object Trip : BottomNavItem("trip", Icons.Default.Route, "Trip")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

@Composable
fun EVChargingApp(
    userPreferencesRepository: UserPreferencesRepository,
    chargingViewModel: com.ganesh.ev.ui.viewmodel.ChargingViewModel,
    onNavControllerReady: (androidx.navigation.NavHostController) -> Unit
) {
    val navController = rememberNavController()
    
    // Pass the controller back to the Activity
    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }
    var currentUserId by remember { mutableStateOf<Long?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

    // Restore the signed-in user up front so entry points that bypass the splash
    // screen (e.g. a notification deeplink to the payment screen) still have user
    // context — bottom bar, user-scoped screens — after the deeplinked flow.
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            try {
                userPreferencesRepository.currentUser.first()?.let { user ->
                    currentUser = user
                    currentUserId = user.id
                }
            } catch (_: Exception) { /* not signed in */ }
        }
    }

    // Once signed in, register this device's FCM token (no UI / permission needed).
    // The POST_NOTIFICATIONS runtime prompt is requested later, at first charging
    // (ChargingScreen), so it doesn't race with HomeScreen's location prompt — only
    // one runtime-permission dialog can be shown at a time (CV-11).
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            DeviceTokenRegistrar.enqueue(appContext)
        }
    }

    // Graceful global sign-out (A4): when the session can't be refreshed, clear
    // state and route to login instead of leaving the user on failing screens.
    LaunchedEffect(Unit) {
        com.ganesh.ev.data.network.SessionEvents.loggedOut.collect {
            userPreferencesRepository.clearUserData()
            RetrofitClient.clearAuthTokens()
            currentUserId = null
            currentUser = null
            val current = navController.currentDestination?.route
            if (current != "login" && current != "splash" && current != "onboarding") {
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
        }
    }

    // Shared ViewModel for Booking flow
    val bookingViewModel: BookingViewModel = viewModel()

    val bottomNavItems =
            listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Bookings,
                    BottomNavItem.History,
                    BottomNavItem.Trip,
                    BottomNavItem.Profile
            )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar =
            currentDestination?.route in
                    listOf("home", "bottom_bookings", "bottom_history", "trip", "profile") ||
                    currentDestination?.route?.startsWith("bookings/") == true

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar && currentUserId != null) {
                    ClayBottomBar {
                        bottomNavItems.forEach { item ->
                            val isSelected =
                                    when (item) {
                                        BottomNavItem.Bookings ->
                                                currentDestination?.route?.startsWith("bookings") ==
                                                        true
                                        BottomNavItem.History ->
                                                currentDestination?.route?.startsWith("history") ==
                                                        true
                                        else ->
                                                currentDestination?.hierarchy?.any {
                                                    it.route == item.route
                                                } == true
                                    }
                            NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = isSelected,
                                    colors =
                                            NavigationBarItemDefaults.colors(
                                                    selectedIconColor =
                                                            MaterialTheme.colorScheme.primary,
                                                    selectedTextColor =
                                                            MaterialTheme.colorScheme.primary,
                                                    unselectedIconColor =
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant,
                                                    unselectedTextColor =
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant,
                                                    indicatorColor =
                                                            MaterialTheme.colorScheme
                                                                    .primaryContainer
                                            ),
                                    onClick = {
                                        val route =
                                                when (item) {
                                                    is BottomNavItem.Bookings ->
                                                            "bookings/${currentUserId ?: 0L}"
                                                    is BottomNavItem.History ->
                                                            "history/${currentUserId ?: 0L}"
                                                    else -> item.route
                                                }
                                        navController.navigate(route) {
                                            popUpTo("home")
                                            launchSingleTop = true
                                        }
                                    }
                            )
                        }
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("splash") {
                SplashScreen(
                        userPreferencesRepository = userPreferencesRepository,
                        onAuthValid = { token ->
                            // Token is still valid — restore session and go to Home
                            coroutineScope.launch {
                                try {
                                    val user = userPreferencesRepository.currentUser.first()
                                    currentUser = user
                                    currentUserId = user?.id
                                } catch (_: Exception) {}
                            }
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        },
                        onAuthExpired = {
                            // Token expired or missing — clear data and go to Login
                            coroutineScope.launch { userPreferencesRepository.clearUserData() }
                            RetrofitClient.clearAuthTokens()
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        },
                        onShowOnboarding = {
                            navController.navigate("onboarding") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                )
            }

            composable("onboarding") {
                OnboardingScreen(
                        onFinished = {
                            coroutineScope.launch {
                                userPreferencesRepository.setOnboardingCompleted()
                            }
                            navController.navigate("login") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                )
            }

            composable("login") {
                LoginScreen(
                        onLoginSuccess = { user, token, refreshToken ->
                            currentUserId = user.id
                            currentUser = user
                            if (token != null) {
                                RetrofitClient.setAuthToken(token)
                                coroutineScope.launch { userPreferencesRepository.saveAuthToken(token) }
                            }
                            if (refreshToken != null) {
                                RetrofitClient.setRefreshToken(refreshToken)
                                coroutineScope.launch { userPreferencesRepository.saveRefreshToken(refreshToken) }
                            }
                            coroutineScope.launch { userPreferencesRepository.saveUser(user) }
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        }
                )
            }

            composable("home") {
                HomeScreen(
                        onLogout = {
                            coroutineScope.launch {
                                // 1. Clear DataStore
                                userPreferencesRepository.clearUserData()
                                // 2. Clear network tokens
                                RetrofitClient.clearAuthTokens()
                                // 3. Reset local state
                                currentUserId = null
                                currentUser = null
                                // 4. Navigate back to login
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onStationClick = { stationId ->
                            navController.navigate(Screen.StationDetail.createRoute(stationId))
                        },
                        userId = currentUserId,
                        onPayNow = { sessionId ->
                            navController.navigate(Screen.PaymentSummary.createRoute(sessionId))
                        }
                )
            }

            composable(
                    route = Screen.StationDetail.route,
                    arguments = listOf(
                        navArgument("stationId") { type = NavType.LongType },
                        navArgument("tab") { type = NavType.IntType; defaultValue = 0 }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://plugsy.in/station/{stationId}" },
                        navDeepLink { uriPattern = "plugsy://station/{stationId}" }
                    )
            ) { backStackEntry ->
                val stationId = backStackEntry.arguments?.getLong("stationId") ?: return@composable
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                StationDetailScreen(
                        stationId = stationId,
                        initialTab = tab,
                        onBack = { navController.popBackStack() },
                        onBookStation = {
                            bookingViewModel.resetState()
                            navController.navigate(Screen.SlotBooking.createRoute(stationId))
                        }
                )
            }

            composable(
                    route = Screen.SlotBooking.route,
                    arguments = listOf(navArgument("stationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val stationId = backStackEntry.arguments?.getLong("stationId") ?: return@composable
                SlotBookingScreen(
                        stationId = stationId,
                        userId = currentUserId,
                        viewModel = bookingViewModel,
                        onBackClick = { navController.popBackStack() },
                        onBookingSuccess = {
                            // When booking succeeds, navigate to confirmation
                            // Since they share the ViewModel, we don't need to pass all args in the
                            // URL anymore,
                            // but we'll leave the route structure mostly intact to avoid
                            // refactoring the entire Nav wrapper
                            currentUserId?.let { userId ->
                                navController.navigate(
                                        Screen.BookingConfirmation.createRoute(
                                                userId,
                                                stationId,
                                                "any", // connectorType no longer needed by
                                                // confirmation API
                                                "any" // vehicleType no longer needed
                                        )
                                ) {
                                    // Once confirmed, clear SlotBooking from backstack so back
                                    // button doesn't go back to checkout
                                    popUpTo(Screen.SlotBooking.route) { inclusive = true }
                                }
                            }
                        }
                )
            }
            composable(
                    route = Screen.BookingConfirmation.route,
                    arguments =
                            listOf(
                                    navArgument("userId") { type = NavType.LongType },
                                    navArgument("stationId") { type = NavType.LongType },
                                    navArgument("connectorType") { type = NavType.StringType },
                                    navArgument("vehicleType") { type = NavType.StringType }
                            )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                val stationId = backStackEntry.arguments?.getLong("stationId") ?: return@composable
                val connectorType =
                        backStackEntry.arguments?.getString("connectorType") ?: return@composable
                val vehicleType =
                        backStackEntry.arguments?.getString("vehicleType") ?: return@composable

                BookingConfirmationScreen(
                        userId = userId,
                        stationId = stationId,
                        connectorType = connectorType,
                        vehicleType = vehicleType,
                        viewModel = bookingViewModel, // Pass the shared viewModel
                        onBack = { navController.popBackStack() },
                        onViewBookings = { 
                            navController.navigate(Screen.MyBookings.createRoute(userId)) { 
                                popUpTo("home") 
                            } 
                        },
                        onGoHome = { navController.navigate("home") { popUpTo("home") } }
                )
            }

            composable("bottom_bookings") {
                currentUserId?.let { userId ->
                    MyBookingsScreen(
                            userId = userId,
                            onBookingClick = { bookingId ->
                                navController.navigate(
                                        Screen.BookingDetail.createRoute(bookingId, userId)
                                )
                            }
                    )
                }
            }

            composable(
                    route = Screen.MyBookings.route,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType }),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://plugsy.in/bookings/{userId}" },
                        navDeepLink { uriPattern = "plugsy://bookings/{userId}" }
                    )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                MyBookingsScreen(
                        userId = userId,
                        onBookingClick = { bookingId ->
                            navController.navigate(
                                    Screen.BookingDetail.createRoute(bookingId, userId)
                            )
                        }
                )
            }

            composable(
                    route = Screen.BookingDetail.route,
                    arguments =
                            listOf(
                                    navArgument("bookingId") { type = NavType.LongType },
                                    navArgument("userId") { type = NavType.LongType }
                            )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: return@composable
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable

                BookingDetailScreen(
                        bookingId = bookingId,
                        userId = userId,
                        onBackClick = { navController.popBackStack() },
                        onStartCharging = { bId, isNew ->
                            navController.navigate(Screen.Charging.createRoute(bId, isNew))
                        },
                        onGoToCharging = { bId, isNew ->
                            navController.navigate(Screen.Charging.createRoute(bId, isNew))
                        }
                )
            }

            composable(
                    route = Screen.Charging.route,
                    arguments = listOf(
                        navArgument("bookingId") { type = NavType.LongType },
                        navArgument("isNewSession") { type = NavType.BoolType; defaultValue = true }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://plugsy.in/charging/{bookingId}?isNewSession={isNewSession}" },
                        navDeepLink { uriPattern = "plugsy://charging/{bookingId}?isNewSession={isNewSession}" }
                    )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: return@composable
                val isNewSession = backStackEntry.arguments?.getBoolean("isNewSession") ?: true

                ChargingScreen(
                        bookingId = bookingId,
                        sessionId = null,
                        isNewSession = isNewSession,
                        viewModel = chargingViewModel,
                        onBackClick = { navController.popBackStack() },
                        onComplete = { sessionId ->
                            navController.navigate(Screen.PaymentSummary.createRoute(sessionId)) {
                                // ── REMOVE CHARGING SCREEN ──
                                // This prevents the background screen from triggering again
                                popUpTo(Screen.Charging.route) { inclusive = true }
                            }
                        }
                )
            }

            composable(
                    route = Screen.PaymentSummary.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://plugsy.in/payment/{sessionId}" },
                        navDeepLink { uriPattern = "plugsy://payment/{sessionId}" }
                    )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                PaymentSummaryScreen(
                        sessionId = sessionId,
                        viewModel = chargingViewModel,
                        onPaymentSuccess = {
                            navController.navigate("home") {
                                // Clear all payment/charging related screens from stack
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onWriteReview = { stationId ->
                            // Open the station on its Reviews tab (index 2) so the user
                            // can rate the session they just paid for.
                            navController.navigate(Screen.StationDetail.createRoute(stationId, tab = 2)) {
                                popUpTo("home")
                            }
                        },
                        onBack = { navController.popBackStack() }
                )
            }

            composable("bottom_history") {
                currentUserId?.let { userId ->
                    ChargingHistoryScreen(
                            userId = userId,
                            onPayNow = { sessionId ->
                                navController.navigate(Screen.PaymentSummary.createRoute(sessionId))
                            }
                    )
                }
            }

            composable("trip") {
                RoutePlanScreen(
                        onStationClick = { stationId ->
                            navController.navigate(Screen.StationDetail.createRoute(stationId))
                        }
                )
            }

            composable(
                    route = Screen.ChargingHistory.route,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                ChargingHistoryScreen(
                        userId = userId,
                        onPayNow = { sessionId ->
                            navController.navigate(Screen.PaymentSummary.createRoute(sessionId))
                        }
                )
            }

            composable(
                    route = Screen.ChargingSession.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable

                ChargingScreen(
                        bookingId = 0L,
                        sessionId = sessionId,
                        onBackClick = { navController.popBackStack() },
                        onComplete = {
                            currentUserId?.let { uid ->
                                navController.navigate("history/$uid") { popUpTo("home") }
                            }
                        }
                )
            }

            composable("profile") {
                ProfileScreen(
                        user = currentUser,
                        onLogout = {
                            coroutineScope.launch {
                                userPreferencesRepository.clearUserData()
                                RetrofitClient.clearAuthTokens()
                                currentUserId = null
                                currentUser = null
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onOpenSaved = { navController.navigate(Screen.Saved.route) },
                        onOpenVehicles = { navController.navigate(Screen.Vehicles.route) },
                        onOpenRecurring = { navController.navigate(Screen.Recurring.route) },
                        onProfileUpdated = { updated ->
                            currentUser = updated
                            coroutineScope.launch { userPreferencesRepository.saveUser(updated) }
                        }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                        userPreferencesRepository = userPreferencesRepository,
                        onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Saved.route) {
                SavedStationsScreen(
                        userId = currentUserId,
                        onStationClick = { stationId ->
                            navController.navigate(Screen.StationDetail.createRoute(stationId))
                        },
                        onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Vehicles.route) {
                VehiclesScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Recurring.route) {
                RecurringBookingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
