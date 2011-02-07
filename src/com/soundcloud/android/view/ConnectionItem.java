package com.soundcloud.android.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.Connection;

import java.util.HashSet;
import java.util.Set;


public class ConnectionItem extends RelativeLayout {
    private CheckBox post_publish;

    public Connection connection;

    public ConnectionItem(Context context, Connection c) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.connection_item, this);

        this.connection = c;

        TextView display_name = (TextView) findViewById(R.id.display_name);
        display_name.setText(c.display_name);

        post_publish = (CheckBox) findViewById(R.id.post_publish);
        post_publish.setChecked(c.post_publish);
        ImageView service_icon = (ImageView) findViewById(R.id.service_icon);
        service_icon.setImageDrawable(getResources().getDrawable(c.type().id));
    }

    public boolean isEnabled() {
        return post_publish.isChecked();
    }

    public static Iterable<Integer> postToServiceIds(ViewGroup vg) {
        Set<Integer> ids = new HashSet<Integer>();
        for (int i=0; i<vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            if (v instanceof ConnectionItem && v.isEnabled()) {
                ids.add(((ConnectionItem)v).connection.id);
            }
        }
        return ids;
    }
}
