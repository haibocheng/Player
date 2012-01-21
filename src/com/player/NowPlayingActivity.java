package com.player;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.player.PlayerService.PlayerBinder;

public class NowPlayingActivity extends Activity {

	private Button playButton, stopButton, prevButton, nextButton;
	private SeekBar trackSeek;
	private ListView tracklistView;
	private TextView currentTrackProgressView, currentTrackDurationView;
	private ArrayAdapter<Track> tracklistAdapter;	
	private PlayerServiceConnection playerServiceConnection;
	private PlayerService playerService;
    private UiRefresher uiRefresher;
    private Timer progressRefresher;
    private int playerStatus;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.now_playing);

        playerServiceConnection = new PlayerServiceConnection();
        progressRefresher = new Timer();

        tracklistView = (ListView)findViewById(R.id.tracklist);
        playButton = (Button)findViewById(R.id.play_button);
        stopButton = (Button)findViewById(R.id.stop_button);
        prevButton = (Button)findViewById(R.id.prev_button);
        nextButton = (Button)findViewById(R.id.next_button);
        trackSeek = (SeekBar)findViewById(R.id.track_seek);
        currentTrackProgressView = (TextView)findViewById(R.id.track_progress);
        currentTrackDurationView = (TextView)findViewById(R.id.track_duration);
               
        registerForContextMenu(tracklistView);        
        playButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				playerService.play();
			}		
		});
        stopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				playerService.stop();
			}		
		});
        prevButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				playerService.prevTrack();
			}		
		});
        nextButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				playerService.nextTrack();
			}		
		});
        tracklistView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				playerService.play(pos);				
			}
		});
        trackSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onProgressChanged(SeekBar arg0, final int pos, boolean user) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						currentTrackProgressView.setText(Track.formatDuration(pos));
					}
				});
				if (user) {
					playerService.seek(pos);
				} 
			}		
			
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}
		});
        progressRefresher.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if (playerStatus == PlayerService.PLAYING) {
					refreshTrack();
				}
			}
		}, 0, 500);		
    	tracklistAdapter = new ArrayAdapter<Track>(this, R.layout.tracklist_item, 0) {
    		
    		@Override
    		public View getView(int pos, View convertView, ViewGroup parent) {
    			View v = convertView;
    			ViewHolder holder = null;
    			if (v == null) {
    				LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    				v = inflater.inflate(R.layout.tracklist_item, null);
    				holder = new ViewHolder();
    				holder.title = (TextView)v.findViewById(R.id.tracklist_item_title);
    				holder.artist = (TextView)v.findViewById(R.id.tracklist_item_artist);
    				holder.duration = (TextView)v.findViewById(R.id.tracklist_item_duration);
    				holder.playicon = (ImageView)v.findViewById(R.id.tracklist_item_playicon);
    				v.setTag(holder);
    			} else {
    				holder = (ViewHolder)v.getTag();
    			}
    			Track track = getItem(pos);
    			String title = track.getTitle(), artist = track.getArtist();
    			holder.title.setText(title);
    			holder.artist.setText(artist);
    			holder.duration.setText(Track.formatDuration(track.getDuration()));
    			if (pos == playerService.getCurrentTrack()) {
    				holder.playicon.setImageResource(R.drawable.playicon);
    			} else {
    				holder.playicon.setImageDrawable(null);
    			}
    			return v;
    		}
    	};
    	tracklistView.setAdapter(tracklistAdapter);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Intent playerServiceIntent = new Intent(this, PlayerService.class);
    	getApplicationContext().bindService(playerServiceIntent, playerServiceConnection, 0);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	synchronized (playerService) {
    		playerService.notifyAll();
    		uiRefresher.done();
    	}
    	playerService.storeTracklist();
    	getApplicationContext().unbindService(playerServiceConnection);
    }
    
    public class PlayerServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			PlayerBinder playerBinder = (PlayerBinder)service;
			playerService = playerBinder.getService();
			uiRefresher = new UiRefresher();
	        (new Thread(uiRefresher)).start();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};
	
    public void refreshTrack() {
    	
			final int progress = playerService.getCurrentPosition(), max = playerService.getCurrentTrackDuration();
			final String durationText = Track.formatDuration(playerService.getCurrentTrackDuration()), progressText = Track.formatDuration(playerService.getCurrentPosition());
			runOnUiThread(new Runnable() {

				@Override
				public void run() {							
					currentTrackDurationView.setText(durationText);
					currentTrackProgressView.setText(progressText);
					trackSeek.setMax(max);
					trackSeek.setProgress(progress);
				}
			});
    }

    public void refreshTracklist() {

    	final ArrayList<Track> currentTracks = playerService.getCurrentTracks();
    	final int currentTrack = playerService.getCurrentTrack();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {							
		    	tracklistAdapter.clear();
		    	for (Track t : currentTracks) {
		    		tracklistAdapter.add(t);
		    	}
		    	tracklistAdapter.notifyDataSetChanged();
		    	tracklistView.setSelection(currentTrack);
			}
		});
    }
    
	public void refreshButtons() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				switch (playerStatus) {
				case PlayerService.PLAYING:
					playButton.setText(R.string.pause_button);
				break;
				default:
					playButton.setText(R.string.play_button);
				break;
				}
				
			}
		});
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
       	MenuInflater inflater = getMenuInflater();
       	inflater.inflate(R.layout.track_menu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    	switch (item.getItemId()) {
    	case R.id.track_menu_remove:
    		playerService.deleteTrack(info.position);
    	break;
    	case R.id.track_menu_info:
    		
    	break;
    	}
    	return super.onContextItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.now_playing_menu, menu);        
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_clear_tracklist:
    		playerService.clearTracklist();
    	break;
    	case R.id.menu_settings:
    		startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    	break;
    	}
    	return super.onOptionsItemSelected(item);
    }

    private class UiRefresher implements Runnable {
    	
    	private boolean done = false;
    	
    	public void done() {
    		done = true;
    	}

		@Override
		public void run() {

			while (!done) {
				synchronized (playerService) {
					playerStatus = playerService.getStatus();
					refreshTrack();
					refreshTracklist();
					refreshButtons();				
					playerService.take();
					try {
						playerService.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
    }
 
    static private class ViewHolder {
    	TextView title;
    	TextView artist;
    	TextView duration;
    	ImageView playicon;
    }
}
