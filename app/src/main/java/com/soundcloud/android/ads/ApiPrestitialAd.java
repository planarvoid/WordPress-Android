package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.events.AdsReceived;
import com.soundcloud.android.model.Urn;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import java.util.List;
import java.util.Map;

class ApiPrestitialAd extends ModelCollection<ApiAdWrapper> implements AdsCollection {

    public ApiPrestitialAd(@JsonProperty("collection") List<ApiAdWrapper> collection,
                           @JsonProperty("_links") Map<String, Link> links,
                           @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    ApiPrestitialAd(List<ApiAdWrapper> collection) {
        super(collection);
    }

    private Optional<VisualPrestitialAd.ApiModel> visualPrestitial() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.visualPrestitial().isPresent()) {
                return adWrapper.visualPrestitial();
            }
        }
        return Optional.absent();
    }

    private Optional<SponsoredSessionAd.ApiModel> sponsoredSession() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.sponsoredSession().isPresent()) {
                return adWrapper.sponsoredSession();
            }
        }
        return Optional.absent();
    }


    @Override
    public AdsReceived toAdsReceived() {
        final Urn displayUrn = visualPrestitial().isPresent()
                               ? visualPrestitial().get().adUrn()
                               : Urn.NOT_SET;
        final Urn sponsoredSessionUrn = sponsoredSession().isPresent()
                                        ? sponsoredSession().get().adUrn()
                                        : Urn.NOT_SET;
        return AdsReceived.forPrestitalAds(displayUrn, sponsoredSessionUrn);
    }

    @Override
    public String contentString() {
        final StringBuilder msg = new StringBuilder(100);
        msg.append("prestitials received:");
        for (ApiAdWrapper wrapper : collection) {
            if (wrapper.visualPrestitial().isPresent()) {
                msg.append(" display");
            } else if (wrapper.sponsoredSession().isPresent()) {
                msg.append(" sponsored session w/ length: ");
                msg.append(wrapper.sponsoredSession().get().adFreeMinutes());
            }
        }
        return msg.toString();
    }

    Optional<AdData> toAdData() {
        for (ApiAdWrapper wrapper : collection) {
            if (wrapper.visualPrestitial().isPresent()) {
                return Optional.of(VisualPrestitialAd.create(wrapper.visualPrestitial().get()));
            } else if (wrapper.sponsoredSession().isPresent()) {
                return Optional.of(SponsoredSessionAd.create(wrapper.sponsoredSession().get()));
            }
        }
        return Optional.absent();
    }
}
