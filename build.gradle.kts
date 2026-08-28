import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test.configure {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val functionalTest by sourceSets.creating {
    java {
        srcDir("src/functionalTest/java")
        compileClasspath += sourceSets.patchedMc.get().output + sourceSets.main.get().output
    }
}

configurations {
    named(functionalTest.compileClasspathConfigurationName).configure {
        extendsFrom(configurations.compileClasspath.get())
    }
    named(functionalTest.runtimeClasspathConfigurationName).configure {
        extendsFrom(configurations.runtimeClasspath.get())
    }
    named(functionalTest.annotationProcessorConfigurationName).configure {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.register<Jar>(functionalTest.jarTaskName) {
    from(functionalTest.output)
    archiveClassifier.set("functionalTests")
    archiveVersion.set("1.0")
    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
}

listOf("runClient", "runServer", "runClient25", "runServer25").forEach { name ->
    tasks.named<RunMinecraftTask>(name).configure {
        dependsOn(functionalTest.jarTaskName)
        classpath(
            configurations.named(functionalTest.runtimeClasspathConfigurationName),
            tasks.named(functionalTest.jarTaskName)
        )
    }
}

val gameTest = tasks.register("gameTest") {
    group = "verification"
    description = "Runs the Horizon-QA in-game tests headlessly and writes a JUnit report."
    dependsOn("runServer25")
}

tasks.named<RunMinecraftTask>("runServer25").configure {
    if (gradle.startParameter.taskNames.any { it == gameTest.name || it.endsWith(":${gameTest.name}") }) {
        jvmArgs(
            "-Dhorizonqa.mode=ci",
            "-Dhorizonqa.reportDir=${layout.buildDirectory.get().asFile}/horizonqa",
            "-Dhorizonqa.tests=gregcolonies_tests"
        )
    }
}
