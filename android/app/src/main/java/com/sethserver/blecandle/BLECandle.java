package com.sethserver.blecandle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Created by sethblack on 3/19/16.
 */
public class BLECandle {
    private static final String TAG = "BLECandle";
    private static final UUID bleCandleServiceUUID =
            UUID.fromString("ff0f007d-0a1a-47b2-ec4e-8a43e4a1207d");
    private static final UUID bleCandleCharacteristicUUID =
            UUID.fromString("ff0f1020-0a1a-47b2-ec4e-8a43e4a1207d");
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private Context mContext;
    private BluetoothDevice mDevice;
    private boolean mConnected = false;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

                mConnected = true;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mConnected = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService s : gatt.getServices()) {
                    Log.i(TAG, "Serivce: " + s.getUuid().toString());

                    if (s.getUuid().equals(bleCandleServiceUUID)) {
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            if (c.getUuid().equals(bleCandleCharacteristicUUID)) {
                                Log.i(TAG, "Characteristic: " + c.getUuid().toString());
                                BLECandle.this.mCharacteristic = c;
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    BLECandle(BluetoothDevice device, Context ctx) {
        mContext = ctx;
        mDevice = device;
    }

    public void connectGatt() {
        mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
    }

    public boolean isConnected() {
        return mConnected;
    }

    public boolean writeToCharacteristic(int value) {
        Log.i(TAG, "Writing to characteristic");

        if (mCharacteristic == null) {
            Log.i(TAG, "Can't write to null Characteristic");
            return false;
        }

        mCharacteristic.setValue(value, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16, 0);

        if(!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
            Log.w(TAG, "Failed to write characteristic");
            return false;
        }

        return true;
    }
}
