package com.soundcloud.android.adapter;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class StartMenuAdapter extends BaseAdapter {

    private Context context;
    private String[] texts = {"Log In", "Sign Up"};

    public StartMenuAdapter(Context context) {
        this.context = context;
    }

    public int getCount() {
        return 2;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout rl;
        TextView tv;
        if (convertView == null) {
            rl = new RelativeLayout(context);
            rl.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, (int)(60*context.getResources().getDisplayMetrics().density)));

            View v = new View(context);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, 1);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            v.setLayoutParams(lp);
            v.setBackgroundResource(R.drawable.black_gradient);
            rl.addView(v);

            tv = new TextView(context);
            tv.setId(android.R.id.text1);
            tv.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(20);
            rl.addView(tv);
        }
        else {
            rl = (RelativeLayout) convertView;
            tv = (TextView)(rl).findViewById(android.R.id.text1);
        }

        tv.setText(texts[position]);
        return rl;
    }
}