package com.player;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.player.PlayerService.PlayerBinder;

public class NowPlayingActivity extends Activity {

	private Button addTracksButton, playButton, prevButton, nextButton;
	private SeekBar trackSeek;
	private ListView tracklistView;
	private TextView currentTrackProgressView, currentTrackDurationView;
	private ArrayAdapter<Track> tracklistAdapter;	
	private PlayerServiceConnection playerServiceConnection = new PlayerServiceConnection();
	private PlayerService playerService = null;
    private UiRefresher uiRefresher = null;
    private TrackRefresher trackRefresher = null;
    private Object uiUpdaterMonitor = new Object(), trackUpdaterMonitor = new Object();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.now_playing);

        tracklistView = (ListView)findViewById(R.id.tracklist);
        playButton = (Button)findViewById(R.id.play_button);
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
			public void onStopTrackingTouch(SeekBar arg0) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			
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
		});

    	tracklistAdapter = new ArrayAdapter<Track>(this, R.layout.tracklist_item, R.id.tracklist_title) {
    		
    		@Override
    		public View getView(int pos, View convertView, ViewGroup parent) {
    			View v = convertView;
    			ViewHolder holder = null;
    			
    			if (v == null) {
    				LayoutInflater inflater = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    				v = inflater.inflate(R.layout.tracklist_item, null);
    				holder = new ViewHolder();
    				holder.trackTitle = (TextView)v.findViewById(R.id.tracklist_title);
    				holder.trackDetails = (TextView)v.findViewById(R.id.tracklist_details);
    				holder.trackTable = (TableLayout)v.findViewById(R.id.tracklist_item);
    				v.setTag(holder);
    			} else {
    				holder = (ViewHolder)v.getTag();
    			}
    			
    			Track track = getItem(pos);    			
    			holder.trackTitle.setText(Integer.toString(pos+1)+". "+track.getTitle());
    			holder.trackDetails.setText(track.getArtist()+" / "+track.getYear()+" - "+track.getAlbum());
    			if (pos == playerService.getCurrentTrack()) {
    				holder.trackTable.setBackgroundColor(Color.DKGRAY);
    			} else {
    				holder.trackTable.setBackgroundColor(Color.BLACK);
    			}
    			return v;
    		}
    	};
    	tracklistView.setAdapter(tracklistAdapter);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
		uiRefresher = new UiRefresher();
		trackRefresher = new TrackRefresher();
        (new Thread(uiRefresher)).start();
        (new Thread(trackRefresher)).start();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	uiRefresher.done();
    	trackRefresher.done();
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
    	playerService.storeTracklist();
    	getApplicationContext().unbindService(playerServiceConnection);
    }
    
    public class PlayerServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			synchronized (uiUpdaterMonitor) {
				PlayerBinder playerBinder = (PlayerBinder)service;
				playerService = playerBinder.getService();
				playerService.untake();
				uiUpdaterMonitor.notifyAll();				
			}
			synchronized (trackUpdaterMonitor) {
				trackUpdaterMonitor.notifyAll();
			}
		}
	};
	
    public void refreshTrack() {
    	
		if (playerService.getStatus() == PlayerService.STOPED) {
			final int progress = 0, max = 0;
			final boolean enabled = false;
			final String durationText = "", progressText = "";
			runOnUiThread(new Runnable() {

				@Override
				public void run() {							
					trackSeek.setProgress(progress);
					trackSeek.setMax(max);
					trackSeek.setEnabled(enabled);
					currentTrackDurationView.setText(durationText);
					currentTrackProgressView.setText(progressText);
				}
			});
		} else {
			final int progress = playerService.getCurrentPosition(), max = playerService.getCurrentTrackDuration();
			final boolean enabled = true;
			final String durationText = Track.formatDuration(playerService.getCurrentTrackDuration()), progressText = Track.formatDuration(playerService.getCurrentPosition());
			runOnUiThread(new Runnable() {

				@Override
				public void run() {							
					trackSeek.setProgress(progress);
					trackSeek.setMax(max);
					trackSeek.setEnabled(enabled);
					currentTrackDurationView.setText(durationText);
					currentTrackProgressView.setText(progressText);
				}
			});
		}
    }

    public void refreshTracklist() {

    	final ArrayList<Track> currentTracks = playerService.getCurrentTracks();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {							
		    	tracklistAdapter.clear();
		    	for (Track t : currentTracks) {
		    		tracklistAdapter.add(t);
		    	}
		    	tracklistAdapter.notifyDataSetChanged();
		    	tracklistView.setSelection(playerService.getCurrentTrack());
			}
		});
    }
    
	public void refreshButtons() {
		if (playerService.getStatus() == PlayerService.PLAYING) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {							
					playButton.setText(R.string.pause_button);
				}
			});
		} else {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {							
					playButton.setText(R.string.play_button);
				}
			});
		}			
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
    	case R.id.track_menu_delete:
    		playerService.deleteTrack(info.position);
    		refreshTracklist();
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
    		refreshTracklist();
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
				synchronized (uiUpdaterMonitor) {
					if (playerService == null) {
						try {
							uiUpdaterMonitor.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					synchronized (playerService) {
						if (playerService.isTaken()) {
							try {
								playerService.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						playerService.take();
						refreshTrack();
						refreshTracklist();
						refreshButtons();
					}
				}
			}
		}    	
    }
    
    private class TrackRefresher implements Runnable {
    	
    	private boolean done = false;
    	
    	public void done() {
    		done = true;
    	}

		@Override
		public void run() {
			synchronized (trackUpdaterMonitor) {
				while (!done) {
					if (playerService == null) {
						try {
							trackUpdaterMonitor.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					refreshTrack();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}    	
    }
    
    static private class ViewHolder {
    	TextView trackTitle;
    	TextView trackDetails;
    	TableLayout trackTable;
    }
}
