package com.soundcloud.android.users;

import com.google.auto.factory.AutoFactory;

import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class UserMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    private UserItem user;

    interface Listener {

        void handleToggleFollow(UserItem user);

        void handleOpenStation(Context context, UserItem user);

        void onDismiss();

    }

    private final Listener listener;

    private PopupMenuWrapper menu;

    public UserMenuRenderer(Listener listener,
                            View button,
                            @Provided PopupMenuWrapper.Factory menuFactory) {

        this.listener = listener;
        this.menu = menuFactory.build(button.getContext(), button);

        menu.inflate(R.menu.user_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
    }

    void render(UserItem user, Urn loggedInUser) {
        this.user = user;

        final boolean isLoggedInUser = user.getUrn().equals(loggedInUser);
        updateFollowAction(user, isLoggedInUser);
        menu.show();
    }

    private void updateFollowAction(UserItem user, boolean isLoggedInUser) {
        final MenuItem item = menu.findItem(R.id.toggle_follow);
        updateFollowActionVisibility(item, isLoggedInUser);
        updateFollowActionTitle(item, user.isFollowedByMe());
    }

    private void updateFollowActionVisibility(MenuItem item, boolean isLoggedInUser) {
        item.setVisible(!isLoggedInUser);
    }

    private void updateFollowActionTitle(MenuItem item, boolean isFollowed) {
        item.setTitle(isFollowed ? R.string.btn_unfollow : R.string.btn_follow);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.toggle_follow:
                listener.handleToggleFollow(user);
                return true;
            case R.id.open_station:
                listener.handleOpenStation(context, user);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDismiss() {
        menu = null;
        listener.onDismiss();
    }

}
