package com.dglogik.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.dglogik.api.DGNode;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

public class InfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Information");
        Utils.applyDGTheme(this);
        setContentView(R.layout.info);
        TextView textView = (TextView) findViewById(R.id.info);

        if (DGMobileContext.CONTEXT == null) {
            textView.setText("Not Started");
            return;
        }

        StringBuilder builder = new StringBuilder();

        for (DGNode node : DGMobileContext.CONTEXT.link.getRootNodes()) {
            builder.append(Utils.createNodeTree(node, 1)).append("\n");
        }

        textView.setVerticalScrollBarEnabled(true);

        textView.setText(builder.toString());
    }
}
