package com.soundcloud.android.home;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
class HomeStorage {

    private ModelCollection<ApiHomeCard> apiHomeCards;

    @Inject
    HomeStorage() {
    }

    public boolean store(ModelCollection<ApiHomeCard> apiHomeCards) {
        this.apiHomeCards = filterInvalidCards(apiHomeCards);
        return true;
    }

    private ModelCollection<ApiHomeCard> filterInvalidCards(ModelCollection<ApiHomeCard> apiHomeCards) {
        return apiHomeCards.copyWithItems(Lists.newArrayList(Iterables.filter(apiHomeCards.getCollection(), this::isValidHomeCard)));
    }

    private boolean isValidHomeCard(ApiHomeCard apiHomeCard) {
        return apiHomeCard.selectionCard().isPresent() || apiHomeCard.singletonSelectionCard().isPresent();
    }

    Observable<List<ApiHomeCard>> homeCards() {
        return Observable.just(apiHomeCards.getCollection());
    }
}
