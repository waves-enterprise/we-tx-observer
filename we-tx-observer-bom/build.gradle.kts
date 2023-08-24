plugins {
    `maven-publish`
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        project.rootProject.subprojects.forEach { project ->
            api(project)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("we-tx-observer-bom") {
            from(components["javaPlatform"])
        }
    }
}
