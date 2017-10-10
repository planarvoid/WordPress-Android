package com.soundcloud.android.playback

import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.utils.ErrorUtils

/**
 * Temporary - here during investigation on whether the resume()
 * method in the players could be called even if no previous play()
 * was issued. If so, we want to crash the app (on beta and below) so we can
 * get info on Fabric - or just throw a silent exception in production.
 *
 * If we verify there is a real need to have a PlaybackItem as a parameter
 * in the resume() method, please remove this class and its callers. Otherwise,
 * remove the parameter in the interface method and simplify SkippyAdapter and friends.
 */
class RemoveParameterFromResume {
    companion object Companion {
        @JvmStatic
        fun handleExceptionAccordingToBuildType(message: String) {
            if (ApplicationProperties.isBetaOrBelow()) {
                throw IllegalStateException(message)
            } else {
                ErrorUtils.handleSilentException(message, IllegalStateException(message))
            }
        }
    }
}
