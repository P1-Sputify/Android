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
 * En klass som anv�nds f�r att skicka str�ngar �ver bluetooth.
 * 
 * @author Patrik
 * 
 */
public class SputifyActivity extends Activity {

	private static final String TAG = "SputifyActivity"; // Anv�nds f�r debug
															// syfte

	private BluetoothAdapter mBluetoothAdapter; // Anv�nds f�r att ha en instans
												// av mobilens bluetooth enhet

	// Konstanter f�r olika requests
	public final int REQUEST_ENABLE_BT = 1; // Anv�nds f�r att kunna identifera
											// r�tt resultat fr�n startBt
											// activity

	public final int REQUEST_SELECT_BT = 2; // Anv�nds f�r att idenfierar
											// resultat fr�n en intent.

	// Gui
	private Button mSelectDevicebutton;
	private Button mSendButton;
	private Button mConnectbutton;
	private TextView mDeviceInfo;
	private EditText mEditMessage;
	private Button mDisconnectButton;

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard
																	// UUID f�r
																	// seriell
																	// kommunikation

	private static String mServerAdress; // Mac adressen till server ( i detta
											// fall v�r bluetooth modul.)

	private BluetoothDevice mSelectedDevice = null; // En pekarare till den
													// valda enheten

	private BluetoothService mBluetoothService;

	// // Konstanter f�r bluetoothStates
	// private final int BT_STATE_DISCONNECTED = 0;
	// private final int BT_STATE_CONNECTED = 1;
	// private final int BT_STATE_CONNECTING = 2;
	// private final int BT_STATE_SENDING = 3;
	// private int btState = BT_STATE_DISCONNECTED;

	// Konstanter f�r handler
	public static final int MESSAGE_STATE_CHANGED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On Create");
		setContentView(R.layout.activity_sputify);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState(); // Kontrollerar ifall Enheten har st�d f�r bluetooth och
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
//		mSendButton.setEnabled(false);	// Knappen ska inte g� att trycka p� om man inte har en ansluting
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
//		mConnectbutton.setEnabled(false);	// Knappen ska inte g� att trycka om man inte har en enhet man vill ansluta till
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
	 * Kontrollerar om Mobilen har st�d f�r bluetooth, om den har det s�
	 * kontrollerar man om den �r p� annars s� fr�gar man anv�ndaren om att
	 * s�tta p� den. Om Mobilen inte har st�d s� st�ngs applikationen ner.
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
				// Fr�ga anv�ndaren f�r att sl� p� bluetooth
				Intent enableBtIntent = new Intent(
						mBluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	/**
	 * Metoden f�rs�ker skapa en anslutning mellan den valda bluetooth enheten.
	 */
	public void connect() {
		mBluetoothService.connect(mSelectedDevice);		// Anropar connect Metoden i mBluetoothservice klassen
	}
	/**
	 * Metoden st�nger ner tr�darna som anv�nds i BluetoothService
	 */
	private void disconnect() {
		mBluetoothService.stop();
	}

	/**
	 * Metoden anv�nds f�r att skicka data till bluetooth enheten B�r ocks�
	 * g�ras i en tr�d
	 * 
	 * @param message
	 *            En str�ng med det man vill skicka
	 */
	private void sendData(byte[] out) {
//		mBluetoothService.write(out);
		
		mBluetoothService.getmManagConnectionThread().write(out);
	}

	/**
	 * Startart en ny activity f�r att v�lja bluetooth enhet
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
				switch (msg.arg1) { // Kan anv�nda arg 1 eftersom det bara �r
									// konstanter som skickas.
				case BluetoothService.STATE_CONNECTED:
					// Det som ska g�ras n�r man �r connected
					Log.i(TAG, "Connected to" + mSelectedDevice.getName());

				case BluetoothService.STATE_LISTEN:
					// Det som ska g�ra n�r man lyssnar

				case BluetoothService.STATE_CONNECTING:
					// Det som ska g�ras n�r man f�rs�ker skapa en ansluting
					Log.i(TAG, "Trying to connect" + mSelectedDevice.getName());

				case BluetoothService.STATE_NONE:
					// Det som ska g�ras n�r servicen inte g�r n�gotting.
					Log.i(TAG, "Ingenting g�rs");
				}

			}
		}
	};
	/**
	 * Retunerar handler objeket
	 * @return
	 * 		Ett handler objekt f�r att skicka meddelande.
	 */
	public Handler getmHandlerBT() {
		return mHandlerBT;
	}
}
