import com.android.build.api.variant.FilterConfiguration
import io.gitlab.arturbosch.detekt.Detekt

plugins {
  alias(libs.plugins.ksp)
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.room)
  alias(libs.plugins.detekt)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.kotlinx.serialization)
}

android {
  namespace = "com.noxtan.player"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.noxtan.player"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0.0"

    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "GIT_SHA", "\"${getCommitSha()}\"")
    buildConfigField("int", "GIT_COUNT", getCommitCount())
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
    create("preview") {
      initWith(getByName("release"))
      signingConfig = signingConfigs["debug"]
      applicationIdSuffix = ".preview"
      versionNameSuffix = "-${getCommitCount()}"
    }
    named("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-${getCommitCount()}"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17 // 💡 17 က Cloud ရဲ့ 11 ထက် ပိုကောင်းပါတယ်
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  val abiCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2,
    "x86" to 3,
    "x86_64" to 4,
  )

  androidComponents {
    onVariants { variant ->
      variant.outputs.forEach { output ->
        val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
        output.versionCode.set((output.versionCode.orNull ?: 0) * 10 + (abiCodes[abi] ?: 0))
      }
    }
  }

  androidResources {
    generateLocaleConfig = true
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/jni/CMakeLists.txt")
      version = "4.1.2"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  // 💡 အသုံးမပြုတော့တဲ့ Library (၃) ခုကို ဖြုတ်ထားပါပြီ
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3.android)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.compose.foundation.layout)
  debugImplementation(libs.androidx.ui.tooling)

  implementation(libs.bundles.compose.navigation3)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.constraintlayout)
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.material)
  implementation(libs.androidx.preferences.ktx)
  implementation(libs.androidx.documentfile)
  implementation(libs.mediasession)
  implementation(libs.saveable)

  implementation(libs.mpv.lib)

  implementation(platform(libs.koin.bom))
  implementation(libs.bundles.koin)

  implementation(libs.seeker)
  implementation(libs.compose.prefs)
  implementation(libs.bundles.about.libs)
  implementation(libs.simple.icons)

  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.detekt.gradle.plugin)
  detektPlugins(libs.detekt.rules.compose)
  detektPlugins(libs.detekt.formatter)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.fsaf)
  implementation(libs.coil.compose)
}

detekt {
  parallel = true
  allRules = false
  buildUponDefaultConfig = true
  config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
  setSource(files(project.projectDir))
  exclude("**/build/**")
  reports {
    html.required.set(true)
    md.required.set(true)
  }
}

fun getCommitCount(): String {
  return "1"
}
fun getCommitSha(): String {
  return "12345"
}
fun runCommand(command: String): String {
  return ""
}

aboutLibraries {
  excludeFields = arrayOf("generated")
}
