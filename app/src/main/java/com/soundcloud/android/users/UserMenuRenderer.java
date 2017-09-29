package com.soundcloud.android.users;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class UserMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    private UserItem userItem;

    interface Listener {

        void handleToggleFollow(UserItem userItem);

        void handleOpenStation(Activity activity, UserItem userItem);

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

    void render(UserItem userItem, boolean showFollowAction) {
        this.userItem = userItem;

        updateFollowAction(userItem.isFollowedByMe(), showFollowAction);
        menu.show();
    }

    private void updateFollowAction(boolean isFollowedByMe, boolean showFollowAction) {
        final MenuItem item = menu.findItem(R.id.toggle_follow);
        updateFollowActionVisibility(item, showFollowAction);
        updateFollowActionTitle(item, isFollowedByMe);
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
                listener.handleToggleFollow(userItem);
                return true;
            case R.id.open_station:
                listener.handleOpenStation(ViewUtils.getFragmentActivity(context), userItem);
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
