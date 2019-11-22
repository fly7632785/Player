package com.jafir.player;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.pockettv.dropscreen.entity.ClingDevice;

/**
 * created by jafir on 2019/2/25
 */
public class DropScreenAdapter extends BaseQuickAdapter<ClingDevice, BaseViewHolder> {
    public DropScreenAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, ClingDevice item) {
        helper.setText(R.id.name, item.getDevice().getDetails().getFriendlyName());
        helper.getView(R.id.name).setSelected(item.isSelected());
    }
}
