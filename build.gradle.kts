plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.0"
    `java-library`
    signing
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.3"
}

group = "io.github.seggan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion: String by project
dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("com.tfowl.ktor:ktor-jsoup:2.3.0")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    testImplementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

signing {
    useGpgCmd()
}

tasks.publish {
    dependsOn(tasks.clean)
}

centralPortal {
    pom {
        url = "https://github.com/Seggan/kasey"
        licenses {
            license {
                name = "Gnu Lesser General Public License, Version 3"
                url = "https://www.gnu.org/licenses/lgpl-3.0.en.html"
            }
        }
        developers {
            developer {
                name = "Seggan"
                email = "seggan21@gmail.com"
                organization = "N/A"
                organizationUrl = "https://github.com/Seggan"
            }
        }
        scm {
            connection = "scm:git:git://github.com/Seggan/kasey.git"
            developerConnection = "scm:git:ssh://github.com:Seggan/kasey.git"
            url = "https://github.com/Seggan/kasey"
        }
        // whyyyy
        withXml {
            val depsNode = asNode().appendNode("dependencies")
            for (dep in project.configurations.getByName("api").dependencies) {
                val depNode = depsNode.appendNode("dependency")
                depNode.appendNode("groupId", dep.group)
                depNode.appendNode("artifactId", dep.name)
                depNode.appendNode("version", dep.version)
            }
        }
    }
}