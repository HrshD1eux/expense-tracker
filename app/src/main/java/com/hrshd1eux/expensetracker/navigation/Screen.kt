package com.hrshd1eux.expensetracker.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Analytics : Screen("analytics")
    object More : Screen("more")
    object AddExpense : Screen("add_expense?expenseId={expenseId}") {
        fun createRoute(expenseId: String? = null): String {
            return if (expenseId != null) "add_expense?expenseId=$expenseId" else "add_expense"
        }
    }
    object Categories : Screen("categories")
    object Security : Screen("security")
    object BackupRestore : Screen("backup_restore")
}
