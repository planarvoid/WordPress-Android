package com.soundcloud.android.view;


import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.Xml;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainMenu extends LinearLayout {
    private int mSelectedMenuId = -1;
    private @NotNull ListView mList;
    private @NotNull MenuAdapter mMenuAdapter;
    private OnMenuItemClickListener mClickListener;


    @SuppressWarnings("UnusedDeclaration")
    public MainMenu(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public MainMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Callback invoked when a menu item is clicked.
     */
    public static interface OnMenuItemClickListener {
        public boolean onMenuItemClicked(int id);
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater.from(getContext()).inflate(R.layout.main_menu, this, true);
        mList = (ListView) findViewById(R.id.root_menu_list);
        mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mClickListener != null) {
                    final int itemId = ((SimpleListMenuItem) mMenuAdapter.getItem(position - mList.getHeaderViewsCount())).id;
                    if (mSelectedMenuId != itemId) {
                        if (mClickListener.onMenuItemClicked(itemId)) {
                            mList.setItemChecked(position, true);
                        }
                    }
                }
            }
        });
        mList.setSelector(getContext().getResources().getDrawable(R.drawable.sidebar_item_background));

        mMenuAdapter = new MenuAdapter(getContext());
        mList.setAdapter(mMenuAdapter);

        // observe user data to refresh icon (used after signup)
        getContext().getContentResolver().registerContentObserver(Content.ME.uri, false, mObserver);
    }

    public void refresh() {
        mMenuAdapter.notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        mClickListener = onMenuItemClickListener;
    }

    public void setSelectedMenuId(int selectedMenuId) {
        mSelectedMenuId = selectedMenuId;
        setSelectedMenuItem();
    }

    public void setMenuItems(int menu) {
        mMenuAdapter.clear();
        XmlResourceParser parser = null;
        try {
            parser = getResources().getLayout(menu);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("item".equals(parser.getName())) {
                        SimpleListMenuItem menuItem = new SimpleListMenuItem(getContext().obtainStyledAttributes(attrs, R.styleable.SimpleMenu));
                        mMenuAdapter.addItem(menuItem);
                    }
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            throw new InflateException("Error inflating menu XML", e);
        } catch (IOException e) {
            throw new InflateException("Error inflating menu XML", e);
        } finally {
            if (parser != null) parser.close();
        }

        mList.setAdapter(mMenuAdapter);
    }

    public void onResume() {
        setSelectedMenuItem();
    }

    public void onDestroy() {
        getContext().getContentResolver().unregisterContentObserver(mObserver);
    }

    public void setOffsetRight(int offsetRight) {
        mMenuAdapter.setOffsetRight(offsetRight);
    }

    private void setSelectedMenuItem(){
        if (mSelectedMenuId != -1){
            final int pos = mMenuAdapter.getPositionById(mSelectedMenuId);
            if (pos >= 0 && mList.getCheckedItemPosition() != pos) {
                mList.setItemChecked(pos, true);
            }
        }
        mMenuAdapter.notifyDataSetChanged();
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mMenuAdapter.notifyDataSetChanged();
        }
    };

    private static class SimpleListMenuItem {
        final int id;
        final CharSequence text;
        final Drawable icon;
        final int layoutId;

        public SimpleListMenuItem(TypedArray a) {
            id = a.getResourceId(R.styleable.SimpleMenu_android_id, 0);
            text = a.getText(R.styleable.SimpleMenu_android_text);
            icon = a.getDrawable(R.styleable.SimpleMenu_android_icon);
            layoutId = a.getResourceId(R.styleable.SimpleMenu_android_layout, 0);
        }
    }

    private static class MenuAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<SimpleListMenuItem> mMenuItems;
        private final SparseIntArray mLayouts;
        private int mOffsetRight;
        private final Context mContext;

        public MenuAdapter(Context context) {
            mContext = context;
            inflater = LayoutInflater.from(context);
            mMenuItems = new ArrayList<SimpleListMenuItem>();
            mLayouts = new SparseIntArray();
            mLayouts.put(R.layout.main_menu_item, 0);
        }

        void clear() {
            mMenuItems.clear();
        }

        public void setOffsetRight(int offsetRight){
            if (offsetRight != mOffsetRight){
                mOffsetRight = offsetRight;
                notifyDataSetChanged();
            }
        }

        private void addItem(SimpleListMenuItem item) {
            mMenuItems.add(item);
            if (mLayouts.get(item.layoutId, -1) == -1) {
                mLayouts.put(item.layoutId, mLayouts.size());
            }
        }

        @Override
        public int getCount() {
            return mMenuItems.size();
        }

        @Override
        public int getViewTypeCount() {
            return mLayouts.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mLayouts.get(mMenuItems.get(position).layoutId, 0);
        }

        @Override
        public Object getItem(int position) {
            return mMenuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMenuItems.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final SimpleListMenuItem menuItem = mMenuItems.get(position);
            final ViewHolder holder;

            if (convertView == null) {
                final int layout_id = mMenuItems.get(position).layoutId;
                convertView = inflater.inflate(layout_id == 0 ? R.layout.main_menu_item : layout_id, null);

                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.main_menu_item_icon);
                holder.text = (TextView) convertView.findViewById(R.id.main_menu_item_text);
                holder.counter = (TextView) convertView.findViewById(R.id.main_menu_dashboard_counter);

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            switch (menuItem.id) {
                case (R.id.nav_stream):
                    final int streamCount = ContentStats.count(Content.ME_SOUND_STREAM);
                    if (streamCount > 0){
                        holder.counter.setText(streamCount > 99 ? "99+" : String.valueOf(streamCount));
                        holder.counter.setVisibility(View.VISIBLE);
                    } else {
                        holder.counter.setVisibility(View.GONE);
                    }
                    break;
                case (R.id.nav_news):
                    final int activitiesCount =  ContentStats.count(Content.ME_ACTIVITIES);
                    if (activitiesCount > 0){
                        holder.counter.setText(activitiesCount > 99 ? "99+" : String.valueOf(activitiesCount));
                        holder.counter.setVisibility(View.VISIBLE);
                    } else {
                        holder.counter.setVisibility(View.GONE);
                    }
                    break;
                default:
                    if (holder.counter != null) holder.counter.setVisibility(View.GONE);
            }

            boolean setDefaultImage = true;
            if (menuItem.id == R.id.nav_you) {
                final User u = SoundCloudApplication.fromContext(mContext).getLoggedInUser();
                if (u != null) {
                    holder.text.setText(u.username);
                    final String listAvatarUri = u.getListAvatarUri(mContext);
                    setDefaultImage = TextUtils.isEmpty(listAvatarUri) ||
                                      ImageLoader.get(mContext).bind(this, holder.image, listAvatarUri) != ImageLoader.BindResult.OK;
                } else {
                    holder.text.setText(menuItem.text);
                }
            } else {
                holder.text.setText(menuItem.text);
            }

            if (setDefaultImage) {
                holder.image.setImageDrawable(menuItem.icon);
            }

            if (mOffsetRight > 0){
                // serious coupling action here. should find something better
                if (convertView instanceof LinearLayout){
                    ((LayoutParams) ((LinearLayout) convertView).getChildAt(((LinearLayout) convertView).getChildCount() - 1).getLayoutParams()).rightMargin = mOffsetRight;
                    convertView.invalidate();
                }
            }
            return convertView;
        }

        private int getPositionById(int mSelectedMenuId) {
            int i = 0;
            for (SimpleListMenuItem menuItem : mMenuItems){
                if (mSelectedMenuId == menuItem.id) return i;
                i++;
            }
            return -1;
        }

        private static class ViewHolder {
            TextView text;
            TextView counter;
            ImageView image;
        }
    }
}