package com.soundcloud.android.tests.killswitch;

import static android.app.Instrumentation.ActivityMonitor;
import static android.content.Intent.ACTION_VIEW;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static junit.framework.Assert.assertEquals;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ForceUpdateDialogElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;

public class KillSwitchTest extends ActivityTest<MainActivity> {

    private ForceUpdateDialogElement forceUpdateDialogElement;
    private ActivityMonitor monitor;

    public KillSwitchTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        forceUpdateDialogElement = new ForceUpdateDialogElement(solo);
    }

    @Override
    protected void addActivityMonitors(Instrumentation instrumentation) {
        IntentFilter intentFilter = new IntentFilter(ACTION_VIEW);
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
                        .willReturn(aResponse().withStatus(200).withBody(body)));
    }

    @Test
    public void testKillSwitchIsShown() throws Exception {
        assertEquals(0, monitor.getHits());

        this.forceUpdateDialogElement.clickUpgrade();

        monitor.waitForActivityWithTimeout(5000);
        assertEquals(1, monitor.getHits());
    }

}
