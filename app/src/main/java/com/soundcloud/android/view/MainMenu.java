package com.soundcloud.android.view;


import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.SearchHistoryAdapter;
import com.soundcloud.android.adapter.SearchSuggestionsAdapter;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.model.User;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.Xml;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainMenu extends LinearLayout {
    private int mSelectedMenuId = -1;

    private ListView mList;
    private MenuAdapter mMenuAdapter;
    private OnMenuItemClickListener mClickListener;

    public void onResume() {
        setSelectedMenuItem();
    }

    /**
     * Callback invoked when a menu item is clicked.
     */
    public static interface OnMenuItemClickListener {

        public boolean onMenuItemClicked(int id);
    }

    public MainMenu(Context context) {
        super(context);
        init();
    }

    public MainMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
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

        mList.setSelector(getContext().getResources().getDrawable(R.drawable.selectable_background_next));
        mMenuAdapter = new MenuAdapter();
    }

    public void setOnItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        mClickListener = onMenuItemClickListener;
    }

    public void setSelectedMenuId(int mSelectedMenuId) {
        this.mSelectedMenuId = mSelectedMenuId;
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

    private void setSelectedMenuItem(){
        if (mSelectedMenuId != -1){
            final int pos = mMenuAdapter.getPositionById(mSelectedMenuId);
            if (pos >= 0 && mList.getCheckedItemPosition() != pos) {
                mList.setItemChecked(pos, true);
                mMenuAdapter.notifyDataSetChanged();
            }
        }
    }

    class SimpleListMenuItem {
        int id;
        CharSequence text;
        Drawable icon;
        int layoutId;

        public SimpleListMenuItem(TypedArray a) {
            this.id         = a.getResourceId(R.styleable.SimpleMenu_android_id, 0);
            this.text       = a.getText(R.styleable.SimpleMenu_android_text);
            this.icon       = a.getDrawable(R.styleable.SimpleMenu_android_icon);
            this.layoutId   = a.getResourceId(R.styleable.SimpleMenu_android_layout, 0);
        }
    }

    private class MenuAdapter extends BaseAdapter {
        final private LayoutInflater inflater;
        private List<SimpleListMenuItem> mMenuItems;
        private SparseIntArray mLayouts;

        public MenuAdapter() {
            inflater = LayoutInflater.from(getContext());
            mMenuItems = new ArrayList<SimpleListMenuItem>();
            mLayouts = new SparseIntArray();
            mLayouts.put(R.layout.main_menu_item, 0);
        }

        void clear() {
            mMenuItems.clear();
        }

        void addItem(SimpleListMenuItem item) {
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
                holder.image = (ImageView) convertView.findViewById(R.id.slm_item_icon);
                holder.text = (TextView) convertView.findViewById(R.id.slm_item_text);

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            boolean setDefaultImage = true;
            if (menuItem.id == R.id.nav_you) {
                final User u = SoundCloudApplication.fromContext(getContext()).getLoggedInUser();
                if (u != null) {
                    holder.text.setText(u.username);
                    final String listAvatarUri = u.getListAvatarUri(getContext());
                    setDefaultImage = TextUtils.isEmpty(listAvatarUri) || ImageLoader.get(getContext()).bind(this, holder.image, listAvatarUri) != ImageLoader.BindResult.OK;
                } else {
                    holder.text.setText(menuItem.text);
                }
            } else {
                holder.text.setText(menuItem.text);
            }

            if (setDefaultImage) {
                holder.image.setImageDrawable(menuItem.icon);
            }

            if (menuItem.id == mSelectedMenuId){
                convertView.setSelected(true);
            }

            return convertView;
        }

        public int getPositionById(int mSelectedMenuId) {
            int i = 0;
            for (SimpleListMenuItem menuItem : mMenuItems){
                if (mSelectedMenuId == menuItem.id) return i;
                i++;
            }
            return -1;
        }

        class ViewHolder {
            TextView text;
            ImageView image;
        }
    }

}