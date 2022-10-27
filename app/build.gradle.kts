plugins {
    id("com.android.application")
    kotlin("android")
    id("secant.android-build-conventions")
    id("com.github.triplet.play")
    id("com.osacky.fladle")
    id("wtf.emulator.gradle")
    id("secant.emulator-wtf-conventions")
}

val packageName = project.property("ZCASH_RELEASE_PACKAGE_NAME").toString()

val testnetNetworkName = "Testnet"

android {
    namespace = "co.electriccoin.zcash.app"

    defaultConfig {
        applicationId = packageName

        // If Google Play deployment is triggered, then these are placeholders which are overwritten
        // when the deployment runs
        versionCode = project.property("ZCASH_VERSION_CODE").toString().toInt()
        versionName = project.property("ZCASH_VERSION_NAME").toString()

        testInstrumentationRunner = "co.electriccoin.zcash.test.ZcashUiTestRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions.add("network")

    val testNetFlavorName = "zcashtestnet"
    productFlavors {
        // would rather name them "testnet" and "mainnet" but product flavor names cannot start with the word "test"
        create(testNetFlavorName) {
            dimension = "network"
            applicationId = "$packageName.testnet" // allow to be installed alongside mainnet
            matchingFallbacks.addAll(listOf("zcashtestnet", "debug"))
        }

        create("zcashmainnet") {
            dimension = "network"
            applicationId = packageName
            matchingFallbacks.addAll(listOf("zcashmainnet", "release"))
        }
    }

    val releaseKeystorePath = project.property("ZCASH_RELEASE_KEYSTORE_PATH").toString()
    val releaseKeystorePassword = project.property("ZCASH_RELEASE_KEYSTORE_PASSWORD").toString()
    val releaseKeyAlias = project.property("ZCASH_RELEASE_KEY_ALIAS").toString()
    val releaseKeyAliasPassword =
        project.property("ZCASH_RELEASE_KEY_ALIAS_PASSWORD").toString()
    val isReleaseSigningConfigured = listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyAliasPassword
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (isReleaseSigningConfigured) {
            // If this block doesn't execute, the output will be unsigned
            create("release").apply {
                storeFile = File(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyAliasPassword
            }
        }
    }

    buildTypes {
        getByName("debug").apply {
            // Note that the build-conventions defines the res configs
            isPseudoLocalesEnabled = true

            // Suffixing app package name and version to avoid collisions with other installed Zcash
            // apps (e.g. from Google Play)
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
        }
        getByName("release").apply {
            isMinifyEnabled = project.property("IS_MINIFY_ENABLED").toString().toBoolean()
            isShrinkResources = project.property("IS_MINIFY_ENABLED").toString().toBoolean()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-project.txt"
            )

            val isSignReleaseBuildWithDebugKey = project.property("IS_SIGN_RELEASE_BUILD_WITH_DEBUG_KEY")
                .toString().toBoolean()

            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else if (isSignReleaseBuildWithDebugKey) {
                // Warning: in this case is the release build signed with the debug key
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        all {
            buildConfigField(
                "boolean",
                "IS_STRICT_MODE_CRASH_ENABLED",
                project.property("IS_CRASH_ON_STRICT_MODE_VIOLATION").toString()
            )
        }
    }

    // Resolve final app name
    applicationVariants.all {
        val defaultAppName = project.property("ZCASH_RELEASE_APP_NAME").toString()
        val debugAppNameSuffix = project.property("ZCASH_DEBUG_APP_NAME_SUFFIX").toString()
        when (this.name) {
            "zcashtestnetDebug" -> {
                resValue("string", "app_name", "$defaultAppName ($testnetNetworkName)$debugAppNameSuffix")
            }
            "zcashmainnetDebug" -> {
                resValue("string", "app_name", "$defaultAppName$debugAppNameSuffix")
            }
            "zcashtestnetRelease" -> {
                resValue("string", "app_name", "$defaultAppName ($testnetNetworkName)")
            }
            "zcashmainnetRelease" -> {
                resValue("string", "app_name", defaultAppName)
            }
        }
    }

    packagingOptions {
        resources.excludes.addAll(
            listOf(
                "**/*.kotlin_metadata",
                ".readme",
                "build-data.properties",
                "META-INF/*.kotlin_module",
                "META-INF/android.arch**",
                "META-INF/androidx**",
                "META-INF/com.android**",
                "META-INF/com.google.android.material_material.version",
                "META-INF/com.google.dagger_dagger.version",
                "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
                "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar",
                "META-INF/services/org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages\$Extension",
                "firebase-**.properties",
                "kotlin/**",
                "play-services-**.properties",
                "protolite-well-known-types.properties",
                "transport-api.properties",
                "transport-backend-cct.properties",
                "transport-runtime.properties"
            )
        )
    }

    playConfigs {
        register(testNetFlavorName) {
            enabled.set(false)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    // just to support baseline profile installation needed by benchmark tests
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.zcash.sdk) // just to configure logging
    implementation(projects.crashAndroidLib)
    implementation(projects.spackleAndroidLib)
    implementation(projects.uiLib)

    androidTestImplementation(projects.testLib)

    if (project.property("IS_USE_TEST_ORCHESTRATOR").toString().toBoolean()) {
        androidTestUtil(libs.androidx.test.orchestrator) {
            artifact {
                type = "apk"
            }
        }
    }
}

val googlePlayServiceKeyFilePath = project.property("ZCASH_GOOGLE_PLAY_SERVICE_KEY_FILE_PATH").toString()
if (googlePlayServiceKeyFilePath.isNotEmpty()) {
    // Update the versionName to reflect bumps in versionCode
    androidComponents {
        val versionCodeOffset = 0  // Change this to zero the final digit of the versionName
        onVariants { variant ->
            for (output in variant.outputs) {
                val processedVersionCode = output.versionCode.map { playVersionCode ->
                    val defaultVersionName = project.property("ZCASH_VERSION_NAME").toString()
                    // Version names will look like `myCustomVersionName.123`
                    playVersionCode?.let {
                        val delta = it - versionCodeOffset
                        if (delta < 0) {
                            defaultVersionName
                        } else {
                            "$defaultVersionName.$delta"
                        }
                    } ?: defaultVersionName
                }

                output.versionName.set(processedVersionCode)
            }
        }
    }

    configure<com.github.triplet.gradle.play.PlayPublisherExtension> {
        serviceAccountCredentials.set(File(googlePlayServiceKeyFilePath))

        // For safety, only allow deployment to internal testing track
        track.set("internal")

        // Automatically manage version incrementing
        resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)

        val deployMode = project.property("ZCASH_GOOGLE_PLAY_DEPLOY_MODE").toString()
        if ("build" == deployMode) {
            releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
            // Prevent upload; only generates a build with the correct version number
            commit.set(false)
        } else if ("deploy" == deployMode) {
            releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
        }
    }
}


fladle {
    // Firebase Test Lab has min and max values that might differ from our project's
    // These are determined by `gcloud firebase test android models list`
    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MIN_SDK = 23

    @Suppress("MagicNumber", "PropertyName", "VariableNaming")
    val FIREBASE_TEST_LAB_MAX_SDK = 33

    val minSdkVersion = run {
        val buildMinSdk = project.properties["ANDROID_APP_MIN_SDK_VERSION"].toString().toInt()
        buildMinSdk.coerceAtLeast(FIREBASE_TEST_LAB_MIN_SDK).toString()
    }
    val targetSdkVersion = run {
        val buildTargetSdk = project.properties["ANDROID_TARGET_SDK_VERSION"].toString().toInt()
        buildTargetSdk.coerceAtMost(FIREBASE_TEST_LAB_MAX_SDK).toString()
    }

    val firebaseTestLabKeyPath = project.properties["ZCASH_FIREBASE_TEST_LAB_API_KEY_PATH"].toString()
    val firebaseProject = project.properties["ZCASH_FIREBASE_TEST_LAB_PROJECT"].toString()

    if (firebaseTestLabKeyPath.isNotEmpty()) {
        serviceAccountCredentials.set(File(firebaseTestLabKeyPath))
    } else if (firebaseProject.isNotEmpty()) {
        projectId.set(firebaseProject)
    }

    @Suppress("MagicNumber")
    flakyTestAttempts.set(1)

    configs {
        create("sanityConfigDebug") {
            clearPropertiesForSanityRobo()

            debugApk.set(
                project.provider {
                    "${buildDir}/outputs/apk/zcashmainnet/debug/app-zcashmainnet-debug.apk"
                }
            )

            testTimeout.set("3m")

            devices.addAll(
                mapOf("model" to "Pixel2", "version" to minSdkVersion),
                mapOf("model" to "Pixel2.arm", "version" to targetSdkVersion)
            )

            flankVersion.set(libs.versions.flank.get())
        }
        create("sanityConfigRelease") {
            clearPropertiesForSanityRobo()

            debugApk.set(
                project.provider {
                    "${buildDir}/outputs/universal_apk/zcashmainnetRelease/app-zcashmainnet-release-universal.apk"
                }
            )

            testTimeout.set("3m")

            devices.addAll(
                mapOf("model" to "Pixel2", "version" to minSdkVersion),
                mapOf("model" to "Pixel2.arm", "version" to targetSdkVersion)
            )

            flankVersion.set(libs.versions.flank.get())
        }
    }
}
