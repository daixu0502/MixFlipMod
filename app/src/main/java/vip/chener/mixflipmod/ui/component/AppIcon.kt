package vip.chener.mixflipmod.ui.component

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.miuixShape

private val iconCache = object : LruCache<String, ImageBitmap>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

@Composable
fun AppIcon(icon: ApplicationInfo, pm: PackageManager, modifier: Modifier) {
    val size = with(LocalDensity.current) { 50.dp.roundToPx().coerceAtLeast(1) }
    val configuration = LocalConfiguration.current
    val cacheKey = "${icon.uid}:${icon.packageName}:${icon.sourceDir}:$size:${configuration.uiMode}:${configuration.locales.toLanguageTags()}"
    var bitmap by remember(cacheKey) { mutableStateOf(iconCache.get(cacheKey)) }
    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            iconCache.get(cacheKey) ?: runCatching {
                val drawable = pm.getApplicationIcon(icon)
                val result = createBitmap(size, size)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(Canvas(result))
                result.asImageBitmap().also { iconCache.put(cacheKey, it) }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(it, "", modifier = modifier.clip(miuixShape(12.dp)))
    } ?: Box(modifier = modifier.clip(miuixShape(12.dp)).background(colorScheme.secondaryContainer))
}
