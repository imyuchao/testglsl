package com.example.testopengl;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG ="MainActivity";

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int REQUEST_CODE_PICK_Video = 2;
    private ImageView imageView;
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);

        // 检查是否有读取外部存储器的权限
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，则请求权限
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    3
            );
        }

        // 点击按钮打开相册
        findViewById(R.id.button).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        findViewById(R.id.button2).setOnClickListener(view -> {

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_Video);

        });


    }

    // 处理相册返回的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Log.i(TAG, "onActivityResult: 图片          --- ");

            // 获取选取的图片的Uri
            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == REQUEST_CODE_PICK_Video && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            String path = getRealPathFromURI(uri);
            Log.i(TAG, "onActivityResult: 视频          ---" + path);
            // 处理视频文件路径
            videoView.setVideoPath(path);
            videoView.start();
        }
    }

    private List<String> getPath() {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        List<String> videoPaths = new ArrayList<String>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                videoPaths.add(path);
            }
            cursor.close();
        }
        return videoPaths;
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            return uri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            String path = cursor.getString(index);
            cursor.close();
            return path;
        }
    }
}