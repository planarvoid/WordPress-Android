package com.soundcloud.android.view.quickaction;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Action item, displayed as menu with icon and text.
 * 
 * @author Lorensius. W. L. T
 *
 */
public class ActionItem extends LinearLayout {
	private boolean selected;
	

    public ActionItem(Context context, Drawable background) {
        super(context);
           LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quickaction_action_item, this);

        setClickable(true);
        setFocusable(true);
        setBackgroundDrawable(background);

        findViewById(R.id.iv_icon).setVisibility(View.GONE);
    }

    public ActionItem(Context context, Drawable background, Drawable icon) {
		this(context, background);
        setIcon(icon);
	}

    /**
     * Set action icon
     *
     * @param icon {@link Drawable} action icon
     */
    public void setIcon(Drawable icon) {
        if (icon != null) {
            ((ImageView) findViewById(R.id.iv_icon)).setImageDrawable(icon);
            findViewById(R.id.iv_icon).setVisibility(View.VISIBLE);
        } else
            findViewById(R.id.iv_icon).setVisibility(View.GONE);

    }

    public ImageView getIconView(){
        return ((ImageView) findViewById(R.id.iv_icon));
    }

    /**
	 * Set selected flag;
	 * 
	 * @param selected Flag to indicate the item is selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	/**
	 * Check if item is selected
	 * 
	 * @return true or false
	 */
	public boolean isSelected() {
		return this.selected;
	}
}