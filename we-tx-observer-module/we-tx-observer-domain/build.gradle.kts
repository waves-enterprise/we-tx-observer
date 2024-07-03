plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":we-tx-observer-common-components"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63")

    kapt("org.hibernate.orm:hibernate-jpamodelgen")
}
