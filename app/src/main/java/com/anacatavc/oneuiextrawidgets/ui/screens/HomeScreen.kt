package com.anacatavc.oneuiextrawidgets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.ui.components.LanguageSelectorRow
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import com.anacatavc.oneuiextrawidgets.utils.AppLanguage

@Composable
fun HomeScreen(
    isSpanish: Boolean,
    selectedLanguage: AppLanguage,
    hasLocationPermission: Boolean,
    hasPhonePermission: Boolean,
    hasNotificationAccess: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onOpenNotificationAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OneUiHeader(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.app_subtitle)
        )

        // Language Selection Card
        OneUiCard(title = stringResource(R.string.language_label)) {
            LanguageSelectorRow(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageChange,
                isSpanish = isSpanish
            )
        }

        // Permissions Status Card
        OneUiCard(title = stringResource(R.string.permissions_card_title)) {
            PermissionRow(
                description = stringResource(R.string.perm_location_desc),
                granted = hasLocationPermission,
                isSpanish = isSpanish,
                onGrant = onRequestRuntimePermissions
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionRow(
                description = stringResource(R.string.perm_phone_desc),
                granted = hasPhonePermission,
                isSpanish = isSpanish,
                onGrant = onRequestRuntimePermissions
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionRow(
                description = stringResource(R.string.perm_notification_desc),
                granted = hasNotificationAccess,
                isSpanish = isSpanish,
                onGrant = onOpenNotificationAccess
            )
        }

        // Available Widgets Gallery Card
        val catalogTitle = if (isSpanish) {
            "Catálogo de Widgets One UI (15 incluidos)"
        } else {
            "One UI Widget Catalog (15 included)"
        }
        OneUiCard(title = catalogTitle) {
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_rain_title),
                desc = stringResource(R.string.widget_weather_rain_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_aqi_title),
                desc = stringResource(R.string.widget_weather_aqi_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_uv_title),
                desc = stringResource(R.string.widget_weather_uv_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_moon_title),
                desc = stringResource(R.string.widget_weather_moon_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_sun_arc_title),
                desc = stringResource(R.string.widget_weather_sun_arc_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_weather_stargazing_title),
                desc = stringResource(R.string.widget_weather_stargazing_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_astrology_title),
                desc = stringResource(R.string.widget_astrology_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_ipod_title),
                desc = stringResource(R.string.widget_music_ipod_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_mp3_title),
                desc = stringResource(R.string.widget_music_mp3_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_discman_title),
                desc = stringResource(R.string.widget_music_discman_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_cassette_title),
                desc = stringResource(R.string.widget_music_cassette_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_vinyl_title),
                desc = stringResource(R.string.widget_music_vinyl_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_music_minidisc_title),
                desc = stringResource(R.string.widget_music_minidisc_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_network_title),
                desc = stringResource(R.string.widget_network_desc)
            )
            WidgetSummaryItem(
                title = stringResource(R.string.widget_battery_thermal_title),
                desc = stringResource(R.string.widget_battery_thermal_desc)
            )
        }
    }
}

@Composable
private fun PermissionRow(
    description: String,
    granted: Boolean,
    isSpanish: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = description,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (granted) {
            Text(
                text = if (isSpanish) "✓ Activo" else "✓ Active",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            FilledTonalButton(onClick = onGrant) {
                Text(
                    text = if (isSpanish) "Permitir" else "Grant",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun WidgetSummaryItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
