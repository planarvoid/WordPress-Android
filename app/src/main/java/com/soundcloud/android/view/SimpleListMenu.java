package com.soundcloud.android.view;


import com.soundcloud.android.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.Xml;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleListMenu extends RelativeLayout {
    private ListView mList;
    private MenuAdapter mAdapter;
    private OnMenuItemClickListener mClickListener;

    /**
     * Callback invoked when a menu item is clicked.
     */
    public static interface OnMenuItemClickListener {
        public void onMenuItemClicked(int id);
        public void onHeaderClicked(int id);
    }

    public SimpleListMenu(Context context) {
        super(context);
        init();
    }

    public SimpleListMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleListMenu(Context context, AttributeSet attrs, int menuResourceId) {
        this(context, attrs);
        setMenuItems(menuResourceId);
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.slm_menu, this, true);
        mList = (ListView) findViewById(android.R.id.list);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mClickListener != null){
                    mClickListener.onMenuItemClicked(((SimpleListMenuItem)mAdapter.getItem(position)).id);
                }
            }
        });
        mAdapter = new MenuAdapter();

    }

    public void setOnItemClickListener(OnMenuItemClickListener onMenuItemClickListener){
        mClickListener = onMenuItemClickListener;
    }


    public void setMenuItems(int menu) {
        mAdapter.clear();
        XmlResourceParser parser = null;
        try {
            parser = getResources().getLayout(menu);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("header".equals(parser.getName())) {
                        final TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.SimpleMenu);
                        if (a.getResourceId(R.styleable.SimpleMenu_android_layout, 0) != 0){
                            View header = View.inflate(getContext(), a.getResourceId(R.styleable.SimpleMenu_android_layout, 0), null);
                            header.setOnClickListener(new OnClickListener(){
                                @Override
                                public void onClick(View v) {
                                    if (mClickListener != null){
                                        mClickListener.onHeaderClicked(a.getResourceId(R.styleable.SimpleMenu_android_id, 0));
                                    }
                                }
                            });
                            mList.addHeaderView(header);
                        }
                    } else if ("item".equals(parser.getName())) {
                        mAdapter.addItem(new SimpleListMenuItem(
                              getContext().obtainStyledAttributes(attrs,R.styleable.SimpleMenu)
                        ));
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
        mList.setAdapter(mAdapter);
    }

    class SimpleListMenuItem {
        int id;
        CharSequence text;
        Drawable icon;
        int layoutId;

        public SimpleListMenuItem(TypedArray a) {
            this.id = a.getResourceId(R.styleable.SimpleMenu_android_id, 0);
            this.text = a.getText(R.styleable.SimpleMenu_android_text);
            this.icon = a.getDrawable(R.styleable.SimpleMenu_android_icon);
            this.layoutId = a.getResourceId(R.styleable.SimpleMenu_android_layout, 0);
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
        }

        void clear(){
            mMenuItems.clear();
        }

        void addItem(SimpleListMenuItem item){
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
            return mLayouts.size() <= 1 ? 1 : mLayouts.size();
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

            final ViewHolder holder;
            if (convertView == null) {
                final int layout_id = mMenuItems.get(position).layoutId;
                convertView = inflater.inflate(layout_id == 0 ? R.layout.slm_menu_item : layout_id, null);

                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.slm_item_icon);
                holder.text = (TextView) convertView.findViewById(R.id.slm_item_text);

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final SimpleListMenuItem menuItem = mMenuItems.get(position);
            if (holder.image != null) holder.image.setImageDrawable(menuItem.icon);
            if (holder.text != null) holder.text.setText(menuItem.text);
            return convertView;
        }

        class ViewHolder {
            TextView text;
            ImageView image;
        }
    }


}