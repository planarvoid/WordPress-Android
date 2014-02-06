package com.soundcloud.android.screens.elements;


import com.soundcloud.android.R;
import com.soundcloud.android.screens.search.SearchPlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;

public class ActionBarElement {
    protected Han solo;
    protected Waiter waiter;

    private static final int SEARCH_SELECTOR = R.id.action_search;

    public ActionBarElement(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void clickHomeButton() {
        solo.clickOnActionBarHomeButton();
    }

    public SearchPlaylistTagsScreen clickSearchButton() {
        solo.clickOnActionBarItem(SEARCH_SELECTOR);
        return new SearchPlaylistTagsScreen(solo);
    }

    public void typeSearchQuery(String query) {
        KeyCharacterMap keymap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);
        for (KeyEvent key : keymap.getEvents(query.toCharArray())) {
            if(key.getAction() == KeyEvent.ACTION_DOWN) {
                solo.sendKey(key.getKeyCode());
            }
        }
    }

    public SearchResultsScreen doSearch(String query) {
        typeSearchQuery(query);
        solo.sendKey(KeyEvent.KEYCODE_ENTER);
        return new SearchResultsScreen(solo);
    }
}
