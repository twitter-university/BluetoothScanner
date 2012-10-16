package com.marakana.android.bluetoothscanner;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int REQUEST_ENABLE_BT = 47;

	private ListView mList;
	private BluetoothAdapter mBluetoothAdapter;
	private ArrayAdapter<String> mArrayAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1);

		setContentView(R.layout.activity_main);
		mList = (ListView) findViewById(R.id.list);
		mList.setAdapter(mArrayAdapter);

		// Check if Bluetooth is supported
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Bluetooth is not supported on the device
			Toast.makeText(this, "Bluetooth is not supported on this device",
					Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}

		// Enable Bluetooth if it's disabled
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);

		queryPairedDevices();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check if this is result from our intent to enable Bluetooth
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode != Activity.RESULT_OK) {
				// Failed to enable bluetooth
			}
		}
	}

	/** Query for already paired devices. */
	private void queryPairedDevices() {
		this.setTitle("Paired Devices");
		mArrayAdapter.clear();

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to the list
				mArrayAdapter
						.add(device.getName() + "\n" + device.getAddress());
			}
		}
	}

	/** Initiates the scanning for Bluetooth devices near by. */
	private void scanForDevices() {
		this.setTitle("Scanning for devices...");
		mArrayAdapter.clear();
		mBluetoothAdapter.startDiscovery();
	}

	/** A BroadcastReceiver for ACTION_FOUND. */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				setTitle("Discovered Devices");
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to the list
				mArrayAdapter
						.add(device.getName() + "\n" + device.getAddress());
			}
		}
	};

	/** Makes this device discoverable via system settings. */
	private void makeDiscoverable() {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(discoverableIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_paired_devices:
			queryPairedDevices();
			return true;
		case R.id.item_scan:
			scanForDevices();
			return true;
		case R.id.item_discoverable:
			makeDiscoverable();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
