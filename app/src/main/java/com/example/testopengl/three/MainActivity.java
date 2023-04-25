package com.example.testopengl.three;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;
import androidx.appcompat.app.AppCompatActivity;
import com.example.testopengl.R;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {
    private GLSurfaceView mGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;
    private MyRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        mRenderer = new MyRenderer(this);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        mSurfaceTexture = new SurfaceTexture(mRenderer.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.my_video);
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.setSurface(new Surface(mSurfaceTexture));
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
        mSurfaceTexture.release();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGLSurfaceView.requestRender();
    }
    /**
     * 这个 Activity 实现创建了一个 GLSurfaceView 并将其与 MyRenderer 关联。在 onResume 方法中，我们创建了一个新的 SurfaceTexture，并将其与 MediaPlayer 关联来播放视频。在 onPause 方法中，我们释放了 SurfaceTexture 对象。在 onFrameAvailable 方法中，我们请求 GLSurfaceView 进行渲染。
     *
     * 这就是在 Android 31 中使用 GLSurfaceView.Renderer 实现对视频的径向模糊处理的示例。这个示例可以作为一个基础框架，用来实现更加复杂和高级的图像处理效果。
     */
}
