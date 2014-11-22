package com.dglogik.mobile;

import com.dglogik.api.server.AbstractTunnelClient;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import ext.javax.servlet.ServletContext;

public class AndroidTunnelClient extends AbstractTunnelClient {
    private WebSocket socket;
    private ServletContext servlet;

    public AndroidTunnelClient(ServletContext cx) {
        super(cx);

        servlet = cx;
    }

    public AndroidTunnelClient setUri(String uri) {
        return this;
    }

    @Override
    public void stop() {
        super.stop();
        DGMobileContext.log("Android Tunnel Client Stopping");
        if (socket != null) socket.close();
        socket = null;
    }

    @Override
    protected void connect() throws Exception {
        Future<WebSocket> future = AsyncHttpClient.getDefaultInstance().websocket(servlet.getInitParameter("tunnel"), null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception e, WebSocket webSocket) {
            }
        });

        socket = future.get();
        init();
        pingOk();
    }

    private void init() {
        DGMobileContext.log("Android Tunnel Client Connected");

        socket.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                DGMobileContext.log("Android Tunnel Client Received: " + s);
                StringReader reader = new StringReader(s);
                try {
                    processRequest(new StringReader(s));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    reader.close();
                }
            }
        });

        socket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                byteBufferList.recycle();
            }
        });

        socket.setPongCallback(new WebSocket.PongCallback() {
            @Override
            public void onPongReceived(String s) {
                DGMobileContext.log("Android Tunnel Client Received a Pong");
                pingOk();
            }
        });

        socket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
                DGMobileContext.log("Android Tunnel Client Closed");
                disconnected();
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
        DGMobileContext.log("Android Tunnel Client Sent: " + str);
    }

    @Override
    protected void sendPing() throws Exception {
        DGMobileContext.log("Android Tunnel Client Sending Ping");
        socket.ping("b00b1e5555");
    }
}
