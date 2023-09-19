plugins {
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.vladmihalcea:hibernate-types-52")
    implementation(project(":we-tx-observer-common-components"))
    api(project(":we-tx-tracker-module:we-tx-tracker-domain"))
    implementation("com.wavesenterprise:we-flyway-starter")

    kapt("org.hibernate:hibernate-jpamodelgen") // Generate JPA Static Metamodel
}
