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
    targetWasm()
    targetDesktop()
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

// Remove the problematic source set configuration
/*
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Common dependencies here
            }
        }
        // Ensure proper source set hierarchy
        val concurrentMain by getting {
            dependsOn(commonMain.get())
        }
        val concurrentTest by getting {
            dependsOn(commonTest.get())
        }
    }
}
*/
