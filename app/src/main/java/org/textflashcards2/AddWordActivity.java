package org.textflashcards2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class AddWordActivity extends Activity implements View.OnClickListener {
	EditText editTextWord;
	EditText editTextTranslation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_add_word);

		editTextWord = (EditText) findViewById(R.id.editTextWord);
		editTextTranslation = (EditText) findViewById(R.id.editTextTranslation);

		Button b = (Button) findViewById(R.id.buttonAdd);
		b.setOnClickListener(this);
	}

	public void onClick(View view) {
		Intent i = new Intent();
		i.putExtra("word", editTextWord.getText().toString());
		i.putExtra("translation", editTextTranslation.getText().toString());
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_add_word, menu);
		return true;
	}

}
