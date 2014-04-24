package se.mah.ad1107.sputify;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SputifyActivity extends Activity {
	
	private static final String TAG = "SputifyActivity"; // Anv�nds f�r debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Anv�nds f�r att ha en instans av mobilens bluetooth enhet
	private BluetoothSocket btSocket = null; // Socketen som kommer att anv�ndas f�r att skicka data
	private OutputStream outStream = null; // utstr�mmen
	
	// Konstanter f�r olika requests
	public final int REQUEST_ENABLE_BT = 1; // Anv�nds f�r att kunna identifera r�tt resultat fr�n startBt activity
	public final int REQUEST_SELECT_BT = 2; // Anv�nds f�r att idenfierar resultat fr�n en intent.
	
	//Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID f�r seriell kommunikation
	private static String mServerAdress; // Mac adressen till server ( i detta fall v�r bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice = null; // En pekarare till den valda enheten
	
	
	
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
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String message = mEditMessage.getText().toString();
				sendData(message);
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
	 * Detta b�r g�ras i en tr�d
	 */
	public void connect() {
		try {
			btSocket = mSelectedDevice.createRfcommSocketToServiceRecord(MY_UUID); // Skapar en Bluetooth socket
			Log.d(TAG, "Socket Creation");
		} catch (IOException e) {
			Log.d(TAG, "Socket Creation Failed" + "\nMacAdress: " + mServerAdress + "\nUUID: " + MY_UUID +"\n" + e.getMessage());
			// Felmeddelande
		}
		
		mBluetoothAdapter.cancelDiscovery(); // Discovry �r kr�vande s� den ska vara avst�ngd innan man f�rs�ker ansluta
		
		try{
			btSocket.connect(); // F�rs�ker ansluta till den valda enheten. Detta anrop kommer att blockera appaen tills att den lyckas eller misslyckas. L�gg i thread n�r vi f�r det att fungera
			Log.d(TAG, "Trying to connect to the bluetooth device"); // Kommer att blockerar programet tills den connectar. D�rf�r b�r detta l�ggas i en thread.
		} catch(IOException e) {
			try {
				Log.d(TAG, "Closing Socket....");
				btSocket.close();
			} catch (Exception e2) {
				Log.d(TAG, "Connection failed and could not close socket" + e2.getMessage());
				finish(); // Krash i programmet
			}
			
		}
		
		Log.d(TAG, "...Creating out Stream....");
		try {
			outStream = btSocket.getOutputStream(); // F�rs�ker att skapa str�mmen som ska skicka data imellan.
		} catch(IOException e) {
			Log.d(TAG, "outputStream creation failed");
			finish();
		}
	}
	
	/**
	 * Metoden anv�nds f�r att skicka data till bluetooth enheten
	 * B�r ocks� g�ras i en tr�d
	 * @param message
	 * 		En str�ng med det man vill skicka
	 */
	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes(); // Skapar en buffer f�r att kunna skicka meddelandet
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Log.d(TAG, "Exception thron while writing data" + e.getMessage());
		}
	}
	
	private void selectBTDevice() {
		Intent intent = new Intent(this, SelectDeviceActivity.class);
		startActivityForResult(intent, REQUEST_SELECT_BT);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "In OnActicityResult");
		if(requestCode == REQUEST_SELECT_BT && resultCode == RESULT_OK ){
			mSelectedDevice = mBluetoothAdapter.getRemoteDevice(data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
			Log.d(TAG, mSelectedDevice.getAddress());
			if(mSelectedDevice != null) {
				mDeviceInfo.setText(mSelectedDevice.toString());
				connect();
			}
			
			switch (requestCode) {
			case REQUEST_SELECT_BT:
				if(resultCode == RESULT_OK) {
					Log.d(TAG, data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
					mSelectedDevice = mBluetoothAdapter.getRemoteDevice(data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
					Log.d(TAG, mSelectedDevice.getAddress());
					if(mSelectedDevice != null) {
						mDeviceInfo.setText(mSelectedDevice.toString());
					}
				}
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
		}
	}
	
}


	

