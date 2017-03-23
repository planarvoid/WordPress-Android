package com.soundcloud.android.ads;

import java.util.concurrent.TimeUnit;

public final class AdConstants {
    public static final int UNSKIPPABLE_TIME_SECS = 15;
    public static final long UNSKIPPABLE_TIME_MS = TimeUnit.SECONDS.toMillis(UNSKIPPABLE_TIME_SECS);

    public static final int IAB_UNIVERSAL_MED_WIDTH = 300;
    public static final int IAB_UNIVERSAL_MED_HEIGHT = 250;
    public static final int IAB_UNIVERSAL_MED_MAX_SCALE = 2;

    static final String KRUX_SEGMENT_PARAM = "krux_segments";
    static final String CORRELATOR_PARAM = "correlator";

    static final int MAX_INLAYS_ON_SCREEN = 3;
}
