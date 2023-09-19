plugins {
    kotlin("plugin.spring")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")

    implementation("com.wavesenterprise:we-flyway-starter")
    implementation("com.wavesenterprise:we-node-client-blocking-client")
    implementation("com.wavesenterprise:we-node-client-json")
    implementation("com.wavesenterprise:we-node-client-feign-client")
    implementation("com.wavesenterprise:we-node-client-grpc-blocking-client")
    implementation("com.wavesenterprise:we-starter-node-client")

    api("net.javacrumbs.shedlock:shedlock-provider-jdbc-template")
    api("net.javacrumbs.shedlock:shedlock-spring")
    compileOnly("io.micrometer:micrometer-core")
    compileOnly("org.springframework.boot:spring-boot-actuator-autoconfigure")

    api(project(":we-tx-observer-module:we-tx-observer-core-spring"))
    api(project(":we-tx-observer-common-components"))

    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.postgresql:postgresql")
    testImplementation("org.testcontainers:postgresql")

    testImplementation("org.flywaydb:flyway-core")
    testImplementation("com.wavesenterprise:we-node-domain-test")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.ninja-squad:springmockk")
    testImplementation("org.awaitility:awaitility")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.hamcrest:hamcrest-library")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    kapt("org.springframework.boot:spring-boot-configuration-processor") // Generatwe configuration metadata

    // For configuration metadata lookup by IDEA
    // Cause of not integrated with kapt (https://youtrack.jetbrains.com/issue/KT-15040)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
