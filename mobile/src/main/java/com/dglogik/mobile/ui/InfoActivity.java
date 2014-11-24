package com.dglogik.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.dglogik.api.DGNode;
import com.dglogik.dslink.node.Poller;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

import java.util.concurrent.TimeUnit;

public class InfoActivity extends Activity {
    private TextView textView;
    private Poller poller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Information");
        Utils.applyDGTheme(this);
        setContentView(R.layout.info);
        textView = (TextView) findViewById(R.id.info);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DGMobileContext.CONTEXT == null) {
            textView.setText("Not Started");
            return;
        }

        textView.setVerticalScrollBarEnabled(true);
        textView.setMovementMethod(new ScrollingMovementMethod());

        poller = new Poller(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTree();
                    }
                });
            }
        });

        poller.poll(TimeUnit.SECONDS, 5, false);
    }

    public void updateTree() {
        StringBuilder builder = new StringBuilder();

        for (DGNode node : DGMobileContext.CONTEXT.link.getRootNodes()) {
            builder.append(Utils.createNodeTree(node, 1)).append("\n");
        }

        textView.setText(builder.toString());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (poller != null) {
            poller.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
