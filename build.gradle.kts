plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    // compile and emit classes compatible with Java 7
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7

    // (optional) if you have a JDK 7 installed and want Gradle to pick it up:
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