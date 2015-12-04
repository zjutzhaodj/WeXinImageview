package com.example.administrator.myapplication;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import Bean.FolderBean;

/**
 * Created by Administrator on 2015/12/3.
 */
public class ListImagPopupwindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mlistview;
    private List<FolderBean> mDatas;

    public ListImagPopupwindow(Context context, List<FolderBean> mDatas) {
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popupwindow, null);
        this.mDatas = mDatas;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews(context);
        initEvents();

    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater inflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(Context context, int resource, List<FolderBean> objects) {
            super(context, 0, objects);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_popupwindow, parent, false);
                holder.mimgl = (ImageView) convertView.findViewById(R.id.id_dir_item_img);
                holder.mDirName = (TextView) convertView.findViewById(R.id.dir_item_name1);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.dir_item_count1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            FolderBean bean = getItem(position);
            MyImageLoader.getInstance(3, MyImageLoader.Type.LIFO).loadImage(bean.getFirstimgPath(), holder.mimgl);
            holder.mDirCount.setText(bean.getCount() + "");
            holder.mDirName.setText(bean.getName());
            return convertView;
        }


        private class ViewHolder {

            ImageView mimgl;
            TextView mDirName;
            TextView mDirCount;
        }

    }

    private void initViews(Context context) {
        mlistview = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mlistview.setAdapter(new ListDirAdapter(context, 0, mDatas));


    }

    public interface OnDirSelectListener {
        void onSelected(FolderBean folderBean);
    }

    private OnDirSelectListener mlisener;

    public void setOnDirSelecterListener(OnDirSelectListener listener) {

        mlisener = listener;
    }

    private void initEvents() {
        mlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mlisener != null) {
                    mlisener.onSelected(mDatas.get(position));
                }
            }
        });


    }


    private void calWidthAndHeight(Context context) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }

}
