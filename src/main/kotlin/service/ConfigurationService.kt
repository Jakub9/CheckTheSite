package de.reeii.checkthesite.service

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import de.reeii.checkthesite.checker.SiteChecker
import de.reeii.checkthesite.model.Configuration
import de.reeii.checkthesite.model.MailContent
import de.reeii.checkthesite.model.MailData
import de.reeii.checkthesite.model.RequestData
import de.reeii.checkthesite.util.ExitReason
import de.reeii.checkthesite.util.exitProgram
import de.reeii.checkthesite.util.parseTimeUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.starProjectedType

private val logger = KotlinLogging.logger {}

/**
 * Service for reading configuration files using [fileService].
 */
class ConfigurationService(private val fileService: FileService, private val consoleService: ConsoleService) {

    var loadedConfig: Configuration? = null
        private set
    var loadedSiteChecker: SiteChecker<Any>? = null
        private set
    var loadedSiteCheckerConfig: Any? = null
        private set

    private val yaml = Yaml.default
    private val configFileName = "config.yaml"

    /**
     * Default configuration that gets created on the first start.
     */
    private val defaultConfig = Configuration(
        RequestData("https://ktor.io", 3, 5, "minutes", "seconds", periodicScheduling = false, printDebug = false),
        MailData(
            false,
            "smtp.example.com",
            465,
            "sender@example.com",
            "1234pwd",
            "sender@example.com",
            "Example sender",
            listOf("recipient@example.com"),
            MailContent("Mail subject", "This is the content of the mail!")
        ),
        "de.reeii.checkthesite.checker.impl.SimpleContainsChecker"
    )

    private val possibleValidationErrors: Map<Configuration.() -> Boolean, String> = mapOf(
        Pair({ requestData.url.isBlank() }, "Request url is empty"),
        Pair({ requestData.delayUnit.isBlank() }, "Request delay unit is empty"),
        Pair({ parseTimeUnit(requestData.delayUnit) == null }, "delayUnit couldn't be parsed"),
        Pair({ parseTimeUnit(requestData.randomnessUnit) == null }, "randomnessUnit couldn't be parsed"),
        Pair({ requestData.minDelay > requestData.maxDelay }, "Min delay is larger than max delay"),
        Pair({
            val delayUnit = parseTimeUnit(requestData.delayUnit)
            val randomnessUnit = parseTimeUnit(requestData.randomnessUnit)
            delayUnit != null && randomnessUnit != null && randomnessUnit.convert(1, delayUnit) == 0L
        }, "randomnessUnit is less precise than delayUnit"),
        Pair({ siteCheckerImpl.isBlank() }, "Site checker implementation is empty")
        // TODO add mail data validation (and maybe refactor the validation process in general)
    )

    /**
     * Initializes the service by loading the
     * 1. main [Configuration] file from [configFileName] which gets saved in [loadedConfig]
     * 2. [SiteChecker] instance as specified in [Configuration.siteCheckerImpl] which gets saved in [loadedSiteChecker]
     * 3. optionally: site checker configuration which gets saved in [loadedSiteCheckerConfig]
     *
     * If one of the required steps fails (config doesn't exist, can't be parsed, whatever), then the whole program
     * closes with an appropriate error description.
     */
    fun initialize() {
        loadMainConfiguration()
        loadSiteChecker()
    }

