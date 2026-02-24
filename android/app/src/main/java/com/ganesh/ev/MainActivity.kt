package com.ganesh.ev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ganesh.ev.data.model.User
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.ganesh.ev.ui.screens.*
import com.ganesh.ev.ui.theme.ClayBottomBar
import com.ganesh.ev.ui.theme.EvTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            EvTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { EVChargingApp(userPreferencesRepository) }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object StationDetail : Screen("station/{stationId}") {
        fun createRoute(stationId: Long) = "station/$stationId"
    }
    object SlotBooking : Screen("booking/slot/{slotId}") {
        fun createRoute(slotId: Long) = "booking/slot/$slotId"
    }
    object BookingConfirmation :
            Screen("booking/confirm/user/{userId}/slot/{slotId}/start/{startTime}/end/{endTime}") {
        fun createRoute(userId: Long, slotId: Long, startTime: String, endTime: String) =
                "booking/confirm/user/$userId/slot/$slotId/start/${startTime.replace(":", "%3A")}/end/${endTime.replace(":", "%3A")}"
    }
    object MyBookings : Screen("bookings/{userId}") {
        fun createRoute(userId: Long) = "bookings/$userId"
    }
    object BookingDetail : Screen("booking/{bookingId}/user/{userId}") {
        fun createRoute(bookingId: Long, userId: Long) = "booking/$bookingId/user/$userId"
    }
    object Charging : Screen("charging/booking/{bookingId}") {
        fun createRoute(bookingId: Long) = "charging/booking/$bookingId"
    }
    object ChargingSession : Screen("charging/session/{sessionId}") {
        fun createRoute(sessionId: Long) = "charging/session/$sessionId"
    }
    object ChargingHistory : Screen("history/{userId}") {
        fun createRoute(userId: Long) = "history/$userId"
    }
    object Profile : Screen("profile")
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Bookings : BottomNavItem("bottom_bookings", Icons.Default.List, "Bookings")
    object History : BottomNavItem("bottom_history", Icons.Default.Refresh, "History")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

@Composable
fun EVChargingApp(userPreferencesRepository: UserPreferencesRepository) {
    val navController = rememberNavController()
    var currentUserId by remember { mutableStateOf<Long?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val bottomNavItems =
            listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Bookings,
                    BottomNavItem.History,
                    BottomNavItem.Profile
            )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar =
            currentDestination?.route in
                    listOf("home", "bottom_bookings", "bottom_history", "profile")

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar && currentUserId != null) {
                    ClayBottomBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected =
                                            currentDestination?.hierarchy?.any {
                                                it.route == item.route
                                            } == true,
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
                                                            "bookings/$currentUserId"
                                                    is BottomNavItem.History ->
                                                            "history/$currentUserId"
                                                    else -> item.route
                                                }
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
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
                            RetrofitClient.clearAuthToken()
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                )
            }

            composable("login") {
                LoginScreen(
                        onLoginSuccess = { user, token ->
                            currentUserId = user.id
                            currentUser = user
                            if (token != null) {
                                RetrofitClient.setAuthToken(token)
                            }
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        }
                )
            }

            composable("home") {
                HomeScreen(
                        onLogout = {
                            currentUserId = null
                            currentUser = null
                            RetrofitClient.clearAuthToken()
                            coroutineScope.launch { userPreferencesRepository.clearUserData() }
                            navController.navigate("login") { popUpTo("home") { inclusive = true } }
                        },
                        onStationClick = { stationId ->
                            navController.navigate(Screen.StationDetail.createRoute(stationId))
                        }
                )
            }

            composable(
                    route = Screen.StationDetail.route,
                    arguments = listOf(navArgument("stationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val stationId = backStackEntry.arguments?.getLong("stationId") ?: return@composable
                StationDetailScreen(
                        stationId = stationId,
                        onBackClick = { navController.popBackStack() },
                        onBookSlot = { slotId ->
                            navController.navigate(Screen.SlotBooking.createRoute(slotId))
                        }
                )
            }

            composable(
                    route = Screen.SlotBooking.route,
                    arguments = listOf(navArgument("slotId") { type = NavType.LongType })
            ) { backStackEntry ->
                val slotId = backStackEntry.arguments?.getLong("slotId") ?: return@composable
                SlotBookingScreen(
                        slotId = slotId,
                        onBackClick = { navController.popBackStack() },
                        onConfirmBooking = { sId, startTime, endTime ->
                            currentUserId?.let { userId ->
                                navController.navigate(
                                        Screen.BookingConfirmation.createRoute(
                                                userId,
                                                sId,
                                                startTime,
                                                endTime
                                        )
                                )
                            }
                        }
                )
            }

            composable(
                    route = Screen.BookingConfirmation.route,
                    arguments =
                            listOf(
                                    navArgument("userId") { type = NavType.LongType },
                                    navArgument("slotId") { type = NavType.LongType },
                                    navArgument("startTime") { type = NavType.StringType },
                                    navArgument("endTime") { type = NavType.StringType }
                            )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                val slotId = backStackEntry.arguments?.getLong("slotId") ?: return@composable
                val startTime =
                        backStackEntry.arguments?.getString("startTime")?.replace("%3A", ":")
                                ?: return@composable
                val endTime =
                        backStackEntry.arguments?.getString("endTime")?.replace("%3A", ":")
                                ?: return@composable

                BookingConfirmationScreen(
                        userId = userId,
                        slotId = slotId,
                        startTime = startTime,
                        endTime = endTime,
                        onViewBookings = {
                            currentUserId?.let { uid ->
                                navController.navigate("bookings/$uid") { popUpTo("home") }
                            }
                        },
                        onGoHome = {
                            navController.navigate("home") { popUpTo("home") { inclusive = true } }
                        }
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
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
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
                        onStartCharging = { bId ->
                            navController.navigate(Screen.Charging.createRoute(bId))
                        },
                        onGoToCharging = { bId ->
                            navController.navigate(Screen.Charging.createRoute(bId))
                        }
                )
            }

            composable(
                    route = Screen.Charging.route,
                    arguments = listOf(navArgument("bookingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: return@composable

                ChargingScreen(
                        bookingId = bookingId,
                        sessionId = null,
                        onBackClick = { navController.popBackStack() },
                        onComplete = {
                            currentUserId?.let { uid ->
                                navController.navigate("bookings/$uid") { popUpTo("home") }
                            }
                        }
                )
            }

            composable("bottom_history") {
                currentUserId?.let { userId -> ChargingHistoryScreen(userId = userId) }
            }

            composable(
                    route = Screen.ChargingHistory.route,
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                ChargingHistoryScreen(userId = userId)
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
                            currentUserId = null
                            currentUser = null
                            RetrofitClient.clearAuthToken()
                            coroutineScope.launch { userPreferencesRepository.clearUserData() }
                            navController.navigate("login") { popUpTo("home") { inclusive = true } }
                        }
                )
            }
        }
    }
}
