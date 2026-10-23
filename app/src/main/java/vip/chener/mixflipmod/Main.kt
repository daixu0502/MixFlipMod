package vip.chener.mixflipmod

import vip.chener.mixflipmod.hook.FlipHomeHook
import vip.chener.mixflipmod.hook.HideOuterHook
import vip.chener.mixflipmod.hook.PersonalAssistantHook
import vip.chener.mixflipmod.hook.SogouHook
import vip.chener.mixflipmod.hook.SystemHook
import vip.chener.mixflipmod.hook.SystemUIHook
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

internal var module: Main? = null

class Main : XposedModule() {

    private val hooks = listOf(
        FlipHomeHook,
        SystemUIHook,
        HideOuterHook,
        SogouHook,
        PersonalAssistantHook,
    )

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        SystemHook.hook(param)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage) return
        hooks.filter { param.packageName in it.targetPackages }.forEach { it.hook(param) }
    }
}
