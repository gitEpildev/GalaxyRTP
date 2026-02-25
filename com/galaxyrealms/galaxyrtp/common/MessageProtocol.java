/*
 * Decompiled with CFR 0.152.
 */
package com.galaxyrealms.galaxyrtp.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class MessageProtocol {
    private MessageProtocol() {
    }

    public static byte[] encodeConnectRequest(String serverName, String worldName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF(serverName);
        out.writeUTF(worldName);
        return baos.toByteArray();
    }

    public static byte[] encodeRtpPending(UUID playerUuid, String worldName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF(playerUuid.toString());
        out.writeUTF(worldName);
        return baos.toByteArray();
    }

    public static String[] decodeConnectRequest(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        String serverName = in.readUTF();
        String worldName = in.readUTF();
        return new String[]{serverName, worldName};
    }

    public static String[] decodeRtpPending(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        String uuidStr = in.readUTF();
        String worldName = in.readUTF();
        return new String[]{uuidStr, worldName};
    }
}

