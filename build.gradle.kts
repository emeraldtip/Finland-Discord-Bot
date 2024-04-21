import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")
}

group = "me.emerald"
version = "0.1.13" //Commit number - 45 for v0.1.x Somehow should get this thing to update with the commit number

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation("net.dv8tion:JDA:5.0.0-beta.21")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("com.github.Fruitloopins:EMCAPIClient:1ab9702b34")
    implementation("commons-io:commons-io:2.16.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("FinlandBot")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "me.emerald.finlandbot.Main"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

