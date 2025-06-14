plugins {
    `java-library`
    `maven-publish`

    alias(libs.plugins.lavalink)
    id("com.github.johnrengelman.shadow")
    id("com.github.breadmoirai.github-release")
}

val pluginVersion = Version(1, 7, 0)

group = "com.dunctebot"
version = "$pluginVersion"
val archivesBaseName = "skybot-lavalink-plugin"
val preRelease = System.getenv("PRERELEASE") == "true"
val verName = "${if (preRelease) "PRE_" else ""}$pluginVersion${if(preRelease) "_${System.getenv("GITHUB_RUN_NUMBER")}" else ""}"


lavalinkPlugin {
    name = "DuncteBot-plugin"
    path = "$group.lavalinkplugin"
    version = verName
    apiVersion = libs.versions.lavalink.api
//    serverVersion = gitHash(libs.versions.lavalink.server)
    serverVersion = libs.versions.lavalink.server
}

dependencies {
    implementation(projects.sourceManagers)
}

// make sure that we can resolve the dependencies
val impl = project.configurations.implementation.get()
impl.isCanBeResolved = true

tasks {
    jar {
        archiveBaseName.set(archivesBaseName)
    }
    shadowJar {
        archiveBaseName.set(archivesBaseName)
        archiveClassifier.set("")

        configurations = listOf(impl)
    }
    build {
        dependsOn(processResources)
        dependsOn(compileJava)
        dependsOn(shadowJar)
    }
    publish {
        dependsOn(publishToMavenLocal)
    }
}

tasks.githubRelease {
    dependsOn(tasks.jar)
    dependsOn(tasks.shadowJar)
    mustRunAfter(tasks.shadowJar)
}

data class Version(val major: Int, val minor: Int, val patch: Int) {
    override fun toString() = "$major.$minor.$patch"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.infnibor"
            artifactId = archivesBaseName
            version = "$version"
        }
    }
    repositories {
        maven {
            url = if (preRelease) {
                uri("https://maven.pcreators.pl/snapshots")
            } else {
                uri("https://maven.pcreators.pl/releases")
            }
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    owner("infnibor")
    repo("skybot-lavalink-plugin")
    targetCommitish(System.getenv("RELEASE_TARGET"))
    releaseAssets(tasks.shadowJar.get().outputs.files.toList())
    tagName(verName)
    releaseName(verName)
    overwrite(false)
    prerelease(preRelease)

    if (preRelease) {
        body("""This is a pre-release version. Please do not use it in production. To use it set `snapshot` to true in your plugin configuration.
            |Example:
            |```yml
            |lavalink:
            |    plugins:
            |        - dependency: "com.infnibor:skybot-lavalink-plugin:$verName"
            |          snapshot: true
            |```
        """.trimMargin())
    } else {
        body(changelog())
    }
}
