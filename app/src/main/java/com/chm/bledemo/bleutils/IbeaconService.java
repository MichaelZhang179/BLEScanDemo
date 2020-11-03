package com.chm.bledemo.bleutils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.text.DecimalFormat;

public class IbeaconService extends Service {

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String Target_UUID = "你的uuid";//过滤搜索的beacon本程序中暂未使用到

    BluetoothManager bluetoothManager;
    BluetoothAdapter mBluetoothAdapter;

    public IbeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getApplication().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("tag", "ibeacon service startLeScan");

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("tag", "stopScanningForBeacons");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {
            int startByte = 2;
            boolean patternFound = false;
            // 寻找ibeacon
            while (startByte <= 5) {
                if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { // Identifies  correct  data  length
                    patternFound = true;
                    break;
                }
                startByte++;
            }
            // 如果找到了的话
            if (patternFound) {
                // 转换为16进制
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                // ibeacon的UUID值
                String uuid = hexString.substring(0, 8) + "-"
                        + hexString.substring(8, 12) + "-"
                        + hexString.substring(12, 16) + "-"
                        + hexString.substring(16, 20) + "-"
                        + hexString.substring(20, 32);

                // ibeacon的Major值
                int major = (scanRecord[startByte + 20] & 0xff) * 0x100
                        + (scanRecord[startByte + 21] & 0xff);

                // ibeacon的Minor值
                int minor = (scanRecord[startByte + 22] & 0xff) * 0x100
                        + (scanRecord[startByte + 23] & 0xff);

                String ibeaconName = device.getName();
                String mac = device.getAddress();
                int txPower = (scanRecord[startByte + 24]);
                Log.d("BLE", bytesToHex(scanRecord));
                Log.d("BLE", "Name：" + ibeaconName + "\nMac：" + mac
                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi);

                Log.d("BLE", "distance：" + calculateAccuracy(txPower, rssi));

                DecimalFormat df = new DecimalFormat("#.##");
                String distance = df.format(calculateAccuracy(txPower, rssi));//将距离保留两位小数

                String sfsdf = "BLE:" + bytesToHex(scanRecord) + "\n" + "Name：" + ibeaconName + "\nMac：" + mac
                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi + "\n" + "DOUBLE distance：" + calculateAccuracy(txPower, rssi)
                        + "distance：" + distance;

                Log.d("tag", "sfsdf=" + sfsdf);
                Log.d("tag", "Name：" + ibeaconName + "\nMac：" + mac + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\ndistance：" + distance);
            }
        }
    };


    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }
}