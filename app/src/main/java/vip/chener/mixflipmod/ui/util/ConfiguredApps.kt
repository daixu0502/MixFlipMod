package vip.chener.mixflipmod.ui.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import vip.chener.mixflipmod.model.appConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ConfiguredApp(
    val packageName: String,
    val label: String,
    val appInfo: ApplicationInfo?,
)

@Composable
internal fun rememberConfiguredApps(): State<Map<String, ConfiguredApp>> {
    val pm = LocalContext.current.packageManager
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val locales = LocalConfiguration.current.locales.toLanguageTags()
    return produceState<Map<String, ConfiguredApp>>(emptyMap(), pm, lifecycle, locales) {
        // Query once per foreground entry, never during composition or scrolling.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = withContext(Dispatchers.IO) { loadConfiguredApps(pm) }
        }
    }
}

private fun loadConfiguredApps(pm: PackageManager): Map<String, ConfiguredApp> = buildMap {
    appConfigs.map { it.packageName }.distinct().forEach { packageName ->
        val info = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
        if (info != null || packageName == "android") {
            val label = info?.let {
                runCatching { pm.getApplicationLabel(it).toString() }.getOrNull()
            } ?: packageName
            put(packageName, ConfiguredApp(packageName, label, info))
        }
    }
}
