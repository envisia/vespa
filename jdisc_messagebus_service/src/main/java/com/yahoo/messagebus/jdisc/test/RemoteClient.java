// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus.jdisc.test;

import com.yahoo.jrt.ListenFailedException;
import com.yahoo.jrt.slobrok.server.Slobrok;
import com.yahoo.messagebus.*;
import com.yahoo.messagebus.network.rpc.RPCNetwork;
import com.yahoo.messagebus.network.rpc.RPCNetworkParams;
import com.yahoo.messagebus.test.SimpleProtocol;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class RemoteClient {

    private final Slobrok slobrok;
    private final String slobrokId;
    private final MessageBus mbus;
    private final ReplyQueue queue = new ReplyQueue();
    private final SourceSession session;

    private RemoteClient(Slobrok slobrok, String slobrokId, Protocol protocol) {
        this.slobrok = slobrok;
        this.slobrokId = slobrok != null ? slobrok.configId() : slobrokId;
        mbus = new MessageBus(new RPCNetwork(new RPCNetworkParams().setSlobrokConfigId(this.slobrokId)),
                              new MessageBusParams().addProtocol(protocol));
        session = mbus.createSourceSession(new SourceSessionParams().setThrottlePolicy(null).setReplyHandler(queue));
    }

    public Result sendMessage(Message msg) {
        return session.send(msg);
    }

    public Reply awaitReply(int timeout, TimeUnit unit) throws InterruptedException {
        return queue.awaitReply(timeout, unit);
    }

    public String slobrokId() {
        return slobrokId;
    }

    public void close() {
        session.destroy();
        mbus.destroy();
        if (slobrok != null) {
            slobrok.stop();
        }
    }

    public static RemoteClient newInstanceWithInternSlobrok() {
        return new RemoteClient(newSlobrok(), null, new SimpleProtocol());
    }

    public static RemoteClient newInstanceWithExternSlobrok(String slobrokId) {
        return new RemoteClient(null, slobrokId, new SimpleProtocol());
    }

    public static RemoteClient newInstanceWithProtocolAndInternSlobrok(Protocol protocol) {
        return new RemoteClient(newSlobrok(), null, protocol);
    }

    private static Slobrok newSlobrok() {
        Slobrok slobrok;
        try {
            slobrok = new Slobrok();
        } catch (ListenFailedException e) {
            throw new IllegalStateException(e);
        }
        return slobrok;
    }
}
