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
	
	private static final String TAG = "SputifyActivity"; // Används för debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Används för att ha en instans av mobilens bluetooth enhet
	private BluetoothSocket btSocket = null; // Socketen som kommer att användas för att skicka data
	private OutputStream outStream = null; // utströmmen
	
	// Konstanter för olika requests
	public final int REQUEST_ENABLE_BT = 1; // Används för att kunna identifera rätt resultat från startBt activity
	public final int REQUEST_SELECT_BT = 2; // Används för att idenfierar resultat från en intent.
	
	//Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID för seriell kommunikation
	private static String mServerAdress; // Mac adressen till server ( i detta fall vår bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice = null; // En pekarare till den valda enheten
	
	
	
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
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String message = mEditMessage.getText().toString();
				sendData(message);
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
	 * Detta bör göras i en tråd
	 */
	public void connect() {
		try {
			btSocket = mSelectedDevice.createRfcommSocketToServiceRecord(MY_UUID); // Skapar en Bluetooth socket
			Log.d(TAG, "Socket Creation");
		} catch (IOException e) {
			Log.d(TAG, "Socket Creation Failed" + "\nMacAdress: " + mServerAdress + "\nUUID: " + MY_UUID +"\n" + e.getMessage());
			// Felmeddelande
		}
		
		mBluetoothAdapter.cancelDiscovery(); // Discovry är krävande så den ska vara avstängd innan man försöker ansluta
		
		try{
			btSocket.connect(); // Försöker ansluta till den valda enheten. Detta anrop kommer att blockera appaen tills att den lyckas eller misslyckas. Lägg i thread när vi får det att fungera
			Log.d(TAG, "Trying to connect to the bluetooth device"); // Kommer att blockerar programet tills den connectar. Därför bör detta läggas i en thread.
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
			outStream = btSocket.getOutputStream(); // Försöker att skapa strömmen som ska skicka data imellan.
		} catch(IOException e) {
			Log.d(TAG, "outputStream creation failed");
			finish();
		}
	}
	
	/**
	 * Metoden används för att skicka data till bluetooth enheten
	 * Bör också göras i en tråd
	 * @param message
	 * 		En sträng med det man vill skicka
	 */
	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes(); // Skapar en buffer för att kunna skicka meddelandet
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


	

