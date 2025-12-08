import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "com.sample.demo"

// To enable all targets at once
    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    targetWasmJs()
    // Commented out deprecated targetDesktop() call
    // targetDesktop()
    targetIos()
    targetAndroid()

    serializationJson()
}


dependencies {
    add("commonMainApi", project(":deps"))
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    add("jvmMainApi", "eu.timerertim.knevo:knevo:0.2.0-RC")
    //add("commonMainApi", project(":korge-dragonbones"))
}

// Configure source sets properly
kotlin {
    sourceSets {
        // Fix the problematic source sets without creating new ones
        findByName("concurrentMain")?.apply {
            dependsOn(commonMain.get())
        }

        findByName("concurrentTest")?.apply {
            dependsOn(commonTest.get())
        }

        // Ensure wasmJsMain exists and depends on commonMain
        findByName("wasmJsMain")?.apply {
            dependsOn(commonMain.get())
        }
    }
}
