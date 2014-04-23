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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SputifyActivity extends Activity {
	private static final String TAG = "SputifyActivity"; // Används för debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Används för att ha en instans av mobilens bluetooth enhet
	private BluetoothSocket btSocket = null; // Socketen som kommer att användas för att skicka data
	private OutputStream outStream = null; // utströmmen
	
	private final int REQUEST_ENABLE_BT = 1; // Används för att kunna identifera rätt resultat från startBt activity
	private ListView mList; // Lista med bluetooth enheter som är paraade
	private ArrayAdapter<String> mArrayAdapter; // Hanterar list items
	private Set<BluetoothDevice> mPairedDevices; // Ett set med bluetoothenheter
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID för seriell kommunikation
	
	private static String mServerAdress; // Mac adressen till server ( i detta fall vår bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice; // En pekarare till den valda enheten
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On Create");
		setContentView(R.layout.activity_sputify);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Hämtar telefonens bluetooth radio
		checkBTState(); // Enable bluetooth om den är inaktiverad
		
		// Skapar listan med parade bluetooth enheter
		mList = (ListView)findViewById(R.id.list);
		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		getPairedDevices();
		mList.setAdapter(mArrayAdapter);
		// När man klickar på ett föremål i listan så ska den selectade enheten visas
		// TODO Ska man hård koda detta istället så att man bara kan ansluta till vår enheten?
		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String text = mArrayAdapter.getItem(position);
				String parts[] = text.split("\n");
				mServerAdress = parts[1];
				mSelectedDevice = mBluetoothAdapter.getRemoteDevice(mServerAdress);
				connect();
				sendData("f");
			}
		});
	}

//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		       if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
//		    	   Toast.makeText(this,"BT ENABLED", Toast.LENGTH_LONG).show();
//		       }
//	}
	
	
	/**
	 * Medoden lägger till redan parade enheter i en arrayAdapter som hanterar listan för parade bluetoothenheter
	 */
	public void getPairedDevices(){
		mPairedDevices = mBluetoothAdapter.getBondedDevices();
		if (mPairedDevices.size() > 0) {
		    // Gå igenom redan alla paraade enheter
		    for (BluetoothDevice device : mPairedDevices) {
		        // Lägg tilll namnet och adressen i adaptern för att visa i listan.
		        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		    }
		}
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
}
