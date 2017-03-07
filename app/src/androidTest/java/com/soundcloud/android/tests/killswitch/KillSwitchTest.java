package com.soundcloud.android.tests.killswitch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ForceUpdateDialogElement;
import com.soundcloud.android.tests.ActivityTest;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;

public class KillSwitchTest extends ActivityTest<MainActivity> {

    private ForceUpdateDialogElement forceUpdateDialogElement;
    private Instrumentation.ActivityMonitor monitor;

    public KillSwitchTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        forceUpdateDialogElement = new ForceUpdateDialogElement(solo);
    }

    @Override
    protected void addActivityMonitors(Instrumentation instrumentation) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_VIEW);
        intentFilter.addDataScheme("market");
        intentFilter.addDataAuthority("details", null);
        monitor = instrumentation.addMonitor(intentFilter, null, false);
    }

    @Override
    protected void removeActivityMonitors(Instrumentation instrumentation) {
        instrumentation.removeMonitor(monitor);
    }

    @Override
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        String body = readBodyOfFile(resources, "android-configuration-killswitch.json");
        stubFor(get(urlPathMatching("/configuration/android"))
                                      .willReturn(aResponse().withStatus(200).withBody(body)));;
    }

    public void testKillSwitchIsShown() {
        assertEquals(0, monitor.getHits());

        this.forceUpdateDialogElement.clickUpgrade();

        monitor.waitForActivityWithTimeout(5000);
        assertEquals(1, monitor.getHits());
    }

}
