package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class ButtonBar extends LinearLayout {

    private int mSeparatorColor;
    private List<MenuItem> mMenuItems;
    private SparseArray<MenuItem> mMenuItemMap;
    private LinearLayout holder;

    public static class MenuItem {
        public int id;
        public CharSequence label;
        public boolean visible;
        public OnClickListener onClickListener;

        Button button;
        View separator;

        public MenuItem(int id, OnClickListener onClickListener) {
            this.id = id;
            this.onClickListener = onClickListener;
            visible = true;
        }
    }

    public ButtonBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ButtonBar);
        mSeparatorColor = a.getColor(R.styleable.ButtonBar_separator_color, 0xFF666666);
        a.recycle();

        mMenuItems = new ArrayList<MenuItem>();
        mMenuItemMap = new SparseArray<MenuItem>();

        setOrientation(LinearLayout.VERTICAL);
        addView(getNewVerticalSeparator());

        holder = new LinearLayout(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        lp.weight = 1;
        holder.setLayoutParams(lp);
        addView(holder);
    }

    public ButtonBar addItem(MenuItem menuItem, int resId) {
        mMenuItems.add(menuItem);
        mMenuItemMap.put(menuItem.id, menuItem);

        menuItem.separator = getNewSeparator();
        menuItem.button = getNewButton(menuItem);
        menuItem.button.setText(resId);

        holder.addView(menuItem.separator);
        holder.addView(menuItem.button);
        setVisibilities();
        return this;
    }

    public void toggleVisibility(int id, boolean visible, boolean updateAll) {
        if (mMenuItemMap.get(id) != null) {
            mMenuItemMap.get(id).visible = visible;
            if (updateAll) setVisibilities();
        }
    }

    public void setTextById(int id, CharSequence cs) {
        if (mMenuItemMap.get(id) != null) mMenuItemMap.get(id).button.setText(cs);
    }

    private void setVisibilities() {
        boolean first = false;
        for (MenuItem menuItem : mMenuItems) {
            if (menuItem.visible) {
                menuItem.button.setVisibility(View.VISIBLE);
                if (!first) {
                    first = true;
                    menuItem.separator.setVisibility(View.GONE);
                } else {
                    menuItem.separator.setVisibility(View.VISIBLE);
                }
            } else {
                menuItem.button.setVisibility(View.GONE);
                menuItem.separator.setVisibility(View.GONE);
            }
        }
    }

    private Button getNewButton(MenuItem menuItem) {
        final Button b = new Button(getContext());
        b.setBackgroundResource(R.drawable.btn_transparent_bg_states);
        b.setTextColor(getResources().getColorStateList(R.drawable.txt_btn_dark_states));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        b.setLayoutParams(lp);
        b.setText(menuItem.label);
        b.setOnClickListener(menuItem.onClickListener);

        return b;
    }

    private View getNewSeparator() {
        final float density = getContext().getResources().getDisplayMetrics().density;
        View v = new View(getContext());
        v.setBackgroundColor(mSeparatorColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (1 * density), ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(lp);
        return v;
    }

    private View getNewVerticalSeparator() {
        final float density = getContext().getResources().getDisplayMetrics().density;
        View v = new View(getContext());
        v.setBackgroundColor(mSeparatorColor);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density)));
        return v;
    }
}