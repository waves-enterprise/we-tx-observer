plugins {
    kotlin("kapt")
    kotlin("plugin.spring")
}

dependencies {
    implementation(kotlin("stdlib"))

    api(project(":we-tx-tracker-module:we-tx-tracker-domain"))
    api(project(":we-tx-tracker-module:we-tx-tracker-api"))
    api(project(":we-tx-observer-common-components"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.postgresql:postgresql")
    testImplementation("com.playtika.testcontainers:embedded-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":we-tx-tracker-module:we-tx-tracker-jpa"))
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
