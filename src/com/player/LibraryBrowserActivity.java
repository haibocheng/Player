package com.player;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub.OnInflateListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LibraryBrowserActivity extends Activity {
	
	final static public int ARTISTS = 0, ALBUMS = 1, TRACKS = 2;
	private int currentLevel;
	private ListView libraryListView;
	private ArrayAdapter<Artist> artistAdapter;
	private ArrayAdapter<Album> albumAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library);
		
		libraryListView = (ListView)findViewById(R.id.library_list);
		libraryListView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				switch (currentLevel) {				
				case ARTISTS:
					//Toast.makeText(getApplicationContext(), artistAdapter.getItem(pos).getName(), 500).show();
					showAlbums(artistAdapter.getItem(pos).getId());
				break;
				case ALBUMS:
					showTracks();
				break;
				case TRACKS:
				break;
				}
			}
		});
		artistAdapter = new ArrayAdapter<Artist>(this, R.layout.library_item, 0) {
			
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
    			holder.name.setText(getItem(pos).getName());
    			return v;
			}
		};
		albumAdapter = new ArrayAdapter<Album>(this, R.layout.library_item, 0) {
			
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
    			holder.name.setText(getItem(pos).getName());
    			return v;
			}
		};
		showArtists();
	}
	
	private void showArtists() {
		currentLevel = ARTISTS;
		String[] proj = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST};
		Cursor artistCursor = managedQuery(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ARTIST);		
		artistAdapter.clear();		
		while (artistCursor.moveToNext()) {
			artistAdapter.add(new Artist(Integer.parseInt(artistCursor.getString(0)), artistCursor.getString(1)));
		}
		libraryListView.setAdapter(artistAdapter);
	}
	
	private void showAlbums(int id) {
		currentLevel = ALBUMS;
		String[] proj = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM};
		Cursor albumCursor = managedQuery(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.ALBUM);
		albumAdapter.clear();
		while (albumCursor.moveToNext()) {
			albumAdapter.add(new Album(Integer.parseInt(albumCursor.getString(0)), albumCursor.getString(1)));
		}
		libraryListView.setAdapter(albumAdapter);
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
    
    private class Artist {
    	private int id;
    	private String name;
    	
    	public Artist(int i, String n) {
    		id = i;
    		name = n;
    	}
    	
    	public int getId() {
    		return id;
    	}
    	
    	public String getName() {
    		return name;
    	}
    }

    private class Album {
    	private int id;
    	private String name;
    	
    	public Album(int i, String n) {
    		id = i;
    		name = n;
    	}
    	
    	public int getId() {
    		return id;
    	}
    	
    	public String getName() {
    		return name;
    	}
    }
}
