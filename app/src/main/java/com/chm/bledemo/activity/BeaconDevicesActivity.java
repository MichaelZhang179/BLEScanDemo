package com.chm.bledemo.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.chm.bledemo.R;
import com.chm.bledemo.bleutils.BeaconInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BeaconDevicesActivity extends BaseActivity {
    private static final String TAG = "BeaconDevicesActivity";
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private List<BeaconInfo> beaconInfos;
    private MyBaseAdapter baseAdapter;
    private List<String> uuids;

    @BindView(R.id.mlistview)
    ListView mListviev;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        ButterKnife.bind(this);

        beaconInfos = new ArrayList<>();
        uuids = new ArrayList<>();

        baseAdapter = new MyBaseAdapter(this);

        secarchIbeacon();

    }

    /***
     * 搜索iBeacon
     */
    public void secarchIbeacon() {

        Log.d(TAG, "start secarchIbeacon()");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //获取此设备的默认蓝牙适配器。
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "mBluetoothAdapter is enabled");
//                Intent startIntent = new Intent(this, IbeaconService.class);
//                startService(startIntent);

                if (mBluetoothAdapter != null) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            }
        } else {
            //开启蓝牙
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
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
//                Log.d(TAG, bytesToHex(scanRecord));
//                Log.d(TAG, "Name：" + ibeaconName + "\nMac：" + mac
//                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
//                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi);
//
//                Log.d(TAG, "distance：" + calculateAccuracy(txPower, rssi));

                DecimalFormat df = new DecimalFormat("#.##");
                String distance = df.format(calculateAccuracy(txPower, rssi));//将距离保留两位小数

//                String sfsdf = "BLE:" + bytesToHex(scanRecord) + "\n" + "Name：" + ibeaconName + "\nMac：" + mac
//                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
//                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi + "\n" + "DOUBLE distance：" + calculateAccuracy(txPower, rssi)
//                        + "distance：" + distance;
//
//                Log.d(TAG, "sfsdf=" + sfsdf);
//                Log.d(TAG, "Name：" + ibeaconName + "\nMac：" + mac + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
//                        + minor + "\ndistance：" + distance);
                if(!uuids.contains(uuid)) {
                    uuids.add(uuid);
                    BeaconInfo info = new BeaconInfo();
                    info.setName(ibeaconName);
                    info.setMacAddress(mac);
                    info.setUuid(uuid);
                    info.setMajor(""+major);
                    info.setMinor(""+minor);

                    beaconInfos.add(info);
                                    Log.d(TAG, "Name：" + ibeaconName + "\nMac：" + mac + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\ndistance：" + distance);
                    mListviev.setAdapter(baseAdapter);
                    baseAdapter.updateAdapter(beaconInfos);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    public class MyBaseAdapter extends BaseAdapter {
        //定义该adapter的数据集合
        private List<BeaconInfo> beaconInfos;
        private Context context;

        public MyBaseAdapter(Context context) {
            this.context = context;
        }

        public void updateAdapter(List<BeaconInfo> beaconInfos) {
            this.beaconInfos = beaconInfos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return beaconInfos != null ? beaconInfos.size(): 0;
        }

        @Override
        public BeaconInfo getItem(int position) {
            return beaconInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i("Tiger", "getView position = " + position);

            ViewHolder holder = null;
            //如果convertView为空，说明还没有初始化
            if (convertView == null) {
                //首先得到LayoutInflater的对象，通过静态方法from
                LayoutInflater ifter = LayoutInflater.from(context);

                convertView = ifter.inflate(R.layout.list_item_beacon, null);

                holder = new ViewHolder();
                holder.nameTextView = (TextView) convertView.findViewById(R.id.name);
                holder.macAddressTextView = (TextView) convertView.findViewById(R.id.mac_address);
                holder.uuidTextView = (TextView) convertView.findViewById(R.id.uuid);
                holder.otherTextView = (TextView) convertView.findViewById(R.id.other);
                //缓存这个ViewHodler
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            //得到数据
            BeaconInfo beaconInfo = beaconInfos.get(position);

            if(beaconInfo != null) {
                String name = !TextUtils.isEmpty(beaconInfo.getName()) ? beaconInfo.getName() : "NULL";
                holder.nameTextView.setText("name: " + name);
                holder.macAddressTextView.setText("mac address: " + beaconInfo.getMacAddress());
                holder.uuidTextView.setText("UUID: " + beaconInfo.getUuid());
                holder.otherTextView.setText("minor: " + beaconInfo.getMinor() + ", major : " + beaconInfo.getMajor());
            }

            return convertView;
        }

        class ViewHolder {
            TextView nameTextView;
            TextView macAddressTextView;
            TextView uuidTextView;
            TextView otherTextView;
        }
    }

}

