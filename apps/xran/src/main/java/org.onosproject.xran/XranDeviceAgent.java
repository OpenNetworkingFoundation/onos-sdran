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

/**
 * Xran device agent interface.
 */
public interface XranDeviceAgent {

    /**
     * Add connected CELL.
     *
     * @param host IP of host trying to connect
     * @param ctx  channel of CELL speaking to
     * @return true if succeeded
     */
    boolean addConnectedCell(String host, ChannelHandlerContext ctx);

    /**
     * Remove disconnected CELL.
     *
     * @param host IP of host disconnected
     * @return true if remove succeeded
     */
    boolean removeConnectedCell(String host);
}
