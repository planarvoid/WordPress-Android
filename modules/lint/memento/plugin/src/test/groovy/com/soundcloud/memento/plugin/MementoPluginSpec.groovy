package com.soundcloud.memento.plugin

import nebula.test.ProjectSpec
import org.gradle.api.java.archives.ManifestMergeSpec
import org.gradle.jvm.tasks.Jar

class MementoPluginSpec extends ProjectSpec {
    def "modifies manifest"() {
        given:
        def lintManifest = project.file("${project.buildDir}/generated/source/apt/main/lint-manifest.txt")
        lintManifest.parentFile.mkdirs()
        lintManifest.createNewFile()
        project.apply plugin: 'java'

        when:
        project.apply plugin: 'com.soundcloud.memento'

        then:
        Jar task = project.tasks.findByName("jar") as Jar
        task.hasCustomActions
        task.taskActions.forEach { it -> it.execute(task) }
        List<ManifestMergeSpec> mergeSpecs = task.manifest.mergeSpecs
        mergeSpecs != null
        mergeSpecs.size() == 1
        mergeSpecs.get(0).mergePaths != null
        mergeSpecs.get(0).mergePaths.contains(lintManifest)
        task.excludes.contains('lint-manifest.txt')
    }

    def "modifies manifest without apt"() {
        given:
        def lintManifest = project.file("${project.buildDir}/classes/java/main/lint-manifest.txt")
        lintManifest.parentFile.mkdirs()
        lintManifest.createNewFile()
        project.apply plugin: 'java'

        when:
        project.apply plugin: 'com.soundcloud.memento'

        then:
        Jar task = project.tasks.findByName("jar") as Jar
        task.hasCustomActions
        task.taskActions.forEach { it -> it.execute(task) }
        List<ManifestMergeSpec> mergeSpecs = task.manifest.mergeSpecs
        mergeSpecs != null
        mergeSpecs.size() == 1
        mergeSpecs.get(0).mergePaths != null
        mergeSpecs.get(0).mergePaths.contains(lintManifest)
        task.excludes.contains('lint-manifest.txt')
    }
}
