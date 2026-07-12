package com.clockin.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Database : Screen("database")
    object Admin : Screen("admin")
    object WriteTag : Screen("write_tag")
    object ExportDatabase : Screen("export_database")
}
