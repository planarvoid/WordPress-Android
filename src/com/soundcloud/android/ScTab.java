package com.soundcloud.android;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ScTab extends LinearLayout {
    public ScTab(Context c, int drawable, String label, Boolean scrolltabs) {
        super(c);
        
        LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater != null){      
        	if (scrolltabs)
        		inflater.inflate(R.layout.sc_tab_scroll, this);
        	else
        		inflater.inflate(R.layout.sc_tab, this);
        		
        }

  
        ImageView iv = (ImageView) findViewById(R.id.oc_tab_icon);
        TextView tv = (TextView) findViewById(R.id.oc_tab_text);
        
        iv.setImageResource(drawable);
        
        getResources();
        
		XmlResourceParser xpp= getResources().getXml(R.drawable.tab_txt); 
        try {
        	tv.setTextColor(ColorStateList.createFromXml(getResources(), xpp));
		} catch (Exception e) {
			e.printStackTrace();
		}
        tv.setText(label);
        tv.setGravity(0x01); /* Center */
        
        //setBackgroundResource(android.R.drawable.tab_indicator);
        setOrientation(LinearLayout.VERTICAL);
    }

}