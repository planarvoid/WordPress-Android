package com.soundcloud.android.discovery;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.NavigationDelegate;
import com.soundcloud.android.main.NavigationTarget;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.optional.Optional;
import org.codehaus.plexus.util.cli.Arg;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.v4.app.FragmentActivity;
import android.view.View;

@SuppressWarnings("ConstantConditions")
@RunWith(MockitoJUnitRunner.class)
public class SelectionItemTest {

    private static final Screen SCREEN = Screen.DISCOVER;

    @Mock private View view;
    @Mock private FragmentActivity activityContext;
    @Mock private NavigationDelegate navigationDelegate;
    @Captor private ArgumentCaptor<NavigationTarget> navigationTargetArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(view.getContext()).thenReturn(activityContext);
    }

    @Test
    public void onClickHandlerNavigatesToAppLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of("appLink"), absent());

        selectionItem.onClickListener(navigationDelegate, SCREEN).onClick(view);

        verifyNavigationTarget("appLink", absent());
    }

    @Test
    public void onClickHandlerNavigatesToWebLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), of("webLink"));

        selectionItem.onClickListener(navigationDelegate, SCREEN).onClick(view);

        verifyNavigationTarget("webLink", of("webLink"));
    }

    @Test
    public void onClickHandlerNavigatesToAppLinkWhenBothAppAndWebLinkPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of("appLink"), of("webLink"));

        selectionItem.onClickListener(navigationDelegate, SCREEN).onClick(view);

        verifyNavigationTarget("appLink", of("webLink"));
    }


    @Test
    public void onClickHandlerDoesntNavigateWhenNeitherAppNorWebLinkPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), absent());

        assertThat(selectionItem.onClickListener(navigationDelegate, SCREEN)).isNull();
    }

    private void verifyNavigationTarget(String target, Optional<String> fallback) {
        verify(navigationDelegate).navigateTo(navigationTargetArgumentCaptor.capture());
        final NavigationTarget navigationTarget = navigationTargetArgumentCaptor.getValue();
        assertThat(navigationTarget.target()).isEqualTo(target);
        assertThat(navigationTarget.screen()).isEqualTo(SCREEN);
        assertThat(navigationTarget.fallback()).isEqualTo(fallback);
    }

    private SelectionItem selectionItemWithLinks(Optional<String> appLink, Optional<String> webLink) {
        return SelectionItem.create(absent(), absent(), absent(), absent(), absent(), absent(), appLink, webLink);
    }
}
