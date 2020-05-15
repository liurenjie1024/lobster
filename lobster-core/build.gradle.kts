plugins {
    scala
}

dependencies {
    implementation(rootProject.extra["depScalaLang"] as String)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

