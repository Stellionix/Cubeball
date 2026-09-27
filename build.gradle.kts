import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.3.2"
}

group = "my.crylonz"
version = "2.1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    val spigotApi = "org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT"

    compileOnly(spigotApi)
    testImplementation(spigotApi)
    implementation("org.bstats:bstats-bukkit:3.0.0")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    enabled = false
}

tasks.withType<ShadowJar>().configureEach {
    archiveClassifier.set("")
    relocate("org.bstats", "me.crylonz")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
