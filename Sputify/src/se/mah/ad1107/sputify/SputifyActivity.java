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
	private static final String TAG = "SputifyActivity"; // Anv�nds f�r debug syfte
	
	private BluetoothAdapter mBluetoothAdapter; // Anv�nds f�r att ha en instans av mobilens bluetooth enhet
	private BluetoothSocket btSocket = null; // Socketen som kommer att anv�ndas f�r att skicka data
	private OutputStream outStream = null; // utstr�mmen
	
	private final int REQUEST_ENABLE_BT = 1; // Anv�nds f�r att kunna identifera r�tt resultat fr�n startBt activity
	private ListView mList; // Lista med bluetooth enheter som �r paraade
	private ArrayAdapter<String> mArrayAdapter; // Hanterar list items
	private Set<BluetoothDevice> mPairedDevices; // Ett set med bluetoothenheter
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID f�r seriell kommunikation
	
	private static String mServerAdress; // Mac adressen till server ( i detta fall v�r bluetooth modul.)
	
	
	private BluetoothDevice mSelectedDevice; // En pekarare till den valda enheten
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On Create");
		setContentView(R.layout.activity_sputify);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // H�mtar telefonens bluetooth radio
		checkBTState(); // Enable bluetooth om den �r inaktiverad
		
		// Skapar listan med parade bluetooth enheter
		mList = (ListView)findViewById(R.id.list);
		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		getPairedDevices();
		mList.setAdapter(mArrayAdapter);
		// N�r man klickar p� ett f�rem�l i listan s� ska den selectade enheten visas
		// TODO Ska man h�rd koda detta ist�llet s� att man bara kan ansluta till v�r enheten?
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
	 * Medoden l�gger till redan parade enheter i en arrayAdapter som hanterar listan f�r parade bluetoothenheter
	 */
	public void getPairedDevices(){
		mPairedDevices = mBluetoothAdapter.getBondedDevices();
		if (mPairedDevices.size() > 0) {
		    // G� igenom redan alla paraade enheter
		    for (BluetoothDevice device : mPairedDevices) {
		        // L�gg tilll namnet och adressen i adaptern f�r att visa i listan.
		        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		    }
		}
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
}
