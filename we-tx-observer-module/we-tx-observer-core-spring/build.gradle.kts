
val hibernateTypesVersion: String by project

plugins {
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
    kotlin("kapt")
}

dependencies {
    implementation("com.wavesenterprise:we-node-client-domain")
    implementation("com.wavesenterprise:we-node-client-blocking-client")
    implementation("com.wavesenterprise:we-node-client-json")
    implementation("com.wavesenterprise:we-node-client-feign-client")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63")
    implementation("com.github.ben-manes.caffeine:caffeine")
    kapt("org.hibernate.orm:hibernate-jpamodelgen")

    implementation("com.frimastudio:slf4j-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("net.javacrumbs.shedlock:shedlock-provider-jdbc-template")
    api("net.javacrumbs.shedlock:shedlock-spring")
    compileOnly("io.micrometer:micrometer-core")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    api(project(":we-tx-observer-module:we-tx-observer-api"))
    api(project(":we-tx-observer-module:we-tx-observer-jpa"))
}
