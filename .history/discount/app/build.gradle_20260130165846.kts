plugins {
    alias(libs.plugins.kotlin.jvm)
    kotlin("plugin.serialization") version "2.2.20"
    application
}
repositories {
    mavenCentral()
    mavenLocal()
}
dependencies {
    implementation(libs.bundles.common)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.database)
    testImplementation(libs.bundles.ktor.test)
    testImplementation(kotlin("test"))
}
java { toolchain { languageVersion = JavaLanguageVersion.of(22) } }
application { mainClass = "io.nexure.discount.ApplicationKt" }

tasks {
    test {
        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }
    }
}
