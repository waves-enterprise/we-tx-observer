

val hibernateTypesVersion: String by project

plugins {
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.vladmihalcea:hibernate-types-52:$hibernateTypesVersion")

    kapt("org.hibernate:hibernate-jpamodelgen") // Generate JPA Static Metamodel
}
