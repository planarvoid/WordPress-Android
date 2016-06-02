package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class OnboardingCollectionItem extends CollectionItem {

    static OnboardingCollectionItem create() {
        return new AutoValue_OnboardingCollectionItem(CollectionItem.TYPE_PLAYLIST_EMPTY);
    }
}
