package com.dglogik.mobile;

import com.dglogik.api.server.AbstractTunnelClient;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import ext.javax.servlet.ServletContext;

public class AndroidTunnelClient extends AbstractTunnelClient {
    private String uri;
    private WebSocket socket;

    public AndroidTunnelClient(ServletContext cx) {
        super(cx);
    }

    public AndroidTunnelClient setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    protected void connect() throws Exception {
        final Exception[] exception = new Exception[1];
        Future<WebSocket> future = AsyncHttpClient.getDefaultInstance().websocket(uri, "HTTP", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception e, WebSocket webSocket) {
                if (e != null) {
                    exception[0] = e;
                    return;
                }

                socket = webSocket;

                pingOk();
            }
        });

        future.get();

        if (exception[0] != null) {
            throw exception[0];
        }

        socket.setPongCallback(new WebSocket.PongCallback() {
            @Override
            public void onPongReceived(String s) {
                if ("ILoveWebSockets".equals(s)) {
                    pingOk();
                }
            }
        });

        socket.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                try {
                    processRequest(new StringReader(s));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        socket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
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
    }

    @Override
    protected void sendPing() throws Exception {
        socket.ping("ILoveWebSockets");
    }
}
