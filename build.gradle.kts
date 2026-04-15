// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
// Source - https://stackoverflow.com/a/72382190
// Posted by Sergio, modified by community. See post 'Timeline' for change history
// Retrieved 2026-04-14, License - CC BY-SA 4.0

buildscript {
    //...
    dependencies {
        classpath 'com.android.tools.build:gradle:8.9.0'

        // ...
    }
}
