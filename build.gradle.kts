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
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    add("jvmMainApi", "eu.timerertim.knevo:knevo:0.2.0-RC")
    //add("commonMainApi", project(":korge-dragonbones"))
}

