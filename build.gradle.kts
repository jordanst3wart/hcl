import org.jreleaser.model.Active

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    `maven-publish`
    signing
    id("org.jreleaser") version "1.16.0"
    // TODO use dokka plugin for docs
    // id("org.jetbrains.dokka") version "1.9.20"
}
// borrowed a lot of setup from: https://github.com/NJAldwin/maven-central-test/blob/master/build.gradle.kts
// https://github.com/NJAldwin/jvm-library-template

group = "bot.stewart"
version = "0.2.0"

val repoName = "hcl"
val repoDescription = "A HCL parser and writer for tfvar files"
val repoUser = "jordanst3wart"
val repoAuthor = "Jordan Stewart"
val repoUrl = "https://github.com/$repoUser/$repoName"
// match semver `x.y.z-something`
val isPrereleasePattern = """\d+\.\d+\.\d+-.+"""

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain(21)
}

// TODO i don't know if this is needed either
tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

tasks.named("jreleaserFullRelease") {
    dependsOn("publish")
}

// hack to create directory for jreleaser
tasks.named("publish") {
    doFirst {
        val outputDir = layout.buildDirectory.dir("jreleaser").get().asFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()

            from(components["java"])

            pom {
                name.set(repoName)
                description.set(repoDescription)
                url.set("https://github.com/jordanst3wart/hcl")
                // url.set(rootProject.jreleaser.project.links.homepage)

                inceptionYear.set("2025")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("jordanst3wart")
                        name.set(repoAuthor)
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/jordanst3wart/hcl.git")
                    // I don't feel like this should be needed
                    developerConnection.set("scm:git:ssh://github.com/jordanst3wart/hcl.git")
                    url.set("https://github.com/jordanst3wart/hcl")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    // dryrun.set(System.getenv("CI").isNullOrBlank())

    project {
        name.set(repoName)
        description.set(repoDescription)
        version.set(rootProject.version.toString())
        authors.set(listOf(repoAuthor))
        license.set("MIT")
        inceptionYear.set("2025")
        links {
            homepage = repoUrl
        }
    }

    release {
        github {
            repoOwner.set(repoUser)
            name.set(repoName)
            branch.set("main")

            // skip tag because we're running release on tag creation
            skipTag.set(true)
            prerelease {
                pattern.set(isPrereleasePattern)
            }
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
        verify.set(true)
    }

    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active.set(Active.ALWAYS)
                active.set(Active.ALWAYS)
                url.set("https://central.sonatype.com/api/v1/publisher")
                stagingRepositories.add("${layout.buildDirectory.get()}/staging-deploy")
                // snapshotSupported.set(true)
                // applyMavenCentralRules.set(true)
                retryDelay.set(90)
                maxRetries.set(5)
            }
        }
    }

    distributions {
        create(repoName) {
            project {
                description.set(repoDescription)
            }
            artifact {
                path.set(tasks.named<Jar>("jar").get().archiveFile.get().asFile)
            }
            artifact {
                path.set(tasks.named<Jar>("sourcesJar").get().archiveFile.get().asFile)
                platform.set("java-sources")
            }
            artifact {
                path.set(tasks.named<Jar>("javadocJar").get().archiveFile.get().asFile)
                platform.set("java-docs")
            }
        }
    }
}

/*
        JRELEASER_GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        JRELEASER_GPG_PASSPHRASE: ${{ secrets.JRELEASER_GPG_PASSPHRASE }}
        JRELEASER_GPG_PUBLIC_KEY: ${{ secrets.JRELEASER_GPG_PUBLIC_KEY }}
        JRELEASER_GPG_SECRET_KEY: ${{ secrets.JRELEASER_GPG_SECRET_KEY }}
        # maven token, or password
        JRELEASER_MAVENCENTRAL_TOKEN: ${{ secrets.JRELEASER_MAVENCENTRAL_TOKEN }}
        JRELEASER_MAVENCENTRAL_USERNAME: ${{ secrets.JRELEASER_MAVENCENTRAL_USERNAME }}
 */
