package com.dglogik.mobile.link;

import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGNode;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.table.Table;
import com.dglogik.table.Tables;
import com.dglogik.value.DGValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGMetaData;

public class AudioSystemNode extends BaseNode<DataValueNode> {
    public AudioSystemNode() {
        super("Volume");
    }

    public void addVolumeStream(final AudioManager audioManager, String name, final int stream) {
        final DataValueNode node = new DataValueNode(name + "_Volume", BasicMetaData.SIMPLE_INT);

        node.setDisplayName(name + " Volume");

        DGMobileContext.CONTEXT.poller(new Action() {
            @Override
            public void run() {
                node.update(audioManager.getStreamVolume(stream));
            }
        }).poll(TimeUnit.SECONDS, 5, false);


        BaseAction setAction = new BaseAction("Set") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                int volume = args.get("volume").toInt();
                audioManager.setStreamVolume(stream, volume, AudioManager.FLAG_SHOW_UI);
                return null;
            }
        };

        BaseAction maxAction = new BaseAction("GetMaximum") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                final int max = audioManager.getStreamMaxVolume(stream);
                return Tables.makeTable(new HashMap<String, DGMetaData>() {{
                    put("maximum", BasicMetaData.SIMPLE_INT);
                }}, new HashMap<String, DGValue>() {{
                    put("maximum", DGValue.make(max));
                }});
            }
        };

        maxAction.setHasReturn(true);

        setAction.addParam("volume", BasicMetaData.SIMPLE_INT);

        addChild(node);
        node.addAction(setAction);
        node.addAction(maxAction);
    }

    public void setupInformationNodes(final AudioManager audioManager) {
        final DataValueNode isMicrophoneActive = new DataValueNode("Is_Microphone_Active", BasicMetaData.SIMPLE_BOOL);
        final DataValueNode isMicrophoneMuted = new DataValueNode("Is_Microphone_Muted", BasicMetaData.SIMPLE_BOOL);
        final DataValueNode isMusicActive = new DataValueNode("Is_Music_Active", BasicMetaData.SIMPLE_BOOL);
        final DataValueNode isSpeakerphoneOn = new DataValueNode("Is_Speakerphone_On", BasicMetaData.SIMPLE_BOOL);

        DGMobileContext.CONTEXT.poller(new Action() {
            @Override
            public void run() {
                isMicrophoneActive.update(audioManager.isMusicActive());
                isMicrophoneMuted.update(audioManager.isMicrophoneMute());
                isMusicActive.update(audioManager.isMusicActive());
                isSpeakerphoneOn.update(audioManager.isSpeakerphoneOn());
            }
        }).poll(TimeUnit.SECONDS, 5, false);



        addChild(isMicrophoneActive);
        addChild(isMicrophoneMuted);
        addChild(isMusicActive);
        addChild(isSpeakerphoneOn);
    }

    @Override
    public void addChild(DataValueNode node) {
        if (node.getName().contains("_")) {
            node.setDisplayName(node.getName().replaceAll("_", " "));
        }

        super.addChild(node);
    }
}
