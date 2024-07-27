package de.reeii.checkthesite.service

import de.reeii.checkthesite.checker.SiteCheckException
import de.reeii.checkthesite.checker.SiteChecker
import de.reeii.checkthesite.model.Configuration
import de.reeii.checkthesite.model.ConsoleCommand
import de.reeii.checkthesite.model.RequestData
import de.reeii.checkthesite.model.RequestResult
import de.reeii.checkthesite.util.ExitReason
import de.reeii.checkthesite.util.exitProgram
import de.reeii.checkthesite.util.getOrThrow
import de.reeii.checkthesite.util.parseTimeUnit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Service for doing HTTP requests as specified in [Configuration.requestData].
 */
class RequestService(private val configurationService: ConfigurationService, consoleService: ConsoleService) {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val client = HttpClient(CIO) {
        install(Logging)
        followRedirects = false
    }

    /**
     * List of observing handlers that perform work after every request.
     */
    private val afterRequest = mutableListOf<(RequestResult) -> Unit>()

    /**
     * If `false`, then the automatic scheduling is disabled.
     *
     * Usually gets set to `false` after a positive check to prevent spamming. After being notified of a positive
     * result for the first time, no further requests will be scheduled until the `confirm` command is used (see the
     * `init` block and [confirmCommand]).
     */
    private val schedulingEnabled = AtomicBoolean(true)

    private lateinit var requestData: RequestData
    private lateinit var delayUnit: TimeUnit
    private lateinit var randomnessUnit: TimeUnit
    private lateinit var delayUnitString: String
    private lateinit var randomnessUnitString: String
    private lateinit var siteChecker: SiteChecker<Any>
    private var siteCheckerConfig: Any? = null

    init {
        consoleService.registerCommands(
            this::class, ConsoleCommand(
                "confirm",
                "Confirms a POSITIVE request result and re-enables automatic request scheduling",
                ::confirmCommand
            )
        )
    }

    /**
     * Loads the configuration in [Configuration.requestData] and enables logging and periodic scheduling depending on
     * the configured values.
     */
    fun loadConfiguration() {
        try {
            requestData = getOrThrow("configured request data") { configurationService.loadedConfig?.requestData }
            siteChecker = getOrThrow("site checker implementation") { configurationService.loadedSiteChecker }
            siteCheckerConfig = configurationService.loadedSiteCheckerConfig
        } catch (e: Throwable) {
            logger.error { "Tried to access ${e.message}, but none was loaded" }
            exitProgram(ExitReason.ERROR)
            return
        }

        // we've already checked in configuration service whether it can be parsed successfully
        delayUnit = parseTimeUnit(requestData.delayUnit)!!
        randomnessUnit = parseTimeUnit(requestData.randomnessUnit)!!
        delayUnitString = delayUnit.toString().lowercase()
        randomnessUnitString = randomnessUnit.toString().lowercase()

        client.plugin(Logging).level = if (requestData.printDebug) {
            LogLevel.ALL
        } else {
            LogLevel.NONE
        }
        if (requestData.periodicScheduling) {
            enablePeriodicScheduling()
        } else {
            addAfterRequestListener { logger.info { "Periodic scheduling is disabled in the configuration, so no further requests will be scheduled" } }
        }
        logger.info { "Successfully loaded configuration for the RequestService" }
        logger.info { "Scheduling delay is between ${requestData.minDelay} and ${requestData.maxDelay} $delayUnitString" }
    }

    /**
     * Adds a [listener] that gets called after every request.
     */
    fun addAfterRequestListener(listener: (RequestResult) -> Unit) {
        afterRequest += listener
    }

    /**
     * Schedules a single HTTP request with a random delay within the configured limits.
     */
    fun schedulePerConfiguration() {
        if (!schedulingEnabled.get()) {
            logger.warn { "Tried to schedule request, but schedulingEnabled is false. Scheduling prevented" }
            return
        }

        val delayRange = with(randomnessUnit) {
            convert(requestData.minDelay, delayUnit)..convert(requestData.maxDelay, delayUnit)
        }
        val delay = delayRange.random()
        val delayInOriginalUnit = delayUnit.convert(delay, randomnessUnit)
        executor.schedule(::performRequest, delay, randomnessUnit)

        if (delayUnit == randomnessUnit) {
            logger.info { "Scheduled HTTP request which will be executed in $delayInOriginalUnit $delayUnitString" }
        } else {
            logger.info { "Scheduled HTTP request which will be executed in ~$delayInOriginalUnit $delayUnitString ($delay $randomnessUnitString)" }
        }
    }

    private fun enablePeriodicScheduling() {
        logger.info { "Enabled periodic scheduling of HTTP requests" }
        addAfterRequestListener { result ->
            if (result == RequestResult.POSITIVE) {
                logger.info { "Turning off automatic scheduling due to a POSITIVE result. Type \"confirm\" to re-enable automatic scheduling " }
                schedulingEnabled.set(false)
            } else if (schedulingEnabled.get()) {
                schedulePerConfiguration()
            } else {
                logger.info { "Automatic scheduling prevented because schedulingEnabled is false" }
            }
        }
    }

    private fun performRequest() = runBlocking {
        logger.info { "Performing scheduled HTTP request to ${requestData.url}" }
        var requestResult = RequestResult.FAILED
        try {
            val response = client.get(requestData.url)
            check(response.status.isSuccess()) { "Response status was ${response.status}" }
            requestResult = if (siteChecker.check(response, siteCheckerConfig)) {
                RequestResult.POSITIVE
            } else {
                RequestResult.NEGATIVE
            }
            logger.info { "HTTP request successful, site check yielded result $requestResult" }
        } catch (e: SiteCheckException) {
            logger.error { "SiteChecker error: ${e.message}" }
        } catch (e: Throwable) {
            logger.error(e) { "Error while performing request" }
        } finally {
            if (requestResult == RequestResult.FAILED) {
                logger.info { "HTTP request unsuccessful with result $requestResult" }
            }
            afterRequest.forEach { it(requestResult) }
        }
    }

    private fun confirmCommand() {
        if (!requestData.periodicScheduling) {
            logger.info { "Command not supported: periodic scheduling is disabled in the configuration" }
            return
        }
        if (schedulingEnabled.getAndSet(true)) {
            logger.info { "Automatic scheduling is already enabled, there's nothing to confirm" }
            return
        }

        logger.info { "Thanks for confirming the previous POSITIVE result! Automatic scheduling is enabled again" }
        schedulePerConfiguration()
    }
}