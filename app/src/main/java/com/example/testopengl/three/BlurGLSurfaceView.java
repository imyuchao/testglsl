package com.example.testopengl.three;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BlurGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture mSurfaceTexture;
    private int mTextureId;
    private BlurFilter mBlurFilter;
    private HandlerThread mRenderThread;
    private Handler mRenderHandler;
    private float[] mTextureMatrix = new float[16];
    private boolean mUpdateSurface = false;
    private float mBlurRadius = 20.0f;
    private float mAspectRatio = 1.0f;

    public BlurGLSurfaceView(Context context) {
        this(context, null);
    }

    public BlurGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3); // Request an OpenGL ES 3.0 compatible context.
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // Only render when we have a frame to display.
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Create SurfaceTexture and get texture ID
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        // Create blur filter
        mBlurFilter = new BlurFilter(getContext());

        // Create render thread and handler
        mRenderThread = new HandlerThread("RenderThread");
        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper());

        // Enable transparency
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mAspectRatio = (float) height / (float) width;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // Update surface texture
        if (mUpdateSurface) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTextureMatrix);
            mUpdateSurface = false;
        }

        // Render blur
        mRenderHandler.post(() -> {
            mBlurFilter.draw(mTextureId, mTextureMatrix, getWidth(), getHeight());
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mUpdateSurface = true;
        requestRender();
    }

    public void setBlurRadius(float radius) {
        mBlurRadius = radius;
        requestRender();
    }
}

