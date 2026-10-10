import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// valores de esta maquina (local.properties no se commitea), como un .env
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// url del central en produccion
val urlProduccion = "" // TODO: la de Elastic Cloud cuando exista, por ejemplo https://.../carga-uy/api/

// el release no se arma sin una url de produccion valida: si no, saldria un apk que no conecta a nada.
// se chequea al armar el release y no al configurar, para que los builds debug sigan andando sin ella
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val url = urlProduccion
    doFirst {
        check(url.startsWith("https://") && url.endsWith("/")) {
            "Falta la URL de produccion en app/build.gradle.kts (urlProduccion): tiene que empezar con https:// y terminar en /"
        }
    }
}

android {
    namespace = "uy.tse.cargauy"
    compileSdk = 37

    defaultConfig {
        applicationId = "uy.tse.cargauy"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // url inicial del servidor; en debug se puede cambiar desde Configuracion sin recompilar
            buildConfigField("String", "URL_SERVIDOR", "\"${propiedadesLocales.getProperty("cargauy.url", "")}\"")
        }
        release {
            // fija: en release no se muestra ni se lee la url de Configuracion
            buildConfigField("String", "URL_SERVIDOR", "\"$urlProduccion\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}