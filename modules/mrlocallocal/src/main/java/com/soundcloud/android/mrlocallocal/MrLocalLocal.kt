package com.soundcloud.android.mrlocallocal

import com.github.tomakehurst.wiremock.WireMockServer
import com.soundcloud.android.mrlocallocal.data.LoggedEvent
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalException
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalResult
import com.soundcloud.android.mrlocallocal.data.Spec

import android.content.Context

import java.io.IOException
import java.lang.Exception
import java.util.HashMap

class MrLocalLocal(context: Context, wireMockServer: WireMockServer, eventGatewayUrl: String) {

    private val specReader: SpecReader = SpecReader(context)
    private val eventLogger: EventLogger = EventLogger(wireMockServer, eventGatewayUrl)
    private val logger: Logger = RealLogger()
    private val specWriter: SpecPrinter = SpecPrinter(logger, eventLogger)
    private var startTimestamp: Long = 0

    fun startEventTracking() {
        startTimestamp = System.currentTimeMillis()
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun verify(specName: String, stringSubstitutions: Map<String, String> = HashMap()) {
        logger.info("The spec name is: $specName")
        val start = System.currentTimeMillis()
        val spec = specReader.readSpec(specName, stringSubstitutions)

        var result: MrLocalLocalResult? = null
        var shouldVerifyAgain = true

        // We retry if we logged less requests than expected as sometimes we verify too fast and the
        // event queue hasn't sent the last event yet. We only retry in this condition and for max.
        while (shouldVerifyAgain) {
            result = verify(spec)

            val timeElapsed = System.currentTimeMillis() - start
            shouldVerifyAgain = !result.wasSuccessful && result.canBeRetried && timeElapsed < RETRY_WINDOW_MILLIS
        }

        if (result == null) {
            throw MrLocalLocalException("There is no result...")
        }

        if (result.wasSuccessful) {
            logger.info(result.message)
        } else {
            throw MrLocalLocalException(result.buildErrorMessage())
        }
    }

    @Throws(Exception::class)
    private fun verify(spec: Spec): MrLocalLocalResult {
        val events = eventLogger.loggedEvents
        return SpecValidator(logger).verify(spec, events, startTimestamp)
    }

    @Throws(IOException::class)
    fun printSpec(vararg whiteListedEventsArray: String) {
        specWriter.printSpec(startTimestamp, *whiteListedEventsArray)
    }

    companion object {
        private val RETRY_WINDOW_MILLIS = 5000
    }
}
