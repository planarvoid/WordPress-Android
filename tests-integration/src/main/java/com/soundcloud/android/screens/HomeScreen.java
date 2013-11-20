package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.R.string;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.Han;

public class HomeScreen extends Screen {
    private static final Class ACTIVITY = LauncherActivity.class;

    public HomeScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SignUpScreen clickLogInButton() {
        solo.clickOnButtonResId(R.string.authentication_log_in);
        waiter.waitForText(solo.getString(string.btn_done));
        return new SignUpScreen(solo);
    }

    public boolean hasItemByUsername(String username){
        return solo.searchText(username, true);
    }
}
