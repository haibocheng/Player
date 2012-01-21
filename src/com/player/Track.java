package com.player;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class Track {

	private String path, artist, album, year, title, genre;
	private int duration = 0;
	
	public Track(File f) {
		path = f.getAbsolutePath();
		try {
			AudioFile af = AudioFileIO.read(f);
			Tag t = af.getTag();
			artist = t.getFirst(FieldKey.ARTIST);
			album = t.getFirst(FieldKey.ALBUM);
			year = t.getFirst(FieldKey.YEAR);
			title = t.getFirst(FieldKey.TITLE);
			genre = t.getFirst(FieldKey.GENRE);
			duration = af.getAudioHeader().getTrackLength()*1000;
		} catch (CannotReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TagException e) {
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			e.printStackTrace();
		} catch (InvalidAudioFrameException e) {
			e.printStackTrace();
		}
	}
	
	public Track(String p) {
		path = p;
	}
	
	public Track(String p, String ar, String al, String t) {
		path = p;
		artist = ar;
		album = al;
		title = t;
	}
	
	public String getPath() {
		return path;
	}
	
	public File getFile() {
		return new File(path);
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
	
	public void setDuration(int d) {
		duration = d;
	}
	
	public int getDuration() {
		return duration;
	}
	
	static public String formatDuration(int p) {
    	String min = Integer.toString((p/1000)/60);
    	String sec = Integer.toString((p/1000)%60);
    	if (sec.length() == 1) sec = "0"+sec;
    	return min+":"+sec;
	}
}
