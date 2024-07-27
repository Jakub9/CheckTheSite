package de.reeii.checkthesite.model

import de.reeii.checkthesite.checker.SiteChecker

enum class RequestResult {
    /**
     * Request was successful and the website got checked positively according to the [SiteChecker] implementation
     */
    POSITIVE,

    /**
     * Request was successful, but the website got checked negatively according to the [SiteChecker] implementation
     */
    NEGATIVE,

    /**
     * Request failed for whatever reason (response code not in 200-299, connection failed, whatever)
     */
    FAILED
}