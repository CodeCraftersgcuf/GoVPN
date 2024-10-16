plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
 
}

android {
    namespace = "com.example.privatevpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.privatevpn"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.29")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.ads.lite)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // MapView and location services
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Network libraries
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.google.android.material:material:1.6.1")

    implementation ("com.airbnb.android:lottie:6.5.2")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")

    // Add the OpenVPN .aar file directly from the libs folder
    implementation(files("libs/ics-openvpn-0.7.22.aar"))

    implementation("com.google.android.gms:play-services-ads:23.4.0")


    implementation ("com.google.android.gms:play-services-location:21.3.0")

    implementation ("com.google.android.gms:play-services-wallet:19.4.0")



}
