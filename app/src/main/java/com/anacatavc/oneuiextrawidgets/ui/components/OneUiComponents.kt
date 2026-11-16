package com.anacatavc.oneuiextrawidgets.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacatavc.oneuiextrawidgets.utils.AppLanguage
import com.anacatavc.oneuiextrawidgets.utils.LocaleHelper

/**
 * Samsung One UI Viewing Area Header (generous top spacing for one-handed reachability).
 */
@Composable
fun OneUiHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 20.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Samsung One UI 26dp Rounded Block Container.
 */
@Composable
fun OneUiCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun LanguageSelectorRow(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    isSpanish: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedLanguage == AppLanguage.SYSTEM,
            onClick = {
                LocaleHelper.setLanguage(AppLanguage.SYSTEM)
                onLanguageSelected(AppLanguage.SYSTEM)
            },
            label = { Text(if (isSpanish) "Auto" else "System") }
        )
        FilterChip(
            selected = selectedLanguage == AppLanguage.SPANISH,
            onClick = {
                LocaleHelper.setLanguage(AppLanguage.SPANISH)
                onLanguageSelected(AppLanguage.SPANISH)
            },
            label = { Text("Español") }
        )
        FilterChip(
            selected = selectedLanguage == AppLanguage.ENGLISH,
            onClick = {
                LocaleHelper.setLanguage(AppLanguage.ENGLISH)
                onLanguageSelected(AppLanguage.ENGLISH)
            },
            label = { Text("English") }
        )
    }
}
