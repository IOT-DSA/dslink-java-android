package com.dglogik.mobile;

import com.dglogik.api.server.AbstractTunnelClient;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import android.os.*;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import ext.javax.servlet.ServletContext;

public class AutobahnTunnelClient extends AbstractTunnelClient {
    private ServletContext context;
    private WebSocketConnection connection;

    public AutobahnTunnelClient(ServletContext cx) {
        super(cx);
        context = cx;
    }

    private boolean c = false;

    public void setUri(String uri) {}

    @Override
    protected void connect() throws Exception {
        Looper.prepare();
        connection = new WebSocketConnection();

        final String url = context.getInitParameter("tunnel");

        new Thread(new Runnable() {
            @Override
            public void run() {
                connection.onPong = new Runnable() {
                    @Override
                    public void run() {
                        DGMobileContext.log("WS Pong");
                        pingOk();
                    }
                };

                try {
                    connection.connect(new URI(url), new WebSocket.WebSocketConnectionObserver() {
                        @Override
                        public void onOpen() {
                            c = true;
                            DGMobileContext.log("WS Open");
                        }

                        @Override
                        public void onClose(WebSocketCloseNotification code, String reason) {
                            DGMobileContext.log("WS Close");
                            disconnected();
                        }

                        @Override
                        public void onTextMessage(String payload) {
                            DGMobileContext.log("WS Message: " + payload);
                            try {
                                processRequest(new StringReader(payload));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onRawTextMessage(byte[] payload) {
                        }

                        @Override
                        public void onBinaryMessage(byte[] payload) {
                        }
                    });
                } catch (WebSocketException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        while (!c) {
            Thread.sleep(10);
        }

        pingOk();
    }

    @Override
    protected Writer responseBegin() throws IOException {
        return new StringWriter();
    }

    @Override
    protected void responseEnd(Writer writer) throws IOException {
        String data = writer.toString();
        connection.sendTextMessage(data);
    }

    @Override
    protected void sendPing() throws Exception {
        connection.ping();
        DGMobileContext.log("WS Ping");
    }
}
