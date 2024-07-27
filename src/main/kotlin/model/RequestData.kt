package de.reeii.checkthesite.model

import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

/**
 * Data regarding the HTTP requests.
 *
 * [minDelay] and [maxDelay] are used to add randomness to the scheduling time. [randomnessUnit] can be used to
 * calculate the random scheduling time more precisely. For example, if [delayUnit] is set to `MINUTES` and min/max
 * delay is (3..5), then [randomnessUnit] can be set to `SECONDS` to allow random scheduling times within (180..300)
 * seconds. Thus, [randomnessUnit] must be equal to or more precise than [delayUnit].
 *
 * @property url URL
 * @property minDelay Minimum delay between two scheduled requests
 * @property maxDelay Maximum delay between two scheduled requests
 * @property delayUnit Unit of [minDelay] and [maxDelay], see [TimeUnit]
 * @property randomnessUnit [TimeUnit] in which the random delay within ([minDelay]..[maxDelay]) is calculated
 * @property periodicScheduling Whether a request is scheduled again after a negative check
 * @property printDebug Whether to print ktor debug messages
 */
@Serializable
data class RequestData (
    val url: String,
    val minDelay: Long,
    val maxDelay: Long,
    val delayUnit: String,
    val randomnessUnit: String,
    val periodicScheduling: Boolean,
    val printDebug: Boolean
)