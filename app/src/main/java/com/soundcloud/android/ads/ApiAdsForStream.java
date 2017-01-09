package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ApiAdsForStream extends ModelCollection<ApiAdWrapper> implements AdsCollection {

    private static final Predicate<ApiAdWrapper> IS_APP_INSTALL = input -> input != null && input.getAppInstall().isPresent();

    private static final class ToAppInstall implements Function<ApiAdWrapper, AppInstallAd> {
        private final DateProvider dateProvider;

        private ToAppInstall(DateProvider dateProvider) {
            this.dateProvider = dateProvider;
        }

        @Override
        public AppInstallAd apply(ApiAdWrapper input) {
            return AppInstallAd.create(input.getAppInstall().get(), dateProvider.getCurrentTime());
        }
    };

    public ApiAdsForStream(@JsonProperty("collection") List<ApiAdWrapper> collection,
                           @JsonProperty("_links") Map<String, Link> links,
                           @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    public ApiAdsForStream(List<ApiAdWrapper> collection) {
        super(collection);
    }

    public List<AppInstallAd> getAppInstalls(DateProvider dateProvider) {
        final ArrayList<ApiAdWrapper> appInstallWrappers =
                Lists.newArrayList(Iterables.filter(getCollection(), IS_APP_INSTALL));

        return Lists.transform(appInstallWrappers, new ToAppInstall(dateProvider));
    }

    @Override
    public AdRequestEvent.AdsReceived toAdsReceived() {
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

        return AdRequestEvent.AdsReceived.forStreamAds(appInstalls, videoAds);
    }

    @Override
    public String contentString() {
        return getCollection().size() + " ads";
    }
}
