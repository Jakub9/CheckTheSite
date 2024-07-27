package de.reeii.checkthesite.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

val logger = KotlinLogging.logger {}

/**
 * Reason for exiting the program.
 */
enum class ExitReason {
    /**
     * Exit initiated by the user (i.e. the exit command)
     */
    USER,

    /**
     * Exit gracefully initiated by the program (e.g. after generating an example config file)
     */
    PROGRAM,

    /**
     * Exit due to a program error
     */
    ERROR
}

/**
 * Exits the whole program with a [exitReason].
 */
fun exitProgram(exitReason: ExitReason) {
    logger.info { "Program closed with exit reason: $exitReason" }
    exitProcess(if (exitReason == ExitReason.ERROR) -1 else 0)
}

@Throws(IllegalStateException::class)
fun <T> getOrThrow(name: String, getter: (Unit) -> T?): T = getter(Unit) ?: throw IllegalStateException(name)

fun parseTimeUnit(unit: String): TimeUnit? = TimeUnit.entries.firstOrNull { it.toString().equals(unit, true) }