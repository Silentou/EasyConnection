plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")    // Add this plugin
    id("signing")
}

android {
    namespace = "com.kamesh.easyconnectionsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Activity & Lifecycle
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.kamesh.easyconnectionsdk"
                artifactId = "easyconnectionsdk"
                version = "1.0.0"

                pom {
                    name.set("EasyConnection SDK")
                    description.set("A simple SDK for managing connections in Android apps.")
                    url.set("https://github.com/Silentou/EasyConnection")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("kamesh")
                            name.set("Kamesh")
                            email.set("kameshrajanitha@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:github.com/Silentou/EasyConnection.git")
                        developerConnection.set("scm:git:ssh://github.com/Silentou/EasyConnection.git")
                        url.set("https://github.com/Silentou/EasyConnection")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("ossrhUsername") as String?
                    password = project.findProperty("ossrhPassword") as String?
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
}
