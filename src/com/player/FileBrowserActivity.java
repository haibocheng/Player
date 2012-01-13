package com.player;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.player.PlayerService.PlayerBinder;

public class FileBrowserActivity extends Activity {

	private Button playerButton, parentDirButton;
	private ListView fileListView;
	
	private MusicFile currentDir;
	private ArrayList<MusicFile> currentFiles = new ArrayList<MusicFile>();
	private Stack<MusicFile> prevDirs = new Stack<MusicFile>();
	
	private HashMap<Integer, Bitmap> albumCovers;
	private HashMap<Integer, String> trackTitles;
	
	private PlayerServiceConnection playerServiceConnection = new PlayerServiceConnection();
	private PlayerService playerService = null;
	
	private Object mutex = new Object();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.file_browser);
        SharedPreferences settings = getSharedPreferences("settings", 0);
        currentDir = new MusicFile(settings.getString("last_dir", Environment.getExternalStorageDirectory().getAbsolutePath()));
        prevDirs.clear();
        
        albumCovers = new HashMap<Integer, Bitmap>();
        trackTitles = new HashMap<Integer, String>();
        
        playerButton = (Button)findViewById(R.id.player_button);
        parentDirButton = (Button)findViewById(R.id.parent_dir_button);
        fileListView = (ListView)findViewById(R.id.files_listview);              
        
        playerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
        parentDirButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if (prevDirs.size() > 0 && prevDirs.lastElement().compareTo(currentDir.getParentFile()) == 0) {
					prevDirs.pop();
				} else {
					prevDirs.push(currentDir);
				}
				browse(new MusicFile(currentDir.getParent()));
			}
		});
    	fileListView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				MusicFile selected_file = currentFiles.get(pos);
				if (selected_file.isFile()) {
					playerService.addTrack(selected_file);					
				} else {
					currentDir.setListPos(pos);
			    	prevDirs.push(currentDir);
					browse(selected_file);
				}
			}
		});
    	
    	browse(currentDir);
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
    	getApplicationContext().unbindService(playerServiceConnection);
    	
        SharedPreferences settings = getSharedPreferences("settings", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_dir", currentDir.getAbsolutePath());
        editor.commit();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if ((keyCode == KeyEvent.KEYCODE_BACK) && prevDirs.size() > 0) {
    		browse(prevDirs.pop());
    		return true;
   		}
    	return super.onKeyDown(keyCode, event);
    }
    
    private void browse(MusicFile dir) {    	
    	if (dir.compareTo(new MusicFile(Environment.getExternalStorageDirectory().getAbsolutePath())) == 0) {
    		parentDirButton.setEnabled(false);
    	} else {
    		parentDirButton.setEnabled(true);
    	}
    	currentFiles.clear();
    	currentDir = dir;
    	File[] fileList = dir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				if ((file.getName().toLowerCase().endsWith("mp3") || file.isDirectory()) && (file.getName()).charAt(0) != '.') {
					return true;
				}
				return false;
			}
		});    	
    	Arrays.sort(fileList, new Comparator<Object>() {

			@Override
			public int compare(Object file1, Object file2) {
				return new String(((File)file1).getName()).compareTo(((File)file2).getName());
			}
		});	
    	Arrays.sort(fileList, new Comparator<Object>() {

			@Override
			public int compare(Object file1, Object file2) {
				if (((File)file1).isDirectory() && ((File)file2).isFile()) {
					return -1;
				}
				if (!((File)file1).isDirectory() && ((File)file2).isDirectory()) {
					return 1;
				}
				return 0;
			}
		});	
    	for (File file : fileList) {
    		currentFiles.add(new MusicFile(file.getAbsolutePath()));
    	}
    	
    	ArrayAdapter<MusicFile> fileListAdapter = new ArrayAdapter<MusicFile>(this, R.layout.file_browser_item, R.id.file_browser_file_name) {
    		
    		@Override
            public View getView(final int pos, View convertView, android.view.ViewGroup parent) {
    			View v = convertView;
    			final ViewHolder holder;

    			if (v == null) {
    				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    				v = inflater.inflate(R.layout.file_browser_item, null);
    				holder = new ViewHolder();
    				holder.fileName = (TextView)v.findViewById(R.id.file_browser_file_name);
    				holder.fileIcon = (ImageView)v.findViewById(R.id.file_browser_file_icon);
    				v.setTag(holder);
    			} else {
    				holder = (ViewHolder)v.getTag();
    			}

    			final MusicFile item = getItem(pos);
               	holder.fileName.setText(item.getName());
                if (!item.isTrack()) {
                	holder.fileIcon.setImageResource(R.drawable.dir);
                } else {
                	holder.fileIcon.setImageResource(R.drawable.icon);
                }
            	holder.fileName.setText(item.getName());
    			
                new Thread(new Runnable() {
					
            		@Override
            		public void run() {
            			synchronized (mutex) {
                			if (!item.isFile()) {
                				if (!albumCovers.containsKey(pos)) {
                					File[] content = item.listFiles(new FileFilter() {
                					
                						@Override
                						public boolean accept(File file) {
                							if (file.getName().toLowerCase().endsWith(".jpg")) {
                								return true;
                							}
                							return false;
                						}
                					});			    			
                					if (content.length > 0) {
                    				
                						BitmapScaler scaler = null;
                						try {
                							scaler = new BitmapScaler(content[0], 80);
                						} catch (IOException e) {
                							e.printStackTrace();
                						}
                						BitmapScaler s = scaler;
                						albumCovers.put(pos, s.getScaled());																
                					}
                    			}
                            	runOnUiThread(new Runnable() {
                            		@Override
                    		        public void run() {
                            			if (albumCovers.containsKey(pos)) {
                            				holder.fileIcon.setImageBitmap(albumCovers.get(pos));
                            			} else {
                            				holder.fileIcon.setImageResource(R.drawable.dir);
                            			}
                    		        }
                               	});										    			
                			} else {
                				if (!trackTitles.containsKey(pos)) {
                					AudioFile afile = null;	
                					try {
                						afile = AudioFileIO.read(item);
                						trackTitles.put(pos, afile.getTag().getFirst(FieldKey.TITLE));			            		
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
                            	runOnUiThread(new Runnable() {
                            		@Override
                            		public void run() {
                            			holder.fileName.setText(trackTitles.get(pos));
                            		}
                				});
                			}
                			mutex.notify();
            			}
            		}
				}).start();
                
                return v;
            };    		
    	};

    	for (MusicFile f : currentFiles) fileListAdapter.add(f);
    	
    	((TextView)findViewById(R.id.file_browser_dir)).setText(currentDir.getAbsolutePath());
    	
    	albumCovers.clear();
    	trackTitles.clear();
    	fileListView.setAdapter(fileListAdapter);
    	if (currentDir.getListPos() != -1) {
    		fileListView.setSelection(currentDir.getListPos());
		}
    }
    
	static private class ViewHolder {
		
		TextView fileName;
		ImageView fileIcon;
	}
	
    public class PlayerServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			PlayerBinder playerBinder = (PlayerBinder)service;
			playerService = playerBinder.getService();
		}
	};
}
