package de.reeii.checkthesite.model

import de.reeii.checkthesite.checker.SiteChecker
import kotlinx.serialization.Serializable

/**
 * Base configuration data object.
 *
 * @property requestData Data regarding the HTTP requests
 * @property mailData Data regarding sending out mails
 * @property siteCheckerImpl Qualified name of the used [SiteChecker] implementation
 */
@Serializable
data class Configuration(
    val requestData: RequestData,
    val mailData: MailData,
    val siteCheckerImpl: String
)