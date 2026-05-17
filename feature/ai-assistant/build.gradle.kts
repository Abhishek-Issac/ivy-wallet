plugins {
    id("ivy.feature")
    id("ivy.room")
}

android {
    namespace = "com.ivy.aiassistant"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)
    implementation(projects.shared.data.core)

    implementation(libs.bundles.ktor)
    implementation(libs.datastore)
    implementation(libs.androidx.security)

    testImplementation(projects.shared.ui.testing)
}
