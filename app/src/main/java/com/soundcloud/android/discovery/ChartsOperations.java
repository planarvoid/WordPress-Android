package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import javax.inject.Inject;

class ChartsOperations {
    private final FeatureFlags featureFlags;

    @Inject
    ChartsOperations(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    Observable<DiscoveryItem> charts() {
        if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)) {
            final ImageResourceStub imageResource = new ImageResourceStub(Urn.forTrack(123));
            final ChartsItem chartsItem = new ChartsItem(
                    Lists.newArrayList(imageResource, imageResource, imageResource),
                    Lists.newArrayList(imageResource, imageResource, imageResource));
            return Observable.just((DiscoveryItem) chartsItem);
        } else {
            return Observable.empty();
        }
    }

    void clearData() {
    }

    private static class ImageResourceStub implements ImageResource {
        private final Urn urn;

        public ImageResourceStub(Urn urn) {
            this.urn = urn;
        }

        @Override
        public Urn getUrn() {
            return urn;
        }

        @Override
        public Optional<String> getImageUrlTemplate() {
            return Optional.absent();
        }
    }
}

