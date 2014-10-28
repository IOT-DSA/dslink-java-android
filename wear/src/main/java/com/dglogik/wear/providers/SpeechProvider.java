package com.dglogik.wear.providers;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import com.dglogik.wear.LinkService;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeechProvider extends Provider {
    public SpeechRecognizer recognizer;

    @Override
    public String name() {
        return "Speech";
    }

    @Override
    public void setup() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(LinkService.INSTANCE.getApplicationContext());

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle results) {
                List<Float> scores = new ArrayList<Float>();
                List<String> possibles = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                {
                    float[] sc = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
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

                final String value = possibles.get(highestScore);
                update(new HashMap<String, Object>() {{
                    put("Recognized", value);
                }});
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    @Override
    public boolean supported() {
        return SpeechRecognizer.isRecognitionAvailable(LinkService.INSTANCE.getApplicationContext());
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("Recognized", ValueType.STRING);
        }};
    }

    @Override
    public void destroy() {
        recognizer.destroy();
    }
}
