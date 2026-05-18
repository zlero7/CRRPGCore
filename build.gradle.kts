plugins {
    kotlin("jvm") version "2.3.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "io.zlero"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnly(files("libs/CRGuild.jar"))
    compileOnly(files("libs/CRFramework-1.0.0.jar"))
    compileOnly(files("libs/Vault.jar"))
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks {
    runServer {
        minecraftVersion("1.20")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("CRRPGCore-${project.version}.jar")

        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
            exclude(dependency("org.jetbrains:annotations"))
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    jar {
        enabled = false
    }

    assemble {
        dependsOn(shadowJar)
    }
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}