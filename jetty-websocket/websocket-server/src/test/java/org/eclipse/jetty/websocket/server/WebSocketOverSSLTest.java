/*******************************************************************************
 * Copyright (c) 2011 Intalio, Inc.
 * ======================================================================
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *   The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *
 *   The Apache License v2.0 is available at
 *   http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/
package org.eclipse.jetty.websocket.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.WebSocket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class WebSocketOverSSLTest
{
    private Server _server;
    private int _port;
    private QueuedThreadPool _threadPool;
//    private WebSocketClientFactory _wsFactory;
    private WebSocket.Connection _connection;

    @After
    public void destroy() throws Exception
    {
        if (_connection != null)
        {
            _connection.close();
        }

//        if (_wsFactory != null)
//            _wsFactory.stop();

        if (_threadPool != null)
        {
            _threadPool.stop();
        }

        if (_server != null)
        {
            _server.stop();
            _server.join();
        }
    }

    private void startClient(final WebSocket webSocket) throws Exception
    {
        Assert.assertTrue(_server.isStarted());

        _threadPool = new QueuedThreadPool();
        _threadPool.setName("wsc-" + _threadPool.getName());
        _threadPool.start();

//        _wsFactory = new WebSocketClientFactory(_threadPool, new ZeroMasker());
//        SslContextFactory cf = _wsFactory.getSslContextFactory();
//        cf.setKeyStorePath(MavenTestingUtils.getTestResourceFile("keystore").getAbsolutePath());
//        cf.setKeyStorePassword("storepwd");
//        cf.setKeyManagerPassword("keypwd");
//        _wsFactory.start();

//        WebSocketClient client = new WebSocketClient(_wsFactory);
//        _connection = client.open(new URI("wss://localhost:" + _port), webSocket).get(5, TimeUnit.SECONDS);
    }

    private void startServer(final WebSocket webSocket) throws Exception
    {
        _server = new Server();
        SslSelectChannelConnector connector = new SslSelectChannelConnector();
        _server.addConnector(connector);
        SslContextFactory cf = connector.getSslContextFactory();
        cf.setKeyStorePath(MavenTestingUtils.getTestResourceFile("keystore").getAbsolutePath());
        cf.setKeyStorePassword("storepwd");
        cf.setKeyManagerPassword("keypwd");
        _server.setHandler(new WebSocketHandler()
        {
            @Override
            public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol)
            {
                return webSocket;
            }
        });
        _server.start();
        _port = connector.getLocalPort();
    }

    @Test
    public void testManyMessages() throws Exception
    {
        startServer(new WebSocket.OnTextMessage()
        {
            private Connection connection;

            @Override
            public void onClose(int closeCode, String message)
            {
            }

            @Override
            public void onMessage(String data)
            {
                try
                {
                    connection.sendMessage(data);
                }
                catch (IOException x)
                {
                    x.printStackTrace();
                }
            }

            @Override
            public void onOpen(Connection connection)
            {
                this.connection = connection;
            }
        });
        int count = 1000;
        final CountDownLatch clientLatch = new CountDownLatch(count);
        startClient(new WebSocket.OnTextMessage()
        {
            @Override
            public void onClose(int closeCode, String message)
            {
            }

            @Override
            public void onMessage(String data)
            {
                clientLatch.countDown();
            }

            @Override
            public void onOpen(Connection connection)
            {
            }
        });

        char[] chars = new char[256];
        Arrays.fill(chars, 'x');
        String message = new String(chars);
        for (int i = 0; i < count; ++i)
        {
            _connection.sendMessage(message);
        }

        Assert.assertTrue(clientLatch.await(20, TimeUnit.SECONDS));

        // While messages may have all arrived, the SSL close alert
        // may be in the way so give some time for it to be processed.
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    public void testWebSocketOverSSL() throws Exception
    {
        final String message = "message";
        final CountDownLatch serverLatch = new CountDownLatch(1);
        startServer(new WebSocket.OnTextMessage()
        {
            private Connection connection;

            @Override
            public void onClose(int closeCode, String message)
            {
            }

            @Override
            public void onMessage(String data)
            {
                try
                {
                    Assert.assertEquals(message, data);
                    connection.sendMessage(data);
                    serverLatch.countDown();
                }
                catch (IOException x)
                {
                    x.printStackTrace();
                }
            }

            @Override
            public void onOpen(Connection connection)
            {
                this.connection = connection;
            }
        });
        final CountDownLatch clientLatch = new CountDownLatch(1);
        startClient(new WebSocket.OnTextMessage()
        {
            @Override
            public void onClose(int closeCode, String message)
            {
            }

            @Override
            public void onMessage(String data)
            {
                Assert.assertEquals(message, data);
                clientLatch.countDown();
            }

            @Override
            public void onOpen(Connection connection)
            {
            }
        });
        _connection.sendMessage(message);

        Assert.assertTrue(serverLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(clientLatch.await(5, TimeUnit.SECONDS));
    }
}
