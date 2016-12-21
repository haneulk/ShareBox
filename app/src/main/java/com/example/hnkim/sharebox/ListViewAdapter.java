package com.example.hnkim.sharebox;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by ESRENLL on 2016-12-15.
 */

public class ListViewAdapter extends BaseAdapter {

    private Context mContext = null;
    private ArrayList<ListData> mListData = new ArrayList<>();
    private ArrayList<Integer> mSelectedItems = new ArrayList<>();

    public ListViewAdapter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public Object getItem(int position) {
        return mListData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_filelist, parent, false);
            holder.mChecked = (CheckBox) convertView.findViewById(R.id.cb_filecheck);
            holder.mIcon = (ImageView) convertView.findViewById(R.id.iv_fileimage);
            holder.mName = (TextView) convertView.findViewById(R.id.tv_filename);
            holder.mDate = (TextView) convertView.findViewById(R.id.tv_filedate);
            holder.mNote = (TextView) convertView.findViewById(R.id.tv_filenote);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder) convertView.getTag();

        ListData mData = mListData.get(position);
        if(mSelectedItems.size()>0 && position!=0) {
            holder.mChecked.setChecked(mData.mChecked);
            holder.mChecked.setVisibility(View.VISIBLE);
        }
        else
            holder.mChecked.setVisibility(View.GONE);
        if(mData.mIcon != null) {
            holder.mIcon.setVisibility(View.VISIBLE);
            holder.mIcon.setImageDrawable(mData.mIcon);
        }
        else
            holder.mIcon.setVisibility(View.GONE);
        holder.mName.setText(mData.mName);
        holder.mDate.setText(mData.mDate);
        holder.mNote.setText(mData.mNote);

        return convertView;
    }

    public void addItem(Drawable icon, String path, String date, String note) {
        ListData addInfo = new ListData();
        addInfo.mChecked = false;
        addInfo.mIcon = icon;
        addInfo.mName = path.substring(path.lastIndexOf('/')+1);
        addInfo.mDate = date;
        addInfo.mNote = note;
        addInfo.mPath = path;
        mListData.add(addInfo);
    }

    public void addItemToHead(Drawable icon, String path, String date, String note) {
        ListData addInfo = new ListData();
        addInfo.mChecked = false;
        addInfo.mIcon = icon;
        addInfo.mName = path.substring(path.lastIndexOf('/')+1);
        addInfo.mDate = date;
        addInfo.mNote = note;
        addInfo.mPath = path;
        mListData.add(0, addInfo);
    }

    public void remove(int pos) {
        for(int i=mSelectedItems.size()-1; i>0; --i){
            int get = mSelectedItems.get(i);
            if(get > pos)
                mSelectedItems.set(i, get-1);
        }
        mSelectedItems.remove((Integer)pos);
        mListData.remove(pos);
        dataChange();
    }

    public void sort(Comparator<ListData> comparator) {
        mSelectedItems.clear();
        Collections.sort(mListData, comparator);
        dataChange();
    }

    public void dataChange() {
        notifyDataSetChanged();
    }

    // return isChecked
    public boolean select(int pos) {
        boolean ret = true;
        if(mSelectedItems.remove((Integer)pos)) {
            mListData.get(pos).mChecked = false;
            ret = false;
        }
        else {
            mSelectedItems.add(pos);
            mListData.get(pos).mChecked = true;
        }
        dataChange();
        return ret;
    }

    public void selectClear() {
        for(ListData data : mListData)
            data.mChecked = false;
        mSelectedItems.clear();
        dataChange();
    }

    public void selectAll() {
        mSelectedItems.clear();
        for(int i=1; i<mListData.size(); ++i) {
            mSelectedItems.add(i);
            mListData.get(i).mChecked = true;
        }
        dataChange();
    }

    public int getSelectedCount() {
        return mSelectedItems.size();
    }

    public class ViewHolder {
        public CheckBox mChecked;
        public ImageView mIcon;
        public TextView mName;
        public TextView mDate;
        public TextView mNote;
    }

    public Integer[] getSelected() {
        return mSelectedItems.toArray(new Integer[0]);
    }
}

