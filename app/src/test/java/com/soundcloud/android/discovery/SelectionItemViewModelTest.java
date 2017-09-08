package com.soundcloud.android.discovery;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.annotation.Nullable;

@RunWith(MockitoJUnitRunner.class)
public class SelectionItemViewModelTest {

    private static final Urn SELECTION_URN = Urn.forSystemPlaylist("upload");
    private static final String APP_LINK = "appLink";
    private static final String WEB_LINK = "webLink";

    @Test
    public void linkReturnsAppLinkWhenPresent() {
        final SelectionItemViewModel selectionItem = selectionItemWithLinks(APP_LINK, null);

        final String link = selectionItem.link();

        assertThat(link).isNotNull();
        assertThat(link).isEqualTo(APP_LINK);
    }

    @Test
    public void linkReturnsWebLinkWhenPresent() {
        final SelectionItemViewModel selectionItem = selectionItemWithLinks(null, WEB_LINK);

        final String link = selectionItem.link();

        assertThat(link).isNotNull();
        assertThat(link).isEqualTo(WEB_LINK);
    }

    @Test
    public void linkReturnsAppLinkWhenBothAppAndWebLinkPresent() {
        final SelectionItemViewModel selectionItem = selectionItemWithLinks(APP_LINK, WEB_LINK);

        final String link = selectionItem.link();

        assertThat(link).isNotNull();
        assertThat(link).isEqualTo(APP_LINK);
    }


    @Test
    public void linkAbsentWhenNeitherIsPresent() {
        final SelectionItemViewModel selectionItem = selectionItemWithLinks(null, null);

        final String link = selectionItem.link();

        assertThat(link).isNull();
    }

    private SelectionItemViewModel selectionItemWithLinks(@Nullable String appLink, @Nullable String webLink) {
        return new SelectionItemViewModel(null, SELECTION_URN, null, null, null, null, null, appLink, webLink, null);
    }
}
