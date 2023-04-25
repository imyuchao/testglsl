package com.example.testopengl.three;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.EGLConfig;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    private BlurFilter mBlurFilter;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureId;
    private MediaPlayer mMediaPlayer;

    public MyRenderer(Context context) {
        mBlurFilter = new BlurFilter(context);
        mMediaPlayer = MediaPlayer.create(context, R.raw.my_video);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mBlurFilter.init();
        mMediaPlayer.start();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mBlurFilter.setAspectRatio((float) width / height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // 更新 SurfaceTexture
        mSurfaceTexture.updateTexImage();
        float[] textureMatrix = new float[16];
        mSurfaceTexture.getTransformMatrix(textureMatrix);

        // 应用径向模糊
        mBlurFilter.drawTexture(mTextureId, textureMatrix);
    }

    @Override
    public void onSurfaceDestroyed() {
        mMediaPlayer.release();
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        mTextureId = createTextureId();
        surfaceTexture.setDefaultBufferSize(mBlurFilter.getWidth(), mBlurFilter.getHeight());
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTextureMatrix);
            }
        });
    }

    private int createTextureId() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        return textures[0];
    }
}
