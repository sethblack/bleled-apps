package com.sethserver.blecandle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
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

                for (BLECandle candle : mDiscoveredCandles) {
                    candle.writeToCharacteristic(mProgress);
                }
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
        }

        mDiscoveredCandles = new ArrayList<BLECandle>();

        mBluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
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
                Log.i(TAG, deviceName);
                BLECandle candle = new BLECandle(device, MainActivity.this);

                candle.connectGatt();

                MainActivity.this.mDiscoveredCandles.add(candle);
                MainActivity.this.mCandlesFound.setText(
                        "Candles Found: " + MainActivity.this.mDiscoveredCandles.size()
                );
            }
        }
    };
}
