plugins {
    java
    `java-test-fixtures`
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "pl.fairydeck"
version = "0.1.0-SNAPSHOT"
description = "Card payment authorization service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")
    testFixturesImplementation("org.testcontainers:testcontainers-postgresql")
}

val testJvmArgs = listOf("-XX:+EnableDynamicAgentLoading", "-Xshare:off")

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
            dependencies {
                implementation(testFixtures(project()))
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
                implementation("com.tngtech.archunit:archunit-junit5:1.5.0")
                implementation("org.wiremock:wiremock-standalone:3.13.2")
            }
            targets {
                all {
                    testTask.configure { jvmArgs(testJvmArgs) }
                }
            }
        }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.testcontainers:testcontainers-junit-jupiter")
                implementation("org.testcontainers:testcontainers-postgresql")
            }
            targets {
                all {
                    testTask.configure {
                        jvmArgs(testJvmArgs)
                        shouldRunAfter(tasks.test)
                    }
                }
            }
        }
    }
}

configurations {
    named("integrationTestImplementation") { extendsFrom(implementation.get()) }
    named("integrationTestRuntimeOnly") { extendsFrom(runtimeOnly.get()) }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
