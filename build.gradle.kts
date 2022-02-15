plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    war
}

group = "projekt.cloud.piece"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
    compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("com.google.code.gson:gson:2.9.0")
    
    val junitVersion = "5.7.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}