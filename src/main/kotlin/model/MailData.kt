package de.reeii.checkthesite.model

import de.reeii.checkthesite.service.MailService
import kotlinx.serialization.Serializable

/**
 * Data regarding sending out mails.
 *
 * @property enabled Whether to initialize the [MailService] to send out mails. If `false`, then all other properties get ignored here
 * @property host Host name of the mail server
 * @property port Port
 * @property username Username for the mail server login
 * @property password Password for the mail server login
 * @property emailFrom Sender email
 * @property nameFrom Sender name
 * @property emailTo List of recipient emails
 * @property content Content of a mail
 */
@Serializable
data class MailData(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val emailFrom: String,
    val nameFrom: String,
    val emailTo: List<String>,
    val content: MailContent
)
