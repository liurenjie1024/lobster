plugins {
    scala
}

dependencies {
    implementation(rootProject.extra["depScalaLang"] as String)
    implementation(rootProject.extra["depScalaReflectionLang"] as String)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

