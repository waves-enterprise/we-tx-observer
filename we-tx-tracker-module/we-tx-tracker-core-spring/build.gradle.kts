
dependencies {
    implementation("com.wavesenterprise:we-node-client-domain")
    implementation("com.wavesenterprise:we-node-client-blocking-client")
    implementation("com.wavesenterprise:we-node-client-json")
    implementation("com.wavesenterprise:we-node-client-feign-client")
    api(project(":we-tx-tracker-module:we-tx-tracker-api"))
    api(project(":we-tx-tracker-module:we-tx-tracker-jpa"))
    api(project(":we-tx-observer-common-components"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template")
    implementation("net.javacrumbs.shedlock:shedlock-spring")
}
