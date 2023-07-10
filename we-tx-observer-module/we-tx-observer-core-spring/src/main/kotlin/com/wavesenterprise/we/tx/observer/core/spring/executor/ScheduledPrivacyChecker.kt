package com.wavesenterprise.we.tx.observer.core.spring.executor

open class ScheduledPrivacyChecker(
    private val privacyAvailabilityChecker: PrivacyAvailabilityChecker,
) {

    open fun checkPrivacyAvailabilityWhileTheyExist() {
        while (true) {
            if (privacyAvailabilityChecker.checkPrivacyAvailability() == 0L) break
        }
    }
}
