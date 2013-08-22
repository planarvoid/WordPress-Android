package com.soundcloud.android.adapter;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.view.GridViewCompat;
import com.soundcloud.android.view.SuggestedUserItemLayout;
import com.soundcloud.android.view.adapter.GridSpacer;

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

public class SuggestedUsersAdapter extends BaseAdapter {

    private final List<SuggestedUser> mSuggestedUsers;
    private DisplayImageOptions mDisplayImageOptions = ImageOptionsFactory.adapterView(R.drawable.placeholder_cells);
    private GridSpacer mGridSpacer;

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
        getGridSpacer(convertView.getResources()).configureItemPadding(convertView, position, getCount());

        return convertView;
    }

    private ItemViewHolder getItemViewHolder(View convertView) {
        ItemViewHolder viewHolder;
        viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) convertView.findViewById(R.id.suggested_user_image);
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
        ImageLoader.getInstance().displayImage(suggestedUser.getAvatarUrl(), viewHolder.imageView, mDisplayImageOptions);
    }

    /**
     * Lazy Grid Spacer initialization as it needs resources for configuration
     */
    private GridSpacer getGridSpacer(Resources resources){
        if (mGridSpacer == null) {
            mGridSpacer = new GridSpacer(
                    resources.getDimensionPixelSize(R.dimen.onboarding_suggested_user_item_spacing),
                    resources.getDimensionPixelSize(R.dimen.onboarding_suggested_user_item_spacing),
                    resources.getInteger(R.integer.suggested_user_grid_num_columns)
            );
        }
        return mGridSpacer;
    }

    private static class ItemViewHolder {
        public ImageView imageView;
        public ToggleButton toggleFollow;
        public TextView username, location;
    }
}
