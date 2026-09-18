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
            // Chỉ mượn lại phần thuần Kotlin của app; CabinGuardApplication.kt cần
            // Android SDK + Hilt nên không đưa vào module JVM này.
            kotlin.srcDir("../app/src/main/java")
            kotlin.include("com/example/cabinguard/CabinGuardCli.kt")
            kotlin.include("com/example/cabinguard/data/model/CabinTelemetry.kt")
            kotlin.include("com/example/cabinguard/domain/sensor/CabinSensorEngine.kt")
            kotlin.include("com/example/cabinguard/domain/sensor/CabinThresholds.kt")
        }
    }
}

application {
    mainClass.set("com.example.cabinguard.CabinGuardCliKt")
}

dependencies {
    implementation("androidx.room:room-common:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // CabinSensorEngine dùng @Inject/@Singleton nên module JVM cũng cần annotation này.
    implementation("javax.inject:javax.inject:1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
