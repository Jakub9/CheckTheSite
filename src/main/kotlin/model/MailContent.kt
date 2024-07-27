package de.reeii.checkthesite.model

import kotlinx.serialization.Serializable

/**
 * Content of a mail to send.
 *
 * @property subject
 * @property text
 */
@Serializable
data class MailContent(
    val subject: String,
    val text: String
)
