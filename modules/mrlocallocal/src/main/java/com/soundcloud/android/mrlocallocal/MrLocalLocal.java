package com.soundcloud.android.mrlocallocal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.soundcloud.android.mrlocallocal.data.LoggedEvent;
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalException;
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalResult;
import com.soundcloud.android.mrlocallocal.data.Spec;

import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MrLocalLocal {
    private static final int RETRY_WINDOW_MILLIS = 5000;

    private final SpecReader specReader;
    private final SpecPrinter specWriter;
    private final EventLogger eventLogger;
    private final Logger logger;
    private long startTimestamp;

    public MrLocalLocal(Context context, WireMockServer wireMockServer, String eventGatewayUrl) {
        specReader = new SpecReader(context);
        eventLogger = new EventLogger(wireMockServer, eventGatewayUrl);
        logger = new RealLogger();
        specWriter = new SpecPrinter(logger, eventLogger);
    }

    public void startEventTracking() {
        startTimestamp = System.currentTimeMillis();
    }

    public void verify(String specName) throws Exception {
        verify(specName, new HashMap<String, String>());
    }

    public void verify(String specName, Map<String, String> stringSubstitutions) throws Exception {
        logger.info("The spec name is: " + specName);
        long start = System.currentTimeMillis();
        Spec spec = specReader.readSpec(specName, stringSubstitutions);

        MrLocalLocalResult result = null;
        boolean shouldVerifyAgain = true;

        // We retry if we logged less requests than expected as sometimes we verify too fast and the
        // event queue hasn't sent the last event yet. We only retry in this condition and for max.
        while (shouldVerifyAgain) {
            result = verify(spec);

            long timeElapsed = System.currentTimeMillis() - start;
            shouldVerifyAgain = !result.wasSuccessful() && result.canBeRetried() && timeElapsed < RETRY_WINDOW_MILLIS;
        }

        if (result.wasSuccessful()) {
            logger.info(result.message());
        } else {
            throw new MrLocalLocalException(result.message());
        }
    }

    private MrLocalLocalResult verify(Spec spec) throws Exception {
        List<LoggedEvent> events = eventLogger.getLoggedEvents();
        return new SpecValidator(logger).verify(spec, events, startTimestamp);
    }

    public void printSpec(String... whiteListedEventsArray) throws IOException {
        specWriter.printSpec(startTimestamp, whiteListedEventsArray);
    }
}
