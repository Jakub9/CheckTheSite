package de.reeii.checkthesite.model

import de.reeii.checkthesite.checker.SiteChecker

/**
 * Info regarding a configuration file for a specific [SiteChecker] implementation
 *
 * @property fileName Name of the file with no directories in between, e.g. `custom-checker-config.yaml`
 * @property defaultConfig Default/example config to be generated on the first start
 */
data class SiteCheckerConfigInfo<T : Any>(
    val fileName: String,
    val defaultConfig: T
)
