plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.21" apply false
    kotlin("jvm") version "2.4.20" apply false
}
stonecutter active "26.1.2"

tasks.register("buildAllTargets") {
    group = "build"
    dependsOn(subprojects.map { "${it.path}:build" })
    doLast {
        val destination = layout.buildDirectory.dir("distributions/${project.property("mod_version")}").get().asFile
        destination.mkdirs()
        subprojects.forEach { target ->
            val jar = target.tasks.named<Jar>("jar").get().archiveFile.get().asFile
            check(jar.isFile) { "Missing installable jar for ${target.name}" }
            java.util.zip.ZipFile(jar).use { zip ->
                check(zip.getEntry("fabric.mod.json") != null)
                check(zip.getEntry("net/johnceo/safaricontest/SafariContest.class") != null)
                check(zip.getEntry("assets/safari-contest/honeyhives.txt") != null)
                check(zip.entries().asSequence().none { it.name.contains("sparklingmutuals") })
            }
            jar.copyTo(destination.resolve(jar.name), overwrite = true)
        }
    }
}
