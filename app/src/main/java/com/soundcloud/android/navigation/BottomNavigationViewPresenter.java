package com.soundcloud.android.navigation;

import static butterknife.ButterKnife.findById;

import com.soundcloud.android.R;
import com.soundcloud.android.main.NavigationModel;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;

public class BottomNavigationViewPresenter extends DefaultActivityLightCycle<Activity> {

    public static class Default extends BottomNavigationViewPresenter {
        private final NavigationModel navigationModel;

        public Default(NavigationModel navigationModel) {
            this.navigationModel = navigationModel;
        }

        @Override
        public void onCreate(Activity host, @Nullable Bundle bundle) {
            super.onCreate(host, bundle);
            BottomNavigationView bottomNavigationView = findById(host, R.id.bottom_navigation_view);
            setup(bottomNavigationView);
        }

        private void setup(BottomNavigationView bottomNavigationView) {
            Menu menu = bottomNavigationView.getMenu();
            menu.clear();

            final Context context = bottomNavigationView.getContext();

            final int itemCount = navigationModel.getItemCount();
            for (int pageIndex = 0; pageIndex < itemCount; pageIndex++) {
                NavigationModel.Target target = navigationModel.getItem(pageIndex);
                MenuItem add = menu.add(0, pageIndex, pageIndex, context.getString(target.getName()));
                add.setIcon(target.getIcon());
            }
        }
    }

    public static class Noop extends BottomNavigationViewPresenter {

    }
}
