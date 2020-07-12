plugins {
    java
    scala
}

dependencies {
    implementation(rootProject.extra["depScalaLang"] as String)
    implementation(rootProject.extra["depScalaReflectionLang"] as String)
//    implementation(project(":lobster-utils"))
    testImplementation(rootProject.extra["depScalaTest"] as String)
    testImplementation(rootProject.extra["depJunit"] as String)
    testImplementation(rootProject.extra["depScalaTestJunitPlugin"] as String)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

