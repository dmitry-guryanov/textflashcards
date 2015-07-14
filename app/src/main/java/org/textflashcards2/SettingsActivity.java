package org.textflashcards2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsActivity extends Activity {
	private EditText editText1;
	private CheckBox checkBoxIsBackward;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		int maxPasses = prefs.getInt("maxPasses", 3);
		boolean isBackward = prefs.getBoolean("isBackward", false);
	
		editText1 = (EditText) findViewById(R.id.editText1);
		editText1.setText(Integer.toString(maxPasses));
		
		checkBoxIsBackward = (CheckBox) findViewById(R.id.checkBoxIsBackward);
		checkBoxIsBackward.setChecked(isBackward);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
		editor.putInt("maxPasses", Integer.parseInt(editText1.getText().toString()));
		editor.putBoolean("isBackward", checkBoxIsBackward.isChecked());
		editor.apply();
	}
}
