package com.soundcloud.android.actionbar.menu;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.settings.SettingsActivity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;

public class DefaultActionMenuController implements ActionMenuController {

    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;

    @Inject
    public DefaultActionMenuController(CastConnectionHelper castConnectionHelper,
            EventBus eventBus) {
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment) {
        fragment.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main, menu);
        castConnectionHelper.addMediaRouterButton(menu, R.id.media_route_menu_item);
    }

    @Override
    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                fragment.startActivity(new Intent(fragment.getActivity(), SearchActivity.class));
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromSearchAction());
                return true;
            case R.id.action_settings:
                fragment.startActivity(new Intent(fragment.getActivity(), SettingsActivity.class));
                return true;
            case R.id.action_record:
                fragment.startActivity(new Intent(fragment.getActivity(), RecordActivity.class));
                return true;
            case R.id.action_who_to_follow:
                fragment.startActivity(new Intent(fragment.getActivity(), WhoToFollowActivity.class));
                return true;
            case R.id.action_activity:
                fragment.startActivity(new Intent(fragment.getActivity(), ActivitiesActivity.class));
                return true;
            default:
                return false;
        }
    }
}
