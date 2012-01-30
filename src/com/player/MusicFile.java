package com.player;

import java.io.File;

import android.view.View;

public class MusicFile extends File {
	
	private static final long serialVersionUID = 1L;
	private String musicName = null, cover = null;
	private int listPos;
	
	public MusicFile(String path) {		
		super(path);
		listPos = -1;
	}
	
	public boolean isTrack() {
		return isFile();
	}
	
	public void setCover(String c) {
		cover = c;
	}
	
	public void setMusicName(String n) {
		musicName = n;
	}
	
	public String getCover() {
		return cover;
	}
	
	public String getMusicName() {
		return musicName;
	}
	
	public void setListPos(int p) {
		listPos = p;
	}
	
	public int getListPos() {
		return listPos;
	}
}
