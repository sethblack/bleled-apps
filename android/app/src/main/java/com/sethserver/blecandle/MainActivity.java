package com.sethserver.blecandle;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int PERMISSION_COARSE_LOCATION = 10;
    private final static String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BLECandle> mDiscoveredCandles;

    private SeekBar mSeekBar;
    private TextView mCandlesFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCandlesFound = (TextView)findViewById(R.id.textCandlesFound);

        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int mProgress = 0;
            private int mPreviousProgress = 0;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mProgress == mPreviousProgress) {
                    return;
                }

                mPreviousProgress = mProgress;

                Log.i(TAG, "Progress: " + mProgress);

                sendMessageToAllConnectedDevices(mProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
            }
        });

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        mDiscoveredCandles = new ArrayList<BLECandle>();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_COARSE_LOCATION);
        } else {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mBluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
                }
            }
        }
    }

    public void sendMessageToAllConnectedDevices(int value) {
        final int inValue = value;

        class myRunnable implements Runnable {
            public void run() {
                for (BLECandle candle : mDiscoveredCandles) {
                    candle.connectGatt();

                    int sleepTicks = 0;

                    while(!candle.hasCharacteristic()) {
                        SystemClock.sleep(25);

                        sleepTicks += 1;

                        if (sleepTicks > 80) {
                            Log.w(TAG, "Candle " + candle.getDeviceAddress() + " took too long to connect");
                            break;
                        }
                    }

                    candle.writeToCharacteristic(inValue);

                    sleepTicks = 0;

                    while(candle.isBusy()) {
                        SystemClock.sleep(25);

                        sleepTicks += 1;

                        if (sleepTicks > 80) {
                            Log.w(TAG, "Candle " + candle.getDeviceAddress() + " took too long to respond to the write");
                            break;
                        }
                    }

                    candle.closeGatt();

                    SystemClock.sleep(10);
                }
            }
        }

        new Thread(new myRunnable()).start();
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();

            String deviceName = device.getName();

            if (deviceName == null) {
                return;
            }

            if (deviceName.equalsIgnoreCase("bleled")) {
                Log.i(TAG, "name: " + deviceName);
                Log.i(TAG, "address: " + device.getAddress());
                Log.i(TAG, "hashCode: " + device.hashCode());

                for (BLECandle c : MainActivity.this.mDiscoveredCandles) {
                    if (c.getDeviceAddress().equals(device.getAddress())) {
                        return;
                    }
                }

                BLECandle candle = new BLECandle(device, MainActivity.this);
                //candle.connectGatt();

                MainActivity.this.mDiscoveredCandles.add(candle);

                MainActivity.this.mCandlesFound.setText(
                        "Candles Found: " + MainActivity.this.mDiscoveredCandles.size()
                );
            }
        }
    };
}
