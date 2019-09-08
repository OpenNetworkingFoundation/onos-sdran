/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.xran.impl.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.sctp.SctpMessage;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.pdu.XrancPdu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Xran channel handler.
 */
@Sharable
public class XranChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log =
            LoggerFactory.getLogger(XranChannelHandler.class);

    private final XranServer xranServer;

    XranChannelHandler(XranServer xranServer) {
        this.xranServer = xranServer;
    }

    /**
     * Given PDU construct an SCTP message.
     *
     * @param pdu PDU packet
     * @return SCTP message
     * @throws IOException IO exception
     */
    public static SctpMessage getSctpMessage(XrancPdu pdu)  {
        BerByteArrayOutputStream os = new BerByteArrayOutputStream(4096); //4096 is the buffersize
        try {
            pdu.encode(os);
        } catch (IOException e) {
          log.warn(ExceptionUtils.getFullStackTrace(e));
        }

        //log.info("************** Sending message: {}", pdu.toString());
        final ByteBuf buf = Unpooled.buffer(os.getArray().length);
        for (int i = 0; i < buf.capacity(); i++) {
            buf.writeByte(os.getArray()[i]);
        }
        return new SctpMessage(0, 0, buf);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws IOException, URISyntaxException {
        final SocketAddress address = ctx.channel().remoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            log.warn("Invalid client connection. Xran Cell is indentifed based on IP");
            ctx.close();
            return;
        }

        final InetSocketAddress inetAddress = (InetSocketAddress) address;
        final String host = inetAddress.getHostString();
        log.info("New client connected to the Server: {}", host);

        log.info("Adding device...");

        xranServer.deviceAgent.addConnectedCell(host, ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        SctpMessage sctpMessage = (SctpMessage) msg;
        ByteBuf byteBuf = sctpMessage.content();

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        XrancPdu recvPdu = new XrancPdu();

        InputStream inputStream = new ByteArrayInputStream(bytes);

        recvPdu.decode(inputStream);
        //log.info("******** Received message: \n" + recvPdu.toString());


        ExecutorService executors = Executors.newCachedThreadPool();
        executors.submit(()-> {
            try {
                xranServer.packetAgent.handlePacket(recvPdu, ctx);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Client dropped the connection");

        final SocketAddress address = ctx.channel().remoteAddress();
        if (!(address instanceof InetSocketAddress)) {
            log.warn("Invalid client connection. Xran Cell is indentifed based on IP");
            ctx.close();
            return;
        }

        final InetSocketAddress inetAddress = (InetSocketAddress) address;
        final String host = inetAddress.getHostString();

        xranServer.deviceAgent.removeConnectedCell(host);

        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        log.warn("exceptionCaught: {}", ExceptionUtils.getStackTrace(cause));
        cause.printStackTrace();
        //ctx.close();
    }
}
