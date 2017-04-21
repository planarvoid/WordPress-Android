package com.soundcloud.android.home;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
class DiscoveryStorage {

    private ModelCollection<ApiDiscoveryCard> apiDiscoveryCards;

    @Inject
    DiscoveryStorage() {
    }

    public boolean store(ModelCollection<ApiDiscoveryCard> apiDiscoveryCards) {
        this.apiDiscoveryCards = filterInvalidCards(apiDiscoveryCards);
        return true;
    }

    private ModelCollection<ApiDiscoveryCard> filterInvalidCards(ModelCollection<ApiDiscoveryCard> apiDiscoveryCards) {
        return apiDiscoveryCards.copyWithItems(Lists.newArrayList(Iterables.filter(apiDiscoveryCards.getCollection(), this::isValidDiscoveryCard)));
    }

    private boolean isValidDiscoveryCard(ApiDiscoveryCard apiDiscoveryCard) {
        return apiDiscoveryCard.selectionCard().isPresent() || apiDiscoveryCard.singletonSelectionCard().isPresent();
    }

    Observable<List<ApiDiscoveryCard>> discoveryCards() {
        return Observable.just(apiDiscoveryCards.getCollection());
    }
}
