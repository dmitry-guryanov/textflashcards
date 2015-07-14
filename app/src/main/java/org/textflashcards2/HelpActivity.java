package org.textflashcards2;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class HelpActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.help);

		
		WebView w = (WebView) findViewById(R.id.webView1);
		w.setWebViewClient(new myWebViewClient(this));
		w.getSettings().setJavaScriptEnabled(true);
		w.loadUrl("file:///android_asset/html/intro1.html");
	}
	
	void close() {
		Intent i = new Intent();
		setResult(RESULT_OK, i);
		finish();
	}
	
    private class myWebViewClient extends WebViewClient {
    	private HelpActivity activity;
    	
    	myWebViewClient(HelpActivity activity) {
    		this.activity = activity;
    	}
    	
        public boolean shouldOverrideUrlLoading(WebView view, String url) 
        {
        	Log.v(Conf.TAG, url);

			if (url.endsWith("finish"))
				activity.close();
			else
				view.loadUrl(url);
			return true;
        }
    }
}

