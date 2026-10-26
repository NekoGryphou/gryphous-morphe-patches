group = "io.github.nekogryphou.morphe.patches"

patches {
    about {
        name = "Gryphou's Morphe Patches"
        description = "Android app patches for Crunchyroll, De'Longhi Comfort, PETKIT, and Smart Life."
        source = "https://github.com/NekoGryphou/gryphous-morphe-patches"
        author = "NekoGryphou"
        contact = "darkosdash@gmail.com"
        website = "https://github.com/NekoGryphou/gryphous-morphe-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    processResources {
        dependsOn(":smartlife-runtime:dex")
        from(project(":smartlife-runtime").layout.buildDirectory.dir("dex")) {
            include("classes.dex")
            into("extensions")
            rename("classes.dex", "smartlife.mpe")
        }
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
