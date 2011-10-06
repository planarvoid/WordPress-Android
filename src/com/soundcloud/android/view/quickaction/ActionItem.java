package com.soundcloud.android.view.quickaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.soundcloud.android.R;

/**
 * Action item, displayed as menu with icon and text.
 * 
 * @author Lorensius. W. L. T
 *
 */
public class ActionItem extends LinearLayout {
	private boolean selected;
	

    public ActionItem(Context context) {
        super(context);
           LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quickaction_action_item, this);

        findViewById(R.id.tv_title).setVisibility(View.GONE);
        findViewById(R.id.iv_icon).setVisibility(View.GONE);
    }

    public ActionItem(Context context, Drawable icon) {
		this(context);
        setIcon(icon);
	}


    /**
	 * Set action title
	 * 
	 * @param title action title
	 */
	public void setTitle(String title) {
                if (title != null){
                    ((TextView) findViewById(R.id.tv_title)).setText(title);
                    findViewById(R.id.tv_title).setVisibility(View.VISIBLE);
                }else
                    findViewById(R.id.tv_title).setVisibility(View.GONE);

	}
	
	/**
	 * Set action icon
	 * 
	 * @param icon {@link Drawable} action icon
	 */
	public void setIcon(Drawable icon) {
                        if (icon != null){
                            ((ImageView) findViewById(R.id.iv_icon)).setImageDrawable(icon);
                            findViewById(R.id.iv_icon).setVisibility(View.VISIBLE);
                        }else
                            findViewById(R.id.iv_icon).setVisibility(View.GONE);

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