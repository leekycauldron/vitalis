/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
      val f = rootProject.file("local.properties")
      if (f.exists()) f.inputStream().use { load(it) }
    }

fun localProp(key: String): String = localProperties.getProperty(key).orEmpty()

android {
  namespace = "com.vitalis"
  compileSdk = 35

  buildFeatures { buildConfig = true }

  defaultConfig {
    applicationId = "com.vitalis"
    minSdk = 31
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }

    // Meta Wearables Device Access Toolkit Setup
    // Without Developer Mode, these values need to be set with credentials from the app registered
    // in Wearables Developer Center
    manifestPlaceholders["mwdat_application_id"] = ""
    manifestPlaceholders["mwdat_client_token"] = ""

    buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProp("ANTHROPIC_API_KEY")}\"")
    buildConfigField("String", "PEXELS_API_KEY", "\"${localProp("PEXELS_API_KEY")}\"")
    buildConfigField("String", "ELEVENLABS_API_KEY", "\"${localProp("ELEVENLABS_API_KEY")}\"")
    buildConfigField("String", "ELEVENLABS_VOICE_ID", "\"${localProp("ELEVENLABS_VOICE_ID")}\"")
    buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"${localProp("GOOGLE_PLACES_API_KEY")}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions { jvmTarget = "1.8" }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  signingConfigs {
    getByName("debug") {
      storeFile = file("sample.keystore")
      storePassword = "sample"
      keyAlias = "sample"
      keyPassword = "sample"
    }
  }
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.material3)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.mwdat.core)
  implementation(libs.mwdat.camera)
  implementation(libs.mwdat.mockdevice)
  implementation(libs.okhttp)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.coil.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.play.services.location)
  androidTestImplementation(libs.androidx.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.uiautomator)
  androidTestImplementation(libs.androidx.test.rules)
}
