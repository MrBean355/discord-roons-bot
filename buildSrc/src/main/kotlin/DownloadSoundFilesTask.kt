import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

private const val DESTINATION_DIR = "src/main/resources/monitored"

/**
 * Downloads all sounds from the PlaySounds page, replacing already downloaded ones.
 */
open class DownloadSoundFilesTask : DefaultTask() {

    @TaskAction
    fun download() {
        val destination = project.file(DESTINATION_DIR)
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()
        val remoteFiles = listRemoteFiles()
        val total = remoteFiles.size
        remoteFiles.forEachIndexed { index, remoteFile ->
            println("Download ${index + 1} of $total")
            downloadFile(remoteFile, DESTINATION_DIR)
        }
    }
}