package com.player;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class MainActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent playerServiceIntent = new Intent(this, PlayerService.class);
		startService(playerServiceIntent);

		Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabSpec spec;
	    Intent intent;

	    intent = new Intent().setClass(this, NowPlayingActivity.class);
	    spec = tabHost.newTabSpec("now_playing").setIndicator(res.getString(R.string.act_now_playing), res.getDrawable(R.drawable.ic_tab_now_playing)).setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, LibraryBrowserActivity.class);
	    spec = tabHost.newTabSpec("library_browser").setIndicator(res.getString(R.string.act_library_browser), res.getDrawable(R.drawable.ic_tab_library_browser)).setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, FileBrowserActivity.class);
	    spec = tabHost.newTabSpec("file_browser").setIndicator(res.getString(R.string.act_file_browser), res.getDrawable(R.drawable.ic_tab_file_browser)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(0);		
	}
}
