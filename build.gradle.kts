import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    id("application")
}

group = "archives.tater.discordito"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.kord:kord-core:${properties["kord_version"]}")
    implementation("io.github.cdimascio:dotenv-kotlin:${properties["dotenv_version"]}")
    implementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

application {
    mainClass = "archives.tater.discordito.Main"
}