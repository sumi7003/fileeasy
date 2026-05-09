import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application") version "8.4.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("org.jetbrains.kotlin.kapt") version "1.9.22"
}

android {
    namespace = "com.xplay.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xplay.player"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "1.1.3-Build11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "product"

    productFlavors {
        create("xplay") {
            dimension = "product"
            applicationId = "com.xplay.player"
            buildConfigField("boolean", "IS_FILEEASY", "false")
            buildConfigField("boolean", "PLAYER_FEATURE_ENABLED", "true")
            buildConfigField("String", "PRODUCT_NAME", "\"Xplay\"")
            buildConfigField("String", "CONTROL_CENTER_NAME", "\"控制中心\"")
            buildConfigField("String", "LOGIN_SUBTITLE", "\"欢迎进入 Xplay 终端管理系统\"")
            buildConfigField("String", "ADMIN_ENTRY_LABEL", "\"管理素材与播放列表\"")
            buildConfigField("String", "SERVER_NOTIFICATION_NAME", "\"Xplay Server\"")
        }

        create("fileeasy") {
            dimension = "product"
            applicationId = "com.xplay.fileeasy"
            versionName = "V1.0"
            buildConfigField("boolean", "IS_FILEEASY", "true")
            buildConfigField("boolean", "PLAYER_FEATURE_ENABLED", "false")
            buildConfigField("String", "PRODUCT_NAME", "\"易传输\"")
            buildConfigField("String", "CONTROL_CENTER_NAME", "\"文件服务\"")
            buildConfigField("String", "LOGIN_SUBTITLE", "\"欢迎进入易传输局域网文件服务\"")
            buildConfigField("String", "ADMIN_ENTRY_LABEL", "\"进入文件管理后台\"")
            buildConfigField("String", "SERVER_NOTIFICATION_NAME", "\"易传输服务\"")
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

val syncWebAdminAssets by tasks.registering(Copy::class) {
    val webAdminDist = layout.projectDirectory.dir("../web-admin/dist")
    val targetAssetsDir = layout.projectDirectory.dir("src/main/assets/web-admin")

    from(webAdminDist)
    into(targetAssetsDir)

    doFirst {
        val distDir = webAdminDist.asFile
        require(distDir.exists()) {
            "Missing web-admin dist assets at ${distDir.absolutePath}. Run `npm run build` in apps/web-admin first."
        }
    }
}

tasks.matching { task ->
    task.name == "preBuild" || task.name.startsWith("pre") && task.name.endsWith("Build")
}.configureEach {
    dependsOn(syncWebAdminAssets)
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Media3 (ExoPlayer replacement)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    
    // Coil for Images
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // Retrofit for API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Ktor Server
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion") // 某些环境下需要
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // ZXing for QR Code
    implementation("com.google.zxing:core:3.5.2")

    testImplementation("junit:junit:4.13.2")
}
