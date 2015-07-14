package org.textflashcards2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements android.view.View.OnClickListener {
    private static final int ACTIVITY_IMPORT_WORDS = 1;
    private static final int ACTIVITY_ADD_WORD = 2;
    private static final int ACTIVITY_SETTINGS = 3;
    private static final int ACTIVITY_HELP = 4;
    TrainerModel m;
    TextView qView;
    TextView aView;
    TextView viewStats;
    Button buttonPassed;
    Button buttonFailed;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        Conf.init(getResources().getString(R.string.app_name));
        if (Conf.DEBUG) Log.v(Conf.TAG, "onCreate");

        setContentView(R.layout.activity_main);

        qView = (TextView) findViewById(R.id.qView);
        aView = (TextView) findViewById(R.id.aView);
        viewStats = (TextView) findViewById(R.id.textViewStats);
        buttonPassed = (Button) findViewById(R.id.buttonPassed);
        buttonFailed = (Button) findViewById(R.id.buttonFailed);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        buttonPassed.setVisibility(View.VISIBLE);
        buttonFailed.setVisibility(View.INVISIBLE);

        buttonPassed.setOnClickListener(this);
        buttonFailed.setOnClickListener(this);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        if (prefs.getInt("notFirstRun", 0) == 0) {
            Intent i = new Intent(this, HelpActivity.class);
            startActivityForResult(i, ACTIVITY_HELP);
        }

        int currentId = prefs.getInt("currentId", -1);
        int maxPasses = prefs.getInt("maxPasses", 3);
        int passes = prefs.getInt("passes", 0);
        int fails = prefs.getInt("fails", 0);
        boolean isBackward = prefs.getBoolean("isBackward", false);

        try {
            m = new TrainerModel(this, maxPasses, isBackward, passes, fails);
        } catch (SQLException e) {
            Toast t = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            t.show();
            return;
        }

        if (m.nWords == 0) {
            buttonPassed.setVisibility(View.INVISIBLE);
            showImportDialog("There are no words, please, import some.");
        } else {
            showQuestion(currentId);
        }
    }



    void showQuestion() {
        showQuestion(-1);
    }

    void showQuestion(int currentId) {
        buttonFailed.setVisibility(View.INVISIBLE);
        buttonPassed.setVisibility(View.VISIBLE);
        buttonPassed.setText("Show answer");

        aView.setText("");

        if (m.selectQuestion(currentId)) {
            qView.setText(m.getQuestion());
        } else {
            m.moveWordsToArchive();
            int passes = m.passes;
            int fails = m.fails;
            m.resetShown();

            showExitDialog("You have looked through all cards, so finish now and repeat after some time.\n" +
                    String.format("You've answered\nright: %d\nwrong: %d", passes, fails));
        }
        progressBar.setMax(m.nWords);
        progressBar.setProgress(m.nWords - m.nWordsLeft);
        viewStats.setText(String.format("%d/%d", m.nWords - m.nWordsLeft, m.nWords));
    }

    void showImportDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent i = new Intent(MainActivity.this, ImportWordsDialog.class);
                        startActivityForResult(i, ACTIVITY_IMPORT_WORDS);
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    void showExitDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onClick(View v) {
        if(v == buttonPassed && buttonFailed.isShown())
            onPassedClick();
        else if(v == buttonFailed) {
            onFailedClick();
        } else if (v == buttonPassed && !buttonFailed.isShown()) {
            onShowTranslationClick();
        }
    }

    void onPassedClick() {
        m.setRight();
        showQuestion();
    }

    void onFailedClick() {
        m.setWrong();
        showQuestion();
    }

    void onShowTranslationClick() {
        aView.setText(m.getAnswer());
        buttonFailed.setVisibility(View.VISIBLE);
        buttonPassed.setText("Right");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (Conf.DEBUG)
            Log.v(Conf.TAG, String.format("onActivityResult: %d, %d", requestCode, resultCode));

        if (resultCode != RESULT_OK)
            return;

        m.openDb();
        switch (requestCode) {
            case ACTIVITY_IMPORT_WORDS:
                try {
                    m.importWords(intent.getStringExtra("path"));
                    showQuestion();
                } catch (ModelParseError e) {
                    Toast t = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
                    t.show();
                }
                break;
            case ACTIVITY_ADD_WORD:
                Word w = new Word(0, intent.getStringExtra("word"),
                        intent.getStringExtra("translation"), 0, 0);
                m.addWord(w);
                m.calcStats();
                if (m.nWords == 1)
                    showQuestion();
                break;
            case ACTIVITY_HELP:
                SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
                editor.putInt("notFirstRun", 1);
                editor.apply();
            default:
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (Conf.DEBUG) Log.v(Conf.TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
        if (Conf.DEBUG) Log.v(Conf.TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
        super.onPause();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onPause");

        if (m == null)
            return;

        if (m.current != null)
            editor.putInt("currentId", m.current.getId());
        editor.putInt("passes", m.passes);
        editor.putInt("fails", m.fails);

        editor.apply();
        m.closeDb();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onResume");

        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int maxPasses = prefs.getInt("maxPasses", 3);
        boolean isBackward = prefs.getBoolean("isBackward", false);
        m.setMaxPasses(maxPasses);
        m.setBackward(isBackward);
        m.openDb();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onStart");

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Conf.DEBUG) Log.v(Conf.TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
            case R.id.menu_settings:
                i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, ACTIVITY_SETTINGS);
                break;
            case R.id.menu_import_words:
                i = new Intent(this, ImportWordsDialog.class);
                startActivityForResult(i, ACTIVITY_IMPORT_WORDS);
                break;
            case R.id.menu_export_words:
                m.exportWords();
                break;
            case R.id.menu_reset:
                m.reset();
                showQuestion();
                break;
            case R.id.menu_delete_all:
                m.deleteAll();
                showImportDialog("You've deleted all words, so now you need to import some.");
                break;
            case R.id.menu_exit:
                finish();
                break;
            case R.id.menu_stats:
                i = new Intent(this, StatsView.class);
                startActivityForResult(i, 0);
                break;
            case R.id.menu_add_word:
                i = new Intent(this, AddWordActivity.class);
                startActivityForResult(i, ACTIVITY_ADD_WORD);
                break;
            case R.id.menu_help:
                i = new Intent(this, HelpActivity.class);
                startActivityForResult(i, ACTIVITY_HELP);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
