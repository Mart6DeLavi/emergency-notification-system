import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
}

group = "com.sensa"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"
    extra["postgresql.version"] = "42.7.11"
    extra["bouncycastle.version"] = "1.85"
    extra["tomcat.version"] = "10.1.59"
    extra["spring-framework.version"] = "6.2.19"
    extra["spring-data-bom.version"] = "3.5.12"
    extra["spring-kafka.version"] = "3.3.16"
    extra["jackson-bom.version"] = "2.18.11"
    extra["micrometer.version"] = "1.15.12"
    extra["netty.version"] = "4.1.137.Final"
    extra["httpclient5.version"] = "5.4.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    implementation("ai.djl:api:0.31.1")
    implementation("ai.djl:model-zoo:0.31.1")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.31.1")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${extra["springCloudVersion"]}")
        mavenBom("software.amazon.awssdk:bom:2.29.30")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:33.7.2-jre")
        force("com.thoughtworks.xstream:xstream:1.4.21")
        force("org.xerial.snappy:snappy-java:1.1.10.9")
        force("org.lz4:lz4-java:1.8.1")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.apache.httpcomponents.core5:httpcore5:5.4.3")
        force("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
    }
}
