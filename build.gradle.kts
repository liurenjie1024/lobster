group = "io.github.lobster"
version = "0.0.1-SNAPSHOT"

val scalaBinaryVersion = "2.12"
val scalaVersion = "2.12.10"

val depScalaLang by project.extra("org.scala-lang:scala-library:${scalaVersion}")

allprojects {
    repositories {
        mavenCentral()
    }
}


