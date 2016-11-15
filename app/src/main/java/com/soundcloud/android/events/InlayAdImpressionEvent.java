package com.soundcloud.android.events;

import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;

import java.util.List;

public class InlayAdImpressionEvent extends LegacyTrackingEvent {
    private static final String impressionName = "app_install";
    private static final Screen page = Screen.STREAM;
    private static final String monetizationType = "mobile_inlay";
    private final Urn ad;
    private final int contextPosition;
    private final List<String> impressionUrls;

    public InlayAdImpressionEvent(AppInstallAd adData,
                                  int position,
                                  long timeStamp) {
        super(KIND_DEFAULT, timeStamp);
        this.ad = adData.getAdUrn();
        this.contextPosition = position;
        this.impressionUrls = adData.getImpressionUrls();
    }

    public int getContextPosition() {
        return contextPosition;
    }

    public Urn getAd() {
        return ad;
    }

    public String getMonetizationType() {
        return monetizationType;
    }

    public String getPageName() {
        return page.get();
    }

    public String getImpressionName() {
        return impressionName;
    }

    public List<String> getImpressionUrls() {
        return impressionUrls;
    }
}