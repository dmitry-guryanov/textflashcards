package org.textflashcards2;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

enum TrainerState {
	Q_PENDING, WAIT_ANSWER, A_PENDING, WAIT_ANSWER2, WAIT_NEXT_CONFIRM,
}

class ModelParseError extends Exception {
	public String msg;
	ModelParseError(String msg) {
		this.msg = msg;
	}
};

public class TrainerModel {
	public static final String KEY_WORD = "word";
    public static final String KEY_TRANSLATION = "translation";
    public static final String KEY_PASSES_NUMBER = "passes_number";
    public static final String KEY_FAILS_NUMBER = "fails_number";
    public static final String KEY_IS_SHOWN = "is_shown";
    public static final String KEY_IN_ARCHIVE = "in_archive";
    public static final String KEY_ID = "_id";

    public static final int Q_COUNT = 3;
    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	
    private static final String DATABASE_CREATE =
    		"CREATE TABLE words (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
    				"word VARCHAR, translation VARCHAR, " +
    				"passes_number INTEGER, fails_number INTEGER, " +
    				"is_shown boolean, in_archive BOOLEAN, UNIQUE (word) ON CONFLICT IGNORE);";

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE = "words";
	private static final int DATABASE_VERSION = 2;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
//			onCreate(db);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS words;");
			db.execSQL(DATABASE_CREATE);
/*			db.execSQL("INSERT INTO words (word, translation, passes_number, fails_number, is_shown, in_archive) values ('cat', 'котэ', 0, 0, 0, 0);");
			db.execSQL("INSERT INTO words (word, translation, passes_number, fails_number, is_shown, in_archive) values ('dog', 'псэ', 0, 0, 0, 0);");
			db.execSQL("INSERT INTO words (word, translation, passes_number, fails_number, is_shown, in_archive) values ('sheep', 'овэц', 0, 0, 0, 0);");
			db.execSQL("INSERT INTO words (word, translation, passes_number, fails_number, is_shown, in_archive) values ('mouse', 'мышш', 0, 0, 0, 0);");
*/
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(Conf.TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS words");
			onCreate(db);
		}
	}
	
	Word current;
	int nWordsInArchive;
	int nWords;
	int nWordsLeft;
	int maxPasses;
	boolean isBackward;
	int passes;
	int fails;
	
	Context ctx;
	
	TrainerModel(Context ctx, int maxPasses, boolean isBackward, int passes, int fails) throws SQLException {
		this.ctx = ctx;
		this.maxPasses = maxPasses;
		this.isBackward = isBackward;
		this.passes = passes;
		this.fails = fails;
		
		openDb();
        calcStats();
	}

	void openDb() {
		if (mDb != null)
			return;
		mDbHelper = new DatabaseHelper(ctx);
		mDb = mDbHelper.getWritableDatabase();
	}
	void closeDb() {
		mDb.close();
		mDb = null;
	}
	
	void calcStats() {
		Cursor c;
		
		c = mDb.rawQuery("SELECT COUNT(*) AS archive_words from " + DATABASE_TABLE + " WHERE " + KEY_IN_ARCHIVE + ";", null);
		c.moveToFirst();
		nWordsInArchive = c.getInt(c.getColumnIndexOrThrow("archive_words"));
		c.close();
		
		c = mDb.rawQuery("SELECT COUNT(*) AS words from " + DATABASE_TABLE + " WHERE NOT " + KEY_IN_ARCHIVE + ";", null);
		c.moveToFirst();
		nWords = c.getInt(c.getColumnIndexOrThrow("words"));
		c.close();
		
		c = mDb.rawQuery("SELECT COUNT(*) AS words_left from " + DATABASE_TABLE + " WHERE NOT " + KEY_IN_ARCHIVE + " AND NOT " + KEY_IS_SHOWN + ";", null);
		c.moveToFirst();
		nWordsLeft = c.getInt(c.getColumnIndexOrThrow("words_left"));
		c.close();
		
		if (Conf.DEBUG)
			Log.v(Conf.TAG, String.format("archive - %d, all - %d, left - %d",
					nWordsInArchive, nWords, nWordsLeft));
	}
	
	boolean selectQuestion() {
		return selectQuestion(-1);
	}
	
	boolean selectQuestion(int currentId) {
		Cursor c = null;
		if (currentId >= 0)
			c = mDb.query(DATABASE_TABLE, null, String.format("%s=%d AND ", KEY_ID, currentId) + "NOT " + KEY_IN_ARCHIVE + " AND NOT " + KEY_IS_SHOWN, null, null, null, null, null);

		if (currentId < 0 || (c != null && c.getCount() == 0))
			c = mDb.query(DATABASE_TABLE, null, "NOT " + KEY_IN_ARCHIVE + " AND NOT " + KEY_IS_SHOWN, null, null, null, "RANDOM()", "1");
		
		if (c.getCount() == 0)
			return false;
		
		c.moveToFirst();

		current = new Word(c);
		c.close();
		nWordsLeft--;
		return true;
	}

	String getQuestion() {
		return isBackward ? current.getTranslation() : current.getWord();
	}
	
	String getAnswer() {
		return isBackward ? current.getWord() : current.getTranslation();
	}

	void setWrong() {
        ContentValues args = new ContentValues();
        args.put(KEY_FAILS_NUMBER, current.getFails() + 1);
        args.put(KEY_IS_SHOWN, 1);

        mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + current.getId(), null);
        fails++;
	}
	
	void setRight() {
        ContentValues args = new ContentValues();
        args.put(KEY_PASSES_NUMBER, current.getPasses() + 1);
        args.put(KEY_IS_SHOWN, 1);

        mDb.update(DATABASE_TABLE, args, KEY_ID + "=" + current.getId(), null);
        passes++;
	}
	
	void moveWordsToArchive() {
        ContentValues args = new ContentValues();
        args.put(KEY_IN_ARCHIVE, 1);
        
		mDb.update(DATABASE_TABLE, args, String.format("%s >= %d", KEY_PASSES_NUMBER, maxPasses), null);
	}
	
	void moveWordsFromArchive() {
        ContentValues args = new ContentValues();
        args.put(KEY_IN_ARCHIVE, 0);
        
		mDb.update(DATABASE_TABLE, args, null, null);
	}

	void resetShown() {
        ContentValues args = new ContentValues();
        args.put(KEY_IS_SHOWN, 0);
        
		mDb.update(DATABASE_TABLE, args, null, null);
		passes = 0;
		fails = 0;
	}

	void addWord(Word q) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_WORD, q.getWord());
		initialValues.put(KEY_TRANSLATION, q.getTranslation());
		initialValues.put(KEY_PASSES_NUMBER, q.getPasses());
		initialValues.put(KEY_FAILS_NUMBER, q.getFails());
		initialValues.put(KEY_IS_SHOWN, 0);
		initialValues.put(KEY_IN_ARCHIVE, 0);

		mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	void addWords(Word ww[]) {
		try {
			mDb.beginTransaction();
			for (int i = 0; i < ww.length; i++)
				addWord(ww[i]);
			mDb.setTransactionSuccessful();
		} catch (SQLException e) {
			Toast t = Toast.makeText(ctx, e.toString(), Toast.LENGTH_LONG);
			t.show();
		} finally {
			mDb.endTransaction();
		}
	}
	
	void importWords(String path) throws ModelParseError {
		String s;
		LinkedList<Word> words = new LinkedList<Word>();
		
		try {
			FileReader fr = new FileReader(new File(path));
			LineNumberReader r = new LineNumberReader(fr);
			
			while((s = r.readLine()) != null) {
				s = s.trim();
				
				if (s.length() > 0 && s.charAt(0) == '#')
					continue;
				
				try {
					words.add(Word.fromString(s));
				} catch (WordParseError e) {
					Toast t = Toast.makeText(ctx, e.toString(), Toast.LENGTH_LONG);
					t.show();
				}
			}
			r.close();
		} catch (IOException e) {
			throw new ModelParseError(e.toString());
		}

		addWords(words.toArray(new Word[0]));
        calcStats();
	}
	
	TextView createTextView(String s, Word w) {
		TextView t = new TextView(ctx);
		SpannableString ss = new SpannableString(s);
		if (w.isShown())
			ss.setSpan(new StrikethroughSpan(), 0, ss.length(), 0);
		else if (w.isInArchive())
			t.setEnabled(false);
		t.setText(ss);
		
		return t;
	}
	
	void fillStats(TableLayout layout) {
		Cursor c = mDb.rawQuery(String.format("SELECT * FROM %s ORDER BY %s, %s;", DATABASE_TABLE, KEY_IN_ARCHIVE, KEY_PASSES_NUMBER), null);
		
		if (c == null || c.getCount() == 0)
			return;
		
		c.moveToFirst();
		
		TableRow r;
		do {
			TextView t;
			Word w = new Word(c);
			
			r = new TableRow(ctx);
		
			t = createTextView(w.getWord(), w);
			r.addView(t);
		
			t = createTextView(Integer.toString(w.getFails()), w);
			r.addView(t);
			
			t = createTextView(Integer.toString(w.getPasses()), w);
			r.addView(t);
		
			layout.addView(r);
		} while (c.moveToNext());
		
		c.close();
	}

	void exportWords() {
		String state = Environment.getExternalStorageState();

		if (!(Environment.MEDIA_MOUNTED.equals(state) &&
				!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
			
			Toast t = Toast.makeText(ctx, "Can't export database. Please, make sure" +
					"that SD Card is available and read-write !", Toast.LENGTH_LONG);
			t.show();
			return;
		}
		
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		
		Calendar calendar = new GregorianCalendar();
		File file = new File(dir, String.format("words-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.txt", calendar));
		
		try {
			FileWriter f = new FileWriter(file);
			Cursor c = null;
			c = mDb.query(DATABASE_TABLE, null, null, null, null, null, null, null);

			c.moveToFirst();

			do {
				Word w = new Word(c);
				f.write(w.toString() + "\n");
			} while (c.moveToNext());
			
			c.close();
			f.close();
		} catch (IOException e) {
			Toast t = Toast.makeText(ctx, e.toString(), Toast.LENGTH_LONG);
			t.show();
			return;
		}
		
		Toast t = Toast.makeText(ctx, String.format("Exported to file %s", file.getPath()), Toast.LENGTH_LONG);
		t.show();
	}
	
	void deleteAll() {
		mDb.delete(DATABASE_TABLE, null, null);
		calcStats();
	}
	
	void reset() {
        ContentValues args = new ContentValues();
        args.put(KEY_IS_SHOWN, 0);
        args.put(KEY_IN_ARCHIVE, 0);
        args.put(KEY_PASSES_NUMBER, 0);
        args.put(KEY_FAILS_NUMBER, 0);
        
		mDb.update(DATABASE_TABLE, args, null, null);
		calcStats();
	}
	
	void setMaxPasses(int maxPasses) {
		this.maxPasses = maxPasses;
	}
	
	void setBackward(boolean isBackward) {
		this.isBackward = isBackward;
	}
}
