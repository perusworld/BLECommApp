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

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class BLECommConfigBuilder {

    protected BLECommCallback callback;
    protected int packetSize = 20;
    protected String sUUID = "fff0";
    protected String txUUID = "fff1";
    protected String rxUUID = "fff2";
    protected String fUUID = "fff3";
    protected long notificationDelay = 50;

    public BLECommConfigBuilder setCallback(BLECommCallback callback) {
        this.callback = callback;
        return this;
    }

    public BLECommConfigBuilder setPacketSize(int packetSize) {
        this.packetSize = packetSize;
        return this;
    }

    public BLECommConfigBuilder setsUUID(String sUUID) {
        this.sUUID = sUUID;
        return this;
    }

    public BLECommConfigBuilder setTxUUID(String txUUID) {
        this.txUUID = txUUID;
        return this;
    }

    public BLECommConfigBuilder setRxUUID(String rxUUID) {
        this.rxUUID = rxUUID;
        return this;
    }

    public BLECommConfigBuilder setfUUID(String fUUID) {
        this.fUUID = fUUID;
        return this;
    }

    public BLECommConfigBuilder setNotificationDelay(long notificationDelay) {
        this.notificationDelay = notificationDelay;
        return this;
    }

    public BLECommConfig build() {
        BLECommConfig ret = new BLECommConfig(callback, packetSize, sUUID, rxUUID, txUUID, fUUID, notificationDelay);
        return ret;
    }

}
