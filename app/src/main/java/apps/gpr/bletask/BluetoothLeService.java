package apps.gpr.bletask;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private String btDeviceAddress;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";

    byte data[] = { 0x10,    0x01, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5, (byte) 0xA5};

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == STATE_CONNECTED){
                broadcastUpdate(ACTION_GATT_CONNECTED);
                bluetoothGatt.discoverServices();
            }else if (newState == STATE_DISCONNECTED){
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothGattCharacteristic characteristic =
                        gatt.getService(SampleGattAttributes.SERVICE_UUID)
                                .getCharacteristic(SampleGattAttributes.CHARACTERISTICS_UUID);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(SampleGattAttributes.CHARACTERISTICS_UUID);

                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }else{

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
                broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            BluetoothGattCharacteristic characteristic =
                    gatt.getService(SampleGattAttributes.SERVICE_UUID)
                            .getCharacteristic(SampleGattAttributes.CHARACTERISTICS_UUID);

            characteristic.setValue(data);
            gatt.writeCharacteristic(characteristic);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    private final IBinder iBinder = new LocalBinder();

    public class LocalBinder extends Binder{
        BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        return true;
    }

    public boolean connect(final String address){
        if (bluetoothAdapter == null || address == null) {
            return false;
        }

        if (btDeviceAddress != null && address.equals(btDeviceAddress)
                && bluetoothGatt != null) {
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        btDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close(){
        if (bluetoothGatt == null){
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (SampleGattAttributes.SERVICE_UUID.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("0x2A03"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }
}
