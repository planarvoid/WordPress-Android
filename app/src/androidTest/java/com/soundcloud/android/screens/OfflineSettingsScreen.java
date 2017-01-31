package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.CheckableElement;
import com.soundcloud.android.framework.viewelements.SeekBarElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.settings.OfflineSettingsActivity;

import android.widget.Switch;

public class OfflineSettingsScreen extends Screen {

    private static final Class ACTIVITY = OfflineSettingsActivity.class;
    private static final int SLIDER_LIMIT_TEXT = R.id.offline_storage_limit;
    private static final int LEGEND_LIMIT_TEXT = R.id.offline_storage_legend_limit;
    private static final int SLIDER_LIMIT_SEEK_BAR = R.id.offline_storage_limit_seek_bar;

    public OfflineSettingsScreen(Han solo) {
        super(solo);
    }

    public OfflineSettingsScreen toggleSyncCollectionOn() {
        testDriver.clickOnText(R.string.pref_offline_offline_collection);
        return this;
    }

    public ConfirmDisableSyncCollectionScreen toggleSyncCollectionOff() {
        testDriver.clickOnText(R.string.pref_offline_offline_collection);
        return new ConfirmDisableSyncCollectionScreen(testDriver, ACTIVITY);
    }

    public ConfirmRemoveOfflineContentScreen clickRemoveOfflineContent() {
        testDriver.clickOnText(R.string.pref_offline_remove_all_offline_content);
        return new ConfirmRemoveOfflineContentScreen(testDriver);
    }

    public String getSliderLimitText() {
        return sliderLimitText().getText();
    }

    public OfflineSettingsScreen tapOnSlider(int percentage) {
        String text = legendLimitText().getText();
        sliderLimitSeekBar().tapAt(percentage);
        waiter.waitForElementTextToChange(legendLimitText(), text);
        return this;
    }

    public boolean isOfflineCollectionChecked() {
        return offlineCollectionCheckable().isChecked();
    }

    public String getLegendLimitText() {
        return legendLimitText().getText();
    }

    private TextElement sliderLimitText() {
        return new TextElement(testDriver.findOnScreenElement(With.id(SLIDER_LIMIT_TEXT)));
    }

    private TextElement legendLimitText() {
        return new TextElement(testDriver.findOnScreenElement(With.id(LEGEND_LIMIT_TEXT)));
    }

    private SeekBarElement sliderLimitSeekBar() {
        return new SeekBarElement(testDriver.findOnScreenElement(With.id(SLIDER_LIMIT_SEEK_BAR)));
    }

    private CheckableElement offlineCollectionCheckable() {
        return new CheckableElement(testDriver.findOnScreenElements(With.className(Switch.class.getName())).get(0));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
