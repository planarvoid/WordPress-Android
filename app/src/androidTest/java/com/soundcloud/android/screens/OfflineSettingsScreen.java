package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.SeekBarElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.settings.OfflineSettingsActivity;

public class OfflineSettingsScreen extends Screen {

    private static final Class ACTIVITY = OfflineSettingsActivity.class;
    private static final int SLIDER_LIMIT_TEXT = R.id.offline_storage_limit;
    private static final int LEGEND_LIMIT_TEXT = R.id.offline_storage_legend_limit;
    private static final int SLIDER_LIMIT_SEEK_BAR = R.id.offline_storage_limit_seek_bar;

    public OfflineSettingsScreen(Han solo) {
        super(solo);
    }

    public UpgradeScreen clickSubscribe() {
        testDriver.clickOnText(R.string.pref_subscription_buy_title);
        return new UpgradeScreen(testDriver);
    }

    public String getSliderLimitText() {
        return sliderLimitText().getText();
    }

    public OfflineSettingsScreen tapOnSlider(int percentage) {
        sliderLimitSeekBar().tapAt(percentage);
        return this;
    }

    public String getLegendLimitText() {
        return legendLimitText().getText();
    }

    private TextElement sliderLimitText() {
        return new TextElement(testDriver.findElement(With.id(SLIDER_LIMIT_TEXT)));
    }

    private TextElement legendLimitText() {
        return new TextElement(testDriver.findElement(With.id(LEGEND_LIMIT_TEXT)));
    }

    private SeekBarElement sliderLimitSeekBar() {
        return new SeekBarElement(testDriver.findElement(With.id(SLIDER_LIMIT_SEEK_BAR)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
