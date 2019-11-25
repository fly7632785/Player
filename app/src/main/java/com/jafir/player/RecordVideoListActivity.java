package com.jafir.player;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jafir.TitleBar;
import com.jafir.player.dao.AppDatabase;
import com.jafir.player.dao.RecordingVideoDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RecordVideoListActivity extends AppCompatActivity {
    RecordVideoListAdapter recordListAdapter;
    @BindView(R.id.toolbar)
    TitleBar toolbar;
    @BindView(R.id.record)
    RecyclerView record;
    @BindView(R.id.multiple_choice)
    View multipleChoice;
    @BindView(R.id.num)
    TextView multipleChoiceNum;
    @BindView(R.id.all_choose)
    TextView multipleChoiceAllChoice;
    @BindView(R.id.delete)
    TextView multipleChoiceDelete;
    @BindView(R.id.no_record)
    LinearLayout noRecord;
    @BindView(R.id.msg)
    TextView mMsg;
    RecordingVideoDao mRecordingVideoDao;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, RecordVideoListActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video_list);
        ButterKnife.bind(this);
        initView();
        AppDatabase appDatabase = AppDatabase.create(getApplication());
        mRecordingVideoDao = appDatabase.recordingVideoDao();
        getRecordingVideos()
                .subscribe(models -> {
                    List<CheckModel<RecordingModel>> list = new ArrayList<>();
                    for (RecordingModel model :
                            models) {
                        list.add(new CheckModel(isMultipleChoice(), model));
                    }
                    setRecordData(list);
                });
    }

    private void initView() {
        toolbar.getLeftLayout().setOnClickListener(view -> {
            finish();
        });

        toolbar.getRightLayout().setOnClickListener(view -> {
            setMultipleChoice(!isMultipleChoice());
            setMutipleChoise(isMultipleChoice());
            showMultipleChoice();

        });
        record.setLayoutManager(new LinearLayoutManager(this));
        recordListAdapter = new RecordVideoListAdapter(R.layout.item_record_video_list);
        recordListAdapter.setOnClickItemListener(checkModel -> {
            RecordingModel model = checkModel.getData();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String path = model.getFilePath();
            File file = new File(path);
            if (Build.VERSION.SDK_INT >= 24) {
                intent.setDataAndType(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".my.package.name.provider", file),
                        "video/*");
            } else {
                intent.setDataAndType(Uri.fromFile(file),
                        "video/*");
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        });
        recordListAdapter.setNotifacation(num -> {
            multipleChoiceNum.setText("" + num);
        });
        record.setAdapter(recordListAdapter);
        noRecord.setVisibility(View.GONE);
    }


    public void setMutipleChoise(boolean status) {
        toolbar.getRightText().setText(status ? "取消" : "编辑");
        multipleChoice.setVisibility(status ? View.VISIBLE : View.GONE);
    }

    public void setRecordData(List<CheckModel<RecordingModel>> data) {
        if (data != null && data.size() > 0) {
            noRecord.setVisibility(View.GONE);
            //多少个缓存
            mMsg.setVisibility(View.VISIBLE);
            long totalSize = 0;
            for (CheckModel<RecordingModel> item : data) {
                totalSize += item.getData().getSize();
            }
            mMsg.setText("共" + data.size() + "个缓存，占用空间" + getCacheSize(totalSize));
        } else {
            noRecord.setVisibility(View.VISIBLE);
            mMsg.setText("共0个缓存，占用空间0M");
        }
        recordListAdapter.setNewData(data);
    }

    public String getCacheSize(long size) {
        try {
            return String.format("%.2f M", (double) size / 1024 / 1024);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0 M";
    }

    @OnClick({R.id.all_choose, R.id.delete})
    public void onClickMultipleChiceTool(View view) {
        switch (view.getId()) {
            case R.id.all_choose:
                recordListAdapter.allChoice();
                break;
            case R.id.delete:
                delete(recordListAdapter.getDeleteData());
                break;
        }
    }

    public void fetchData() {
        setMutipleChoise(isMultipleChoice());
        showMultipleChoice();
    }

    public void notifyDelete(boolean b) {
        new Handler().postDelayed(() -> fetchData(), 500);
    }


    boolean multipleChoiceType = false;

    public boolean isMultipleChoice() {
        return multipleChoiceType;
    }

    public void setMultipleChoice(boolean multipleChoiceType) {
        this.multipleChoiceType = multipleChoiceType;
    }

    public void showMultipleChoice() {
        getRecordingVideos()
                .subscribe(models -> {
                    List<CheckModel<RecordingModel>> list = new ArrayList<>();
                    for (RecordingModel model :
                            models) {
                        list.add(new CheckModel(isMultipleChoice(), model));
                    }
                    setRecordData(list);
                });
    }

    public void delete(List<RecordingModel> deleteData) {
        setMultipleChoice(false);
        //删除文件
        for (RecordingModel item : deleteData) {
            String avatarUrl = item.getCoverUrl();
            String filePath = item.getFilePath();
            new FileManager().deleteFolderFile(avatarUrl, true);
            new FileManager().deleteFolderFile(filePath, true);
        }
        deleteRecordingVideo(deleteData);
        notifyDelete(true);
    }


    /**
     * 获取所有录制视频model
     */
    public Single<List<RecordingModel>> getRecordingVideos() {
        return mRecordingVideoDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 增加录制视频model
     */
    public void addRecordingVideo(RecordingModel... list) {
        Completable.create(emitter -> mRecordingVideoDao.insertOrUpdateRecordVideo(list))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    /**
     * 删除录制视频model
     */
    public void deleteRecordingVideo(List<RecordingModel> list) {
        Completable.create(emitter -> mRecordingVideoDao.delete(list.toArray(new RecordingModel[0])))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }
}
