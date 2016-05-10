package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import android.util.Log;

import javax.inject.Inject;
import java.util.List;

class ChartsPresenter {
    @Inject
    ChartsPresenter() {
    }

    void onNewAndHotClicked() {
        Log.d("charts", "new and hot");
    }

    void onTopFiftyClicked() {
        Log.d("charts", "top 50");
    }

    Observable<List<DiscoveryItem>> buildObservable() {
        final ImageResourceStub imageResource = new ImageResourceStub(Urn.forTrack(123));
        final List<DiscoveryItem> value = Lists.newArrayList((DiscoveryItem) new ChartItem(Lists.newArrayList(imageResource, imageResource, imageResource), Lists.newArrayList(imageResource, imageResource, imageResource)));
        return Observable.just(value);
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
