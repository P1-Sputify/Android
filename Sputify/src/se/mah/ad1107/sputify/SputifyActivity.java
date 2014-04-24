package se.mah.ad1107.sputify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * En klass som används för att skicka strängar över bluetooth.
 * @author Patrik
 *
 */
public class SputifyActivity extends Activity {
	
	private static final String TAG = "SputifyActivity"; // Används för debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Används för att ha en instans av mobilens bluetooth enhet
	
	// Konstanter för olika requests
	public final int REQUEST_ENABLE_BT = 1; // Används för att kunna identifera rätt resultat från startBt activity
	public final int REQUEST_SELECT_BT = 2; // Används för att idenfierar resultat från en intent.
	
	//Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	private Button mDisconnectButton;
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID för seriell kommunikation
	private static String mServerAdress; // Mac adressen till server ( i detta fall vår bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice = null; // En pekarare till den valda enheten
	
	// Threads
	private ConnectThread mConnectThread;
	private manageConnectionThread mManagConnectionThread;
	
	
	
	// Konstanter för bluetoothStates
	private final int BT_STATE_DISCONNECTED = 0;
	private final int BT_STATE_CONNECTED = 1;
	private final int BT_STATE_CONNECTING = 2;
	private final int BT_STATE_SENDING = 3;
	
	private int btState = BT_STATE_DISCONNECTED;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On Create");
		setContentView(R.layout.activity_sputify);
		
		mDeviceInfo = (TextView)findViewById(R.id.text_BluetoothInfo);
		mDeviceInfo.setText("Ingen bluetooth enhet är vald");
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Hämtar telefonens bluetooth radio
		checkBTState(); // Enable bluetooth om den är inaktiverad
		
