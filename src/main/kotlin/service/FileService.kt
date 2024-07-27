package de.reeii.checkthesite.service

import de.reeii.checkthesite.util.ExitReason
import de.reeii.checkthesite.util.exitProgram
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

/**
 * Service dealing with files in the base [directory].
 */
class FileService {

    /**
     * Location where all configuration files are saved.
     *
     * If the ugly init code works, then it should be the location of the JAR file.
     */
    private val directory: Path

    init {
        val uri = this::class.java.protectionDomain.codeSource.location.toURI()
        if (uri == null) {
            logger.error { "Couldn't locate base directory" }
            exitProgram(ExitReason.ERROR)
        }
        directory = Paths.get(uri!!).parent
        logger.info { "Successfully initialized the FileService with base path ${directory.absolutePathString()}" }
    }

    /**
     * Writes [content] into a file called [fileName] located in the base [directory]. If [overrideFile] is `true`,
     * overrides existing content if the file already exists.
     *
     * @throws IOException if [overrideFile] is `false` and [fileName] already exists
     */
    @Throws(IOException::class)
    fun writeFile(fileName: String, content: String, overrideFile: Boolean = false) {
        val file = directory.resolve(fileName)
        val openOptions = if (overrideFile) {
            arrayOf(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        } else {
            arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
        }
        file.writeText(content, options = openOptions)
    }

    /**
     * Creates an [InputStream] from the file called [fileName].
     *
     * @throws IOException if  [fileName] doesn't exist or opening the input stream fails
     */
    @Throws(IOException::class)
    fun readFile(fileName: String): InputStream {
        if (!exists(fileName)) {
            throw IOException("File $fileName does not exist")
        }
        val file = directory.resolve(fileName)
        return file.inputStream()
    }

    /**
     * @return Whether a file called [fileName] exists in the base [directory]
     */
    fun exists(fileName: String): Boolean = Files.exists(directory.resolve(fileName))
}