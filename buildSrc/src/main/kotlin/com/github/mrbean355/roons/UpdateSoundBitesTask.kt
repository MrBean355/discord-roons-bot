package com.github.mrbean355.roons

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class UpdateSoundBitesTask : DefaultTask() {

    @get:Internal
    abstract val destination: DirectoryProperty

    @TaskAction
    fun run() {
        SoundBiteDownloader.download(project, destination.get().asFile)
    }
}