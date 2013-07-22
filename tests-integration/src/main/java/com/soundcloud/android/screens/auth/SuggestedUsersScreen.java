package com.soundcloud.android.screens.auth;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class SuggestedUsersScreen {
    private final Waiter waiter;
    private Han solo;

    public SuggestedUsersScreen(Han driver) {
        solo    = driver;
        waiter  = new Waiter(solo);
    }


}
