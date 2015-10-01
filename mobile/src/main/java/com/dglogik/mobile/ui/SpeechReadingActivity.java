package com.dglogik.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.speech.RecognizerIntent;

import com.dglogik.common.Wrapper;
import com.dglogik.mobile.DSContext;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SpeechReadingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ResultReceiver receiver = null;
        Runnable handle = null;
        final Wrapper<String> value = new Wrapper<>(null);
        if (getIntent() != null && getIntent().hasExtra("receiver")) {
            receiver = (ResultReceiver) getIntent().getExtras().get("receiver");
        } else if (DSContext.CONTEXT != null) {
            handle = new Runnable() {
                @Override
                public void run() {
                    Node node = DSContext.CONTEXT.currentDeviceNode.getChild("Recognized_Speech");
                    if (node != null) {
                        node.setValue(new Value(value.getValue()));
                    }
                }
            };
        }

        Runnable done = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };

        Bundle bundle = new Bundle();

        if (resultCode != RESULT_OK) {
            bundle.putString("input", "");
            if (receiver != null) {
                receiver.send(RESULT_OK, bundle);
            }
            done.run();
            return;
        }

        List<Float> scores = new ArrayList<>();
        List<String> possibles = data.getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);

        if (possibles == null) {
            DSContext.log("Possibles is null.");
            done.run();
            return;
        }

        HashSet<String> set = new HashSet<>();
        set.addAll(possibles);
        System.out.println(StringUtils.join(set, ","));

        {
            float[] sc = data.getExtras().getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
            if (sc == null) {
                return;
            }

            for (float score : sc) {
                scores.add(score);
            }
        }
        int highestScore = 0;

        for (String possible : possibles) {
            int index = possibles.indexOf(possible);
            float lastHighest = scores.get(highestScore);
            if (scores.get(index) > lastHighest) {
                highestScore = index;
            }
        }

        value.setValue(possibles.get(highestScore));
        bundle.putString("input", value.getValue());
        DSContext.log("Voice Input: " + value.getValue());
        if (receiver != null) {
            receiver.send(RESULT_OK, bundle);
        } else if (handle != null) {
            handle.run();
        }

        done.run();
    }
}
