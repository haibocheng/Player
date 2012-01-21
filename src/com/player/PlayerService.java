package com.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;

public class PlayerService extends Service {
	
	static public final int STOPED = 0, PLAYING = 1, PAUSED = 2;
	
	private MediaPlayer mediaPlayer;
	private ArrayList<Track> currentTracks;
	private int currentTrackPosition;
	private int status;
	private boolean taken;
	private IBinder playerBinder;
	
	@Override
	public void onCreate() {
		super.onCreate();

		mediaPlayer = new MediaPlayer();
		currentTracks = new ArrayList<Track>();
		currentTrackPosition = -1;
		status = STOPED;
		playerBinder = new PlayerBinder();
		
		DbOpenHelper dbOpenHelper = new DbOpenHelper(getApplicationContext());
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		Cursor c = db.query(DbOpenHelper.TABLE_NAME, null, null, null, null, null, null);		
		while (c.moveToNext()) {
			currentTracks.add(new Track(new File(c.getString(1))));
		}
		dbOpenHelper.close();
		
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer arg0) {
				if (currentTrackPosition == -2 || currentTrackPosition == currentTracks.size()-1) {
					stop();
				} else {
					nextTrack();
				}
			}
		});		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return playerBinder;	    
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	public void take() {
		taken = true;
	}
	
	public void untake() {
		synchronized (this) {
			taken = false;
			notifyAll();
		}
	}
	
	public ArrayList<Track> getCurrentTracks() {
		return currentTracks;
	}
	
	public Track getTrack(int pos) {
		return currentTracks.get(pos);
	}
	
	public Track getCurrentTrack() {
		if (currentTrackPosition < 0) {
			return null;
		} else {
			return currentTracks.get(currentTrackPosition);
		}
	}
	
	public int getCurrentTrackPosition() {
		return currentTrackPosition;
	}
	
	public void addTrack(File track) {
		currentTracks.add(new Track(track));
		untake();
	}
	
	public void deleteTrack(int pos) {
		if (pos < currentTrackPosition) currentTrackPosition--;
		if (pos == currentTrackPosition) currentTrackPosition = -2;
		currentTracks.remove(pos);
		untake();
	}
	
	public void clearTracklist() {
		if (currentTrackPosition >= 0) currentTrackPosition = -2;
		currentTracks.clear();
		untake();
	}
	
	public void playTrack(int pos) {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
		}
		mediaPlayer.reset();
		FileInputStream file = null;
		try {
			file = new FileInputStream(currentTracks.get(pos).getFile());
			mediaPlayer.setDataSource(file.getFD());
			mediaPlayer.prepare();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mediaPlayer.start();
		currentTrackPosition = pos;
		status = PLAYING;
		untake();
	}
	
	public void play(int pos) {
		playTrack(pos);
	}
	
	public void play() {
		switch (status) {
		case STOPED:
			if (!currentTracks.isEmpty()) {
				play(0);
			}
		break;
		case PLAYING:
			mediaPlayer.pause();
			status = PAUSED;
		break;
		case PAUSED:
			mediaPlayer.start();
			status = PLAYING;
		break;
		}
		untake();
	}
	
	public void pause() {
		mediaPlayer.pause();
		status = PAUSED;
		untake();
	}
	
	public void stop() {
		mediaPlayer.stop();
		mediaPlayer.reset();
		currentTrackPosition = -1;
		status = STOPED;
		untake();
	}
	
	public void nextTrack() {
		if (currentTrackPosition < currentTracks.size()-1) {
			playTrack(currentTrackPosition+1);
		}		
	}
	
	public void prevTrack() {
		if (currentTrackPosition > 0) {
			playTrack(currentTrackPosition-1);
		}
	}
	
	public int getStatus() {
		return status;
	}
	
	public int getCurrentPosition() {
		switch (status) {
		case STOPED:
			return 0;
		default:
			return mediaPlayer.getCurrentPosition();
		}
	}
	
	public int getCurrentTrackDuration() {
		switch (status) {
		case STOPED:
			return 0;
		default:
			return currentTracks.get(currentTrackPosition).getDuration();
		}
	}
	
	public void seek(int s) {
		mediaPlayer.seekTo(s);
		untake();
	}
	
	public boolean isTaken() {
		return taken;
	}
	
	public void storeTracklist() {
		DbOpenHelper dbOpenHelper = new DbOpenHelper(getApplicationContext());
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		dbOpenHelper.onUpgrade(db, 1, 1);

		int i = 0;
		for (Track track : currentTracks) {
			ContentValues c = new ContentValues();
			c.put(DbOpenHelper.KEY_POSITION, i);
			c.put(DbOpenHelper.KEY_FILE, track.getFile().getAbsolutePath());
			db.insert(DbOpenHelper.TABLE_NAME, null, c);
			i++;
		}
		dbOpenHelper.close();
	}

	public class PlayerBinder extends Binder {
		
		public PlayerService getService() {
			return PlayerService.this;
		}
	}	
}
