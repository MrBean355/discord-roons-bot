package com.github.mrbean355.roons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val FILE_EXTENSION = "mp3"
private const val MANIFEST_FILE = "manifest.json"
private const val TEMP_OUTPUT_DIR = "temp_sounds"

internal object SoundBiteDownloader {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun download(project: Project, destination: File): Unit = runBlocking(Dispatchers.IO) {
        val semaphore = Semaphore(10)
        val complete = AtomicInteger(0)
        val outputDir = project.file(TEMP_OUTPUT_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        val allSounds = PlaySoundsService.listSounds()

        coroutineScope {
            allSounds.forEach { soundFile ->
                launch {
                    semaphore.withPermit {
                        val downloadedFile = PlaySoundsService.downloadSound(soundFile, outputDir)
                        SoundBiteConverter.convert(
                            project = project,
                            input = downloadedFile,
                            volume = soundFile.volume,
                            outputName = downloadedFile.nameWithoutExtension + ".$FILE_EXTENSION"
                        )
                        project.logger.lifecycle("Completed ${complete.incrementAndGet()} / ${allSounds.size}")
                    }
                }
            }
        }

        val manifest = buildList {
            outputDir.listFiles()?.forEach { file ->
                check(file.extension == FILE_EXTENSION) { "Unsupported file type: ${file.name}" }
                add(file.name)
            }
        }

        File(outputDir, MANIFEST_FILE)
            .writeText(json.encodeToString(manifest))

        destination.deleteRecursively()
        outputDir.copyRecursively(destination, overwrite = true)
        outputDir.deleteRecursively()
    }
}