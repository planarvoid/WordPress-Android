package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.view.GridViewCompat;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

class SuggestedUsersAdapter extends BaseAdapter {

    private ImageOperations mImageOperations;

    private final List<SuggestedUser> mSuggestedUsers;

    public SuggestedUsersAdapter(List<SuggestedUser> suggestedUsers, ImageOperations imageOperations) {
        mSuggestedUsers = suggestedUsers;
        mImageOperations = imageOperations;
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
            viewHolder = getItemViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final boolean checked = ((GridViewCompat) parent).getCheckedItemPositions().get(position);
            ((SuggestedUserItemLayout) convertView).setChecked(checked);
        }

        configureViewHolder(getItem(position), viewHolder);
        return convertView;
    }

    private ItemViewHolder getItemViewHolder(View convertView) {
        ItemViewHolder viewHolder;
        viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) convertView.findViewById(R.id.user_image);
        viewHolder.username = (TextView) convertView.findViewById(R.id.username);
        viewHolder.location = (TextView) convertView.findViewById(R.id.location);
        viewHolder.toggleFollow = (ToggleButton) convertView.findViewById(R.id.toggle_btn_follow);
        viewHolder.toggleFollow.setFocusable(false);
        viewHolder.toggleFollow.setClickable(false);
        return viewHolder;
    }

    private void configureViewHolder(SuggestedUser suggestedUser, ItemViewHolder viewHolder) {
        viewHolder.username.setText(suggestedUser.getUsername());
        final String location = suggestedUser.getLocation();
        if (TextUtils.isEmpty(location)) {
            viewHolder.location.setVisibility(View.GONE);
        } else {
            viewHolder.location.setText(location);
            viewHolder.location.setVisibility(View.VISIBLE);
        }

        Resources resources = viewHolder.imageView.getResources();
        mImageOperations.displayInAdapterView(
                suggestedUser.getUrn(),
                ImageSize.getFullImageSize(resources),
                viewHolder.imageView);
    }

    private static class ItemViewHolder {
        public ImageView imageView;
        public ToggleButton toggleFollow;
        public TextView username, location;
    }
}
