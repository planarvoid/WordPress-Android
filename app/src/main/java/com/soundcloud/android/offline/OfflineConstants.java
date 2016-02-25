package com.soundcloud.android.offline;

import java.util.concurrent.TimeUnit;

class OfflineConstants {
    static final long PENDING_REMOVAL_DELAY = TimeUnit.SECONDS.toMillis(15);
    static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(1);
}
