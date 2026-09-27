package com.hrshd1eux.expensetracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.presentation.theme.PaymentCashColor
import com.hrshd1eux.expensetracker.presentation.theme.PaymentUpiColor

@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // UPI Option
        val isUpiSelected = selectedMethod == PaymentMethod.UPI
        val upiBgColor by animateColorAsState(
            targetValue = if (isUpiSelected) PaymentUpiColor else Color.Transparent,
            animationSpec = tween(200),
            label = "upiBgColor"
        )
        val upiTextColor by animateColorAsState(
            targetValue = if (isUpiSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(200),
            label = "upiTextColor"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(upiBgColor)
                .clickable { onMethodSelected(PaymentMethod.UPI) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = upiTextColor
                )
                Text(
                    text = "UPI",
                    color = upiTextColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Cash Option
        val isCashSelected = selectedMethod == PaymentMethod.CASH
        val cashBgColor by animateColorAsState(
            targetValue = if (isCashSelected) PaymentCashColor else Color.Transparent,
            animationSpec = tween(200),
            label = "cashBgColor"
        )
        val cashTextColor by animateColorAsState(
            targetValue = if (isCashSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(200),
            label = "cashTextColor"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cashBgColor)
                .clickable { onMethodSelected(PaymentMethod.CASH) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = cashTextColor
                )
                Text(
                    text = "Cash",
                    color = cashTextColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
