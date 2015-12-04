package com.example.administrator.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Bean.FolderBean;


public class MainActivity extends Activity {
    private GridView mgridview;
    private ImageAdapter mImageAdapter;
    private RelativeLayout mBottomLy;
    private TextView mDirname;
    private TextView mDirCount;
    private List<String> mImgs;
    private ProgressDialog mProgressDialog;
    private ListImagPopupwindow listImagPopupwindow;

    private File mCurrentDir;
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private int mMaxCount;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x110) {
                mProgressDialog.dismiss();
                data2View();
                initDirpopwindow();
            }
        }
    };

    private void initDirpopwindow() {

        listImagPopupwindow = new ListImagPopupwindow(this, mFolderBeans);
        listImagPopupwindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        listImagPopupwindow.setOnDirSelecterListener(new ListImagPopupwindow.OnDirSelectListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith(".png") || filename.endsWith(".jpeg"))
                            return true;
                        return false;
                    }
                }));

                mImageAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mgridview.setAdapter(mImageAdapter);

                mDirCount.setText(mImgs.size() + "");
                mDirname.setText(folderBean.getName());
                listImagPopupwindow.dismiss();
            }
        });

    }

    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);

    }

    private void data2View() {

        if (mCurrentDir == null) {
            Toast.makeText(this, "没有图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImageAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mgridview.setAdapter(mImageAdapter);
        mDirCount.setText(mMaxCount + "");
        mDirname.setText(mCurrentDir.getName());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas();
        initEvent();
    }

    private void initEvent() {
        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listImagPopupwindow.setAnimationStyle(R.style.PopWindow);
                listImagPopupwindow.showAsDropDown(mBottomLy, 0, 0);
                lightOff();
            }
        });

    }

    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 利用contentprovider扫描手机中所有图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载");
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + " = ? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=? ",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DEFAULT_SORT_ORDER);

                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;

                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstimgPath(path);
                    }

                    if (parentFile.list() == null) {
                        continue;
                    }
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg") || filename.endsWith(".png") || filename.endsWith(".jpeg"))
                                return true;
                            return false;
                        }
                    }).length;

                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);
                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }

                }
                cursor.close();
                mDirPaths = null;
                mHandler.sendEmptyMessage(0x110);

            }
        }.start();

    }

    private void initView() {
        mgridview = (GridView) findViewById(R.id.gridview);
        mBottomLy = (RelativeLayout) findViewById(R.id.rl_bottom);
        mDirname = (TextView) findViewById(R.id.dir_name);
        mDirCount = (TextView) findViewById(R.id.dir_num);

    }


}