		mSelectDevicebutton = (Button)findViewById(R.id.button_SelectBluetoothDevice);
		mSelectDevicebutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectBTDevice();
			}
		});
		
		mEditMessage = (EditText)findViewById(R.id.editText_Message);
		
		mSendButton = (Button)findViewById(R.id.button_SendMessage);
		mSendButton.setEnabled(false); // Ska inte gå och skicka än.
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String message = mEditMessage.getText().toString();
				sendData(message);
			}
		});
		
		mConnectbutton = (Button)findViewById(R.id.button_connectButton);
		mConnectbutton.setEnabled(false); // Knappen ska inte gå och klicka på än
		mConnectbutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				connect();
				
			}
		});
		
		mDisconnectButton = (Button)findViewById(R.id.button_disconnect);
		mDisconnectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mManagConnectionThread = null;
				mConnectThread = null;
				mSendButton.setEnabled(false); // Knappen ska inte gå att trycka på
			}
		});
	}
		
	
	/**
	 * Kontrollerar om Mobilen har stöd för bluetooth, om den har det så kontrollerar man om den är på annars så
	 * frågar man användaren om att sätta på den. Om Mobilen inte har stöd så stängs applikationen ner.
	 */
	private void checkBTState() {
		if(mBluetoothAdapter == null) { 
			// Mobilen har inte bluetooth
			Log.d(TAG, "Device not supporting bluetooth");
			finish(); // Avslutar applikation.
		} else {
			if(mBluetoothAdapter.isEnabled()) {
				Log.d(TAG, " Bluetooth is Enabled ");
			} else {
				Log.d(TAG, "Prompting user for activiting bluetooth");
				// Fråga användaren för att slå på bluetooth
				Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}
	
	/**
	 * Metoden försöker skapa en anslutning mellan den valda bluetooth enheten.
	 */
	public void connect() {
		mConnectThread = new ConnectThread(mSelectedDevice);
		mConnectThread.run();
		mManagConnectionThread = new manageConnectionThread(mConnectThread.mmSocket);
	}
	
	/**
	 * Metoden används för att skicka data till bluetooth enheten
	 * Bör också göras i en tråd
	 * @param message
	 * 		En sträng med det man vill skicka
	 */
	private void sendData(String message) {
		mManagConnectionThread.write(message);
	}
	
	/**
	 * Startart en ny activity för att välja bluetooth enhet
	 */
	private void selectBTDevice() {
		Intent intent = new Intent(this, SelectDeviceActivity.class);
		startActivityForResult(intent, REQUEST_SELECT_BT);
	}
	
	/**
	 * Används för att upptadera mDeviceInfo textfield.
	 */
	private void updateInfo() {
		switch(btState) {
		case BT_STATE_DISCONNECTED:
			mDeviceInfo.setText("Ingen Bluetooth enhet är ansluten");
		case BT_STATE_CONNECTED:
			mDeviceInfo.setText("Du är ansluten till" + mSelectedDevice.getName());
		case BT_STATE_CONNECTING:
			mDeviceInfo.setText("Försöker ansluta till" + mSelectedDevice.getName());
		case BT_STATE_SENDING:
			mDeviceInfo.setText("Skickar till" + mSelectedDevice.getName());
		}
	}
	
	 
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "In OnActicityResult");
		if(requestCode == REQUEST_SELECT_BT && resultCode == RESULT_OK ){
			mSelectedDevice = mBluetoothAdapter.getRemoteDevice(data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
			Log.d(TAG, mSelectedDevice.getAddress());
			if(mSelectedDevice != null) {
				mDeviceInfo.setText(mSelectedDevice.toString());
			}
			
			switch (requestCode) {
			case REQUEST_SELECT_BT:
				if(resultCode == RESULT_OK) {
					Log.d(TAG, data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
					mSelectedDevice = mBluetoothAdapter.getRemoteDevice(data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
					Log.d(TAG, mSelectedDevice.getAddress());
					if(mSelectedDevice != null) {
						mDeviceInfo.setText(mSelectedDevice.toString());
						mConnectbutton.setEnabled(true); // Man ska kunna upprätta en anslutning
					}
				}
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
		}
	}
	/**
	 * En tråd som används för att skapa en anslutning till en bluetooth enhet
	 * @author Patrik
	 *
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		
		/**
		 * Skapar en socket som är kopplat till en Bluetooth enhet
		 * @param device
		 * 		Ett objekt av typen BluetoothDevice som man ska ansluta till
		 */
		public ConnectThread(BluetoothDevice device) {
			// Använd ett temporärt objekt som sennare kommer att tilldelas mmSocket
			BluetoothSocket tmp = null;
			mmDevice = device;
			
			// Försök att skapa en socket
			try {
				tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID); // Skapar en Bluetooth socket
				Log.d(TAG, "Socket Creation");
			} catch (IOException e) {
				Log.d(TAG, "Socket Creation Failed" + "\nMacAdress: " + mServerAdress + "\nUUID: " + MY_UUID +"\n" + e.getMessage());
				// Felmeddelande
			}
			mmSocket = tmp;
		}
		/**
		 * Skapar en anslutning
		 */
		@Override
		public void run() {
			try{
				mmSocket.connect(); // Försöker ansluta till den valda enheten. Detta anrop kommer att blockera tråden tills att den lyckas eller misslyckas. 
				Log.d(TAG, "Trying to connect to the bluetooth device");
				
			} catch(IOException e) {
				try {
					Log.d(TAG, "Closing Socket....");
					mmSocket.close();
				} catch (Exception e2) {
					Log.d(TAG, "Connection failed and could not close socket" + e2.getMessage());
					finish(); // Krash i programmet
				}
			}
		}
		/**
		 * Används för att stänga anslutningen
		 */
		public void cancel() {
			try{
				mmSocket.close();
			} catch(IOException e) {
				//Felmeddlenade
			}
		}
	}
	/**
	 * Tråden hanterar dataöverförningen
	 * @author Patrik
	 *
	 */
	private class manageConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutputStream; // För denna sprinten behövs förmodligen inte denna
		
		/**
		 * En Konstruktor som skapar en ut och in ström
		 * @param socket
		 * 		En Bluetoothsocket som är ansluten till en bluetooth enhet
		 */
		public manageConnectionThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpInStream = null;
			OutputStream tmpOutStream = null;
			try
			{
				tmpInStream = socket.getInputStream();
				tmpOutStream = socket.getOutputStream();
			} catch(IOException e) {
				Log.d(TAG, " Output or Input stream faield");
			}
			mmInStream = tmpInStream;
			mmOutputStream = tmpOutStream;
		}
		
		/**
		 * Används för att läsa av inkommande data
		 */
		public void run() {
			byte[] buffer = new byte [1024];
		}
		/**
		 * Skriver en sträng till outPutStream.
		 * @param message
		 * 		Det meddelande som man vill skicka
		 */
		public void write(String message) {
			byte[] buffer = message.getBytes();
			try{
				mmOutputStream.write(buffer);
			} catch(IOException e) {
				// Visa felmeddleande
			}
		}
		/**
		 * Används för att stänga socketen.
		 */
		public void cancel() {
			try{
				mmOutputStream.close();
				mmSocket.close();
			} catch (IOException e) {};
		}
	}
}


	

