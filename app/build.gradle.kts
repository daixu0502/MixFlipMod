import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.kotlin.compose)
}

apksign {
    storeFileProperty = "androidStoreFile"
    storePasswordProperty = "androidStorePassword"
    keyAliasProperty = "androidKeyAlias"
    keyPasswordProperty = "androidKeyPassword"
}


android {
    namespace = "vip.chener.mixflipmod"
    compileSdk = 37

    defaultConfig {
        applicationId = "vip.chener.mixflipmod"
        minSdk = 35
        targetSdk = 36
        versionCode = 30300
        versionName = "3.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("unminified") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

// CI reads resolved values so the workflow follows app versions and SDK changes.
val releaseMetadata = mapOf(
    "versionName" to requireNotNull(android.defaultConfig.versionName),
    "versionCode" to requireNotNull(android.defaultConfig.versionCode),
    "compileSdk" to requireNotNull(android.compileSdk),
    "compileSdkMinor" to (android.compileSdkMinor ?: 0),
    "buildToolsVersion" to android.buildToolsVersion,
)
val releaseMetadataFile = layout.buildDirectory.file("release-metadata.json")
tasks.register("writeReleaseMetadata") {
    group = "publishing"
    description = "Exports the app version and SDK requirements for GitHub Releases."
    inputs.properties(releaseMetadata)
    outputs.file(releaseMetadataFile)
    doLast {
        releaseMetadataFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(groovy.json.JsonOutput.toJson(releaseMetadata))
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            require(output is VariantOutputImpl)

            val vName = output.versionName.get()
            val vCode = output.versionCode.get()

            output.outputFileName.set("MixFlipMod_${vName}_${vCode}.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.dexkit)
}
