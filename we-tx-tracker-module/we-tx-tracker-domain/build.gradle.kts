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
    implementation("com.vladmihalcea:hibernate-types-52")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

    kapt("org.hibernate:hibernate-jpamodelgen")
}
