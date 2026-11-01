package com.hayhak.currencyconverter.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.SUPPORTED_CURRENCIES
import com.hayhak.currencyconverter.ui.components.UpdateAvailableDialog
import com.hayhak.currencyconverter.ui.dashboard.DashboardViewModel
import com.hayhak.currencyconverter.ui.locale.AppLanguages
import com.hayhak.currencyconverter.ui.theme.ThemeMode
import com.hayhak.currencyconverter.ui.theme.ThemeViewModel
import com.hayhak.currencyconverter.util.AnalyticsHelper
import com.hayhak.currencyconverter.util.FeedbackHelper
import com.hayhak.currencyconverter.util.PlayStoreHelper
import com.hayhak.currencyconverter.util.PlayUpdateChecker
import com.hayhak.currencyconverter.util.PlayUpdateInfo
import com.hayhak.currencyconverter.util.ShareHelper
import com.hayhak.currencyconverter.util.UpdateCheckStatus
import com.hayhak.currencyconverter.ui.widget.WidgetConfigViewModel
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import com.hayhak.currencyconverter.util.currencyName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    favViewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToHelp: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {}
) {
    val favState     by favViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode    by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by themeViewModel.dynamicColor.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localeKey = androidx.core.os.ConfigurationCompat.getLocales(LocalConfiguration.current).toLanguageTags()

    var checkingUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<PlayUpdateInfo?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, localeKey) {
        if (searchQuery.isBlank()) SUPPORTED_CURRENCIES
        else SUPPORTED_CURRENCIES.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                context.currencyName(it.code).contains(searchQuery, ignoreCase = true)
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var favoritesExpanded by remember { mutableStateOf(false) }
    val currentLangTag = AppLanguages.currentTag()
    val currentLangLabel = AppLanguages.all.find {
        it.tag.equals(currentLangTag, ignoreCase = true) ||
            currentLangTag.startsWith(it.tag)
    }?.nativeName ?: stringResource(R.string.settings_language_system)

    if (showLanguageDialog) {
        LanguageDialog(
            currentTag = currentLangTag,
            onDismiss = { showLanguageDialog = false },
            onSelect = { tag ->
                AppLanguages.apply(tag)
                AnalyticsHelper.logLanguageSelected(context, tag)
                showLanguageDialog = false
            }
        )
    }

    val favoritesListState = rememberLazyListState()
    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader(stringResource(R.string.settings_language))
        Surface(
            onClick = { showLanguageDialog = true },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (currentLangTag.isBlank()) stringResource(R.string.settings_language_system)
                        else currentLangLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        SectionHeader(stringResource(R.string.settings_appearance))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchRow(
                icon     = Icons.Default.Palette,
                title    = stringResource(R.string.settings_material_you),
                subtitle = stringResource(R.string.settings_material_you_sub),
                checked  = dynamicColor,
                onCheckedChange = { themeViewModel.setDynamicColor(it) }
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        ThemeSelector(current = themeMode, onSelect = themeViewModel::setTheme)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = { favoritesExpanded = !favoritesExpanded },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_favorites), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_favorites_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Icon(
                    imageVector = if (favoritesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            visible = favoritesExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value          = searchQuery,
                    onValueChange  = { searchQuery = it },
                    modifier       = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    placeholder    = { Text(stringResource(R.string.settings_search_currency)) },
                    leadingIcon    = { Icon(Icons.Default.Search, null) },
                    singleLine     = true
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    LazyColumn(
                        state = favoritesListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalListScrollbar(favoritesListState, scrollbarColor),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredCurrencies, key = { it.code }) { currency ->
                            val isFavorite = favState.favorites.contains(currency.code)
                            Surface(
                                onClick     = { favViewModel.toggleFavorite(currency.code) },
                                shape       = MaterialTheme.shapes.medium,
                                color       = if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                                              else            MaterialTheme.colorScheme.surface,
                                tonalElevation = if (isFavorite) 0.dp else 1.dp,
                                modifier    = Modifier.padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment   = Alignment.CenterVertically
                                ) {
                                    Text(currency.flag, fontSize = 22.sp, modifier = Modifier.padding(end = 14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(currency.code, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            currencyName(currency.code),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    }
                                    Icon(
                                        imageVector     = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint            = if (isFavorite) MaterialTheme.colorScheme.primary
                                                          else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        WidgetSettingsSection()
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        AboutSection(
            checkingUpdate = checkingUpdate,
            onCheckUpdate = {
                if (checkingUpdate) return@AboutSection
                scope.launch {
                    checkingUpdate = true
                    val result = withContext(Dispatchers.IO) {
                        PlayUpdateChecker.checkDetailed(context)
                    }
                    checkingUpdate = false
                    when (result.status) {
                        UpdateCheckStatus.AVAILABLE -> manualUpdateInfo = result.info
                        UpdateCheckStatus.UP_TO_DATE -> Toast.makeText(
                            context,
                            context.getString(R.string.update_check_up_to_date),
                            Toast.LENGTH_SHORT
                        ).show()
                        UpdateCheckStatus.UNAVAILABLE -> Toast.makeText(
                            context,
                            context.getString(R.string.update_check_unavailable),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onNavigateToHelp = onNavigateToHelp,
            onNavigateToPrivacy = onNavigateToPrivacy
        )
    }

    manualUpdateInfo?.let { info ->
        UpdateAvailableDialog(
            updateInfo = info,
            onDismiss = { manualUpdateInfo = null }
        )
    }
}

@Composable
private fun WidgetSettingsSection(viewModel: WidgetConfigViewModel = hiltViewModel()) {
    val base by viewModel.base.collectAsStateWithLifecycle()
    val codes by viewModel.codes.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val majors = listOf("USD", "EUR", "GBP", "CHF", "TRY", "JPY", "CAD", "AUD")
    val options = (codes + favorites + majors).distinct().filter { it != base }
    val baseInfo = SUPPORTED_CURRENCIES.find { it.code == base } ?: return
    val addCurrency = SUPPORTED_CURRENCIES.find { it.code !in codes && it.code != base } ?: SUPPORTED_CURRENCIES.first()

    SectionHeader(stringResource(R.string.settings_widget))
    Text(
        stringResource(R.string.settings_widget_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    CurrencySelector(
        label = stringResource(R.string.dashboard_base_currency),
        currency = baseInfo,
        onSelect = { viewModel.save(it.code, codes) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    Text(stringResource(R.string.settings_widget_pairs), style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(6.dp))
    options.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            row.forEach { code ->
                val selected = code in codes
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) codes - code else (codes + code).distinct().take(4)
                        viewModel.save(base, next)
                    },
                    label = { Text("${SUPPORTED_CURRENCIES.find { it.code == code }?.flag.orEmpty()} $code") },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (codes.size < 4) {
        CurrencySelector(
            label = stringResource(R.string.settings_widget_add),
            currency = addCurrency,
            onSelect = { viewModel.save(base, (codes + it.code).distinct().take(4)) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@Composable
private fun AboutSection(
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    SectionHeader(stringResource(R.string.settings_about))

    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.settings_developer_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        stringResource(
                            R.string.settings_version_label,
                            packageInfo.versionName ?: "",
                            versionCode
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.settings_update),
                subtitle = if (checkingUpdate) {
                    stringResource(R.string.update_check_checking)
                } else {
                    stringResource(R.string.settings_update_desc)
                },
                onClick = onCheckUpdate
            )
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.HelpOutline,
                title = stringResource(R.string.settings_help),
                subtitle = stringResource(R.string.settings_help_desc),
                onClick = onNavigateToHelp
            )
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.Star,
                title = stringResource(R.string.settings_rate_us),
                subtitle = stringResource(R.string.settings_rate_us_desc),
                onClick = { PlayStoreHelper.openListing(context) }
            )
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.Email,
                title = stringResource(R.string.settings_feedback),
                subtitle = stringResource(R.string.settings_feedback_desc),
                onClick = { FeedbackHelper.openFeedbackEmail(context) }
            )
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.Share,
                title = stringResource(R.string.settings_share),
                subtitle = stringResource(R.string.settings_share_desc),
                onClick = {
                    val playUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                    val body = context.getString(R.string.settings_share_text, playUrl)
                    ShareHelper.shareText(context, context.getString(R.string.settings_share), body)
                }
            )
            HorizontalDivider()
            AboutLinkRow(
                icon = Icons.Default.Policy,
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_policy_desc),
                onClick = onNavigateToPrivacy
            )
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LanguageDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            LazyColumn {
                item {
                    LanguageRow(
                        selected = currentTag.isBlank(),
                        label = stringResource(R.string.settings_language_system),
                        onClick = { onSelect(AppLanguages.SYSTEM) }
                    )
                }
                items(AppLanguages.all) { lang ->
                    LanguageRow(
                        selected = currentTag.equals(lang.tag, ignoreCase = true) ||
                            currentTag.startsWith("${lang.tag}-"),
                        label = lang.nativeName,
                        onClick = { onSelect(lang.tag) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun LanguageRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style    = MaterialTheme.typography.titleMedium,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp).padding(end = 0.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to (Icons.Default.WbAuto    to stringResource(R.string.settings_theme_system)),
        ThemeMode.LIGHT  to (Icons.Default.LightMode to stringResource(R.string.settings_theme_light)),
        ThemeMode.DARK   to (Icons.Default.DarkMode  to stringResource(R.string.settings_theme_dark)),
        ThemeMode.AMOLED to (Icons.Default.Contrast  to stringResource(R.string.settings_theme_amoled)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowItems ->
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (mode, pair) ->
                    val (icon, label) = pair
                    FilterChip(
                        selected    = current == mode,
                        onClick     = { onSelect(mode) },
                        label       = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                        modifier    = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun Modifier.verticalListScrollbar(
    state: LazyListState,
    color: Color,
    width: Float = 4f
): Modifier = drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty() || info.totalItemsCount == 0) return@drawWithContent

    val viewport = info.viewportEndOffset - info.viewportStartOffset
    val averageSize = visible.sumOf { it.size }.toFloat() / visible.size
    val estimatedTotal = averageSize * info.totalItemsCount
    if (estimatedTotal <= viewport) return@drawWithContent

    val thumbHeight = (viewport / estimatedTotal * size.height).coerceAtLeast(32f)
    val first = visible.first()
    val scrolled = first.index * averageSize - first.offset
    val thumbOffset = (scrolled / estimatedTotal * size.height)
        .coerceIn(0f, size.height - thumbHeight)

    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - width - 2f, thumbOffset),
        size = Size(width, thumbHeight),
        cornerRadius = CornerRadius(width / 2f, width / 2f)
    )
}
