package com.example.testopengl.three;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BlurFilter {

    private static final String VERTEX_SHADER_CODE =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTexCoord = (uTextureMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;\n" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "uniform float uBlurRadius;\n" +
                    "uniform float uAspectRatio;\n" +
                    "const float PI = 3.14159265359;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTexCoord;\n" +
                    "    vec2 texSize = vec2(textureSize(uTexture, 0));\n" +
                    "    float aspectRatio = uAspectRatio;\n" +
                    "    float blurRadius = uBlurRadius;\n" +
                    "    float sigma = blurRadius / 2.0;\n" +
                    "    float alpha = 2.0 * sigma * sigma;\n" +
                    "    float beta = -1.0 / alpha;\n" +
                    "    float gamma = sqrt(beta * beta - 1.0 / (PI * alpha));\n" +
                    "    float kernelSum = 0.0;\n" +
                    "    vec4 blurColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                    "    for (float x = -5.0; x <= 5.0; x += 1.0) {\n" +
                    "        float u = texCoord.x + x * (1.0 / texSize.x) * aspectRatio;\n" +
                    "        float weight = gamma * exp(beta * x * x);\n" +
                    "        blurColor += texture(uTexture, vec2(u, texCoord.y)) * weight;\n" +
                    "        kernelSum += weight;\n" +
                    "    }\n" +
                    "    gl_FragColor = blurColor / kernelSum;\n" +
                    "}";

    private static final int COORDS_PER_VERTEX = 2;
    private static final int TEX_COORDS_PER_VERTEX = 2;
    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
    private static final int TEX_COORD_STRIDE = TEX_COORDS_PER_VERTEX * 4;
    private static final float[] VERTEX_COORDS = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    private static final float[] TEX_COORDS = {
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    private Context mContext;
    private int mProgram;
    private int mMVPMatrixLocation;
    private int mTextureMatrixLocation;
    private int mTextureLocation;
    private int mBlurRadiusLocation;
    private int mAspectRatioLocation;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mMVPMatrixHandle;
    private int mTextureMatrixHandle;
    private int mTextureHandle;
    private int mBlurRadiusHandle;
    private int mAspectRatioHandle;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;
    private float[] mMVPMatrix = new float[16];
    private float[] mTextureMatrix = new float[16];
    private int mWidth;
    private int mHeight;
    private float mAspectRatio;

    public BlurFilter(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_COORDS);
        mVertexBuffer.position(0);

        mTexCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_COORDS);
        mTexCoordBuffer.position(0);

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vertexShader);
        GLES30.glAttachShader(mProgram, fragmentShader);
        GLES30.glLinkProgram(mProgram);

        mMVPMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix");
        mTextureLocation = GLES30.glGetUniformLocation(mProgram, "uTexture");
        mBlurRadiusLocation = GLES30.glGetUniformLocation(mProgram, "uBlurRadius");
        mAspectRatioLocation = GLES30.glGetUniformLocation(mProgram, "uAspectRatio");
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordHandle = GLES30.glGetAttribLocation(mProgram, "aTexCoord");

        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(mTextureMatrix, 0);
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        mAspectRatio = (float) width / height;
    }

    public void drawFrame(int textureId) {
        GLES30.glUseProgram(mProgram);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(mTextureLocation, 0);
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mTextureMatrixHandle, 1, false, mTextureMatrix, 0);
        GLES30.glUniform1f(mBlurRadiusHandle, 20f);
        GLES30.glUniform1f(mAspectRatioHandle, mAspectRatio);

        mVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        mTexCoordBuffer.position(0);
        GLES30.glVertexAttribPointer(mTexCoordHandle, TEX_COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, TEX_COORDS_STRIDE, mTexCoordBuffer);
        GLES30.glEnableVertexAttribArray(mTexCoordHandle);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }

    public void drawTexture(int textureId, float[] textureMatrix) {
        GLES30.glUseProgram(mProgram);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(mTextureLocation, 0);
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mTextureMatrixHandle, 1, false, textureMatrix, 0);
        GLES30.glUniform1f(mBlurRadiusHandle, 20f);
        GLES30.glUniform1f(mAspectRatioHandle, mAspectRatio);

        mVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        mTexCoordBuffer.position(0);
        GLES30.glVertexAttribPointer(mTexCoordHandle, TEX_COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, TEX_COORDS_STRIDE, mTexCoordBuffer);
        GLES30.glEnableVertexAttribArray(mTexCoordHandle);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        return program;
    }

    /*
    在这个实现中，我们首先创建了一个 MyRenderer 类来实现 GLSurfaceView.Renderer 接口，并在构造函数中创建了 BlurFilter 和 MediaPlayer 的实例。

在 onSurfaceCreated 回调中，我们初始化了 BlurFilter，并启动了 MediaPlayer。

在 onSurfaceChanged 回调中，我们调用了 BlurFilter 的 setAspectRatio 方法来设置纹理的宽高比，并调用了 GLES30.glViewport 方法来设置视口大小。

在 onDrawFrame 回调中，我们首先调用 SurfaceTexture 的 updateTexImage 方法来更新纹理图像和纹理变换矩阵，然后将纹理和纹理变换矩

阵传递给了 BlurFilter 的 drawTexture 方法来应用径向模糊。

最后，在 setSurfaceTexture 方法中，我们创建了一个新的纹理对象并将其与 SurfaceTexture 绑定，然后设置了 SurfaceTexture 的默认缓冲区大小和帧可用监听器，在每次帧可用时更新纹理和纹理变换矩阵。

这个实现可以用于一个 GLSurfaceView 控件，通过 setRenderer 方法将其与 MyRenderer 实例关联。我们还需要在 Activity 中创建一个 GLSurfaceView，并在 onResume 和 onPause 方法中分别调用 GLSurfaceView 的 onResume 和 onPause 方法来控制 GLSurfaceView 的生命周期。
     */
}
