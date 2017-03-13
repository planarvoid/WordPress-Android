package com.soundcloud.android.ads;

import android.content.Context;
import android.os.Looper;

import com.krux.androidsdk.aggregator.KruxEventAggregator;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.soundcloud.java.checks.Preconditions.checkState;

@Singleton
class KruxSegmentProvider {

    private final static boolean DEBUG_MODE = false;

    private Optional<String> latestSegments = Optional.absent();

    @Inject
    KruxSegmentProvider(final Context context) {
        try {
            checkState(Thread.currentThread() == Looper.getMainLooper().getThread());
            KruxEventAggregator.initialize(context.getApplicationContext(),
                                           context.getString(R.string.krux_configuration_id),
                                           segments -> latestSegments = Strings.isBlank(segments)
                                                   ? Optional.absent()
                                                   : Optional.of(segments),
                                           DEBUG_MODE);
        } catch (Exception e) {
            // If it fails to initialize, getSegments() will return Optional.absent()
            ErrorUtils.handleSilentException(new IllegalStateException("Krux failed to initialize: " + e.getMessage()));
        }
    }

    Optional<String> getSegments() {
        return latestSegments;
    }
}
