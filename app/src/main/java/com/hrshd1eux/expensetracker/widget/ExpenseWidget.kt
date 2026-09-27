package com.hrshd1eux.expensetracker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hrshd1eux.expensetracker.MainActivity
import com.hrshd1eux.expensetracker.data.local.database.AppDatabase
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.data.preferences.dataStore
import com.hrshd1eux.expensetracker.security.KeystoreManager
import com.hrshd1eux.expensetracker.util.CurrencyFormatter
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.LocalDate

private data class WidgetData(
    val todayTotal: Long,
    val upiTotal: Long,
    val cashTotal: Long,
    val monthlyBudget: Long,
    val monthTotal: Long,
    val dailySafeToSpend: Long,
    val currencySymbol: String,
    val currencyCode: String
)

/**
 * Home-screen Glance widget styled with the app's emerald & mint brand identity.
 *
 * Size behaviour (SizeMode.Responsive with 3 defined tiers):
 *   SMALL  (width < 200 dp)  — Brand header, Today's total, mint "+ Add" pill
 *   MEDIUM (200 dp .. 260 dp) — Above + UPI / Cash split badges + Daily Safe-to-Spend
 *   LARGE  (> 260 dp)        — Full financial dashboard with monthly budget progress
 */
class ExpenseWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_SIZE = DpSize(130.dp, 80.dp)
        private val MEDIUM_SIZE = DpSize(200.dp, 100.dp)
        private val LARGE_SIZE = DpSize(270.dp, 140.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    // Brand Emerald & Mint Theme Colors
    private val bgEmeraldDark = Color(0xFF002718)
    private val cardEmeraldSurface = Color(0xFF003D29)
    private val brandMint = Color(0xFF89F8C7)
    private val textWhite = Color(0xFFFFFFFF)
    private val textMuted = Color(0xFFB3CCBE)
    private val upiBlue = Color(0xFFA6C8FF)
    private val upiBadgeBg = Color(0xFF0E2838)
    private val cashBadgeBg = Color(0xFF07301F)
    private val budgetBadgeBg = Color(0xFF004D34)
    private val btnMintBg = Color(0xFF89F8C7)
    private val btnTextDark = Color(0xFF003824)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = withContext(Dispatchers.IO) {
            val (symbol, code, monthlyBudget) = try {
                val dataStorePrefs = context.applicationContext.dataStore.data.first()
                val s = dataStorePrefs[PreferenceDataStore.KEY_CURRENCY_SYMBOL] ?: "₹"
                val c = dataStorePrefs[PreferenceDataStore.KEY_CURRENCY_CODE] ?: "INR"
                val b = dataStorePrefs[androidx.datastore.preferences.core.longPreferencesKey("monthly_budget_paise")] ?: 0L
                Triple(s, c, b)
            } catch (_: Exception) {
                Triple("₹", "INR", 0L)
            }

            try {
                val keystoreManager = KeystoreManager()
                val prefs = context.getSharedPreferences("db_sec", Context.MODE_PRIVATE)
                val passphrase = keystoreManager.getOrCreateDatabasePassphrase(prefs)
                val factory = com.hrshd1eux.expensetracker.data.local.database.SafeDatabaseOpenHelperFactory(passphrase)

                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppDatabase.DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                val start = DateTimeUtils.getDayStartEpochMillis()
                val end = DateTimeUtils.getDayEndEpochMillis()

                val total = db.expenseDao().getTotalForDateRangeSync(start, end)
                val upi = db.expenseDao().getTotalByPaymentMethodForDateRangeSync(start, end, "UPI")
                val cash = db.expenseDao().getTotalByPaymentMethodForDateRangeSync(start, end, "CASH")

                val monthStart = DateTimeUtils.getMonthStartEpochMillis()
                val monthEnd = DateTimeUtils.getMonthEndEpochMillis()
                val monthTotal = db.expenseDao().getTotalForDateRangeSync(monthStart, monthEnd)

                val now = LocalDate.now()
                val daysInMonth = now.lengthOfMonth()
                val daysLeft = (daysInMonth - now.dayOfMonth + 1).coerceAtLeast(1)
                val remainingBudget = (monthlyBudget - monthTotal).coerceAtLeast(0L)
                val dailySafeToSpend = if (monthlyBudget > 0L) remainingBudget / daysLeft else 0L

                WidgetData(
                    todayTotal = total,
                    upiTotal = upi,
                    cashTotal = cash,
                    monthlyBudget = monthlyBudget,
                    monthTotal = monthTotal,
                    dailySafeToSpend = dailySafeToSpend,
                    currencySymbol = symbol,
                    currencyCode = code
                )
            } catch (_: Exception) {
                WidgetData(0L, 0L, 0L, 0L, 0L, 0L, symbol, code)
            }
        }

        provideContent {
            val size = LocalSize.current
            val isSmall = size.width < MEDIUM_SIZE.width
            val isLarge = size.width >= LARGE_SIZE.width || size.height >= LARGE_SIZE.height

            val formattedTotal = CurrencyFormatter.formatPaise(widgetData.todayTotal, widgetData.currencySymbol, widgetData.currencyCode)
            val formattedUpi = CurrencyFormatter.formatPaise(widgetData.upiTotal, widgetData.currencySymbol, widgetData.currencyCode)
            val formattedCash = CurrencyFormatter.formatPaise(widgetData.cashTotal, widgetData.currencySymbol, widgetData.currencyCode)
            val formattedSafe = CurrencyFormatter.formatPaise(widgetData.dailySafeToSpend, widgetData.currencySymbol, widgetData.currencyCode)

            val addIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("expensetracker://add_expense"),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addAction = actionStartActivity(addIntent)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openAppAction = actionStartActivity(openAppIntent)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(ColorProvider(bgEmeraldDark))
                    .padding(10.dp)
                    .clickable(openAppAction)
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(ColorProvider(cardEmeraldSurface))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Header Row: Brand/Date Tag + Add Action Pill
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLarge) "EXPENSE TRACKER · TODAY" else "TODAY",
                            style = TextStyle(
                                color = ColorProvider(brandMint),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // High-contrast stylish "+ Add" pill
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(14.dp)
                                .background(ColorProvider(btnMintBg))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .clickable(addAction),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSmall) "+ Add" else "+ Add Expense",
                                style = TextStyle(
                                    color = ColorProvider(btnTextDark),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Spend Hero Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTotal,
                            style = TextStyle(
                                color = ColorProvider(textWhite),
                                fontSize = if (isSmall) 22.sp else 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Medium & Large: Breakdown & Insights
                    if (!isSmall) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // UPI Badge
                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(8.dp)
                                    .background(ColorProvider(upiBadgeBg))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "UPI  $formattedUpi",
                                    style = TextStyle(
                                        color = ColorProvider(upiBlue),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            // Cash Badge
                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(8.dp)
                                    .background(ColorProvider(cashBadgeBg))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Cash  $formattedCash",
                                    style = TextStyle(
                                        color = ColorProvider(brandMint),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        // If Monthly Budget is active, display Daily Safe-to-Spend
                        if (widgetData.monthlyBudget > 0L) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(8.dp)
                                    .background(ColorProvider(budgetBadgeBg))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Safe to spend: $formattedSafe / day",
                                    style = TextStyle(
                                        color = ColorProvider(brandMint),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        // Small widget footer: brief summary if budget active
                        if (widgetData.monthlyBudget > 0L) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "Safe: $formattedSafe/day",
                                style = TextStyle(
                                    color = ColorProvider(brandMint),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
