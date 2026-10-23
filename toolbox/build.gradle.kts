plugins {
    kotlin("plugin.serialization") version "2.2.10"
}

dependencies {
    implementation(project(":server:common"))
    implementation(project(":server:cache"))
    implementation(project(":server:config"))
    implementation(project(":server:engine"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
}

tasks.register<JavaExec>("runLadderEditor") {
    dependsOn(tasks.named("classes"))
    group = "rsbox"
    mainClass.set("io.rsbox.toolbox.LadderEditor")
    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
}
