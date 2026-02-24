plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
}

group = "com.ai.aidicted"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // ── Koog AI Agent framework ───────────────────────────────────────────────
    implementation("ai.koog:koog-agents:0.6.2")

    // ── Ktor HTTP client (for Slack webhook) ──────────────────────────────────
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

    // ── Kotlinx ───────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")

    // ── Google Play Developer API (reviews) ───────────────────────────────────
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20260129-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.28.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.46.0")

    // ── Logging ───────────────────────────────────────────────────────────────
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("com.ai.aidicted.agent.MainKt")
}

kotlin {
    jvmToolchain(17)
}

// Fat JAR — bundles all dependencies for `java -jar` execution
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.ai.aidicted.agent.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
