/**
 *
 * This is the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package com.yosanai.blecommj;

import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class ProtocolDataHandler extends SimpleDataHandler {

    public static String TAG = ProtocolDataHandler.class.getSimpleName();

    public static final int PingIn = 0xCC;
    public static final int PingOut = 0xDD;
    public static final int Data =  0xEE;
    public static final int ChunkedDataStart = 0xEB;
    public static final int ChunkedData = 0xEC;
    public static final int ChunkedDataEnd = 0xED;
    public static final int EOMFirst = 0xFE;
    public static final int EOMSecond = 0xFF;
    public static final int cmdLength = 3;
    public byte[] PingOutCmd;
    ByteArrayOutputStream chunkedData = null;

    private boolean inSync;

    protected long notificationDelay;

    public ProtocolDataHandler(BLEComm bleComm, BLECommCallback callback, int packetSize, long notificationDelay) {
        super(bleComm, callback, packetSize-cmdLength);
        this.notificationDelay = notificationDelay;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(PingOut);
        bos.write(EOMFirst);
        bos.write(EOMSecond);
        PingOutCmd = bos.toByteArray();
    }

    @Override
    public void onConnectionFinalized() {
        inSync = false;
    }

    public void pingIn() {
        sendRaw(PingOutCmd);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (null != callback) {
                    callback.onConnect();
                }
            }
        }, notificationDelay);
    }

    public void pingOut() {
        //NOOP
    }

    @Override
    protected void writeString(String data, int from, int to) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean chunked = !(from == 0 && to == data.length());
        Log.d(TAG, "Chunked " + chunked + " from " + from + " to " + to);
        bos.write(chunked ? ((loc == 0) ? ChunkedDataStart : (to == data.length()? ChunkedDataEnd : ChunkedData)): Data);
        try {
            bos.write(data.substring(from, to).getBytes());
        } catch (IOException e) {
            //NOOP
        }
        bos.write(EOMFirst);
        bos.write(EOMSecond);
        super.sendRaw(bos.toByteArray());
    }

    protected int toUnsigned(byte data) {
        return data & 0xff;
    }

    @Override
    public void onData(String newData, byte[] data) {
        int len = newData.length();
        byte[] msgData = null;
        if (cmdLength < len) {
            msgData = new byte[len - cmdLength];
            System.arraycopy(data, 1, msgData, 0, len-cmdLength);
        }
        if (EOMFirst == toUnsigned(data[len - 2]) && EOMSecond == toUnsigned(data[len - 1])) {
            int cmd = toUnsigned(data[0]);
            if (cmd == PingIn) {
                pingIn();
            } else if (cmd == PingOut) {
                pingOut();
            } else if (cmd == Data) {
                super.onData(new String(msgData), msgData);
            } else if (cmd == ChunkedDataStart) {
                chunkedData = new ByteArrayOutputStream();
                try {
                    chunkedData.write(msgData);
                } catch (IOException e) {
                    //NOOP
                }
            } else if (cmd == ChunkedData) {
                try {
                    chunkedData.write(msgData);
                } catch (IOException e) {
                    //NOOP
                }
            } else if (cmd == ChunkedDataEnd) {
                try {
                    chunkedData.write(msgData);
                } catch (IOException e) {
                    //NOOP
                }
                super.onData(new String(chunkedData.toByteArray()),chunkedData.toByteArray());
                chunkedData = null;
            } else {
                //Unknown
            }
        }
    }
}
