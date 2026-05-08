package de.moritzf.picoboard

import java.time.Duration

public data class PicoBoardOptions @JvmOverloads constructor(
    val readTimeout: Duration = Duration.ofMillis(250),
    val pollingInterval: Duration = Duration.ofMillis(50),
    val pollingReadFailureRetries: Int = 10,
) {
    init {
        require(!readTimeout.isNegative && !readTimeout.isZero) {
            "readTimeout must be greater than zero"
        }
        require(!pollingInterval.isNegative && !pollingInterval.isZero) {
            "pollingInterval must be greater than zero"
        }
        require(pollingReadFailureRetries >= 0) {
            "pollingReadFailureRetries must be zero or greater"
        }
    }
}
