package com.dglogik.mobile.link;

import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.table.Table;
import com.dglogik.value.DGValue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VolumeSystemNode extends BaseNode<DataValueNode> {
    public VolumeSystemNode() {
        super("Volume");
    }

    public void createStream(final AudioManager audioManager, String name, final int stream) {
        final DataValueNode node = new DataValueNode(name, BasicMetaData.SIMPLE_INT);

        DGMobileContext.CONTEXT.poller(new Action() {
            @Override
            public void run() {
                node.update(audioManager.getStreamVolume(stream));
            }
        }).poll(TimeUnit.SECONDS, 5, false);


        BaseAction setAction = new BaseAction("Set" + name) {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                int volume = args.get("volume").toInt();
                audioManager.setStreamVolume(stream, volume, AudioManager.FLAG_SHOW_UI);
                return null;
            }
        };

        setAction.addParam("volume", BasicMetaData.SIMPLE_INT);

        addChild(node);
        addAction(setAction);
    }
}
