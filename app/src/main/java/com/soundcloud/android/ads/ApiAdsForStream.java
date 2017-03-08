package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.events.AdsReceived;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.soundcloud.java.collections.Lists.newArrayList;

class ApiAdsForStream extends ModelCollection<ApiAdWrapper> implements AdsCollection {

    private static final class ToAdData implements Function<ApiAdWrapper, AdData> {
        private final DateProvider dateProvider;

        private ToAdData(DateProvider dateProvider) {
            this.dateProvider = dateProvider;
        }

        @Override
        public AdData apply(ApiAdWrapper input) {
            if (input.getAppInstall().isPresent()) {
                return AppInstallAd.create(input.getAppInstall().get(), dateProvider.getCurrentTime());
            } else {
                return VideoAd.create(input.getVideoAd().get(), dateProvider.getCurrentTime(), AdData.MonetizationType.INLAY);
            }
        }
    }

    public ApiAdsForStream(@JsonProperty("collection") List<ApiAdWrapper> collection,
                           @JsonProperty("_links") Map<String, Link> links,
                           @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    ApiAdsForStream(List<ApiAdWrapper> collection) {
        super(collection);
    }

    List<AdData> getAds(DateProvider dateProvider) {
        return Lists.transform(newArrayList(Iterables.filter(getCollection(), supportedInlayAds())), new ToAdData(dateProvider));
    }

    private Predicate<ApiAdWrapper> supportedInlayAds() {
        return ad -> ad.getAppInstall().isPresent() || ad.getVideoAd().isPresent();
    }

    @Override
    public AdsReceived toAdsReceived() {
        final List<Urn> appInstalls = new ArrayList<>();
        final List<Urn> videoAds = new ArrayList<>();

        for (ApiAdWrapper ad : getCollection()) {
            final Optional<ApiAppInstallAd> appInstall = ad.getAppInstall();
            final Optional<ApiVideoAd> videoAd = ad.getVideoAd();
            if (appInstall.isPresent()) {
                appInstalls.add(appInstall.get().getAdUrn());
            } else if (videoAd.isPresent()) {
                videoAds.add(videoAd.get().getAdUrn());
            }
        }

        return AdsReceived.forStreamAds(appInstalls, videoAds);
    }

    @Override
    public String contentString() {
        return getCollection().size() + " ads";
    }
}
