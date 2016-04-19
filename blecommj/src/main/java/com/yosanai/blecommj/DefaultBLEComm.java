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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class DefaultBLEComm extends BluetoothGattCallback implements BLEComm {

    public static final String TAG = DefaultBLEComm.class.getName();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private BluetoothAdapter bluetoothAdapter;
    private BLECommCallback callback;
    private DataHandler dataHandler;
    private int packetSize;
    private long notificationDelay;

    private UUID sUUID;
    private UUID rxUUID;
    private UUID txUUID;
    private UUID fUUID;

    private Set<String> features = new HashSet<>();

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic txChr;
    private BluetoothGattCharacteristic rxChr;

    public DefaultBLEComm() {

    }

    public boolean init(Activity activity, BLECommConfig config) {
        boolean ret = false;
        this.callback = config.getCallback();
        this.packetSize = config.getPacketSize();
        this.notificationDelay = config.getNotificationDelay();
        this.sUUID = BLEScan.getUUID(config.getsUUID());
        this.rxUUID = BLEScan.getUUID(config.getRxUUID());
        this.txUUID = BLEScan.getUUID(config.getTxUUID());
        this.fUUID = BLEScan.getUUID(config.getfUUID());
        if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (null != bluetoothAdapter) {
                ret = true;
            }
        }
        return ret;
    }

    public void cleanup() {
        if (null != dataHandler) {
            dataHandler.cleanup();
        }
        dataHandler = null;
        bluetoothAdapter = null;
        callback = null;
        gatt = null;
        txChr = null;
        rxChr = null;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Connected to ble object.");
            Log.i(TAG, "start service discovery:" +
                    gatt.discoverServices());

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected from ble object.");
            if (null != callback) {
                callback.onDisconnect();
            }
            this.cleanup();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        BluetoothGattService service = gatt.getService(sUUID);
        features.clear();
        if (null != service) {
            BluetoothGattCharacteristic rxChr = service.getCharacteristic(rxUUID);
            BluetoothGattDescriptor descriptor = rxChr.getDescriptor(fUUID);
            if (null != descriptor) {
                if (gatt.readDescriptor(descriptor)) {
                    //NOOP
                } else {
                    Log.e(TAG, "Can't readDescriptor");
                }
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (BluetoothGatt.GATT_SUCCESS == status) {
            if (null != descriptor) {
                String value = new String(descriptor.getValue());
                if (null != value && !value.isEmpty()) {
                    StringTokenizer tok = new StringTokenizer(value, ",");
                    while (tok.hasMoreTokens()) {
                        features.add(tok.nextToken());
                    }
                }
                if (features.isEmpty()) {
                    features.add(DATA_SIMPLE);
                }
            }
            BluetoothGattService service = descriptor.getCharacteristic().getService();
            if (null != service) {
                rxChr = descriptor.getCharacteristic();
                txChr = service.getCharacteristic(txUUID);
                if (null != rxChr && null != txChr) {
                    if (gatt.setCharacteristicNotification(rxChr, true)) {
                        BluetoothGattDescriptor desc = rxChr.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (gatt.writeDescriptor(desc)) {
                            //NOOP
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS && descriptor.getUuid().toString().equals(CLIENT_CHARACTERISTIC_CONFIG)) {
            postConnection();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (null != dataHandler) {
            dataHandler.onData(characteristic.getStringValue(0), characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (null != dataHandler) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (null != callback) {
                        dataHandler.onSent();
                    }
                }
            }, notificationDelay);
        }
    }

    @Override
    public void postConnection() {
        if (features.contains(DATA_SIMPLE)) {
            dataHandler = new SimpleDataHandler(this, callback, packetSize);
        } else if (features.contains(DATA_PROTOCOL)) {
            dataHandler = new ProtocolDataHandler(this, callback, packetSize, notificationDelay);
        } else {
            Log.e(TAG, "Unknown/empty data handler, defaulting to simple");
            dataHandler = new SimpleDataHandler(this, callback, packetSize);
        }

        if (null != dataHandler) {
            dataHandler.onConnectionFinalized();
        }
    }


    @Override
    public boolean connect(Activity activity, String address) {
        boolean ret = false;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (null != device) {
            gatt = device.connectGatt(activity, false, this);
            ret = true;
        }
        return ret;
    }

    @Override
    public void send(String data) {
        if (null != dataHandler) {
            dataHandler.send(data);
        }
    }

    @Override
    public void writeRawData(byte[] rawData) {
        if (null != txChr) {
            txChr.setValue(rawData);
            gatt.writeCharacteristic(txChr);
        }
    }

    @Override
    public void disconnect() {
        if (null != gatt) {
            gatt.disconnect();
        }
    }

    @Override
    public Set<String> getFeatures() {
        return features;
    }
}
