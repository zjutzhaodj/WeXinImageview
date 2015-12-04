package com.example.administrator.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
    private String mDirPath;
    private List<String> mImgsPaths;
    private LayoutInflater minflater;
    private static Set<String> mSelectedImg = new HashSet<>();


    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {

        this.mDirPath = dirPath;
        this.mImgsPaths = mDatas;
        minflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return mImgsPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgsPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            convertView = minflater.inflate(R.layout.itemgridview, null);
            viewHolder.mImg = (ImageView) convertView.findViewById(R.id.item_img1);
            viewHolder.mImgbutton = (ImageButton) convertView.findViewById(R.id.item_select);
            convertView.setTag(viewHolder);
        } else {

            viewHolder = (ViewHolder) convertView.getTag();
        }


        viewHolder.mImg.setImageResource(R.drawable.abc_ic_clear_mtrl_alpha);
        viewHolder.mImgbutton.setImageResource(R.drawable.abc_ic_voice_search_api_mtrl_alpha);
        viewHolder.mImg.setColorFilter(null);

        final String filePath = mDirPath + "/" + mImgsPaths.get(position);
        final ViewHolder finalViewHolder = viewHolder;
        viewHolder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectedImg.contains(filePath)) {
                    mSelectedImg.remove(filePath);
                    finalViewHolder.mImg.setColorFilter(null);

                } else {
                    mSelectedImg.add(filePath);
                    finalViewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));

                }
//                notifyDataSetChanged();
            }
        });

        if (mSelectedImg.contains(filePath)) {
            finalViewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
        }

        MyImageLoader.getInstance(3, MyImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImgsPaths.get(position), viewHolder.mImg);

        return convertView;

    }
}

class ViewHolder {
    ImageView mImg;
    ImageButton mImgbutton;
}