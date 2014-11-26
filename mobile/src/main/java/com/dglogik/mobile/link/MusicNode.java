package com.dglogik.mobile.link;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.table.Table;
import com.dglogik.value.DGValue;

import java.util.Map;

public class MusicNode extends BaseNode<DataValueNode> {
    public MusicNode() {
        super("Music");
    }

    public void init() {
        final BaseAction playArtistAction = new BaseAction("PlayArtist") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                String artist = args.get("artist").toString();
                log("Playing Artist: " + artist);
                DGMobileContext.CONTEXT.playSearchArtist(artist);
                return null;
            }
        };

        final BaseAction playSongAction = new BaseAction("PlaySong") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                String song = args.get("song").toString();
                log("Playing Song: " + song);
                DGMobileContext.CONTEXT.playSearchSong(song);
                return null;
            }
        };

        final BaseAction playAction = new BaseAction("Play") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("play");
                return null;
            }
        };

        final BaseAction pauseAction = new BaseAction("Pause") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("pause");
                return null;
            }
        };

        final BaseAction togglePauseAction = new BaseAction("TogglePause") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("togglepause");
                return null;
            }
        };

        final BaseAction stopAction = new BaseAction("Stop") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("play");
                return null;
            }
        };

        final BaseAction nextAction = new BaseAction("Next") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("next");
                return null;
            }
        };

        final BaseAction previousAction = new BaseAction("Previous") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                sendMusicCommand("previous");
                return null;
            }
        };

        playArtistAction.addParam("artist", BasicMetaData.SIMPLE_STRING);
        playSongAction.addParam("song", BasicMetaData.SIMPLE_STRING);

        addAction(playArtistAction);
        addAction(playSongAction);
        addAction(playAction);
        addAction(pauseAction);
        addAction(togglePauseAction);
        addAction(stopAction);
        addAction(nextAction);
        addAction(previousAction);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.music.metachanged");
        filter.addAction("com.htc.music.metachanged");
        filter.addAction("fm.last.android.metachanged");
        filter.addAction("com.sec.android.app.music.metachanged");
        filter.addAction("com.nullsoft.winamp.metachanged");
        filter.addAction("com.amazon.mp3.metachanged");
        filter.addAction("com.miui.player.metachanged");
        filter.addAction("com.real.IMP.metachanged");
        filter.addAction("com.sonyericsson.music.metachanged");
        filter.addAction("com.rdio.android.metachanged");
        filter.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        filter.addAction("com.andrew.apollo.metachanged");
        filter.addAction("com.spotify.music.metadatachanged");

        final DataValueNode artistNode = new DataValueNode("Song_Artist", BasicMetaData.SIMPLE_STRING);
        final DataValueNode albumNode = new DataValueNode("Song_Album", BasicMetaData.SIMPLE_STRING);
        final DataValueNode trackNode = new DataValueNode("Song_Track", BasicMetaData.SIMPLE_STRING);

        artistNode.setDisplayName("Song Artist");
        artistNode.setDisplayName("Song Album");
        artistNode.setDisplayName("Song Title");

        artistNode.initializeValue = new Action() {
            @Override
            public void run() {
                if (songArtist != null) {
                    artistNode.update(songArtist);
                }
            }
        };

        albumNode.initializeValue = new Action() {
            @Override
            public void run() {
                if (songAlbum != null) {
                    albumNode.update(songAlbum);
                }
            }
        };

        trackNode.initializeValue = new Action() {
            @Override
            public void run() {
                if (songTitle != null) {
                    trackNode.update(songTitle);
                }
            }
        };

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");
                String track = intent.getStringExtra("track");

                artistNode.update(artist);
                albumNode.update(album);
                trackNode.update(track);

                songArtist = artist;
                songAlbum = album;
                songTitle = track;
            }
        };

        DGMobileContext.CONTEXT.getApplicationContext().registerReceiver(receiver, filter);

        DGMobileContext.CONTEXT.onCleanup(new Action() {
            @Override
            public void run() {
                DGMobileContext.CONTEXT.getApplicationContext().unregisterReceiver(receiver);
            }
        });

        addChild(artistNode);
        addChild(albumNode);
        addChild(trackNode);
    }

    private String songArtist;
    private String songTitle;
    private String songAlbum;

    public void log(String msg) {
        DGMobileContext.log(msg);
    }

    public void sendMusicCommand(String cmd) {
        DGMobileContext.CONTEXT.sendMusicCommand(cmd);
    }
}
