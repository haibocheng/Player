package com.player;

import android.app.Activity;
import android.os.Bundle;

public class LibraryBrowserActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_browser);
		
    	System.out.println("!!LIBRARY_START!!");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
    	System.out.println("!!LIBRARY_STOP!!");
		
	}
	
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	System.out.println("!!LIBRARY_RESUME!!");
    	
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	System.out.println("!!LIBRARY_PAUSE!!");
    }
	
}
