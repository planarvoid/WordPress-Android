package com.soundcloud.android.view;


import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SearchHistoryAdapter;
import com.soundcloud.android.adapter.SearchSuggestionsAdapter;
import com.soundcloud.android.model.Search;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.Editable;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainMenu extends LinearLayout {
    private int mSelectedMenuId = -1;

    private ListView mList;
    private MenuAdapter mMenuAdapter;
    private OnMenuItemClickListener mClickListener;

    private SearchHistoryAdapter mSearchHistoryAdapter;
    private SearchSuggestionsAdapter mSuggestionsAdapter;

    private boolean mInSearchMode;
    private EditText mQueryText;
    private View mFocusCatcher;

    /**
     * Callback invoked when a menu item is clicked.
     */
    public static interface OnMenuItemClickListener {

        public void onMenuItemClicked(int id);
        public void onSearchQuery(Search query);
        public void onSearchSuggestedTrackClicked(long id);
        public void onSearchSuggestedUserClicked(long id);
    }
    public MainMenu(Context context) {
        super(context);
        init();
    }

    public MainMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public boolean gotoMenu() {
        if (mInSearchMode){
            toggleSearchMode();
            return true;
        } else {
            return false;
        }
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater.from(getContext()).inflate(R.layout.main_menu, this, true);
        mList = (ListView) findViewById(R.id.root_menu_list);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mClickListener != null) {
                    if (!mInSearchMode) {
                        mClickListener.onMenuItemClicked(((SimpleListMenuItem) mMenuAdapter.getItem(position - mList.getHeaderViewsCount())).id);
                    } else if (parent.getAdapter() == mSuggestionsAdapter){
                        switch (mSuggestionsAdapter.getItemViewType(position)){
                            case SearchSuggestionsAdapter.TYPE_TRACK:
                                mClickListener.onSearchSuggestedTrackClicked(id);
                                break;
                            case SearchSuggestionsAdapter.TYPE_USER:
                                mClickListener.onSearchSuggestedUserClicked(id);
                                break;
                        }
                    } else {
                        mClickListener.onSearchQuery(mSearchHistoryAdapter.getItem(position));
                    }
                    closeKeyboard();
                }
            }
        });

        mList.setSelector(getContext().getResources().getDrawable(R.drawable.selectable_background_next));

        mQueryText = (EditText) findViewById(R.id.root_menu_query);
        mQueryText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !mInSearchMode) {
                    toggleSearchMode();
                }
            }
        });

        mQueryText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && mClickListener != null) {
                    mClickListener.onSearchQuery(new Search(mQueryText.getText().toString(), Search.ALL));
                    toggleSearchMode();
                    mQueryText.setText("");
                    closeKeyboard();
                    return true;

                } else {
                    return false;
                }
            }
        });


        mMenuAdapter = new MenuAdapter();
        mSearchHistoryAdapter = new SearchHistoryAdapter(getContext(), R.layout.search_history_row_dark);
        mSuggestionsAdapter = new SearchSuggestionsAdapter(getContext(),null);
        mFocusCatcher = findViewById(R.id.root_menu_focus_catcher);
    }

    public void setOffsetRight(int mOffsetRight) {
        ((RelativeLayout.LayoutParams) mQueryText.getLayoutParams()).rightMargin = mOffsetRight;
        mSuggestionsAdapter.setOffsetRight(mOffsetRight);
        requestLayout();
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mQueryText.getWindowToken(), 0);
    }

    private void toggleSearchMode(){
        if (!mInSearchMode){
            mInSearchMode = true;
            if (mQueryText.length() > 0){
                mList.setAdapter(mSuggestionsAdapter);
                mSuggestionsAdapter.notifyDataSetChanged();
            } else {
                mList.setAdapter(mSearchHistoryAdapter);
                SearchHistoryAdapter.refreshHistory(getContext().getContentResolver(), mSearchHistoryAdapter);
            }
            mQueryText.addTextChangedListener(mTextWatcher);

        } else {
            mInSearchMode = false;
            mList.setAdapter(mMenuAdapter);
            mQueryText.removeTextChangedListener(mTextWatcher);
            mFocusCatcher.requestFocus();
        }
    }

    public void setOnItemClickListener(OnMenuItemClickListener onMenuItemClickListener){
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
                        mMenuAdapter.addItem(new SimpleListMenuItem(
                                getContext().obtainStyledAttributes(attrs, R.styleable.SimpleMenu)
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
        mList.setAdapter(mMenuAdapter);
    }

    class SimpleListMenuItem {
        int id;
        CharSequence text;
        Drawable icon;
        int layoutId;
        boolean selected;

        public SimpleListMenuItem(TypedArray a) {
            this.id       = a.getResourceId(R.styleable.SimpleMenu_android_id, 0);
            this.text     = a.getText(R.styleable.SimpleMenu_android_text);
            this.icon     = a.getDrawable(R.styleable.SimpleMenu_android_icon);
            this.layoutId = a.getResourceId(R.styleable.SimpleMenu_android_layout, 0);
            this.selected = this.id == mSelectedMenuId;
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

                if (menuItem.selected) {
                    convertView.setBackgroundResource(R.drawable.sidebar_item_background_selected);
                } else {
                    convertView.setBackgroundResource(R.drawable.sidebar_item_background);
                }

                int paddingTopBottom = (int) getResources().getDimension(R.dimen.search_header_padding_topbottom);
                int paddingLeftRight = (int) getResources().getDimension(R.dimen.search_header_padding_leftright);
                convertView.setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder.image != null) holder.image.setImageDrawable(menuItem.icon);
            if (holder.text != null) holder.text.setText(menuItem.text);

            return convertView;
        }

        class ViewHolder {
            TextView text;
            ImageView image;
        }
    }


    private final TextWatcher mTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence seq, int start, int before, int count) {
            if (count == 0){
                mList.setAdapter(mSearchHistoryAdapter);
            } else {
                final String s = seq.toString();
                mSuggestionsAdapter.getFilter().filter(
                        s.contains(",") ?
                                s.subSequence(s.lastIndexOf(",") + 1, s.length()).toString().trim() :
                                s.trim());

                if (mList.getAdapter() != mSuggestionsAdapter) mList.setAdapter( mSuggestionsAdapter);
            }

        }
    };


}