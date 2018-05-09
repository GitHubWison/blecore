package cn.zfs.bledebuger.base;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zengfs on 2016/1/18.
 */
public abstract class BaseListAdapter<T> extends BaseAdapter {
	private Context context;
	private List<T> data;

	public BaseListAdapter(Context context) {
		data = new ArrayList<>();
		this.context = context;
	}
	
	public BaseListAdapter(Context context, List<T> data) {
		this.context = context;
		this.data = data;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
		notifyDataSetChanged();
	}
	
	public Context getContext() {
        return context;
    }
	
	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public T getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BaseHolder<T> holder;
		if (convertView == null) {
			holder = getHolder();
		} else {
			holder = (BaseHolder<T>) convertView.getTag();
		}
		holder.setData(data.get(position), position);
		return holder.getConvertView();
	}

	protected abstract BaseHolder<T> getHolder();
}