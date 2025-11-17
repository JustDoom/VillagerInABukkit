plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.imjustdoom.villagerinabucket"
if (!project.hasProperty(" ")) {
    fun getShortCommitHash(): Provider<String> = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }
    version = "${rootProject.version}-${getShortCommitHash().get()}"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
}

tasks {
    javadoc {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            links("https://jd.papermc.io/paper/1.21.6/")
            title = "${project.name} ${project.version} API"
            addStringOption("Xdoclint:none", "-quiet") // Suppress warnings for missing Javadoc
        }
    }

    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-bukkit:3.1.0"))
            include(dependency("org.bstats:bstats-base:3.1.0"))
        }
        relocate("org.bstats", project.group.toString() + ".bstats")
        archiveFileName.set("${rootProject.name}-paper-${project.version}.jar")
    }

    processResources {
        val pluginName = rootProject.name
        val pluginVersion = project.version
        val pluginGroup = project.group

        filesMatching("**/plugin.yml") {
            expand(
                "name" to pluginName,
                "version" to pluginVersion,
                "group" to pluginGroup
            )
        }
    }

    runServer {
        minecraftVersion("1.21.10")
        downloadPlugins {
            modrinth("luckperms", "v5.4.145-bukkit")
        }
        dependsOn(shadowJar)
    }

    build {
        dependsOn(shadowJar)
        dependsOn(javadoc)
    }
}

publishing {
    repositories {
        maven {
            name = "imjustdoom"
            url = uri("https://repo.imjustdoom.com/releases")
            credentials {
                username = System.getenv("MAVEN_NAME")
                password = System.getenv("MAVEN_SECRET")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            version = project.version.toString()
            from(components["java"])
        }
    }
}