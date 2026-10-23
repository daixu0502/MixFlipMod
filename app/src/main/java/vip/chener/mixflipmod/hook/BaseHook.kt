package vip.chener.mixflipmod.hook

import vip.chener.mixflipmod.Prefs
import vip.chener.mixflipmod.hook.util.safeHook
import vip.chener.mixflipmod.model.OptionValue
import vip.chener.mixflipmod.model.PrefSpec
import vip.chener.mixflipmod.model.appConfigs
import vip.chener.mixflipmod.module
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

abstract class BaseHook {
    abstract val targetPackages: List<String>

    open fun hook(param: PackageReadyParam) {
        val cfg = appConfigs.firstOrNull { it.packageName == param.packageName } ?: return
        val prefs = module!!.getRemotePreferences(Prefs.NAME)
        cfg.prefs
            .filter { spec ->
                when (spec) {
                    is PrefSpec.Switch -> prefs.getBoolean(spec.prefKey, false)
                    is PrefSpec.OptionSelect -> {
                        val stored = when (spec.defaultValue) {
                            is OptionValue.Str -> {
                                val v = prefs.getString(spec.prefKey, spec.defaultValue.value)
                                if (v != null) OptionValue.Str(v) else spec.defaultValue
                            }
                            is OptionValue.IntVal -> OptionValue.IntVal(prefs.getInt(spec.prefKey, spec.defaultValue.value))
                            is OptionValue.FloatVal -> OptionValue.FloatVal(prefs.getFloat(spec.prefKey, spec.defaultValue.value))
                        }
                        stored != spec.defaultValue
                    }
                    else -> false
                }
            }
            .forEach { spec ->
                safeHook("[${param.packageName}] ${spec.prefKey}") {
                    setupHooks(spec.prefKey, param)
                }
            }
    }

    protected open fun setupHooks(prefKey: String, param: PackageReadyParam) {}
}
