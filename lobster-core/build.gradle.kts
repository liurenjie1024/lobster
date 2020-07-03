plugins {
    scala
}

dependencies {
    implementation(rootProject.extra["depScalaLang"] as String)
    implementation(project(":lobster-utils"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

