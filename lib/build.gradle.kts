plugins {
   id("com.android.library")
   id("org.jetbrains.kotlin.android")
   `maven-publish`
}

val libGroupId = "com.sd.lib.android"
val libArtifactId = "compose-layer"
val libVersion = "1.0.0-beta07"

android {
   namespace = "com.sd.lib.compose.layer"
   compileSdk = libs.versions.androidCompileSdk.get().toInt()
   defaultConfig {
      minSdk = 21
   }

   kotlinOptions {
      freeCompilerArgs += "-module-name=$libGroupId.$libArtifactId"
   }

   buildFeatures {
      compose = true
   }
   composeOptions {
      kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
   }

   publishing {
      singleVariant("release") {
         withSourcesJar()
      }
   }
}

kotlin {
   jvmToolchain(8)
}

dependencies {
   implementation(libs.androidx.compose.foundation)
   implementation(libs.androidx.activity.compose)
   implementation(libs.sd.aligner)
}

publishing {
   publications {
      create<MavenPublication>("release") {
         groupId = libGroupId
         artifactId = libArtifactId
         version = libVersion

         afterEvaluate {
            from(components["release"])
         }
      }
   }
}