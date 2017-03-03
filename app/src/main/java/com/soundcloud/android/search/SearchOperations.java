package com.soundcloud.android.search;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rx.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(
        value = {"SE_BAD_FIELD"},
        justification = "we never serialize search operations")
public class SearchOperations {

    enum ContentType {
        NORMAL, PREMIUM
    }

    private final SearchStrategyFactory searchStrategyFactory;
    private final TrackRepository trackRepository;

    @Inject
    SearchOperations(SearchStrategyFactory searchStrategyFactory, TrackRepository trackRepository) {
        this.searchStrategyFactory = searchStrategyFactory;
        this.trackRepository = trackRepository;
    }

    public Observable<SearchResult> searchResult(String query, Optional<Urn> queryUrn, SearchType searchType) {
        return searchStrategyFactory.getSearchStrategy(searchType).searchResult(query, queryUrn, ContentType.NORMAL);
    }

    public Observable<SearchResult> searchResult(String query, Optional<Urn> queryUrn, SearchType searchType, ContentType contentType) {
        return searchStrategyFactory.getSearchStrategy(searchType).searchResult(query, queryUrn, contentType);
    }

    Observable<SearchResult> searchPremiumResultFrom(List<Urn> searchableItems,
                                                     Optional<Link> nextHref,
                                                     Urn queryUrn) {
        return trackRepository.trackListFromUrns(searchableItems)
                              .map(tracks -> Lists.transform(tracks, TrackItem::from))
                              .map(Lists::<ListItem>newArrayList)
                              .map(trackItems -> SearchResult.fromSearchableItems(trackItems, nextHref, queryUrn));
    }

    Observable<SearchResult> searchPremiumResult(String query, SearchType searchType) {
        return searchStrategyFactory.getSearchStrategy(searchType).searchResult(query,
                                                                                Optional.absent(),
                                                                                ContentType.PREMIUM);
    }

    SearchPagingFunction pagingFunction(final SearchType searchType) {
        return new SearchPagingFunction(searchType);
    }

    private Observable<SearchResult> nextResultPage(Link nextHref, SearchType searchType) {
        return searchStrategyFactory.getSearchStrategy(searchType).nextResultPage(nextHref);
    }

    class SearchPagingFunction implements PagingFunction<SearchResult> {

        private final SearchType searchType;
        private final List<Urn> allUrns = new ArrayList<>();

        private Urn queryUrn = Urn.NOT_SET;

        SearchPagingFunction(SearchType searchType) {
            this.searchType = searchType;
        }

        SearchQuerySourceInfo getSearchQuerySourceInfo(int clickPosition, Urn clickUrn, String query) {
            SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn, clickPosition, clickUrn, query);
            searchQuerySourceInfo.setQueryResults(allUrns);
            return searchQuerySourceInfo;
        }

        @Override
        public Observable<SearchResult> call(SearchResult searchResult) {
            addPremiumItem(searchResult.getPremiumContent());
            allUrns.addAll(Lists.transform(removeFirstUpsellItemIfAny(searchResult.getItems()), Entity::getUrn));

            final Optional<Urn> queryUrn = searchResult.queryUrn;
            if (queryUrn.isPresent()) {
                this.queryUrn = queryUrn.or(Urn.NOT_SET);
            }

            final Optional<Link> nextHref = searchResult.nextHref;
            if (nextHref.isPresent()) {
                return nextResultPage(nextHref.get(), searchType);
            } else {
                return Pager.finish();
            }
        }

        private void addPremiumItem(Optional<SearchResult> premiumContent) {
            if (premiumContent.isPresent()) {
                allUrns.add(premiumContent.get().getFirstItemUrn());
            }
        }

        private List<ListItem> removeFirstUpsellItemIfAny(List<ListItem> resultItems) {
            if (isFirstItemForUpsell(resultItems)) {
                return resultItems.subList(1, resultItems.size());
            }
            return resultItems;
        }

        private boolean isFirstItemForUpsell(List<ListItem> searchableItems) {
            return !searchableItems.isEmpty() && searchableItems.get(0)
                                                                .getUrn()
                                                                .equals(UpsellSearchableItem.UPSELL_URN);
        }

        @VisibleForTesting
        List<Urn> getAllUrns() {
            return allUrns;
        }
    }
}
