package de.reeii.checkthesite.checker.impl

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import de.reeii.checkthesite.checker.SiteCheckException
import de.reeii.checkthesite.checker.SiteChecker
import de.reeii.checkthesite.model.ConsoleCommand
import de.reeii.checkthesite.model.SiteCheckerConfigInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.encodeToString
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * Simple site checker which demonstrates how to write custom site checker implementations.
 *
 * In addition, it's fully functional! It checks whether a website contains (or doesn't contain) a string that's
 * specified in a configuration file.
 */
@Suppress("unused")
class SimpleContainsChecker : SiteChecker<SimpleContainsConfig> {

    override val configInfo
        get() = SiteCheckerConfigInfo(
            "simple-contains-config.yaml",
            SimpleContainsConfig("The website must contain this text!", ignoreCase = true, containsOrNot = true)
        )

    override val customCommands: Array<ConsoleCommand>
        get() = arrayOf(ConsoleCommand("example", "Example for a custom command by a site checker implementation") {
            logger.info { "Pong!" }
        })

    override fun decodeConfiguration(yaml: Yaml, inputStream: InputStream) =
        yaml.decodeFromStream<SimpleContainsConfig>(inputStream)

    override fun encodeConfiguration(yaml: Yaml, config: SimpleContainsConfig) = yaml.encodeToString(config)

    @Throws(SiteCheckException::class)
    override suspend fun check(response: HttpResponse, config: SimpleContainsConfig?): Boolean {
        if (config == null) {
            throw SiteCheckException("Site config cannot be null")
        }
        return config.containsOrNot == (response.body() as String).contains(config.containedString, config.ignoreCase)
    }
}