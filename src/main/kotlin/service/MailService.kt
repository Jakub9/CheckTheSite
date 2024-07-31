package de.reeii.checkthesite.service

import de.reeii.checkthesite.checker.SiteChecker
import de.reeii.checkthesite.model.Configuration
import de.reeii.checkthesite.model.ConsoleCommand
import de.reeii.checkthesite.model.MailData
import de.reeii.checkthesite.model.RequestResult
import de.reeii.checkthesite.util.ExitReason
import de.reeii.checkthesite.util.exitProgram
import de.reeii.checkthesite.util.getOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

private val logger = KotlinLogging.logger {}

/**
 * Service for sending out mails.
 */
class MailService(
    private val configurationService: ConfigurationService,
    private val requestService: RequestService,
    consoleService: ConsoleService
) {

    private lateinit var mailData: MailData
    private lateinit var siteChecker: SiteChecker<Any>
    private lateinit var session: Session

    init {
        consoleService.registerCommands(
            this::class,
            ConsoleCommand(
                "testmail",
                "Sends a test email to check whether the mail service is correctly configured",
                ::sendTestMail
            )
        )
    }

    /**
     * Initializes the mail service with the data in [Configuration.mailData].
     *
     * If [MailData.enabled] is `false`, then this method does nothing.
     */
    fun initialize() {
        try {
            mailData = getOrThrow("configured mail data") { configurationService.loadedConfig?.mailData }
            siteChecker = getOrThrow("site checker implementation") { configurationService.loadedSiteChecker }
        } catch (e: Throwable) {
            logger.error { "Tried to access ${e.message}, but none was loaded" }
            exitProgram(ExitReason.ERROR)
            return
        }
        if (!mailData.enabled) {
            logger.info { "Mail sending is disabled in the configuration, so the MailService won't be initialized" }
            return
        }

        val properties = Properties()
        with(properties) {
            put("mail.smtp.host", mailData.host)
            put("mail.smtp.port", mailData.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.socketFactory.port", mailData.port.toString())
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        }

        session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(mailData.username, mailData.password)
        })
        logger.info { "Successfully initialized the MailService" }
        registerListener()
    }

    @Throws(IllegalStateException::class, AddressException::class, MessagingException::class)
    private fun sendMail() {
        check(this::session.isInitialized) {
            "Session isn't initialized"
        }

        val recipientCount = mailData.emailTo.count()
        logger.info { "Sending email to the specified $recipientCount recipient${if (recipientCount == 1) "" else "s"}" }
        val message = MimeMessage(session)
        val mailContent = siteChecker.editMailContent(mailData.content)
        with(message) {
            setFrom(InternetAddress(mailData.emailFrom, mailData.nameFrom))
            setRecipients(Message.RecipientType.BCC, mailData.emailTo.map(::InternetAddress).toTypedArray())
            subject = mailContent.subject
            setText(mailContent.text)
        }
        Transport.send(message)
        logger.info { "Email got successfully sent" }
    }

    /**
     * Registers an after request listener in [RequestService] to send out mails after positive site checks.
     */
    private fun registerListener() {
        requestService.addAfterRequestListener {
            if (it == RequestResult.POSITIVE) {
                try {
                    sendMail()
                } catch (e: Throwable) {
                    logger.error(e) { "Error while sending mail" }
                }
            }
        }
        logger.info { "Registered MailService to react on HTTP requests" }
    }

    private fun sendTestMail() {
        if (!this::session.isInitialized) {
            logger.info { "Mail service isn't initialized, command aborted" }
            return
        }
        logger.info { "Sending a test email with exactly the same data like regular emails" }
        try {
            sendMail()
        } catch (e: Throwable) {
            logger.error(e) { "Error while sending mail" }
        }
    }
}