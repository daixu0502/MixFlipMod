package vip.chener.mixflipmod.ui.screen

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import vip.chener.mixflipmod.R
import vip.chener.mixflipmod.model.AppConfig
import vip.chener.mixflipmod.ui.component.AppIcon
import vip.chener.mixflipmod.ui.util.XposedServiceState
import vip.chener.mixflipmod.ui.util.ConfiguredApp
import vip.chener.mixflipmod.ui.util.checkScopes
import vip.chener.mixflipmod.ui.util.scopePackage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun GroupSwitchScreen(
    prefs: SharedPreferences,
    @StringRes titleRes: Int,
    configs: List<AppConfig>,
    apps: Map<String, ConfiguredApp>,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val scrollBehavior = MiuixScrollBehavior()
    val visibleConfigs = remember(configs, apps) {
        configs.filter { it.packageName in apps }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = stringResource(titleRes),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = padding,
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)) {
                    visibleConfigs.forEach { config ->
                        val prefKey = config.prefs.first().prefKey
                        val checked = remember(prefs, prefKey) {
                            mutableStateOf(prefs.getBoolean(prefKey, false))
                        }
                        val app = apps.getValue(config.packageName)
                        val appInfo = app.appInfo
                        SwitchPreference(
                            title = app.label,
                            summary = config.packageName,
                            checked = checked.value,
                            startAction = {
                                appInfo?.let {
                                    AppIcon(
                                        icon = it,
                                        pm = pm,
                                        modifier = Modifier.padding(end = 8.dp).size(36.dp),
                                    )
                                }
                            },
                            onCheckedChange = {
                                checked.value = it
                                prefs.edit { putBoolean(prefKey, it) }
                                runCatching {
                                    checkScopes(XposedServiceState.service, listOf(config.scopePackage), it)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
