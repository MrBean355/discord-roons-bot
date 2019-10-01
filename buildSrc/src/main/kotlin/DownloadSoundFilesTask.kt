import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

private const val DESTINATION_DIR = "src/main/resources/monitored"

/**
 * Downloads all sounds from the PlaySounds page, replacing already downloaded ones.
 */
open class DownloadSoundFilesTask : DefaultTask() {

    @TaskAction
    fun download() {
        val remoteFiles = listRemoteFiles()
        println("Found ${remoteFiles.size} remote files.")

        // Set up local directories:
        val destination = project.file(DESTINATION_DIR)
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        // Download all remote files:
        remoteFiles.forEachIndexed { index, remoteFile ->
            println("Download file #${index + 1}: ${remoteFile.fileName}")
            downloadFile(remoteFile, DESTINATION_DIR)
        }
    }
}