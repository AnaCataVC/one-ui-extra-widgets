package com.anacatavc.oneuiextrawidgets.ui.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anacatavc.oneuiextrawidgets.R
import com.anacatavc.oneuiextrawidgets.data.preferences.AppPreferences
import com.anacatavc.oneuiextrawidgets.data.preferences.WidgetFontScaleMode
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiCard
import com.anacatavc.oneuiextrawidgets.ui.components.OneUiHeader
import com.anacatavc.oneuiextrawidgets.ui.theme.OneUiTheme
import com.anacatavc.oneuiextrawidgets.widgets.WidgetInstanceHelper
import com.anacatavc.oneuiextrawidgets.widgets.theme.OneUiGlanceTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Native AppWidget configuration screen launched from One UI Home (initial placement or
 * long-press -> Settings) to adjust background opacity for a single widget instance.
 */
class WidgetConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultIntent)

        val appWidgetInfo = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        } else {
            null
        }

        // Security guard: reject invalid IDs or widgets not owned by this application.
        if (appWidgetInfo == null || appWidgetInfo.provider?.packageName != packageName) {
            finish()
            return
        }

        val widgetLabel = appWidgetInfo.loadLabel(packageManager).toString()

        setContent {
            OneUiTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    widgetLabel = widgetLabel,
                    onSave = { opacityPct, fontScaleMode ->
                        AppPreferences.saveWidgetOpacity(this, appWidgetId, opacityPct)
                        AppPreferences.saveWidgetFontScale(this, appWidgetId, fontScaleMode)
                        WidgetInstanceHelper.updateWidgetInstance(this, appWidgetId)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    widgetLabel: String,
    onSave: suspend (Int, WidgetFontScaleMode) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var opacityPct by remember { mutableIntStateOf(AppPreferences.DEFAULT_WIDGET_OPACITY) }
    var fontScaleMode by remember { mutableStateOf(WidgetFontScaleMode.SYSTEM) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(appWidgetId) {
        opacityPct = AppPreferences.getWidgetOpacity(context, appWidgetId)
        fontScaleMode = AppPreferences.getWidgetFontScale(context, appWidgetId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OneUiHeader(
                title = stringResource(R.string.widget_config_title),
                subtitle = widgetLabel.ifBlank { stringResource(R.string.widget_config_subtitle) }
            )

            OneUiCard(title = stringResource(R.string.widget_config_preview_title)) {
                TransparencyPreviewBox(
                    widgetLabel = widgetLabel,
                    opacityPct = opacityPct,
                    fontScaleMode = fontScaleMode
                )
            }

            OneUiCard(title = stringResource(R.string.widget_config_opacity_label)) {
                Text(
                    text = "$opacityPct%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = opacityPct.toFloat(),
                    onValueChange = { opacityPct = it.roundToInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 75, 100).forEach { preset ->
                        FilterChip(
                            selected = opacityPct == preset,
                            onClick = { opacityPct = preset },
                            label = { Text("$preset%") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (opacityPct < 25) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.widget_config_low_contrast_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OneUiCard(title = stringResource(R.string.widget_config_font_scale_label)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        WidgetFontScaleMode.SYSTEM to R.string.widget_config_font_system,
                        WidgetFontScaleMode.COMPACT to R.string.widget_config_font_compact,
                        WidgetFontScaleMode.LARGE to R.string.widget_config_font_large
                    )
                    options.forEach { (mode, labelRes) ->
                        FilterChip(
                            selected = fontScaleMode == mode,
                            onClick = { fontScaleMode = mode },
                            label = { Text(stringResource(labelRes)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.widget_config_cancel))
                }
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            onSave(opacityPct, fontScaleMode)
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.widget_config_save))
                }
            }
        }
    }
}

@Composable
private fun TransparencyPreviewBox(
    widgetLabel: String,
    opacityPct: Int,
    fontScaleMode: WidgetFontScaleMode
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    val baseSurface = if (isDark) Color(0xEB1C1C1E) else Color(0xF2F4F5F8)
    val previewSurface = OneUiGlanceTokens.scaleColorAlpha(baseSurface, opacityPct)
    val previewText = if (isDark) Color(0xFFF5F6F9) else Color(0xFF121316)
    val systemScale = OneUiGlanceTokens.systemFontScale(context)
    val titleSp = OneUiGlanceTokens.scaledSp(16f, systemScale, fontScaleMode)
    val subSp = OneUiGlanceTokens.scaledSp(12f, systemScale, fontScaleMode)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0381FE),
                        Color(0xFF35E8F7),
                        Color(0xFF7C3AED)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OneUiGlanceTokens.CornerRadius))
                .background(previewSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = widgetLabel.ifBlank { stringResource(R.string.widget_config_preview_sample) },
                fontSize = titleSp,
                fontWeight = FontWeight.Bold,
                color = previewText,
                maxLines = 1
            )
            Text(
                text = "${stringResource(R.string.widget_config_opacity_label)}: $opacityPct%",
                fontSize = subSp,
                color = previewText.copy(alpha = 0.78f),
                maxLines = 1
            )
        }
    }
}
