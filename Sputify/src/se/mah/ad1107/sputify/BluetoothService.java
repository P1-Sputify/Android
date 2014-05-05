package se.mah.ad1107.sputify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class will manage connection to other bluetooth devices and incomming
 * and outgoing messages.
 * 
 * @author Patrik
 * 
 */
public class BluetoothService {
	// Debugging
	private static final String TAG = "BluetoothService";

	// Konstaner
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String mServerAdress; // Mac adressen till server ( i detta
											// fall vår bluetooth modul.)

	private final BluetoothAdapter mBTAdapter = BluetoothAdapter
			.getDefaultAdapter(); // Hämtar en referens till mobilens Bluetooth
									// telefon, om denna är null stöder mobilen
									// ej bluetooth

	// Konstaner som indikerar bluetooth State
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// devices

	// Klassvariabler
	private ConnectThread mConnectThread;
	private manageConnectionThread mManagConnectionThread;
	private int mState; // Används för att visa vilken state bluetooth befinner
						// sig i.
	private Handler mHandler; // Handler för att skicka meddeleande tillbaka
								// till Activityn som har gui
	private Context mContext; // Contexten som man vill att servicen ska vara
								// connectad till.

	/**
	 * A Constructor that prepares a new Session.
	 * 
	 * @param applicationContext
	 *            The context that hosts the gui.
	 * @param handler
	 *            A Handler that sends messages back to the activity
	 */
	public BluetoothService(Context applicationContext, Handler handler) {
		mState = STATE_NONE;
		mHandler = handler;
		mContext = applicationContext;
	}

	/**
	 * The method changes the bluetooth state
	 * 
	 * @param state
	 *            One of the constants STATE_NONE, STATE_LISTEN,
	 *            STATE_CONNECTING, STATE_CONNECTED
	 */
	public synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + "--> " + state);
		mState = state;

		mHandler.obtainMessage(SputifyActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();

	}

	/**
	 * Returns the current state
	 * 
	 * @return 
	 * 		An integer represting the state
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * The method starts a new Session
	 * 
	 * @return
	 */
	public synchronized void start() {
		Log.d(TAG, "start() ");

		// Avbryt tråd om någon försöker göra en connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt mManageConnectionThread om den är igång
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		setState(STATE_NONE); // Ingeting görs just nu.
	}

	/**
	 * Initiating a connection to a bluetooth device
	 * 
	 * @param device
	 *            The device to make a connection with.
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "Trying to iniitiate connection to: " + device);

		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}

		}

		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		mConnectThread = new ConnectThread(device);
		mConnectThread.start(); // Startar en connection
		setState(STATE_CONNECTING); // Enheterna försöker skapa en connection
	}
	/**
	 * Skapar en ny tråd som lyssnar för inkommande data
	 * @param socket
	 * 		Den socket som man vill använda för att skicka
	 * @param device
	 * 		Den bluetooth enhet man vill skicka till
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		// Avbryt tråden som har gjort en ansluting
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt tråd som har ansluting om sådan finns
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		// Starta en ny manageConnectThread
		mManagConnectionThread = new manageConnectionThread(socket);
		mManagConnectionThread.start();

		// Skicka tillbaka meddlenade till UI
		mHandler.obtainMessage(SputifyActivity.MESSAGE_STATE_CHANGED, STATE_CONNECTED, -1);
	}
	
	/**
	 * Retunerar en referens till manageConnection tråden
	 * @return
	 * 		En referens till manage connection thread
	 */
	public manageConnectionThread getmManagConnectionThread() {
		return mManagConnectionThread;
	}

	/**
	 * The method is used to stop all threads
	 */
	public synchronized void stop() {
		Log.d(TAG, "stop");

		// Avbryt ConnectThread
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt manageConnected thread
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		setState(STATE_NONE);
	}
	/**
	 * Anropa denna i den aktiviten man vill skicka ifrån
	 * @param out
	 * 		En byte array med det man vill skicka
	 */
	public void write(byte[] out) {
		// Create temporary object
		manageConnectionThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mManagConnectionThread;
		}
		// Perform the write unsynchronized
		r.write(out);	
		
	}

	/**
	 * Sends a message back to the ui that the connection has been lost
	 */
	private void connectionLost() {
		setState(STATE_NONE);

		// TODO Sänd tillbaka till ui
	}

	/**
	 * Sends a message back to the ui that the connection attempt failed
	 */
	private void connectionFailed() {
		setState(STATE_NONE);

		// TODO Sänd tillbaka till UI
	}

	/**
	 * This Thread is used to attempt to make a connection with a remote
	 * bluetooth device
	 * 
	 * @author Patrik
	 * 
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null; // Använder en temporär socket så för
										// att inte tilldela den
										// "riktiga variablen för en socket null"
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				Log.i(TAG, "Socket Creation Sucessfull");
			} catch (IOException e) {
				Log.e(TAG, "Socket Creation Failed");
			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			Log.i(TAG, "mConnectThread started");
			setName("ConnectThread");

			try {
				// Ett blockerand anrop. Kommer bara att återvända ifall ett
				// execption kastas eller lyckad ansluting
				mmSocket.connect();
			} catch (IOException e) {
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"Unable to close socket, during connection failure",
							e2);
				}

			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}

			// Starta manageConnection thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket failed to close during canel", e);
			}

		}
	}

	/**
	 * This thread is used to listen for incomming data and send data.
	 * 
	 * @author Patrik
	 * 
	 */
	public class manageConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public manageConnectionThread(BluetoothSocket socket) {
			Log.d(TAG, "create manageConnectionThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Hämta utström och inström från bluetoohsocket
			try {
				tmpIn = mmSocket.getInputStream();
				tmpOut = mmSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp streams not created", e);
			}

			mmOutStream = tmpOut;
			mmInStream = tmpIn;
		}

		@Override
		public void run() {
			Log.i(TAG, "Run manageConnectionThread");
			setName("ManageConnectedThread");
			byte[] buffer = new byte[1024]; // En buffert för att lagra den data
											// som ska skickas. Storleken
											// behöver nog ändras.

			int bytes; // En räknare för att kunna ha längden av meddelandet man
						// skickar

			// Så länge som tråden är aktiv ska den lyssna efter inkommande
			// meddelande
			while (true) {
				try {
					bytes = mmInStream.read();
				} catch (IOException e) {
					Log.e(TAG, "Connection Lost", e);
					connectionLost();
					break; // Man kan inte läsa från inströmmen om det inte
							// finns någon ansluting.
				}
				// TODO Skicka meddelande till gui.
			}
		}

	
		/**
		 * Skriver till ut ström som sedan skicka vidare
		 * @param out
		 * 		Det man vill skriva ut
		 */
	    public void write(byte[] out) {
	    	try {
				mmOutStream.write(out);
			} catch (IOException e) {
				Log.e(TAG, "Failed to send message");
				e.printStackTrace();
			}
	    }

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
