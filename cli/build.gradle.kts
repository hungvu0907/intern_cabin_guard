plugins {
    application
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }

    sourceSets {
        main {
            kotlin.srcDir("../app/src/main/java")
            kotlin.include("com/example/cabinguard/CabinGuardApplication.kt")
            kotlin.include("com/example/cabinguard/data/model/CabinTelemetry.kt")
            kotlin.include("com/example/cabinguard/domain/sensor/CabinSensorEngine.kt")
        }
    }
}

application {
    mainClass.set("com.example.cabinguard.CabinGuardApplicationKt")
}

dependencies {
    implementation("androidx.room:room-common:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
