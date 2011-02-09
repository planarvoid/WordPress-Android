package com.soundcloud.android.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.task.LoadConnectionsTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class ConnectionItem extends RelativeLayout {
    private CheckBox post_publish;
    public final Connection connection;

    public ConnectionItem(Context context, final Connection c) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.connection_item, this);

        this.connection = c;

        final TextView display_name = (TextView) findViewById(R.id.display_name);
        display_name.setText(c.display_name);

        post_publish = (CheckBox) findViewById(R.id.post_publish);
        TextView configure = (TextView) findViewById(R.id.txt_configure);

        if (c.active()) {
            post_publish.setChecked(c.post_publish);
            post_publish.setVisibility(VISIBLE);
            configure.setVisibility(GONE);
        } else {
            post_publish.setVisibility(INVISIBLE);
            configure.setVisibility(VISIBLE);
            display_name.setTextColor(getResources().getColor(R.color.darker_gray));
        }


        ImageView service_icon = (ImageView) findViewById(R.id.service_icon);
        service_icon.setImageDrawable(getResources().getDrawable(c.type().resId));

        final OnClickListener toggle = new OnClickListener() {
            public void onClick(View v) {
                if (c.active()) {
                    post_publish.toggle();
                } else {
                    Log.d(TAG, "configure " + c.type());

                }
            }
        };
        display_name.setOnClickListener(toggle);
        configure.setOnClickListener(toggle);
        setOnClickListener(toggle);
    }

    public boolean isEnabled() {
        return post_publish.isChecked();
    }
}
