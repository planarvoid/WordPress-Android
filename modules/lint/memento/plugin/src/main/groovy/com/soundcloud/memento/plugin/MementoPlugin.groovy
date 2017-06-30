package com.soundcloud.memento.plugin

import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class MementoPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.withPlugin('java') {
            final Jar task = project.tasks.findByName("jar") as Jar
            if (task == null) {
                return
            }
            task.doFirst {
                project.buildDir.eachFileRecurse(FileType.FILES) {
                    project.println it.path
                }
                addManifestIfExists(it, project.file("${project.buildDir}/generated/source/apt/main/lint-manifest.txt"))
                addManifestIfExists(it, project.file("${project.buildDir}/classes/java/main/lint-manifest.txt"))
                it.exclude("lint-manifest.txt")
            }
        }
    }

    static void addManifestIfExists(task, manifestFile) {
        if (!manifestFile.exists()) {
            return
        }
        task.manifest { manifest ->
            manifest.from manifestFile
        }
    }
}
