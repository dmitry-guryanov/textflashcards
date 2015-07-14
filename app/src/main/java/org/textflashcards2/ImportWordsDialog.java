package org.textflashcards2;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

class ImportListItem {
	File file;
	
	ImportListItem(File file) {
		this.file = file;
	}
	
	public String toString() {
        String name = this.file.getName();
        if (this.file.isDirectory())
                name = name + "/";
		return name;
	}
}

public class ImportWordsDialog extends ListActivity {
	Button buttonSelect;
	ArrayAdapter<ImportListItem> adapter;
    File pwd;
    File top;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        String state = Environment.getExternalStorageState();

        if (!(Environment.MEDIA_MOUNTED.equals(state) &&
                !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please, make sure that SD Card is available !");
            builder.setCancelable(false);
            builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ImportWordsDialog.this.finish();
                        }});

            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        top = Environment.getExternalStorageDirectory();
        pwd = top;

        adapter = new ArrayAdapter<ImportListItem>(this, R.layout.file_list_item);
        setListAdapter(adapter);

        showFileList();
	}
	
	void showFileList() {

		File files[] = pwd.listFiles();

		adapter.clear();

        if (!pwd.equals(top))
            adapter.add(new ImportListItem(new File("..")));

        for (int i = 0; i < files.length; i++)
    		adapter.add(new ImportListItem(files[i]));

	}

	public void onListItemClick(ListView l, View v, int position, long id) {
        File file = adapter.getItem(position).file;

        if (file.isDirectory()) {
            pwd = new File(pwd, file.getName());
            showFileList();
        } else {
            Intent i = new Intent();
            i.putExtra("path", adapter.getItem(position).file.getAbsolutePath());

            setResult(RESULT_OK, i);
            finish();
        }
	}

}
