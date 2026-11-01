import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---------------------------------------------------------------------------
// Segredos locais (nunca commitados): local.properties e keystore.properties
// ---------------------------------------------------------------------------
fun loadProps(name: String): Properties = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

val localProps = loadProps("local.properties")
val keystoreProps = loadProps("keystore.properties")
val tmdbApiKey: String = localProps.getProperty("TMDB_API_KEY")
    ?: System.getenv("TMDB_API_KEY")
    ?: ""
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null
// URL de uma lista real para testes manuais. Só entra no build debug; nunca no release nem no git.
val iptvTestUrl: String = localProps.getProperty("IPTV_TEST_URL") ?: System.getenv("IPTV_TEST_URL") ?: ""

// ---------------------------------------------------------------------------
// Versão derivada da tag git: vX.Y.Z -> versionName "X.Y.Z", versionCode X*10000+Y*100+Z.
// Commits após a tag viram "X.Y.Z-dev.N"; sem nenhuma tag, "0.0.0-dev".
// ---------------------------------------------------------------------------
val gitDescribe: String = providers.exec {
    commandLine("sh", "-c", "git describe --tags --match 'v*' --long 2>/dev/null || true")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim()

val describeRegex = Regex("""^v(\d+)\.(\d+)\.(\d+)-(\d+)-g[0-9a-f]+$""")
val (appVersionName, appVersionCode) = describeRegex.find(gitDescribe)?.let { m ->
    val (major, minor, patch, ahead) = m.destructured
    val base = "$major.$minor.$patch"
    val name = if (ahead == "0") base else "$base-dev.$ahead"
    name to (major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt())
} ?: ("0.0.0-dev" to 1)

android {
    namespace = "com.wpdas.primetv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wpdas.primetv"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        // APK universal com as duas ABIs das TV Boxes comuns.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "GITHUB_REPO", "\"wpdas/prime-tv\"")
        buildConfigField("String", "IPTV_TEST_URL", "\"\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "IPTV_TEST_URL", "\"${iptvTestUrl.replace("\"", "\\\"")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time e afins em API 24/25.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE*")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose + TV
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Player
    implementation(libs.bundles.media3)

    // Persistência
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.tink.android)

    // Rede / serialização / imagens / QR
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.bundles.network)
    implementation(libs.bundles.coil)
    implementation(libs.zxing.core)

    // Testes unitários
    testImplementation(libs.bundles.unit.test)

    // Testes instrumentados
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
