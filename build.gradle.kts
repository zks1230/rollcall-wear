buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.com.android.application) apply(false)
    alias(libs.plugins.compose.compiler) apply false
}
