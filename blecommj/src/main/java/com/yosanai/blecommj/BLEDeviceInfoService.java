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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class BLEDeviceInfoService extends BluetoothGattCallback {

    public static final String TAG = BLEDeviceInfoService.class.getName();

    private BluetoothGatt gatt;
    private BLEObject bleObject;
    private BLEDeviceInfoServiceCallback callback;
    private Activity activity;
    private int connState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    List<UUID> scanValues;

    public BLEDeviceInfoService(BLEDeviceInfoServiceCallback callback, Activity activity) {
        this.callback = callback;
        this.activity = activity;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            connState = STATE_CONNECTED;
            Log.i(TAG, "Connected to ble object.");
            Log.i(TAG, "start service discovery:" +
                    gatt.discoverServices());

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            connState = STATE_DISCONNECTED;
            Log.i(TAG, "Disconnected from ble object.");
            callback.onDone();
            cleanup();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        BluetoothGattService infoService = gatt.getService(BLEScan.DEVICE_INFO);
        if (null != infoService) {
            readCharacteristics();
        }
    }

    public void readCharacteristics() {
        BluetoothGattService infoService = gatt.getService(BLEScan.DEVICE_INFO);
        if (null != infoService && !scanValues.isEmpty()) {
            BluetoothGattCharacteristic chr = infoService.getCharacteristic(scanValues.remove(0));
            if (null != chr) {
                gatt.readCharacteristic(chr);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(BLEScan.MANU_NAME)) {
            bleObject.setManufacturerName(characteristic.getStringValue(0));
        } else if (characteristic.getUuid().equals(BLEScan.MODEL_NUM)) {
            bleObject.setModelNumber(characteristic.getStringValue(0));
        } else if (characteristic.getUuid().equals(BLEScan.SERIAL_NUM)) {
            bleObject.setSerialNumber(characteristic.getStringValue(0));
        } else if (characteristic.getUuid().equals(BLEScan.HW_REV)) {
            bleObject.setHardwareRevision(characteristic.getStringValue(0));
        } else if (characteristic.getUuid().equals(BLEScan.FW_REV)) {
            bleObject.setFirmwareRevision(characteristic.getStringValue(0));
        } else if (characteristic.getUuid().equals(BLEScan.SW_REV)) {
            bleObject.setSoftwareRevision(characteristic.getStringValue(0));
        } else {
            Log.e(TAG, "Unknown characteristic " + characteristic.getUuid().toString());
        }
        if (bleObject.hasDeviceInfo()) {
            disconnect();
        } else if (!scanValues.isEmpty()) {
            readCharacteristics();
        }
    }

    public void cleanup() {
        if (null != gatt) {
            gatt.close();
        }
        gatt = null;
        bleObject = null;
    }

    public void readDeviceInfo(BLEObject bleObject) {
        Log.d(TAG, "Connecting.");
        this.bleObject = bleObject;
        scanValues = new ArrayList<>(Arrays.asList(BLEScan.SCANABLES));
        gatt = bleObject.device.connectGatt(activity, false, this);
        connState = STATE_CONNECTING;
    }

    public void disconnect() {
        if (null != gatt) {
            gatt.disconnect();
        }
    }
}
