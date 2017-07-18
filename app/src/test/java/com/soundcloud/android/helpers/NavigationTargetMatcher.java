package com.soundcloud.android.helpers;


import com.soundcloud.android.navigation.NavigationTarget;
import org.mockito.ArgumentMatcher;

public class NavigationTargetMatcher {

    public static ArgumentMatcher<NavigationTarget> matchesNavigationTarget(final NavigationTarget navigationTarget) {
        // match on every field of NavigationTarget except creationDate
        return expectedNavigationTarget -> expectedNavigationTarget.deeplink().equals(navigationTarget.deeplink()) &&
                expectedNavigationTarget.linkNavigationParameters().equals(navigationTarget.linkNavigationParameters()) &&
                expectedNavigationTarget.deeplinkTarget().equals(navigationTarget.deeplinkTarget()) &&
                expectedNavigationTarget.screen().equals(navigationTarget.screen()) &&
                expectedNavigationTarget.referrer().equals(navigationTarget.referrer()) &&
                expectedNavigationTarget.queryUrn().equals(navigationTarget.queryUrn()) &&
                expectedNavigationTarget.targetUrn().equals(navigationTarget.targetUrn()) &&
                expectedNavigationTarget.discoverySource().equals(navigationTarget.discoverySource()) &&
                expectedNavigationTarget.topResultsMetaData().equals(navigationTarget.topResultsMetaData()) &&
                expectedNavigationTarget.stationsInfoMetaData().equals(navigationTarget.stationsInfoMetaData()) &&
                expectedNavigationTarget.searchQuerySourceInfo().equals(navigationTarget.searchQuerySourceInfo()) &&
                expectedNavigationTarget.promotedSourceInfo().equals(navigationTarget.promotedSourceInfo()) &&
                expectedNavigationTarget.chartsMetaData().equals(navigationTarget.chartsMetaData()) &&
                expectedNavigationTarget.uiEvent().equals(navigationTarget.uiEvent());
    }
}
