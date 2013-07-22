package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.GridViewCompat;

import android.view.ViewGroup;
import android.widget.TextView;

public class SuggestedUsersCategoryScreen {

    private Han solo;

    public SuggestedUsersCategoryScreen(Han driver) {
        solo = driver;
    }

    public String followRandomUser(){
        solo.waitForViewId(R.id.username, 5000);
        GridViewCompat gridViewCompat = (GridViewCompat) solo.getCurrentGridView();
        ViewGroup viewGroup = (ViewGroup) gridViewCompat.getChildAt((int) (Math.random() * gridViewCompat.getChildCount()));
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        solo.clickOnView(textView);
        return textView.getText().toString();
    }
}
