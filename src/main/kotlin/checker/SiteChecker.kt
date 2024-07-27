package de.reeii.checkthesite.checker

import com.charleskorn.kaml.Yaml
import de.reeii.checkthesite.checker.impl.SimpleContainsChecker
import de.reeii.checkthesite.model.Configuration
import de.reeii.checkthesite.model.ConsoleCommand
import de.reeii.checkthesite.model.MailContent
import de.reeii.checkthesite.model.SiteCheckerConfigInfo
import io.ktor.client.statement.*
import java.io.InputStream

/**
 * Interface for a specific site checker implementation.
 *
 * Responsible for deciding whether something has changed/appeared on the website to send out a notification, which is
 * done in [check]. Only [check] has to be overridden, all other properties/methods are optional.
 *
 * In addition, the implementation can optionally require a configuration file (yaml or any other format). If a
 * configuration file is desired, then [T] must contain the type of the corresponding data class and [configInfo],
 * [decodeConfiguration] and [encodeConfiguration] must be overridden.
 *
 * See [SimpleContainsChecker] for an example for setting up a site checker implementation.
 *
 * @param T Type of the data class corresponding to the configuration file. Can be [Any] if not needed
 */
interface SiteChecker<T : Any> {

    /**
     * Information regarding the configuration file. `null` if a configuration file isn't required.
     */
    val configInfo: SiteCheckerConfigInfo<T>?
        get() = null

    /**
     * List of custom console commands for the site checker
     */
    val customCommands: Array<ConsoleCommand>
        get() = emptyArray()

    /**
     * Edits the [content] from [Configuration.mailData] to dynamically produce different mail content.
     */
    fun editMailContent(content: MailContent): MailContent = content

    /**
     * Reads the configuration file from [inputStream] and returns the corresponding configuration object or `null` if
     * a configuration file isn't required.
     */
    fun decodeConfiguration(yaml: Yaml, inputStream: InputStream): T? = null

    /**
     * Takes a [config] object and returns its encoded string representation or `null` if a configuration file isn't
     * required.
     */
    fun encodeConfiguration(yaml: Yaml, config: T): String? = null

    /**
     * Checks the request response.
     *
     * @param response Response to the request that's specified in [Configuration.requestData]
     * @param config Site checker configuration or `null` if it doesn't exist
     * @return `true` if something has changed/appeared on the website to send out a notification
     */
    @Throws(SiteCheckException::class)
    suspend fun check(response: HttpResponse, config: T?): Boolean
}