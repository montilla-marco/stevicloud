plugins {
    java
    `project-report`
    id("org.springframework.boot") version "4.1.0" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "project-report")

    repositories {
        mavenCentral()
    }

    group = "org.stevidigital"
    version = "0.1.0-SNAPSHOT"

    java {
        toolchain { languageVersion = JavaLanguageVersion.of(25) }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core:3.27.3")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> { useJUnitPlatform() }
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-parameters"))
    }
}
