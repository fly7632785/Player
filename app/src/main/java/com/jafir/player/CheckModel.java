package com.jafir.player;

public class CheckModel<T> {
    boolean status = false;
    boolean showCheckBox = false;
    T data;

    public CheckModel(boolean showCheckBox, T data) {
        this.showCheckBox = showCheckBox;
        this.data = data;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isShowCheckBox() {
        return showCheckBox;
    }

    public void setShowCheckBox(boolean showCheckBox) {
        this.showCheckBox = showCheckBox;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
