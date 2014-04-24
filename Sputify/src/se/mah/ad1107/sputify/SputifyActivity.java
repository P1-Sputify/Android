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
 * En klass som anv�nds f�r att skicka str�ngar �ver bluetooth.
 * @author Patrik
 *
 */
public class SputifyActivity extends Activity {
	
	private static final String TAG = "SputifyActivity"; // Anv�nds f�r debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Anv�nds f�r att ha en instans av mobilens bluetooth enhet
	
	// Konstanter f�r olika requests
	public final int REQUEST_ENABLE_BT = 1; // Anv�nds f�r att kunna identifera r�tt resultat fr�n startBt activity
	public final int REQUEST_SELECT_BT = 2; // Anv�nds f�r att idenfierar resultat fr�n en intent.
	
	//Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	private Button mDisconnectButton;
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID f�r seriell kommunikation
	private static String mServerAdress; // Mac adressen till server ( i detta fall v�r bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice = null; // En pekarare till den valda enheten
	
	// Threads
	private ConnectThread mConnectThread;
	private manageConnectionThread mManagConnectionThread;
	
	
	
	// Konstanter f�r bluetoothStates
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
		mDeviceInfo.setText("Ingen bluetooth enhet �r vald");
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // H�mtar telefonens bluetooth radio
		checkBTState(); // Enable bluetooth om den �r inaktiverad
		
		mSelectDevicebutton = (Button)findViewById(R.id.button_SelectBluetoothDevice);
		mSelectDevicebutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectBTDevice();
			}
		});
		
		mEditMessage = (EditText)findViewById(R.id.editText_Message);
		
		mSendButton = (Button)findViewById(R.id.button_SendMessage);
		mSendButton.setEnabled(false); // Ska inte g� och skicka �n.
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String message = mEditMessage.getText().toString();
				sendData(message);
			}
		});
		
		mConnectbutton = (Button)findViewById(R.id.button_connectButton);
		mConnectbutton.setEnabled(false); // Knappen ska inte g� och klicka p� �n
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
				mSendButton.setEnabled(false); // Knappen ska inte g� att trycka p�
			}
		});
	}
		
	
	/**
	 * Kontrollerar om Mobilen har st�d f�r bluetooth, om den har det s� kontrollerar man om den �r p� annars s�
	 * fr�gar man anv�ndaren om att s�tta p� den. Om Mobilen inte har st�d s� st�ngs applikationen ner.
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
				// Fr�ga anv�ndaren f�r att sl� p� bluetooth
				Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}
	
	/**
	 * Metoden f�rs�ker skapa en anslutning mellan den valda bluetooth enheten.
	 */
	public void connect() {
		mConnectThread = new ConnectThread(mSelectedDevice);
		mConnectThread.run();
		mManagConnectionThread = new manageConnectionThread(mConnectThread.mmSocket);
	}
	
	/**
	 * Metoden anv�nds f�r att skicka data till bluetooth enheten
	 * B�r ocks� g�ras i en tr�d
	 * @param message
	 * 		En str�ng med det man vill skicka
	 */
	private void sendData(String message) {
		mManagConnectionThread.write(message);
	}
	
	/**
	 * Startart en ny activity f�r att v�lja bluetooth enhet
	 */
	private void selectBTDevice() {
		Intent intent = new Intent(this, SelectDeviceActivity.class);
		startActivityForResult(intent, REQUEST_SELECT_BT);
	}
	
	/**
	 * Anv�nds f�r att upptadera mDeviceInfo textfield.
	 */
	private void updateInfo() {
		switch(btState) {
		case BT_STATE_DISCONNECTED:
			mDeviceInfo.setText("Ingen Bluetooth enhet �r ansluten");
		case BT_STATE_CONNECTED:
			mDeviceInfo.setText("Du �r ansluten till" + mSelectedDevice.getName());
		case BT_STATE_CONNECTING:
			mDeviceInfo.setText("F�rs�ker ansluta till" + mSelectedDevice.getName());
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
						mConnectbutton.setEnabled(true); // Man ska kunna uppr�tta en anslutning
					}
				}
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
		}
	}
	/**
	 * En tr�d som anv�nds f�r att skapa en anslutning till en bluetooth enhet
	 * @author Patrik
	 *
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		
		/**
		 * Skapar en socket som �r kopplat till en Bluetooth enhet
		 * @param device
		 * 		Ett objekt av typen BluetoothDevice som man ska ansluta till
		 */
		public ConnectThread(BluetoothDevice device) {
			// Anv�nd ett tempor�rt objekt som sennare kommer att tilldelas mmSocket
			BluetoothSocket tmp = null;
			mmDevice = device;
			
			// F�rs�k att skapa en socket
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
				mmSocket.connect(); // F�rs�ker ansluta till den valda enheten. Detta anrop kommer att blockera tr�den tills att den lyckas eller misslyckas. 
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
		 * Anv�nds f�r att st�nga anslutningen
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
	 * Tr�den hanterar data�verf�rningen
	 * @author Patrik
	 *
	 */
	private class manageConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutputStream; // F�r denna sprinten beh�vs f�rmodligen inte denna
		
		/**
		 * En Konstruktor som skapar en ut och in str�m
		 * @param socket
		 * 		En Bluetoothsocket som �r ansluten till en bluetooth enhet
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
		 * Anv�nds f�r att l�sa av inkommande data
		 */
		public void run() {
			byte[] buffer = new byte [1024];
		}
		/**
		 * Skriver en str�ng till outPutStream.
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
		 * Anv�nds f�r att st�nga socketen.
		 */
		public void cancel() {
			try{
				mmOutputStream.close();
				mmSocket.close();
			} catch (IOException e) {};
		}
	}
}


	

