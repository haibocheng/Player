package com.zanehuy.player;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.zanehuy.player.R;
import com.zanehuy.player.PlayerService.PlayerBinder;

public class FileBrowserActivity extends Activity {

    private ListView fileListView;
    private TextView currentDirView;
    private File currentDir;
    private ArrayList<File> currentFiles;
    private ArrayAdapter<File> fileListAdapter;
    private Stack<Dir> dirHistory;
    private boolean parentAllowed;
    private PlayerServiceConnection playerServiceConnection = new PlayerServiceConnection();
    private PlayerService playerService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_browser);
        SharedPreferences settings = getSharedPreferences("settings", 0);

        currentDir = new File(settings.getString("last_dir", Environment.getExternalStorageDirectory().getAbsolutePath()));
        currentFiles = new ArrayList<File>();
        dirHistory = new Stack<Dir>();
        dirHistory.push(new Dir(currentDir.getAbsolutePath(), 0));

        currentDirView = (TextView)findViewById(R.id.file_browser_dir);
        currentDirView.setSelected(true);
        fileListView = (ListView)findViewById(R.id.files_listview);
        fileListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                File selectedFile;
                if (pos == 0) {
                    if (parentAllowed) {
                        selectedFile = currentDir.getParentFile();
                    } else {
                        return;
                    }
                } else {
                    selectedFile = currentFiles.get(pos-1);
                }
                if (selectedFile.isFile()) {
                    playerService.addTrack(playerService.new Track(selectedFile.getPath()));
                } else {
                    dirHistory.push(new Dir(currentDir.getAbsolutePath(), pos));
                    browse(selectedFile);
                }
            }
        });
        fileListAdapter = new ArrayAdapter<File>(this, R.layout.file_browser_item, 0) {

            @Override
            public View getView(final int pos, View convertView, android.view.ViewGroup parent) {
                View v = convertView;
                ViewHolder holder;
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
                File item = getItem(pos);
                   holder.fileName.setText(item.getName());
                if (!item.isFile()) {
                    holder.fileIcon.setImageResource(R.drawable.dir);
                } else {
                    holder.fileIcon.setImageResource(R.drawable.file);
                }
                holder.fileName.setText(item.getName());
                return v;
            };
        };
        fileListView.setAdapter(fileListAdapter);
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
        if ((keyCode == KeyEvent.KEYCODE_BACK) && dirHistory.size() > 1) {
            browse(new File(dirHistory.pop().getPath()));
            return true;
           }
        return super.onKeyDown(keyCode, event);
    }

    private void browse(File dir) {
        if (dir.compareTo(new File(Environment.getExternalStorageDirectory().getAbsolutePath())) == 0) {
            parentAllowed = false;
        } else {
            parentAllowed = true;
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
            currentFiles.add(new File(file.getAbsolutePath()));
        }
        fileListAdapter.clear();
        fileListAdapter.add(new File(".."));
        for (File f : currentFiles) {
            fileListAdapter.add(f);
        }
        fileListAdapter.notifyDataSetChanged();
        currentDirView.setText(currentDir.getAbsolutePath());
           fileListView.setSelection(dirHistory.lastElement().getPos());
    }

    private class PlayerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            PlayerBinder playerBinder = (PlayerBinder)service;
            playerService = playerBinder.getService();
        }
    }

    static private class ViewHolder {
        TextView fileName;
        ImageView fileIcon;
    }

    private class Dir {

        private String path;
        private int pos;

        public Dir(String path, int pos) {
            this.path = path;
            this.pos = pos;
        }

        public String getPath() {
            return path;
        }

        public int getPos() {
            return pos;
        }
    }
}
