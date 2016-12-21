package com.example.hnkim.sharebox;

/**
 * Created by hnkim on 2016-12-21.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ExplorerActivity extends AppCompatActivity {

    public static final int REQUEST_NEW_EXPLORER = 19681;

    public static final int CLIPBOARD_STATE_NONE = 0;
    public static final int CLIPBOARD_STATE_COPIED = 1;
    public static final int CLIPBOARD_STATE_CUT = 2;

    SwipeRefreshLayout mSwipeRefreshLayout = null;
    private ListView mListView = null;
    private ListViewAdapter mAdapter = null;
    private File mCurrentDir = null;
    private ArrayList<String> mPathBarStatus = null;
    private Set<String> mSearchBlackListSet = new HashSet<>();
    private ArrayList<String> mClipBoard = null;
    private int mClipBoardState = CLIPBOARD_STATE_NONE;
    private boolean mIsSearched = false;
    private ArrayList<String> mSearchedListPath = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        mIsSearched = getIntent().getBooleanExtra("mIsSearched", false);
        if(mIsSearched)
            mSearchedListPath = getIntent().getStringArrayListExtra("mSearchedListPath");

        String path = getIntent().getStringExtra("mCurrentDirPath");
        if(path==null)
            mCurrentDir = Environment.getExternalStorageDirectory();
        else
            mCurrentDir = new File(path);
        if(mCurrentDir==null) {
            Toast.makeText(this, "경로가 잘못되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        mPathBarStatus = getIntent().getStringArrayListExtra("mPathBarStatus");
        if(mPathBarStatus==null) {
            mPathBarStatus = new ArrayList<>();
            mPathBarStatus.add("루트");
        }
        else if(!mIsSearched)
            mPathBarStatus.add(mCurrentDir.getName());
        LinearLayout pathBar = (LinearLayout) findViewById(R.id.ll_pathbar);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for(String title : mPathBarStatus) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(lparams);
            tv.setText(" > " + title);
            pathBar.addView(tv);
        }

        final HorizontalScrollView hsv = (HorizontalScrollView)findViewById(R.id.hsv_pathbar);
        hsv.postDelayed(new Runnable() {
            @Override
            public void run() {
                hsv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);

        mClipBoard = getIntent().getStringArrayListExtra("mClipBoard");
        mClipBoardState = getIntent().getIntExtra("mClipBoardState", CLIPBOARD_STATE_NONE);

        mSearchBlackListSet.add(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android");
        Log.d("TAG", Environment.getExternalStorageDirectory().getAbsolutePath());

        mListView = (ListView) findViewById(R.id.lv_filelist);
        mListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mAdapter.getSelectedCount()>0) {
                    if (!((ListData)mAdapter.getItem(position)).mName.equals(".."))
                        mAdapter.select(position);
                    else
                        fileClick("./..");
                }
                else
                    fileClick( ((ListData)mAdapter.getItem(position)).mPath );
            }
        });
        mListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(!((ListData)mAdapter.getItem(position)).mName.equals(".."))
                    mAdapter.select(position);
                return true;
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_filelist);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                readFileList();
            }
        });

        if(AndroidExplorer.checkWritePermission(this))
            readFileList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.explorer_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item_explorer_search: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("검색");
                alert.setMessage("검색할 파일명을 입력해주세요. 해당 파일명을 포함한 모든 파일을 검색합니다.");
                final EditText input = new EditText(this);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if(value.length()==0) {
                            Toast.makeText(ExplorerActivity.this, "파일명을 입력하지 않아 검색을 취소합니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayList<File> files = new ArrayList();
                        for(File cf : mCurrentDir.listFiles()) {
                            if(isBlacklistFile(cf)) continue;
                            if(cf.isDirectory())
                                files = AndroidExplorer.getFileListLike(cf, value, files);
                            else if(cf.getName().contains(value))
                                files.add(cf);
                        }
                        ArrayList<String> filesPath = new ArrayList<String>();
                        for(File file : files)
                            filesPath.add(file.getAbsolutePath());

                        Intent intent = new Intent(ExplorerActivity.this, ExplorerActivity.class);
                        intent.putExtra("mIsSearched", true);
                        intent.putExtra("mSearchedListPath", filesPath);
                        intent.putExtra("mCurrentDir", mCurrentDir);
                        intent.putExtra("mPathBarStatus", mPathBarStatus);
                        startActivityForResult(intent, REQUEST_NEW_EXPLORER);
                    }
                });
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Canceled.
                            }
                        });
                alert.show();

                return true;
            }
            case R.id.item_explorer_selectall:
                mAdapter.selectAll();
                return true;

            case R.id.item_explorer_cancelall:
                mAdapter.selectClear();
                return true;

            case R.id.item_explorer_copy:
            case R.id.item_explorer_cut: {
                if (!checkItemSelected()) return true;
                mClipBoard = new ArrayList<>();
                for(int idx : mAdapter.getSelected())
                    mClipBoard.add(((ListData)mAdapter.getItem(idx)).mPath);
                mAdapter.selectClear();
                if (item.getItemId() == R.id.item_explorer_copy)
                    mClipBoardState = CLIPBOARD_STATE_COPIED;
                else
                    mClipBoardState = CLIPBOARD_STATE_CUT;
                Intent intent = new Intent();
                intent.putExtra("mClipBoard", mClipBoard);
                intent.putExtra("mClipBoardState", mClipBoardState);
                setResult(RESULT_OK, intent);
                Toast.makeText(this, "지정된 파일이 클립보드에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.item_explorer_paste: {
                if(mIsSearched) {
                    Toast.makeText(this, "검색 중에는 붙여넣기가 불가능합니다.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (mClipBoardState == CLIPBOARD_STATE_NONE || mClipBoard == null) {
                    Toast.makeText(this, "복사된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                String currentPath = mCurrentDir.getAbsolutePath();
                boolean pasteComplete = true;
                for (String srcPath : mClipBoard) {
                    String dstPath = currentPath + srcPath.substring(srcPath.lastIndexOf('/'));
                    if (mClipBoardState == CLIPBOARD_STATE_CUT) {
                        if (!AndroidFileHandler.move(new File(srcPath), new File(dstPath))) {
                            pasteComplete = false;
                            break;
                        }
                    } else if (mClipBoardState == CLIPBOARD_STATE_COPIED) {
                        if (!AndroidFileHandler.copy(new File(srcPath), new File(dstPath))) {
                            pasteComplete = false;
                            break;
                        }
                    }
                }
                if (pasteComplete) {
                    if (mClipBoardState == CLIPBOARD_STATE_CUT) {
                        mClipBoardState = CLIPBOARD_STATE_NONE;
                        mClipBoard = null;
                        setResult(RESULT_OK, new Intent());
                    }
                    Toast.makeText(this, "붙여넣기를 완료하였습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "오류가 발생하여 붙여넣기를 중단합니다.", Toast.LENGTH_SHORT).show();
                }
                readFileList();
                return true;
            }
            case R.id.item_explorer_delete: {
                if (!checkItemSelected()) return true;
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("정말로 삭제하시겠습니까?");
                // Set an EditText view to get user input
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        for (int i = mAdapter.getCount() - 1; i > 0; --i) {
                            ListData data = (ListData) mAdapter.getItem(i);
                            if (data.mChecked) {
                                if (AndroidFileHandler.remove(new File(data.mPath)))
                                    mAdapter.remove(i);
                                else
                                    Toast.makeText(ExplorerActivity.this, "삭제가 실패하였습니다.(" + data.mName + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                        mAdapter.dataChange();
                        mClipBoard = new ArrayList<>();
                        mClipBoardState = CLIPBOARD_STATE_NONE;
                        setResult(RESULT_OK, new Intent());
                        Toast.makeText(ExplorerActivity.this, "선택된 파일이 삭제되었습니다..", Toast.LENGTH_SHORT).show();
                    }
                });
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(ExplorerActivity.this, "삭제가 취소되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                alert.show();
                return true;
            }
            case R.id.item_explorer_rename: {
                if (!checkItemSelected(1)) return true;
                int selectedIdx = mAdapter.getSelected()[0];
                String renamePath = ((ListData) mAdapter.getItem(selectedIdx)).mPath;
                File renameFile = new File(renamePath);
                inputRenameDialog(renameFile);
                return true;
            }
            case R.id.item_explorer_properties: {
                if(!checkItemSelected()) return true;

                long fileSizeSum = 0;
                int countDir = 0, countFile = 0;
                for(int idx : mAdapter.getSelected()) {
                    File file = new File(((ListData)mAdapter.getItem(idx)).mPath);
                    if(file.isDirectory()) {
                        fileSizeSum += AndroidExplorer.getDirectorySize(file, null);
                        countDir += AndroidExplorer.getDirectoryCountOnlyDir(file);
                        countFile += AndroidExplorer.getDirectoryCountOnlyFile(file, null);
                        ++countDir;
                    }
                    else {
                        fileSizeSum += file.length();
                        ++countFile;
                    }
                }

                LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.dialog_properties,
                        (ViewGroup) findViewById(R.id.dp_layout));

                TextView tvName = (TextView) layout.findViewById(R.id.dp_name);
                TextView tvPath = (TextView) layout.findViewById(R.id.dp_path);
                TextView tvSize = (TextView) layout.findViewById(R.id.dp_size);
                TextView tvLastModified = (TextView) layout.findViewById(R.id.dp_last);
                TextView tvCountDIr = (TextView) layout.findViewById(R.id.dp_countdir);
                TextView tvCountFile = (TextView) layout.findViewById(R.id.dp_countfile);

                if(mAdapter.getSelectedCount() > 1) {
                    tvName.setVisibility(View.GONE);
                    tvPath.setVisibility(View.GONE);
                    tvLastModified.setVisibility(View.GONE);
                    ((TextView) layout.findViewById(R.id.dp_name_title)).setVisibility(View.GONE);
                    ((TextView) layout.findViewById(R.id.dp_path_title)).setVisibility(View.GONE);
                    ((TextView) layout.findViewById(R.id.dp_last_title)).setVisibility(View.GONE);
                }
                else {
                    int selected = mAdapter.getSelected()[0];
                    File selectedFile = new File(((ListData)mAdapter.getItem(selected)).mPath);
                    tvName.setText(selectedFile.getName());
                    tvPath.setText(selectedFile.getAbsolutePath());
                    tvLastModified.setText(new Date(selectedFile.lastModified()).toString());
                }
                tvSize.setText(AndroidExplorer.changeFormat(fileSizeSum));
                if(countDir==0) {
                    tvCountDIr.setVisibility(View.GONE);
                    ((TextView) layout.findViewById(R.id.dp_countdir_title)).setVisibility(View.GONE);
                }
                else
                    tvCountDIr.setText(countDir + " 개");
                tvCountFile.setText(countFile + " 개");

                AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
                alert.setView(layout);
                alert.setTitle("파일 정보");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // ok clicked...
                    }
                });
                AlertDialog dialog = alert.create();
                dialog.show();
                Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if(b != null)
                    b.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                return true;
            }
            case R.id.item_explorer_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void inputRenameDialog(final File renameFile) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("이름 바꾸기");
        alert.setMessage("변경할 파일명을 입력해주세요.");
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if(AndroidFileHandler.rename(renameFile, value))
                    Toast.makeText(ExplorerActivity.this, "이름이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ExplorerActivity.this, "이름 바꾸기가 실패하였습니다.", Toast.LENGTH_SHORT).show();
                mAdapter.selectClear();
                readFileList();
            }
        });
        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
        alert.show();
    }

    private boolean checkItemSelected() {
        if(mAdapter.getSelectedCount()==0) {
            Toast.makeText(this, "파일을 선택해주세요. 길게 클릭할 시 파일이 선택됩니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private boolean checkItemSelected(int count) {
        if(mAdapter.getSelectedCount()==count)
            return true;
        Toast.makeText(this, "파일을 " + count + "개 선택해주세요. 길게 클릭할 시 파일을 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void fileClick(String filePath) {
        if(filePath.substring(filePath.lastIndexOf('/')+1).equals("..")) {
            finish();
            return;
        }
        File clickedFile = null;
        try {
            clickedFile = new File(filePath).getCanonicalFile();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "파일 읽기에 실패하였습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(clickedFile.isDirectory()) {
            Intent intent = new Intent(this, ExplorerActivity.class); // this, this.class
            intent.putExtra("mCurrentDirPath", clickedFile.getAbsolutePath());
            intent.putExtra("mPathBarStatus", mPathBarStatus);
            intent.putExtra("mClipBoard", mClipBoard);
            intent.putExtra("mClipBoardState", mClipBoardState);
            startActivityForResult(intent, REQUEST_NEW_EXPLORER);
        }
        else {
            // execute clicked file
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);

            // set Type
            Log.d("TAG", "cf: " + clickedFile.getName());
            String mime = AndroidExplorer.getMime(clickedFile.getName());
            if(mime!=null && mime.length()>0)
                intent.setDataAndType(Uri.fromFile(clickedFile), mime);
            else
                intent.setData(Uri.fromFile(clickedFile));
            Log.d("TAG", "mime: " + mime);

            if(intent.resolveActivity(getPackageManager()) != null)
                startActivity(intent);
            else
                Toast.makeText(this, "이 파일을 실행할 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readFileList() {
        mSwipeRefreshLayout.setRefreshing(true);

        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ListViewAdapter adapter = new ListViewAdapter(ExplorerActivity.this);
                if (mIsSearched) {
                    for (String path : mSearchedListPath) {
                        File file = new File(path);
                        if (!file.exists()) continue;
                        if (isBlacklistFile(file)) continue;
                        adapter.addItem(
                                getFileImage(file),
                                path,
                                path,
                                getListAdapterNote(file)
                        );
                    }
                }
                else { // not serached, read files in current directory
                    File[] files = mCurrentDir.listFiles();
                    if (files == null) {
                        Toast.makeText(ExplorerActivity.this, "파일 목록을 불러오는데 실패하였습니다.", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.post(new Runnable(){
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 E요일, HH:mm:ss", Locale.KOREAN);
                    for (File file : files) {
                        if (isBlacklistFile(file)) continue;
                        adapter.addItem(
                                getFileImage(file),
                                file.getAbsolutePath(),
                                simpleDateFormat.format(new Date(file.lastModified())),
                                getListAdapterNote(file)
                        );
                    }
                }
                mAdapter = adapter;
                sortAdapterBySettings();
                mAdapter.addItemToHead(getDrawable(R.drawable.folder_icon), "./..", "상위 폴더로", "");
                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mListView.setAdapter(mAdapter);
                    }
                });
                mSwipeRefreshLayout.post(new Runnable(){
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        readThread.start();
    }

    private String getListAdapterNote(File file) {
        String note;
        if (file.isDirectory()) {
            int count = AndroidExplorer.getDirectoryCount(file, null);
            if (count == 0)
                note = "빈 폴더";
            else
                note = count + " 항목";
        } else
            note = AndroidExplorer.changeFormat(file.length());
        return note;
    }

    private void sortAdapterBySettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String sortbase = sharedPref.getString(SettingsActivity.KEY_SORTBASE, "이름 오름차순");
        if(sortbase.equals("이름 오름차순"))
            mAdapter.sort(ListData.NAME_ASC_COMPARATOR);
        else if(sortbase.equals("이름 내림차순"))
            mAdapter.sort(ListData.NAME_DESC_COMPARATOR);
        else if(sortbase.equals("날짜 오름차순"))
            mAdapter.sort(ListData.DATE_ASC_COMPARATOR);
        else if(sortbase.equals("날짜 내림차순"))
            mAdapter.sort(ListData.DATE_DESC_COMPARATOR);
    }

    private boolean isBlacklistFile(File file) {
        if(file.getName().charAt(0)=='.' && !getSettingShowHiddenFile()) return true;
        if (mSearchBlackListSet.contains(file.getAbsolutePath())) return true;
        return false;
    }

    private boolean getSettingShowHiddenFile() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(SettingsActivity.KEY_SHOW_HIDDEN_FILE, false);
    }

    private Drawable getFileImage(File file) {
        if(file.isDirectory())
            return getDrawable(R.drawable.folder_icon);

        switch (AndroidExplorer.getFileCategory(file.getName())) {
            case AndroidExplorer.CAT_APP:
                return getDrawable(R.drawable.c_app);

            case AndroidExplorer.CAT_DOCU:
                return getDrawable(R.drawable.c_doc);

            case AndroidExplorer.CAT_IMAGE:
                Drawable dr = Drawable.createFromPath(file.getAbsolutePath());
                if(dr==null) {
                    return getDrawable(R.drawable.c_etc);
                }
                Bitmap bitmap = ((BitmapDrawable)dr).getBitmap();
                return new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 48, 48, true));
            case AndroidExplorer.CAT_MUSIC:
                return getDrawable(R.drawable.c_music);

            case AndroidExplorer.CAT_VIDEO:
                return getDrawable(R.drawable.c_video);

            default :
                return getDrawable(R.drawable.c_etc);


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(requestCode == AndroidExplorer.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE ||
                    requestCode == AndroidExplorer.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
                readFileList();
        }
        else {
            if(requestCode == AndroidExplorer.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE)
                Toast.makeText(this, "읽기 권한을 읽지 못했습니다.", Toast.LENGTH_SHORT).show();
            else if(requestCode == AndroidExplorer.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
                Toast.makeText(this, "쓰기 권한을 얻지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_NEW_EXPLORER && resultCode==RESULT_OK) {
            mClipBoard = data.getStringArrayListExtra("mClipBoard");
            mClipBoardState = data.getIntExtra("mClipBoardState", CLIPBOARD_STATE_NONE);
            Intent intent = new Intent();
            intent.putExtra("mClipBoard", mClipBoard);
            intent.putExtra("mClipBoardState", mClipBoardState);
            setResult(RESULT_OK, intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
