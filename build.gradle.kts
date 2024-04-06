import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
}

group = "me.emerald"
version = "0.0.1"

repositories {
    mavenCentral()
}
base {
    archivesBaseName = "Finlandbot"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation("net.dv8tion:JDA:5.0.0-beta.21")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("FinlandBot")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "main"))
        }
    }
}

tasks {
    testClasses {
        dependsOn(shadowJar)
    }
}

