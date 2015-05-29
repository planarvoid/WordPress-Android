package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ButtonBar extends LinearLayout {

    private int separatorColor;
    private List<MenuItem> menuItems;
    private SparseArray<MenuItem> menuItemMap;
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
        separatorColor = a.getColor(R.styleable.ButtonBar_separator_color, 0xFF666666);
        a.recycle();

        menuItems = new ArrayList<>();
        menuItemMap = new SparseArray<>();

        setOrientation(LinearLayout.VERTICAL);
        addView(getNewVerticalSeparator());

        holder = new LinearLayout(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        lp.weight = 1;
        holder.setLayoutParams(lp);
        addView(holder);
    }

    public ButtonBar addItem(MenuItem menuItem, int resId) {
        menuItems.add(menuItem);
        menuItemMap.put(menuItem.id, menuItem);

        menuItem.separator = getNewSeparator();
        menuItem.button = getNewButton(menuItem);
        menuItem.button.setText(resId);

        holder.addView(menuItem.separator);
        holder.addView(menuItem.button);
        setVisibilities();
        return this;
    }

    private void setVisibilities() {
        boolean first = false;
        for (MenuItem menuItem : menuItems) {
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
        b.setBackgroundResource(R.drawable.item_background_dark);
        b.setTextColor(getResources().getColorStateList(R.drawable.txt_btn_dark_states));
        b.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.button_bar_text_size));
        int padding = getResources().getDimensionPixelSize(R.dimen.button_bar_button_padding);
        b.setPadding(padding, 0, padding, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        b.setLayoutParams(lp);
        b.setText(menuItem.label, TextView.BufferType.NORMAL);
        b.setOnClickListener(menuItem.onClickListener);

        return b;
    }

    private View getNewSeparator() {
        final float density = getContext().getResources().getDisplayMetrics().density;
        View v = new View(getContext());
        v.setBackgroundColor(separatorColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (1 * density), ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(lp);
        return v;
    }

    private View getNewVerticalSeparator() {
        final float density = getContext().getResources().getDisplayMetrics().density;
        View v = new View(getContext());
        v.setBackgroundColor(separatorColor);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density)));
        return v;
    }
}
