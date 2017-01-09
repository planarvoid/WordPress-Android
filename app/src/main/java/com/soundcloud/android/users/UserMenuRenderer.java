package com.soundcloud.android.users;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class UserMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    private User user;

    interface Listener {

        void handleToggleFollow(User user);

        void handleOpenStation(Context context, User user);

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

    void render(User user, boolean showFollowAction) {
        this.user = user;

        updateFollowAction(user, showFollowAction);
        menu.show();
    }

    private void updateFollowAction(User user, boolean showFollowAction) {
        final MenuItem item = menu.findItem(R.id.toggle_follow);
        updateFollowActionVisibility(item, showFollowAction);
        updateFollowActionTitle(item, user.isFollowing());
    }

    private void updateFollowActionVisibility(MenuItem item, boolean showFollowAction) {
        item.setVisible(showFollowAction);
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
