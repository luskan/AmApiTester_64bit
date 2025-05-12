plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    // compile and emit classes compatible with Java 7
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(7))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.java.dev.jna:jna:5.13.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    // Set the jar name to apitester without version:
    archiveBaseName.set("apitester")
    archiveVersion.set("")

    // Tell Gradle which class is your entry point:
    manifest {
        attributes["Main-Class"] = "org.example.Main" // Java main class
    }

    // include compiled classes and resources:
    from(sourceSets.main.get().output)

    // unpack and include all runtime dependencies:
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    // avoid duplicate files (e.g. META-INF services)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
