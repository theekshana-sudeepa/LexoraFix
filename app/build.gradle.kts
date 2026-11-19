import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Load local.properties for sensitive keys
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.lexoratech.lexorafix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lexoratech.lexorafix"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // PayHere Configuration (from local.properties)
        buildConfigField("String", "PAYHERE_MERCHANT_ID",
            "\"${localProps.getProperty("payhere.merchant.id", "")}\"")
        buildConfigField("String", "PAYHERE_MERCHANT_SECRET",
            "\"${localProps.getProperty("payhere.merchant.secret", "")}\"")
        buildConfigField("boolean", "PAYHERE_SANDBOX",
            localProps.getProperty("payhere.sandbox", "true").toBoolean().toString())

        // Email Configuration (from local.properties)
        buildConfigField("String", "EMAIL_SENDER_ADDRESS",
            "\"${localProps.getProperty("email.sender.address", "")}\"")
        buildConfigField("String", "EMAIL_APP_PASSWORD",
            "\"${localProps.getProperty("email.app.password", "")}\"")
        buildConfigField("String", "ADMIN_EMAIL",
            "\"${localProps.getProperty("admin.email", "")}\"")
        // Maps API Key (from local.properties)
        manifestPlaceholders["MAPS_API_KEY"] = localProps.getProperty("maps.api.key", "")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production PayHere
            buildConfigField("boolean", "PAYHERE_SANDBOX", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Exclude duplicate LICENSE files from JavaMail
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md", "META-INF/LICENSE", "META-INF/NOTICE")
        }
    }
}


dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.viewpager2)
    implementation(libs.swiperefreshlayout)

    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

    // Image Loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // JSON Parsing
    implementation(libs.gson)

    // Image Cropper (uCrop)
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Animated Bottom Bar
    implementation("nl.joery.animatedbottombar:library:1.1.0")

    // PayHere Payment Gateway
    implementation("com.github.PayHereDevs:payhere-android-sdk:v3.0.17")

    // JavaMail — Email sending via SMTP (Google App Password)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // Biometric & Security
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Lombok — annotation-driven getters/setters/constructors for model classes
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
