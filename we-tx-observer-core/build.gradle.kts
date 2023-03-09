import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
