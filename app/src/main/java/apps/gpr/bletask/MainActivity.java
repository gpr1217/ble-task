package apps.gpr.bletask;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter;
    private Handler handler;
    private BluetoothLeScanner btScanner;
    ListView ble_devices_list;
    List<String> devicesList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    boolean isScanning = false;

    boolean testData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ble_devices_list = findViewById(R.id.ble_devices_list);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,
                                        android.R.id.text1,devicesList);
        ble_devices_list.setAdapter(adapter);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this,"BLE not supported",Toast.LENGTH_LONG).show();
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        handler = new Handler();
        enableBT();
        ble_devices_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    String btDeviceInfo = (String) ble_devices_list.getItemAtPosition(i);
                    if (btDeviceInfo.contains("-")) {
                        String[] arr = btDeviceInfo.split("-");
                        if (arr.length > 0)
                            connectToBLE(arr[0], arr[1]);
                    } else {
                        Toast.makeText(getApplicationContext(), "Error: No Device found", Toast.LENGTH_LONG).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    // Navigate to a Server Activity where we can define Gatt characteristics and read/write data
    private void connectToBLE(String btDeviceName,String btDeviceAddress){
        try {
            Intent intent = new Intent(this, DeviceConnectionActivity.class);
            intent.putExtra("device_name", btDeviceName);
            intent.putExtra("device_address", btDeviceAddress);
            if (isScanning) {
                btScanner.stopScan(leScanCallback);
            }
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            devicesList.clear();
            bleStartScan();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void enableBT(){
        try {
            if (btAdapter == null || !btAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void bleStartScan(){
        try {
            if (btAdapter == null || btScanner == null)
                return;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    btScanner.stopScan(leScanCallback);
                }
            }, 10000);

            isScanning = true;
            btScanner.startScan(leScanCallback);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    boolean deviceFound = false;
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String dName = result.getDevice().getName();
            String dAddress = result.getDevice().getAddress();
            if (dAddress != null && dName != null) {
                if (devicesList.contains(dName + "-" + dAddress)){
                    Log.d("leScanCallback","Contains");
                }else {
                    deviceFound = true;
                    devicesList.add(dName + "-" + dAddress);
                }
            }else{
                if (devicesList.isEmpty() && !deviceFound)
                    devicesList.add("No Devices Found");
            }

            adapter.notifyDataSetChanged();
        }
    };
}
