package com.soundcloud.android.discovery;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class SelectionItemTest {

    @Mock private View view;
    @Mock private FragmentActivity activityContext;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        when(view.getContext()).thenReturn(activityContext);
    }

    @Test
    public void onClickHandlerNavigatesToAppLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of("appLink"), absent());

        selectionItem.onClickListener(navigator).onClick(view);

        verify(navigator).openLink(activityContext, "appLink");
    }

    @Test
    public void onClickHandlerNavigatesToWebLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), of("webLink"));

        selectionItem.onClickListener(navigator).onClick(view);

        verify(navigator).openLink(activityContext, "webLink");
    }


    @Test
    public void onClickHandlerNavigatesToAppLinkWhenBothAppAndWebLinkPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of("appLink"), of("webLink"));

        selectionItem.onClickListener(navigator).onClick(view);

        verify(navigator).openLink(activityContext, "appLink");
    }

    @Test
    public void onClickHandlerDoesntNavigateWhenNeitherAppNorWebLinkPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), absent());

        assertThat(selectionItem.onClickListener(navigator)).isNull();
    }

    private SelectionItem selectionItemWithLinks(Optional<String> appLink, Optional<String> webLink) {
        return SelectionItem.create(absent(), absent(), absent(), absent(), absent(), appLink, webLink);
    }
}
