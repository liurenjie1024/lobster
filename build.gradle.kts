group = "io.github.lobster"
version = "0.0.1-SNAPSHOT"

val scalaBinaryVersion = "2.12"
val scalaVersion = "2.12.10"
val scalaTestVersion = "3.2.0"
val junitVersion = "4.13"

val depScalaLang by project.extra("org.scala-lang:scala-library:${scalaVersion}")
val depScalaReflectionLang by project.extra("org.scala-lang:scala-reflect:${scalaVersion}")
val depScalaTest by project.extra("org.scalatest:scalatest_${scalaBinaryVersion}:${scalaTestVersion}")
val depJunit by project.extra("junit:junit:${junitVersion}")
val depScalaTestJunitPlugin by project.extra("org.scalatestplus:junit-4-12_${scalaBinaryVersion}:3.2.0.0")

allprojects {
    repositories {
        mavenCentral()
    }
}


