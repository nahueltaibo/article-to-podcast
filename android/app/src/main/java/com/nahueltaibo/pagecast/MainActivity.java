package com.nahueltaibo.pagecast;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Placeholder screen used to test distribution through Play internal testing. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(22);
        text.setPadding(48, 48, 48, 48);
        text.setText("Pagecast\n\nInstall test, version " + BuildConfig.VERSION_NAME);
        setContentView(text);
    }
}
