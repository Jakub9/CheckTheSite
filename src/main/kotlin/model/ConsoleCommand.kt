package de.reeii.checkthesite.model

import kotlin.reflect.KClass

/**
 * Command that can be used in the console.
 *
 * @property commands List of one or multiple commands to execute the same function
 * @property description Description that gets printed in the help command
 * @property execute Function that gets executed when the command is used
 */
data class ConsoleCommand(
    val commands: List<String>,
    val description: String,
    val execute: () -> Unit,
) {
    constructor(command: String, description: String, execute: () -> Unit) : this(listOf(command), description, execute)

    /**
     * Source of this command, i.e. which class (usually a service) created/registered it
     */
    var source: KClass<*>? = null
}
