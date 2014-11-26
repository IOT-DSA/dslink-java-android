package com.dglogik.mobile;

import com.dglogik.api.server.AbstractTunnelClient;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import ext.javax.servlet.ServletContext;

public class AndroidTunnelClient extends AbstractTunnelClient {
    public WebSocket socket;
    @SuppressWarnings("FieldCanBeLocal")
    private ServletContext servlet;
    private String uri;

    public AndroidTunnelClient(ServletContext cx, String uri) {
        super(cx);

        servlet = cx;
        this.uri = uri;
    }

    @Override
    public void stop() {
        super.stop();
        //DGMobileContext.log("Android Tunnel Client Stopping");
        if (socket != null) socket.close();
    }

    @Override
    protected void connect() throws Exception {
        Future<WebSocket> future = AsyncHttpClient.getDefaultInstance().websocket(uri, null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception e, WebSocket webSocket) {
                if (e != null) {
                    e.printStackTrace();
                    try { Thread.sleep(1000); } catch (Exception ignored) {}
                }
            }
        });

        socket = future.get();
        init();
        sendPing();
    }

    private void init() {
        //DGMobileContext.log("Android Tunnel Client Connected");

        socket.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                //DGMobileContext.log("Android Tunnel Client Received: " + s);
                try {
                    StringReader reader = new StringReader(s);
                    processRequest(reader);
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        socket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                //DGMobileContext.log("Android Tunnel Client Got Data");
                byteBufferList.recycle();
            }
        });

        socket.setPongCallback(new WebSocket.PongCallback() {
            @Override
            public void onPongReceived(String s) {
                //DGMobileContext.log("Android Tunnel Client Received a Pong");
                pingOk();
            }
        });

        socket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception e) {
                //Thread.dumpStack();
                if (e != null) {
                    e.printStackTrace();
                }
                //DGMobileContext.log("Android Tunnel Client Closed");
                disconnected();
                if (socket.isOpen()) {
                    socket.close();
                }
                socket = null;
            }
        });
    }

    @Override
    protected Writer responseBegin() throws IOException {
        return new StringWriter();
    }

    @Override
    protected void responseEnd(Writer out) throws IOException {
        String str = out.toString();
        socket.send(str);
        //DGMobileContext.log("Android Tunnel Client Sent: " + str);
    }

    @Override
    protected void sendPing() throws Exception {
        //DGMobileContext.log("Android Tunnel Client Sending Ping");
        socket.ping("b00b1e5555");
    }
}
