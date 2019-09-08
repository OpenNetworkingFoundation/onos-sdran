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

package org.onosproject.xran;

import io.netty.channel.ChannelHandlerContext;
import org.onosproject.xran.asn1lib.pdu.XrancPdu;

import java.io.IOException;

/**
 * Xran packet processor interface.
 */
public interface XranPacketProcessor {
    /**
     * Handle an incoming packet.
     *
     * @param pdu pdu of incoming packet
     * @param ctx channel received the packet
     * @throws IOException io exception
     * @throws InterruptedException interrupted exception
     */
    void handlePacket(XrancPdu pdu, ChannelHandlerContext ctx) throws IOException, InterruptedException;
}
