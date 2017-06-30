package com.soundcloud.memento.plugin

import nebula.test.IntegrationSpec
import nebula.test.functional.PreExecutionAction
import org.gradle.api.Plugin
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin

import java.nio.file.Files
import java.util.zip.ZipFile

class MementoIntegrationSpec extends IntegrationSpec {
    def ln = System.getProperty('line.separator')

    def 'plugin applies with java plugin'() {
        given:
        applyPlugin(JavaPlugin, MementoPlugin)

        expect:
        runTasksSuccessfully('help')
    }

    def 'plugin applies without java plugin'() {
        given:
        applyPlugin(MementoPlugin)

        expect:
        runTasksSuccessfully('help')
    }

    def 'plugin picks up lint-manifest.txt'() {
        given:
        applyPlugin(JavaPlugin, MementoPlugin)
        writeHelloWorld('com.test.registry')
        def manifestLint = createFile('build/generated/source/apt/main/lint-manifest.txt')
        manifestLint << "Lint-Registry: com.test.registry.MyIssueRegistry.java"

        when:
        runTasksSuccessfully('jar')

        then:
        def resultJar = new ZipFile(file('build/libs/plugin-picks-up-lint-manifest-txt.jar'))
        def manifest = resultJar.getEntry("META-INF/MANIFEST.MF")
        manifest != null
        def manifestContent = resultJar.getInputStream(manifest).text
        manifestContent.contains('Lint-Registry: com.test.registry.MyIssueRegistry.java')
    }

    def 'plugin picks up lint-manifest.txt without apt'() {
        given:
        applyPlugin(JavaPlugin, MementoPlugin)
        writeHelloWorld('com.test.registry')

        when:
        // Necessary since otherwise the classes folder gets cleared
        runTasksSuccessfully('classes')
        def manifestLint = createFile('build/classes/java/main/lint-manifest.txt')
        manifestLint << "Lint-Registry: com.test.registry.MyIssueRegistry.java"
        runTasksSuccessfully('jar')

        then:
        def resultJar = new ZipFile(file('build/libs/plugin-picks-up-lint-manifest-txt-without-apt.jar'))
        def manifest = resultJar.getEntry("META-INF/MANIFEST.MF")
        manifest != null
        def manifestContent = resultJar.getInputStream(manifest).text
        manifestContent.contains('Lint-Registry: com.test.registry.MyIssueRegistry.java')
    }

    def 'plugin picks up lint-manifest.txt reverted order'() {
        given:
        applyPlugin(MementoPlugin, JavaPlugin)
        writeHelloWorld('com.test.registry')
        def manifestLint = createFile('build/generated/source/apt/main/lint-manifest.txt')
        manifestLint << "Lint-Registry: com.test.registry.MyIssueRegistry.java"

        when:
        runTasksSuccessfully('jar')

        then:
        def resultJar = new ZipFile(file('build/libs/plugin-picks-up-lint-manifest-txt-reverted-order.jar'))
        def manifest = resultJar.getEntry("META-INF/MANIFEST.MF")
        manifest != null
        def manifestContent = resultJar.getInputStream(manifest).text
        manifestContent.contains('Lint-Registry: com.test.registry.MyIssueRegistry.java')
    }

    def 'plugin builds jar without lint-manifest.txt'() {
        given:
        applyPlugin(JavaPlugin, MementoPlugin)
        writeHelloWorld("com.test.registry")

        expect:
        runTasksSuccessfully('jar')
    }

    void applyPlugin(Class<? extends Plugin>... plugins) {
        plugins.each {
            buildFile << "${applyPlugin(it)}$ln"
        }
    }
}
