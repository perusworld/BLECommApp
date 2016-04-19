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
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Saravana Perumal Shanmugam
 *
 */
public class BLEScan {

    public static final String TAG = BLEScan.class.getName();

    public static final UUID DEVICE_INFO = BLEScan.getUUID("180A");
    public static final UUID MANU_NAME = BLEScan.getUUID("2A29");
    public static final UUID MODEL_NUM = BLEScan.getUUID("2A24");
    public static final UUID SERIAL_NUM = BLEScan.getUUID("2A25");
    public static final UUID HW_REV = BLEScan.getUUID("2A27");
    public static final UUID FW_REV = BLEScan.getUUID("2A26");
    public static final UUID SW_REV = BLEScan.getUUID("2A28");

    public static final UUID[] SCANABLES = new UUID[]{
            MANU_NAME, MODEL_NUM, SERIAL_NUM, HW_REV, FW_REV, SW_REV
    };

    Handler handler;
    BluetoothAdapter bluetoothAdapter;
    Map<String, BLEObject> scanned = new HashMap<>();
    UUID service;
    BLEScanCallback callback;
    Activity activity;

    public static UUID getUUID(String uuidStr) {
        UUID ret = null;
        if (null != uuidStr && 4 == uuidStr.trim().length()) {
            if (4 == uuidStr.trim().length()) {
                ret = UUID.fromString("0000" + uuidStr.trim() + "-0000-1000-8000-00805F9B34FB");
            } else {
                ret = UUID.fromString(uuidStr);
            }
        }
        return ret;
    }

    public boolean init(Activity activity, UUID service) {
        boolean ret = false;
        this.activity = activity;
        handler = new Handler();
        scanned.clear();
        this.service = service;
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

    public void scan(long timeout, BLEScanCallback callback) {
        scan(timeout, false, callback);
    }

    public void updateDeviceInfo() {
        boolean done = true;
        BLEDeviceInfoService infoService = new BLEDeviceInfoService(new BLEDeviceInfoServiceCallback() {
            @Override
            public void onDone() {
                updateDeviceInfo();
            }
        }, activity);
        for (BLEObject bleObj : scanned.values()) {
            if (null == bleObj.getSerialNumber()) {
                infoService.readDeviceInfo(bleObj);
                done = false;
                break;
            } else {
                //NOOP
            }
        }
        if (done) {
            callback.onDone(scanned.values());
        }
    }

    public void scan(long timeout, final boolean updateInfo, BLEScanCallback callbck) {
        this.callback = callbck;
        scanned.clear();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(scanCallback);
                if (updateInfo) {
                    updateDeviceInfo();
                } else if (null != callback) {
                    callback.onDone(scanned.values());
                }
            }
        }, timeout);
        if (null == service) {
            bluetoothAdapter.startLeScan(scanCallback);
        } else {
            bluetoothAdapter.startLeScan(new UUID[]{service}, scanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback scanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (scanned.containsKey(device.getAddress())) {
                        scanned.get(device.getAddress()).device = device;
                    } else {
                        scanned.put(device.getAddress(), new BLEObject(device, device.getAddress(), device.getName()));
                    }
                }
            };


}
