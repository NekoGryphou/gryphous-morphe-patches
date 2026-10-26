plugins { `java-library` }

repositories {
    mavenCentral()
    google()
}

// Signature-only app stubs are never packaged. No extracted app code is needed to build.
val stubs = sourceSets.create("stubs")
val androidApi = configurations.create("androidApi")
val dexCompiler = configurations.create("dexCompiler")
dependencies {
    androidApi("com.google.android:android:4.1.1.4") { isTransitive = false }
    compileOnly("xmlpull:xmlpull:1.1.3.1")
    dexCompiler("com.android.tools:r8:9.5.26")
}
stubs.compileClasspath += androidApi
sourceSets.main {
    compileClasspath += stubs.output + androidApi
}
sourceSets.test {
    compileClasspath += stubs.output + androidApi
    runtimeClasspath += stubs.output + androidApi
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

val stubsJar = tasks.register<Jar>("stubsJar") {
    archiveClassifier.set("stubs")
    from(stubs.output)
}

tasks.register<JavaExec>("dex") {
    dependsOn(tasks.jar, stubsJar)
    classpath = dexCompiler
    mainClass.set("com.android.tools.r8.D8")
    val output = layout.buildDirectory.dir("dex")
    inputs.file(tasks.jar.flatMap { it.archiveFile })
    inputs.file(stubsJar.flatMap { it.archiveFile })
    inputs.files(androidApi)
    outputs.dir(output)
    doFirst {
        output.get().asFile.mkdirs()
        args("--release", "--min-api", "24", "--lib", androidApi.singleFile.absolutePath,
            "--lib", System.getProperty("java.home"),
            "--classpath", stubsJar.get().archiveFile.get().asFile.absolutePath,
            "--output", output.get().asFile.absolutePath,
            tasks.jar.get().archiveFile.get().asFile.absolutePath)
    }
}

tasks.register<JavaExec>("verifyRuntime") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.morphe.extension.smartlife.RuntimeTest")
}

tasks.check { dependsOn("verifyRuntime") }
