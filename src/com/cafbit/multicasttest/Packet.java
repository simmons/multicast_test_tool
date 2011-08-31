/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
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
package com.cafbit.multicasttest;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Encapsulate packet details that we are interested in.
 * @author simmons
 */
public class Packet {
    public InetAddress src;
    public int srcPort;
    public InetAddress dst;
    public int dstPort;
    public String description;

    public Packet() {}
    public Packet(DatagramPacket dp, DatagramSocket socket) {
        src = dp.getAddress();
        srcPort = dp.getPort();
        dst = socket.getLocalAddress();
        dstPort = socket.getLocalPort();
    }
}
