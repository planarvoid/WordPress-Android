package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.record.RecordScreen;

public class MoreScreen extends Screen {

    public MoreScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public ActivitiesScreen clickActivitiesLink() {
        activityLink().click();
        return new ActivitiesScreen(testDriver);
    }

    public ProfileScreen clickMyProfileLink() {
        headerLayout().click();
        return new ProfileScreen(testDriver);
    }

    public RecordScreen clickRecordLink() {
        recordLink().click();
        return new RecordScreen(testDriver);
    }

    public BasicSettingsScreen clickBasicSettingsLink() {
        basicSettingsLink().click();
        return new BasicSettingsScreen(testDriver);
    }

    public OfflineSettingsScreen clickOfflineSettingsLink() {
        offlineSettingsLink().click();
        return new OfflineSettingsScreen(testDriver);
    }

    public UpgradeScreen clickSubscribe() {
        upgradeLink().click();
        return new UpgradeScreen(testDriver);
    }

    public HomeScreen clickLogoutAndConfirm() {
        signoutLink().click();
        testDriver.clickOnText(android.R.string.ok);
        return new HomeScreen(testDriver);
    }

    public String getUserName() {
        return username().getText();
    }

    private ViewElement headerLayout() {
        return testDriver.findOnScreenElement(With.id(R.id.header_layout));
    }

    private ViewElement activityLink() {
        return testDriver.findOnScreenElement(With.id(R.id.more_activity_link));
    }

    private ViewElement recordLink() {
        return testDriver.findOnScreenElement(With.id(R.id.more_record_link));
    }

    private ViewElement basicSettingsLink() {
        return testDriver.findOnScreenElement(With.id(R.id.more_basic_settings_link));
    }

    private ViewElement offlineSettingsLink() {
        return testDriver.findOnScreenElement(With.id(R.id.more_offline_sync_settings_link));
    }

    private ViewElement upgradeLink() {
        return testDriver.findOnScreenElement(With.id(R.id.more_upsell));
    }

    private ViewElement signoutLink() {
        testDriver.scrollToBottom();
        return testDriver.findOnScreenElement(With.id(R.id.more_sign_out_link));
    }

    private TextElement username() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.username)));
    }
}
