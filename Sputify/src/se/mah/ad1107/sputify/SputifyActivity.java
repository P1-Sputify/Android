package se.mah.ad1107.sputify;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class SputifyActivity extends ActionBarActivity {
	private BluetoothAdapter mBtAdapter;
	private static final int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sputify);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		mBtAdapter = BluetoothAdapter.getDefaultAdapter(); // Hämtar en referens till mobilens bluetooth enhet
		Toast.makeText(this, "Bluetooth avaiable: " + BluetoothAvaiable(), Toast.LENGTH_LONG).show();
		if (BluetoothAvaiable()) {
			EnableBluetooth();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sputify, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_sputify,
					container, false);
			return rootView;
		}
	}

	/*
	 * Metoden kontrollerar om mobilen har bluetooth eller inte
	 */
	private boolean BluetoothAvaiable() {
		if (mBtAdapter == null) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Sätt igång bluetooth på enheten
	 */
	private void EnableBluetooth() {
		if (BluetoothAvaiable() && !mBtAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE); // Skapar en intent
																// som ska fråga
																// användaren
																// efter input.
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
			Toast.makeText(this, "Bluetooth turned on!", Toast.LENGTH_LONG).show();
		}
	}
}
