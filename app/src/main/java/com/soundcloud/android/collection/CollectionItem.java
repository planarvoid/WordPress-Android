package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.optional.Optional;

public abstract class CollectionItem implements ListItem {

    static final int TYPE_PREVIEW = 0;
    static final int TYPE_ONBOARDING = 1;
    static final int TYPE_OFFLINE_ONBOARDING = 2;
    static final int TYPE_UPSELL = 3;
    protected static final int TYPE_RECENTLY_PLAYED_BUCKET = 4;
    protected static final int TYPE_PLAY_HISTORY_BUCKET = 5;

    public abstract int getType();

    boolean isSingleSpan() {
        return false;
    }

    @Override
    public Urn getUrn() {
        return Urn.NOT_SET;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    @AutoValue
    abstract static class OnboardingCollectionItem extends CollectionItem {
        static OnboardingCollectionItem create() {
            return new AutoValue_CollectionItem_OnboardingCollectionItem(CollectionItem.TYPE_ONBOARDING);
        }
    }

    @AutoValue
    abstract static class OfflineOnboardingCollectionItem extends CollectionItem {
        static OfflineOnboardingCollectionItem create() {
            return new AutoValue_CollectionItem_OfflineOnboardingCollectionItem(CollectionItem.TYPE_OFFLINE_ONBOARDING);
        }
    }

    @AutoValue
    abstract static class UpsellCollectionItem extends CollectionItem {
        static UpsellCollectionItem create() {
            return new AutoValue_CollectionItem_UpsellCollectionItem(CollectionItem.TYPE_UPSELL);
        }
    }
}
