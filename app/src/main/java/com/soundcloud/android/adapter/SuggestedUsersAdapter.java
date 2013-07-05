package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.view.GridViewCompat;
import com.soundcloud.android.view.SuggestedUserItemLayout;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

public class SuggestedUsersAdapter extends BaseAdapter {

    private static final ImageLoader.Options IMAGE_OPTIONS = ImageLoader.Options.listFadeIn();
    private final List<SuggestedUser> mSuggestedUsers;

    public SuggestedUsersAdapter(List<SuggestedUser> suggestedUsers) {
        mSuggestedUsers = suggestedUsers;
    }

    @Override
    public int getCount() {
        return mSuggestedUsers.size();
    }

    @Override
    public SuggestedUser getItem(int position) {
        return mSuggestedUsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mSuggestedUsers.get(position).getId();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridviewcompat getCheckedItemPositions
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder viewHolder;
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.suggested_user_grid_item,null);
            viewHolder = new ItemViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.suggested_user_image);
            viewHolder.username = (TextView) convertView.findViewById(R.id.username);
            viewHolder.location = (TextView) convertView.findViewById(R.id.location);
            viewHolder.toggleFollow = (ToggleButton) convertView.findViewById(R.id.toggle_btn_follow);
            viewHolder.toggleFollow.setFocusable(false);
            viewHolder.toggleFollow.setClickable(false);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final boolean checked = ((GridViewCompat) parent).getCheckedItemPositions().get(position);
            ((SuggestedUserItemLayout) convertView).setChecked(checked);
        }

        final SuggestedUser suggestedUser = getItem(position);
        viewHolder.username.setText(suggestedUser.getUsername());
        viewHolder.location.setText(suggestedUser.getLocation());

        final ImageLoader.BindResult result = ImageLoader.get(parent.getContext()).bind(viewHolder.imageView,
                suggestedUser.getAvatarUrl(), null, IMAGE_OPTIONS);
        if (result != ImageLoader.BindResult.OK) {
            viewHolder.imageView.setImageResource(R.drawable.placeholder_cells);
        }
        return convertView;
    }

    private static class ItemViewHolder {
        public ImageView imageView;
        public ToggleButton toggleFollow;
        public TextView username, location;
    }
}