    private fun loadMainConfiguration() {
        logger.info { "Attempting to load configuration data from $configFileName" }
        if (!fileService.exists(configFileName)) {
            logger.info {
                "Configuration file $configFileName not found. Generating example config file, please set it up and restart the program"
            }
            generateConfiguration(configFileName, defaultConfig) { yaml, config ->
                yaml.encodeToString(config)
            }
            exitProgram(ExitReason.PROGRAM)
            return
        }

        val decodedConfig = decodeYaml(configFileName) { yaml, inputStream ->
            yaml.decodeFromStream<Configuration>(inputStream)
        }
        val validationErrors = validateConfiguration(decodedConfig)
        if (validationErrors.isNotEmpty()) {
            logger.error { "Configuration in $configFileName contains errors (see below), please fix them and restart the program" }
            validationErrors.forEach { logger.error { it } }
            exitProgram(ExitReason.ERROR)
        }
        loadedConfig = decodedConfig
        logger.info { "Successfully loaded configuration data from $configFileName" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadSiteChecker() {
        val config = loadedConfig
        if (config == null) {
            logger.error { "Tried to access configuration, but none was loaded" }
            exitProgram(ExitReason.ERROR)
            return
        }

        logger.info { "Attempting to load site checker implementation ${config.siteCheckerImpl}" }
        val siteCheckerClass: KClass<out Any>? = try {
            Class.forName(config.siteCheckerImpl).kotlin
        } catch (e: ClassNotFoundException) {
            logger.error { "Couldn't find site checker implementation class ${config.siteCheckerImpl}" }
            exitProgram(ExitReason.ERROR)
            null
        } catch (e: IOException) {
            logger.error(e) { }
            exitProgram(ExitReason.ERROR)
            null
        }
        if (!siteCheckerClass!!.supertypes.any { it.classifier == SiteChecker::class.starProjectedType.classifier }) {
            logger.error { "${siteCheckerClass.simpleName} doesn't implement the SiteChecker interface" }
            exitProgram(ExitReason.ERROR)
        }

        loadedSiteChecker = try {
            siteCheckerClass.createInstance() as SiteChecker<Any>
        } catch (e: IllegalArgumentException) {
            logger.error { "Couldn't instantiate ${siteCheckerClass.simpleName}. Does it have a single no-arg constructor?" }
            exitProgram(ExitReason.ERROR)
            null
        }
        consoleService.registerCommands(siteCheckerClass, *loadedSiteChecker!!.customCommands)
        logger.info { "Successfully loaded site checker implementation ${siteCheckerClass.simpleName}" }

        if (loadedSiteChecker!!.configInfo == null) {
            logger.info { "${siteCheckerClass.simpleName} doesn't require any configuration" }
            return  // we're done here, no need to create/load config if not desired
        }
        val configInfo = loadedSiteChecker!!.configInfo!!
        logger.info { "${siteCheckerClass.simpleName} requires configuration in ${configInfo.fileName}. Attempting to load configuration data" }

        if (!fileService.exists(configInfo.fileName)) {
            logger.info {
                "Configuration file ${configInfo.fileName} not found. Generating example config file, please set it up and restart the program"
            }
            generateConfiguration(configInfo.fileName, configInfo.defaultConfig) { yaml, cfg ->
                loadedSiteChecker!!.encodeConfiguration(yaml, cfg) ?: ""
            }
            exitProgram(ExitReason.PROGRAM)
        }
        loadedSiteCheckerConfig = decodeYaml(configInfo.fileName, loadedSiteChecker!!::decodeConfiguration)
        logger.info { "Successfully loaded ${siteCheckerClass.simpleName} configuration data from ${configInfo.fileName}" }
    }

    private fun <T> generateConfiguration(fileName: String, config: T, encoder: (Yaml, T) -> String) {
        val configYaml = try {
            encoder(yaml, config)
        } catch (e: SerializationException) {
            logger.error { "$fileName couldn't be serialized. Is the underlying class marked as @Serializable?" }
            exitProgram(ExitReason.ERROR)
            return
        }

        try {
            fileService.writeFile(fileName, configYaml)
        } catch (e: FileAlreadyExistsException) {
            logger.error { "Tried to generate an example configuration file, but $fileName already exists" }
            exitProgram(ExitReason.ERROR)
        } catch (e: IOException) {
            logger.error(e) { }
            exitProgram(ExitReason.ERROR)
        }
    }

    private fun <T> decodeYaml(fileName: String, decoder: (Yaml, InputStream) -> T): T {
        val inputStream: InputStream? = try {
            fileService.readFile(fileName)
        } catch (e: IOException) {
            logger.error(e) { }
            exitProgram(ExitReason.ERROR)
            null
        }

        val decodedConfig: T? = try {
            decoder(yaml, inputStream!!)
        } catch (e: Exception) {
            logger.error { "Failed parsing $fileName. Are some required fields missing?" }
            exitProgram(ExitReason.ERROR)
            null
        }
        return decodedConfig!!
    }

    private fun validateConfiguration(config: Configuration): List<String> {
        val errors = mutableListOf<String>()
        for ((predicate, errorMessage) in possibleValidationErrors) {
            if (predicate(config)) {
                errors += errorMessage
            }
        }
        return errors
    }
}