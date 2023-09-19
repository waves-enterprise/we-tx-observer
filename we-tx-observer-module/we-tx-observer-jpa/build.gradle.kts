plugins {
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    api(project(":we-tx-observer-module:we-tx-observer-domain"))
    api(project(":we-tx-observer-common-components"))

    implementation("com.wavesenterprise:we-node-client-json")
    implementation("com.wavesenterprise:we-flyway-starter")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.vladmihalcea:hibernate-types-52")

    testImplementation("org.postgresql:postgresql")
    testImplementation("org.testcontainers:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.wavesenterprise:we-node-domain-test")

    kapt("org.hibernate:hibernate-jpamodelgen") // Generate JPA Static Metamodel
}
