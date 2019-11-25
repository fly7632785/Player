package com.jafir.player;

import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

public class RecordVideoListAdapter extends BaseQuickAdapter<CheckModel<RecordingModel>, BaseViewHolder> {
    public RecordVideoListAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, CheckModel<RecordingModel> item) {
        GlideApp.with(mContext).load(item.getData().getCoverUrl())
                .error(R.drawable.default_img)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.default_img)
                .into((ImageView) helper.getView(R.id.img));
        helper.setText(R.id.name, item.getData().getName());
        helper.setText(R.id.create_time, "录制时间: "+item.getData().getCreateTime());
        helper.setText(R.id.duration, "时长: "+item.getData().getDuration());
        helper.setText(R.id.size, "大小: "+getCacheSize(item.getData().getSize()));
        helper.setGone(R.id.checkbox, item.isShowCheckBox());
        helper.setChecked(R.id.checkbox, item.isStatus());
        helper.setOnClickListener(R.id.item, view -> {
            if (item.isShowCheckBox()) {
                helper.setChecked(R.id.checkbox, !item.isStatus());
                item.setStatus(!item.isStatus());
                int num = 0;
                for (CheckModel checkModel :
                        getData()) {
                    if (checkModel.isStatus()) {
                        num++;
                    }
                }
                if (notifacation != null) {
                    notifacation.num(num);
                }
            } else {
                if (onClickListener != null) {
                    onClickListener.onClick(item);
                }
            }
        });

    }

    public String getCacheSize(long size) {
        try {
            return String.format("%.2f M", (double)size / 1024 / 1024);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0 M";
    }

    public void allChoice() {
        for (CheckModel checkModel :
                getData()) {
            checkModel.setStatus(true);
        }
        notifyDataSetChanged();
        if (notifacation != null) {
            notifacation.num(getData().size());
        }
    }

    Notifacation notifacation;

    public void setNotifacation(Notifacation notifacation) {
        this.notifacation = notifacation;
    }

    OnClickItemListener onClickListener;

    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {
        this.onClickListener = onClickItemListener;
    }

    public List<RecordingModel> getDeleteData() {
        List<RecordingModel> list = new ArrayList<>();
        for (CheckModel<RecordingModel> checkModel :
                getData()) {
            if (checkModel.isStatus()) {
                RecordingModel channelModel = checkModel.getData();
                list.add(channelModel);
            }
        }
        return list;
    }

    public interface Notifacation {
        void num(int num);
    }

    public interface OnClickItemListener {
        void onClick(CheckModel<RecordingModel> checkModel);
    }
}
