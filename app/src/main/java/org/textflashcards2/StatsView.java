package org.textflashcards2;

import android.app.Activity;
import android.database.SQLException;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class StatsView extends Activity {
	TableLayout layout;
	TextView t;
	TrainerModel m;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			m = new TrainerModel(this, 0, false, 0, 0);
		} catch (SQLException e) {
			Toast t = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
			t.show();
			return;
		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats_view);
		
		layout = (TableLayout) findViewById(R.id.tableLayout1);
		layout.setStretchAllColumns(true);

		m.fillStats(layout);
	}
}
