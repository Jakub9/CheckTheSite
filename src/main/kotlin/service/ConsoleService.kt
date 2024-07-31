package de.reeii.checkthesite.service

import de.reeii.checkthesite.model.ConsoleCommand
import de.reeii.checkthesite.util.ExitReason
import de.reeii.checkthesite.util.exitProgram
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

/**
 * Service for handling console input.
 */
class ConsoleService {

    private val baseCommands = arrayOf(
        ConsoleCommand("exit", "Exits the program") { exitProgram(ExitReason.USER) },
        ConsoleCommand(listOf("help", "h"), "Prints a list of all commands", ::printHelp)
    )

    private val scanner = Scanner(System.`in`)
    private val allCommands = mutableListOf<ConsoleCommand>()

    init {
        registerCommands(this::class, *baseCommands)
    }

    /**
     * Registers [commands] which belong to the (service) class in [source] for user input.
     */
    fun registerCommands(source: KClass<*>, vararg commands: ConsoleCommand) {
        commands.forEach { it.source = source }
        allCommands += commands
    }

    /**
     * Infinite loop for reading and handling user input from the console.
     */
    fun loop() {
        while (true) {
            try {
                handleInput(scanner.nextLine())
            } catch (e: NoSuchElementException) {
                logger.error(e) {}
                exitProgram(ExitReason.ERROR)
            } catch (e: IllegalStateException) {
                logger.error(e) {}
                exitProgram(ExitReason.ERROR)
            }
        }
    }

    private fun handleInput(line: String) {
        for (command in allCommands) {
            if (command.commands.contains(line)) {
                command.execute()
                return
            }
        }
        logger.info { "Command not found: $line" }
    }

    private fun printHelp() {
        val nullSource = "null"
        val longestSource = allCommands.maxOfOrNull { it.source?.simpleName?.length ?: nullSource.length } ?: 0
        allCommands.groupBy { it.source?.simpleName ?: nullSource }.forEach { (sourceName, commands) ->
            commands.forEach {
                println("%-${longestSource + 2}s %s".format("[$sourceName]", it.commands.joinToString()))
                println(it.description)
            }
        }
    }
}