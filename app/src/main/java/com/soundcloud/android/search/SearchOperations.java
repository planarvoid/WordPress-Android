package com.soundcloud.android.search;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
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
class SearchOperations {

    enum ContentType {
        NORMAL, PREMIUM
    }

    private final SearchStrategyFactory searchStrategyFactory;

    @Inject
    SearchOperations(SearchStrategyFactory searchStrategyFactory) {
        this.searchStrategyFactory = searchStrategyFactory;
    }

    Observable<SearchResult> searchResult(String query, Optional<Urn> queryUrn, SearchType searchType) {
        return searchStrategyFactory.getSearchStrategy(searchType).searchResult(query, queryUrn, ContentType.NORMAL);
    }

    Observable<SearchResult> searchPremiumResultFrom(List<PropertySet> propertySets,
                                                     Optional<Link> nextHref,
                                                     Urn queryUrn) {
        final SearchResult searchResult = SearchResult.fromPropertySets(propertySets, nextHref, queryUrn);
        return Observable.just(searchResult);
    }

    Observable<SearchResult> searchPremiumResult(String query, SearchType searchType) {
        return searchStrategyFactory.getSearchStrategy(searchType).searchResult(query,
                                                                                Optional.<Urn>absent(),
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
            allUrns.addAll(PropertySets.extractUrns(removeFirstUpsellItemIfAny(searchResult.getItems())));

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

        private List<PropertySet> removeFirstUpsellItemIfAny(List<PropertySet> resultItems) {
            if (isFirstItemForUpsell(resultItems)) {
                return resultItems.subList(1, resultItems.size());
            }
            return resultItems;
        }

        private boolean isFirstItemForUpsell(List<PropertySet> propertySets) {
            return !propertySets.isEmpty() && propertySets.get(0)
                                                          .get(EntityProperty.URN)
                                                          .equals(SearchUpsellItem.UPSELL_URN);
        }

        @VisibleForTesting
        List<Urn> getAllUrns() {
            return allUrns;
        }
    }
}
