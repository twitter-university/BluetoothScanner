package com.marakana.android.bluetoothscanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Chat extends Activity {
	private static final String TAG = "BluetoothScanner";
	private static final String UUID_STRING = "665bbe80-174f-11e2-892e-0800200c9a66";
	private static final int MESSAGE_READ = 47; // Used by handler

	private EditText input;
	private TextView output;

	private AcceptThread acceptThread;
	private ConnectedThread connectedThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		input = (EditText) findViewById(R.id.input);
		output = (TextView) findViewById(R.id.output);

		acceptThread = new AcceptThread();
		acceptThread.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		acceptThread.cancel();
	}

	/** Takes the input and sends it to the server. */
	public void onClickGo(View v) {
		String inputString = input.getText().toString();
		if (connectedThread != null)
			connectedThread.write(inputString.getBytes());
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				tmp = BluetoothAdapter.getDefaultAdapter()
						.listenUsingRfcommWithServiceRecord(TAG,
								UUID.fromString(UUID_STRING));
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmServerSocket = tmp;

			Log.d(TAG, "AcceptThread created");
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					Log.d(TAG, "AcceptThread run() connected");
					try {
						manageConnectedSocket(socket);
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** Process the data coming via the Bluetooth connection. */
	private void manageConnectedSocket(BluetoothSocket socket)
			throws IOException {
		connectedThread = new ConnectedThread(socket);
		connectedThread.run();
		Log.d(TAG, "manageConnectedSocket");
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// Send the obtained bytes to the UI activity
					Message msg = mHandler.obtainMessage(MESSAGE_READ, bytes,
							-1, buffer);
					mHandler.sendMessage(msg);
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	/** Handle messages from the ConnectedThread. */
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what != MESSAGE_READ)
				return;

			output.append("\n" + msg.obj);
		}

	};
}
