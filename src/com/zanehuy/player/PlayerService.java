package com.zanehuy.player;

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
import android.provider.MediaStore.Audio;

public class PlayerService extends Service {

    static public final int STOPED = -1, PAUSED = 0, PLAYING = 1;
    private MediaPlayer mediaPlayer;
    private ArrayList<Track> tracklist;
    private int status, currentTrackPosition;
    private boolean taken;
    private IBinder playerBinder;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        tracklist = new ArrayList<Track>();
        currentTrackPosition = -1;
        setStatus(STOPED);
        playerBinder = new PlayerBinder();

        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer arg0) {
                if (currentTrackPosition == tracklist.size()-1) {
                    stop();
                } else {
                    nextTrack();
                }
            }
        });
        restoreTracklist();
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

    private void untake() {
        synchronized (this) {
            taken = false;
            notifyAll();
        }
    }

    public boolean isTaken() {
        return taken;
    }

    private void setStatus(int s) {
        status = s;
    }

    public int getStatus() {
        return status;
    }

    public ArrayList<Track> getTracklist() {
        return tracklist;
    }

    public Track getTrack(int pos) {
        return tracklist.get(pos);
    }

    public Track getCurrentTrack() {
        if (currentTrackPosition < 0) {
            return null;
        } else {
            return tracklist.get(currentTrackPosition);
        }
    }

    public int getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    public void addTrack(Track track) {
        tracklist.add(track);
        untake();
    }

    public void addTrack(int id) {
        tracklist.add(new Track(id));
        untake();
    }

    public void removeTrack(int pos) {
        if (pos == currentTrackPosition) {
            stop();
        }
        if (pos < currentTrackPosition) {
            currentTrackPosition--;
        }
        tracklist.remove(pos);
        untake();
    }

    public void clearTracklist() {
        if (status > STOPED) {
            stop();
        }
        tracklist.clear();
        untake();
    }

    public void playTrack(int pos) {
        if (status > STOPED) {
            stop();
        }
        FileInputStream file = null;
        try {
            file = new FileInputStream(new File(tracklist.get(pos).getPath()));
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
        setStatus(PLAYING);
        untake();
    }

    public void play(int pos) {
        playTrack(pos);
    }

    public void play() {
        switch (status) {
        case STOPED:
            if (!tracklist.isEmpty()) {
                playTrack(0);
            }
        break;
        case PLAYING:
            mediaPlayer.pause();
            setStatus(PAUSED);
        break;
        case PAUSED:
            mediaPlayer.start();
            setStatus(PLAYING);
        break;
        }
        untake();
    }

    public void pause() {
        mediaPlayer.pause();
        setStatus(PAUSED);
        untake();
    }

    public void stop() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        currentTrackPosition = -1;
        setStatus(STOPED);
        untake();
    }

    public void nextTrack() {
        if (currentTrackPosition < tracklist.size()-1) {
            playTrack(currentTrackPosition+1);
        }
    }

    public void prevTrack() {
        if (currentTrackPosition > 0) {
            playTrack(currentTrackPosition-1);
        }
    }

    public int getCurrentTrackProgress() {
        if (status > STOPED) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public int getCurrentTrackDuration() {
        if (status > STOPED) {
            return tracklist.get(currentTrackPosition).getDuration();
        } else {
            return 0;
        }
    }

    public void seekTrack(int p) {
        if (status > STOPED) {
            mediaPlayer.seekTo(p);
            untake();
        }
    }

    public void storeTracklist() {
        DbOpenHelper dbOpenHelper = new DbOpenHelper(getApplicationContext());
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        dbOpenHelper.onUpgrade(db, 1, 1);
        for (int i = 0; i < tracklist.size(); i++) {
            ContentValues c = new ContentValues();
            c.put(DbOpenHelper.KEY_POSITION, i);
            c.put(DbOpenHelper.KEY_FILE, tracklist.get(i).getPath());
            db.insert(DbOpenHelper.TABLE_NAME, null, c);
        }
        dbOpenHelper.close();
    }

    private void restoreTracklist() {
        DbOpenHelper dbOpenHelper = new DbOpenHelper(getApplicationContext());
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor c = db.query(DbOpenHelper.TABLE_NAME, null, null, null, null, null, null);
        tracklist.clear();
        while (c.moveToNext()) {
            tracklist.add(new Track(c.getString(1)));
        }
        dbOpenHelper.close();
    }

    public class PlayerBinder extends Binder {

        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public class Track {

        private String path, artist, album, year, title, genre;
        private int id, duration;

        public Track(String p) {
            path = p;
            String[] proj = {Audio.Media.ARTIST, Audio.Media.ALBUM, Audio.Media.YEAR, Audio.Media.TITLE, Audio.Media.DURATION, Audio.Media._ID};
            Cursor trackCursor = getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, proj, Audio.Media.DATA+" = '"+path+"' ", null, null);
            trackCursor.moveToNext();
            artist = trackCursor.getString(0);
            album = trackCursor.getString(1);
            year = trackCursor.getString(2);
            title = trackCursor.getString(3);
            duration = Integer.parseInt(trackCursor.getString(4));
            id = Integer.parseInt(trackCursor.getString(5));
        }

        public Track(int id) {
            String[] proj = {Audio.Media.ARTIST, Audio.Media.ALBUM, Audio.Media.YEAR, Audio.Media.TITLE, Audio.Media.DURATION, Audio.Media.DATA};
            Cursor trackCursor = getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, proj, "_ID = "+id+" ", null, null);
            trackCursor.moveToNext();
            artist = trackCursor.getString(0);
            album = trackCursor.getString(1);
            year = trackCursor.getString(2);
            title = trackCursor.getString(3);
            duration = Integer.parseInt(trackCursor.getString(4));
            path = trackCursor.getString(5);
        }

        public String getPath() {
            return path;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public String getYear() {
            return year;
        }

        public String getTitle() {
            return title;
        }

        public String getGenre() {
            return genre;
        }

        public int getId() {
            return id;
        }

        public int getDuration() {
            return duration;
        }
    }

    public static String formatTrackDuration(int d) {
        String min = Integer.toString((d/1000)/60);
        String sec = Integer.toString((d/1000)%60);
        if (sec.length() == 1) sec = "0"+sec;
        return min+":"+sec;
    }
}
