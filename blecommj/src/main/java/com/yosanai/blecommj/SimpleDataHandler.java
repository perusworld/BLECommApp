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

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class SimpleDataHandler implements DataHandler {

    public static final String TAG = SimpleDataHandler.class.getSimpleName();

    protected BLEComm bleComm;
    protected BLECommCallback callback;
    protected int packetSize;
    protected int len = 0;
    protected int loc = 0;
    protected int idx = 0;
    protected int dataLength = 0;
    protected Queue<String> dataPipe = new ArrayDeque<>();

    public SimpleDataHandler(BLEComm bleComm, BLECommCallback callback, int packetSize) {
        this.bleComm = bleComm;
        this.callback = callback;
        this.packetSize = packetSize;
        len = packetSize;
        loc = 0;
        idx = 0;
        dataLength = 0;
    }

    public void sendChunk(String data) {
        if (null == data) {
            data = dataPipe.peek();
        } else {
            dataPipe.offer(data);
        }
        len = packetSize;
        if (null == data) {
            loc = 0;
            idx = 0;
            dataLength = 0;
        } else {
            dataLength = data.length();
            if (loc < dataLength) {
                int rmdr = dataLength - loc;
                if (rmdr <= len) {
                    len = rmdr;
                }
                Log.d(TAG, "Sending from " + loc + " to " + (loc + len));
                writeString(data, loc, loc + len);
                loc += len;
                idx += 1;
            } else {
                dataPipe.remove();
                len = packetSize;
                loc = 0;
                idx = 0;
                dataLength = 0;
                if (!dataPipe.isEmpty()) {
                    sendChunk(null);
                }
            }
        }
    }

    @Override
    public void send(String data) {
        if (data.length() <= packetSize) {
            writeString(data);
        } else {
            sendChunk(data);
        }

    }

    protected void sendRaw(byte[] data) {
        bleComm.writeRawData(data);
    }

    protected void writeString(String data) {
        writeString(data, 0, data.length());
    }

    protected void writeString(String data, int from, int to) {
        bleComm.writeRawData(data.substring(from, to).getBytes());
    }

    @Override
    public void onSent() {
        sendChunk(null);
    }

    @Override
    public void onData(String data, byte[] bytes) {
        if (null != callback) {
            callback.onData(data);
        }
    }

    @Override
    public void onConnectionFinalized() {
        if (null != callback) {
            callback.onConnect();
        }
    }

    @Override
    public void cleanup() {
        bleComm = null;
        callback = null;
        dataPipe.clear();
    }
}
