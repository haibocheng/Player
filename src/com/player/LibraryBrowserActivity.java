package com.player;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LibraryBrowserActivity extends Activity {
	
	final static public int ARTISTS = 0, ALBUMS = 1, TRACKS = 2;
	private int currentLevel;
	private ListView libraryListView;
	private ArrayAdapter<String> listAdapter;
	private ArrayAdapter<String> trackListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library);
		
		libraryListView = (ListView)findViewById(R.id.library_list);
//		libraryListView.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				switch (currentLevel) {
//				case ARTISTS:
//					showAlbums();
//				break;
//				case ALBUMS:
//					showTracks();
//				break;
//				case TRACKS:
//				break;
//				}
//			}
//		});
		listAdapter = new ArrayAdapter<String>(this, R.layout.library_item, 0) {
	
			@Override
			public View getView(int pos, View convertView, ViewGroup parent) {
    			View v = convertView;
    			ArtistViewHolder holder = null;    			
    			if (v == null) {
    				LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    				v = inflater.inflate(R.layout.library_item, null);
    				holder = new ArtistViewHolder();
    				holder.name = (TextView)v.findViewById(R.id.library_item_name);
    				v.setTag(holder);
    			} else {
    				holder = (ArtistViewHolder)v.getTag();
    			}    			
    			holder.name.setText(getItem(pos));
    			return v;
			}
		};
		showArtists();
	}
	
	private void showArtists() {
		currentLevel = ARTISTS;
		String[] proj = {MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media._ID};
		Cursor artistCursor = managedQuery(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ARTIST);
		listAdapter.clear();
		while (artistCursor.moveToNext()) {
			listAdapter.add(artistCursor.getString(0));
		}
		libraryListView.setAdapter(listAdapter);
	}
	
	private void showAlbums() {
		currentLevel = ALBUMS;
		String[] proj = {MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media._ID};
		Cursor artistCursor = managedQuery(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ALBUM);
		listAdapter.clear();
		while (artistCursor.moveToNext()) {
			listAdapter.add(artistCursor.getString(0));
		}
		libraryListView.setAdapter(listAdapter);
	}
	
	private void showTracks() {
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    static private class ArtistViewHolder {
    	TextView name;
    }
}
