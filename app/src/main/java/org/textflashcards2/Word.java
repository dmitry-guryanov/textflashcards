package org.textflashcards2;

import android.database.Cursor;

class WordParseError extends Exception {
	public String msg;
	WordParseError(String msg) {
		this.msg = msg;
	}
	
	public String toString() {
		return String.format("WordParseError: %s", msg);
	}
};

public class Word implements Comparable<Word> {
	private String w;
	private String t;
	private int passes;
	private int fails;
	private int id;
	private boolean inArchive = false;
	private boolean isShown = false;
	
	Word(int id, String w, String t, int passes, int fails) {
		this.id = id;
		this.w = w;
		this.t = t;
		this.passes = passes;
		this.fails = fails;
	}
	
	Word(Cursor c) {
		id = c.getInt(c.getColumnIndexOrThrow(TrainerModel.KEY_ID));
		w = c.getString(c.getColumnIndexOrThrow(TrainerModel.KEY_WORD));
		t = c.getString(c.getColumnIndexOrThrow(TrainerModel.KEY_TRANSLATION));
		passes = c.getInt(c.getColumnIndexOrThrow(TrainerModel.KEY_PASSES_NUMBER));
		fails =  c.getInt(c.getColumnIndexOrThrow(TrainerModel.KEY_FAILS_NUMBER));
		inArchive = (c.getInt(c.getColumnIndexOrThrow(TrainerModel.KEY_IN_ARCHIVE)) > 0);
		isShown = (c.getInt(c.getColumnIndexOrThrow(TrainerModel.KEY_IS_SHOWN)) > 0);
	}
	
	static Word fromString(String s) throws WordParseError {
		String []strs;
		
		strs = s.split("\\|");
		if(strs.length == 4)
			return new Word(0, strs[0], strs[1],
					Integer.parseInt(strs[2]), Integer.parseInt(strs[3]));
		else if (strs.length == 2)
			return new Word(0, strs[0], strs[1], 0, 0);
		else
			throw new WordParseError(String.format("Can't parse string '%s'", s));
	}
	
	String getWord() {
		return w;
	}
	
	String getTranslation() {
		return t;
	}
	
	@Override
	public String toString() {
		return String.format("%s|%s|%d|%d", w, getTranslation(), passes, fails);
	}
	
	public int compareTo(Word q) {
		if (fails < q.fails)
			return -1;
		else if (fails == q.fails)
			return 0;
		else
			return 1;		
	}
	
	int getPasses() {
		return passes;
	}
	
	int getFails() {
		return fails;
	}
	
	int getId() {
		return id;
	}
	
	boolean isShown() {
		return isShown;
	}
	
	boolean isInArchive() {
		return inArchive;
	}
}
