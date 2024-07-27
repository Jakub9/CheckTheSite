package de.reeii.checkthesite.checker.impl

import kotlinx.serialization.Serializable

/**
 * Configuration data for [SimpleContainsChecker].
 *
 * @property containedString String that gets checked whether the website contains it or not
 * @property ignoreCase Whether the contains check is case-insensitive
 * @property containsOrNot If `false`, then it's checked whether the website does *not* contain [containedString]
 */
@Serializable
data class SimpleContainsConfig(
    val containedString: String,
    val ignoreCase: Boolean,
    val containsOrNot: Boolean
)