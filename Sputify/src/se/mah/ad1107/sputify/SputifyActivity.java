package se.mah.ad1107.sputify;

import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * En klass som används för att skicka strängar över bluetooth.
 * 
 * @author Patrik
 * 
 */
public class SputifyActivity extends Activity {

	private static final String TAG = "SputifyActivity"; // Används för debug
															// syfte

	private BluetoothAdapter mBluetoothAdapter; // Används för att ha en instans
												// av mobilens bluetooth enhet

	// Konstanter för olika requests
	public final int REQUEST_ENABLE_BT = 1; // Används för att kunna identifera
											// rätt resultat från startBt
											// activity

	public final int REQUEST_SELECT_BT = 2; // Används för att idenfierar
											// resultat från en intent.

	// Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	private Button mDisconnectButton;

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard
																	// UUID för
																	// seriell
																	// kommunikation

	private static String mServerAdress; // Mac adressen till server ( i detta
											// fall vår bluetooth modul.)

	private BluetoothDevice mSelectedDevice = null; // En pekarare till den
													// valda enheten

	private BluetoothService mBluetoothService;

	// // Konstanter för bluetoothStates
	// private final int BT_STATE_DISCONNECTED = 0;
	// private final int BT_STATE_CONNECTED = 1;
	// private final int BT_STATE_CONNECTING = 2;
	// private final int BT_STATE_SENDING = 3;
	// private int btState = BT_STATE_DISCONNECTED;

	// Konstanter för handler
	public static final int MESSAGE_STATE_CHANGED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On Create");
		setContentView(R.layout.activity_sputify);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState(); // Kontrollerar ifall Enheten har stöd för bluetooth och
						// aktiverar bluetooth.
		
		if (mBluetoothAdapter != null) {
			mBluetoothService = new BluetoothService(this, mHandlerBT);
		}
		
		// Device info
		mDeviceInfo = (TextView)findViewById(R.id.text_BluetoothInfo);
		mDeviceInfo.setText("No Device Selected");
		
		
		// Message Field
		mEditMessage = (EditText)findViewById(R.id.editText_Message);
		
		// Send button
		mSendButton = (Button)findViewById(R.id.button_SendMessage);
//		mSendButton.setEnabled(false);	// Knappen ska inte gå att trycka på om man inte har en ansluting
		mSendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i(TAG, "Button Send pressed");
				String message = mEditMessage.getText().toString();
				byte [] out = message.getBytes();
				sendData(out);
			}
			
		});
		
		// Select Device Button
		mSelectDevicebutton = (Button)findViewById(R.id.button_SelectBluetoothDevice);
		mSelectDevicebutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectBTDevice();
			}
		});
		
		// Connect Button
		mConnectbutton = (Button)findViewById(R.id.button_connectButton);
//		mConnectbutton.setEnabled(false);	// Knappen ska inte gå att trycka om man inte har en enhet man vill ansluta till
		mConnectbutton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				connect();
			}
		});
		
		// Disconnect Button
		mDisconnectButton = (Button)findViewById(R.id.button_disconnect);
		mDisconnectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				disconnect();
			}
		});

	}

	/**
	 * Kontrollerar om Mobilen har stöd för bluetooth, om den har det så
	 * kontrollerar man om den är på annars så frågar man användaren om att
	 * sätta på den. Om Mobilen inte har stöd så stängs applikationen ner.
	 */
	private void checkBTState() {
		if (mBluetoothAdapter == null) {
			// Mobilen har inte bluetooth
			Log.d(TAG, "Device not supporting bluetooth");
			finish(); // Avslutar applikation.
		} else {
			if (mBluetoothAdapter.isEnabled()) {
				Log.d(TAG, " Bluetooth is Enabled ");
			} else {
				Log.d(TAG, "Prompting user for activiting bluetooth");
				// Fråga användaren för att slå på bluetooth
				Intent enableBtIntent = new Intent(
						mBluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	/**
	 * Metoden försöker skapa en anslutning mellan den valda bluetooth enheten.
	 */
	public void connect() {
		mBluetoothService.connect(mSelectedDevice);		// Anropar connect Metoden i mBluetoothservice klassen
	}
	/**
	 * Metoden stänger ner trådarna som används i BluetoothService
	 */
	private void disconnect() {
		mBluetoothService.stop();
	}

	/**
	 * Metoden används för att skicka data till bluetooth enheten Bör också
	 * göras i en tråd
	 * 
	 * @param message
	 *            En sträng med det man vill skicka
	 */
	private void sendData(byte[] out) {
//		mBluetoothService.write(out);
		
		mBluetoothService.getmManagConnectionThread().write(out);
	}

	/**
	 * Startart en ny activity för att välja bluetooth enhet
	 */
	private void selectBTDevice() {
		Intent intent = new Intent(this, SelectDeviceActivity.class);
		startActivityForResult(intent, REQUEST_SELECT_BT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "In OnActicityResult");
		if (requestCode == REQUEST_SELECT_BT && resultCode == RESULT_OK) {
			mSelectedDevice = mBluetoothAdapter.getRemoteDevice(data
					.getExtras().getString(
							SelectDeviceActivity.EXTRA_DEVICE_ADRESS));
			Log.d(TAG, mSelectedDevice.getAddress());
			if (mSelectedDevice != null) {
				mDeviceInfo.setText(mSelectedDevice.toString());
			}

			switch (requestCode) {
			case REQUEST_SELECT_BT:
				if (resultCode == RESULT_OK) {
				}
			default:
				super.onActivityResult(requestCode, resultCode, data);
			}
		}
	}
	
	private final Handler mHandlerBT = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_STATE_CHANGED:
				switch (msg.arg1) { // Kan använda arg 1 eftersom det bara är
									// konstanter som skickas.
				case BluetoothService.STATE_CONNECTED:
					// Det som ska göras när man är connected
					Log.i(TAG, "Connected to" + mSelectedDevice.getName());

				case BluetoothService.STATE_LISTEN:
					// Det som ska göra när man lyssnar

				case BluetoothService.STATE_CONNECTING:
					// Det som ska göras när man försöker skapa en ansluting
					Log.i(TAG, "Trying to connect" + mSelectedDevice.getName());

				case BluetoothService.STATE_NONE:
					// Det som ska göras när servicen inte gör någotting.
					Log.i(TAG, "Ingenting görs");
				}

			}
		}
	};
	/**
	 * Retunerar handler objeket
	 * @return
	 * 		Ett handler objekt för att skicka meddelande.
	 */
	public Handler getmHandlerBT() {
		return mHandlerBT;
	}
}
