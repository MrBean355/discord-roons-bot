/*
 * Copyright 2023 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.PlaySound
import com.github.mrbean355.roons.component.PlaySounds
import com.github.mrbean355.roons.telegram.TelegramNotifier
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.elastictranscoder.ElasticTranscoderClient
import software.amazon.awssdk.services.elastictranscoder.model.CreateJobRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

const val FILE_EXTENSION = "mp3"

private const val S3_INPUT_BUCKET = "bulldog-sounds-input"
private const val S3_OUTPUT_BUCKET = "bulldog-sounds"
private const val TRANSCODER_PIPELINE_ID = "1712082859281-9m36m8"
private const val TRANSCODER_PRESET_ID = "1351620000001-300040" // Audio MP3 - 128k

@Component
class SoundStore(
    private val playSounds: PlaySounds,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger,
) {
    private val s3 by lazy { S3Client.builder().region(Region.US_EAST_1).build() }
    private val transcoder by lazy { ElasticTranscoderClient.builder().region(Region.US_EAST_1).build() }
    private val queue = LinkedBlockingQueue<String>()
    private val busy = AtomicBoolean(false)
    private val lock = ReentrantReadWriteLock()
    private var soundsCache: Map<String, PlaySound> = emptyMap()

    @PostConstruct
    fun onPostConstruct(): Unit = runBlocking(IO) {
        downloadAllSoundBites()
    }

    /** @return collection of all available sound bites. */
    fun listAll(): Collection<PlaySound> = lock.read {
        soundsCache.values
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? = lock.read {
//        File(SOUNDS_DIR, soundFileName.trim()).let {
//            if (it.exists()) it else null
//        }
        TODO()
    }

    /**
     * Synchronise our collection of sound bites with the PlaySounds page.
     * Re-downloads all sounds to make sure that sounds with the same name but different content are also downloaded.
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun synchroniseSoundBites(): Unit = runBlocking(IO) {
        val old = soundsCache
        downloadAllSoundBites()

        sendTelegramNotification(
            addedFiles = soundsCache.keys - old.keys,
            removedFiles = old.keys - soundsCache.keys
        )
    }

    private suspend fun downloadAllSoundBites() {
        val ourFileNames = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(S3_INPUT_BUCKET).build())
            .contents().map { it.key().substringBeforeLast('.') }
        val theirFiles = playSounds.listRemoteFiles()
        val theirFileNames = theirFiles.map { it.name }

        val toDownload = theirFiles.filter { theirs ->
            theirs.name !in ourFileNames
        }
        val toDelete = ourFileNames.filter { ourName ->
            ourName !in theirFileNames
        }

        coroutineScope {
            toDownload.forEach { file ->
                launch {
                    val bytes = playSounds.downloadFile(file)
                    logger.info("Uploading ${file.name}")
                    s3.putObject(
                        PutObjectRequest.builder().bucket(S3_INPUT_BUCKET).key(file.name).build(),
                        RequestBody.fromBytes(bytes)
                    )
                    //enqueueTranscode(file.name)
                }
            }
        }

        coroutineScope {
            toDelete.forEach { file ->
                launch {
                    logger.info("Deleting $file")
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(S3_INPUT_BUCKET).key(file).build())
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(S3_OUTPUT_BUCKET).key("${file}.$FILE_EXTENSION").build())
                }
            }
        }

        // FIXME:
        //  Needs to happen elsewhere, as the output bucket won't be finalised, since Transcoder is async.
        lock.write {
            soundsCache = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(S3_OUTPUT_BUCKET).build())
                .contents()
                .associate {
                    it.key() to PlaySound(it.key(), it.eTag().removeSurrounding("\""))
                }
        }
    }

    private fun enqueueTranscode(name: String) {
        queue += name

        if (!busy.getAndSet(true)) {
            GlobalScope.launch {
                while (true) {
                    val item = queue.poll()
                        ?: break

                    transcoder.createJob(
                        CreateJobRequest.builder()
                            .pipelineId(TRANSCODER_PIPELINE_ID)
                            .input { it.key(item) }
                            .output {
                                it.presetId(TRANSCODER_PRESET_ID)
                                it.key("$item.$FILE_EXTENSION")
                            }
                            .build()
                    )

                    delay(250)
                }

                busy.set(false)
            }
        }
    }

    private fun sendTelegramNotification(addedFiles: Collection<String>, removedFiles: Collection<String>) {
        val message = buildString {
            if (addedFiles.isNotEmpty()) {
                append("<b>Added:</b> ")
                appendLine(addedFiles.removeExtensions().joinToString())
            }
            if (removedFiles.isNotEmpty()) {
                append("<b>Removed:</b> ")
                appendLine(removedFiles.removeExtensions().joinToString())
            }
        }
        if (message.isNotEmpty()) {
            telegramNotifier.sendChannelMessage("🔊 Play Sounds Updated 🔊\n\n$message")
        }
    }

    private fun Iterable<String>.removeExtensions(): List<String> =
        map { it.substringBeforeLast('.') }
}