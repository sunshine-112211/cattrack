package com.cattrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Data : Screen("data", "数据", Icons.Default.BarChart)
    object Report : Screen("report", "报告", Icons.Default.Assessment)
    object Profile : Screen("profile", "档案", Icons.Default.Pets)
    object Device : Screen("device", "设备", Icons.Default.Bluetooth)
    object Scan : Screen("scan", "扫描设备")
    object AddCat : Screen("add_cat", "添加猫咪")
    object CatDetail : Screen("cat_detail/{catId}", "猫咪详情") {
        fun createRoute(catId: Long) = "cat_detail/$catId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Data, Screen.Report, Screen.Profile, Screen.Device)

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.route == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                com.cattrack.app.ui.home.HomeScreen(
                    onNavigateToData = { navController.navigate(Screen.Data.route) },
                    onNavigateToReport = { navController.navigate(Screen.Report.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDevice = { navController.navigate(Screen.Device.route) }
                )
            }
            composable(Screen.Data.route) {
                com.cattrack.app.ui.data.DataScreen()
            }
            composable(Screen.Report.route) {
                com.cattrack.app.ui.report.ReportScreen()
            }
            composable(Screen.Profile.route) {
                com.cattrack.app.ui.profile.CatProfileScreen(
                    onAddCat = { navController.navigate(Screen.AddCat.route) },
                    onNavigateToCatDetail = { catId ->
                        navController.navigate(Screen.CatDetail.createRoute(catId))
                    }
                )
            }
            composable(Screen.Device.route) {
                com.cattrack.app.ui.device.DeviceScreen(
                    onScanDevice = { navController.navigate(Screen.Scan.route) }
                )
            }
            composable(Screen.Scan.route) {
                com.cattrack.app.ui.device.ScanScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddCat.route) {
                com.cattrack.app.ui.profile.AddCatScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
