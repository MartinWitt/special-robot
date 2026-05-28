plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":tools"))
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
}

tasks.shadowJar {
    archiveBaseName = "spoon-claude"
    archiveClassifier = ""
    archiveVersion = ""
    manifest {
        attributes["Main-Class"] = "io.github.martinwitt.spoonclaude.Main"
    }
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

val assembleDist by tasks.registering(Copy::class) {
    dependsOn(tasks.shadowJar)
    into(layout.buildDirectory.dir("spoon-claude-dist"))
    from(tasks.shadowJar.get().archiveFile) {
        rename { "spoon-claude.jar" }
    }
    from(rootProject.file("skills")) {
        into("skills")
    }
}


val bundleSkill by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Bundle SKILL.md, references, and the shaded JAR in Claude-Skill layout"
    dependsOn(tasks.shadowJar)
    into(layout.buildDirectory.dir("skill/spoon-claude"))
    from(rootProject.file("skills/SKILL.md"))
    from(rootProject.file("skills")) {
        include("spoon-*.md")
        into("references")
    }
    from(tasks.shadowJar.get().archiveFile) {
        rename { "spoon-claude.jar" }
        into("scripts")
    }
}

val packageSkill by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Package the Claude Skill bundle as a zip"
    dependsOn(bundleSkill)
    archiveFileName.set("spoon-claude-skill.zip")
    destinationDirectory.set(layout.buildDirectory.dir("skill"))
    from(layout.buildDirectory.dir("skill/spoon-claude")) {
        into("spoon-claude")
    }
}

tasks.assemble {
    dependsOn(assembleDist)
}
