package com.example.testopengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer  implements GLSurfaceView.Renderer {

    private int[] textureIds = new int[3];
    private int width;
    private int height;
    private float centerX;
    private float centerY;
    private float radius;
    private float blurRadius;
    private int numSamples;
    private int blurProgram;
    private int blurRadiusLocation;
    private int blurDirectionLocation;
    private int[] frameBufferIds = new int[1];
    private int[] renderBufferIds = new int[1];

    Context mcontext;


    public MyRenderer(Context context) {
        mcontext = context;
        // 初始化帧缓冲对象、纹理对象和缓冲区对象
        int[] fboIds = new int[1];
        int[] textureIds = new int[1];
        int[] bufferIds = new int[2];
        GLES31.glGenFramebuffers(1, fboIds, 0);
        GLES31.glGenTextures(1, textureIds, 0);
        GLES31.glGenBuffers(2, bufferIds, 0);
        mFboId = fboIds[0];
        mTextureId = textureIds[0];
        vertexBufferId = bufferIds[0];
        textureBufferId = bufferIds[1];
    }

    int vertexBufferId;
    int textureBufferId;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // 初始化纹理
        GLES31.glGenTextures(3, textureIds, 0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureIds[0]);
        // 设置纹理参数
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        // 加载视频帧作为纹理
        // ...

        // 创建着色器程序
        // ...

        // 创建高斯模糊着色器程序
        String blurVertexShader = "#version 310 es\n" +
                "\n" +
                "in vec4 a_Position;\n" +
                "in vec2 a_TexCoord;\n" +
                "\n" +
                "out vec2 v_TexCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "}\n";
        String blurFragmentShader = "#version 310 es\n" +
                "\n" +
                "precision mediump float;\n" +
                "\n" +
                "in vec2 v_TexCoord;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "\n" +
                "uniform vec2 u_Direction;\n" +
                "uniform float u_Radius;\n" +
                "\n" +
                "out vec4 o_Color;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                "    float totalWeight = 0.0;\n" +
                "    vec2 offset = vec2(0.0, 0.0);\n" +
                "    vec2 texCoord = v_TexCoord;\n" +
                "\n" +
                "    for (int i = -5; i <= 5; i++) {\n" +
                "        float weight = exp(-float(i * i) / (2.0 * u_Radius * u_Radius));\n" +
                "        totalWeight += weight;\n" +
                "\n" +
                "        offset = vec2(float(i) * u_Direction.x, float(i) * u_Direction.y);\n" +
                "        color += texture(u_Texture, texCoord + offset) * weight;\n" +
                "    }\n" +
                "\n" +
                "    o_Color = color / totalWeight;\n" +
                "}\n";
        blurProgram = createProgram(BLUR_VERTEX_SHADER, BLUR_FRAGMENT_SHADER);
        blurRadiusLocation = GLES31.glGetUniformLocation(blurProgram, "u_Radius");
        blurDirectionLocation = GLES31.glGetUniformLocation(blurProgram, "u_Direction");

        // 获取属性和统一变量位置
        positionLocation = GLES31.glGetAttribLocation(blurProgram, "a_Position");
        textureCoordLocation = GLES31.glGetAttribLocation(blurProgram, "u_Texture");

    }

    private static final String BLUR_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TexCoord;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    " gl_Position = a_Position;\n" +
                    " v_TexCoord = a_TexCoord;\n" +
                    "}\n";

    // blurFragmentShader
    private static final String BLUR_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES u_Texture;\n" +
                    "uniform vec2 u_Resolution;\n" +
                    "uniform vec2 u_Center;\n" +
                    "uniform float u_Radius;\n" +
                    "uniform float u_SampleCount;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    " vec2 dir = vec2(cos(6.28318531/u_SampleCount), sin(6.28318531/u_SampleCount)) * u_Radius;\n" +
                    " vec4 color = vec4(0.0);\n" +
                    " for(float i = 0.0; i < u_SampleCount; i += 1.0) {\n" +
                    " vec2 offset = dir * i;\n" +
                    " color += texture2D(u_Texture, v_TexCoord + offset / u_Resolution);\n" +
                    " }\n" +
                    " gl_FragColor = color / u_SampleCount;\n" +
                    "}\n";
    int positionLocation;
    int textureCoordLocation;

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        this.width = width;
        this.height = height;
        centerX = width / 2f;
        centerY = height / 2f;
        radius = Math.min(width, height) / 3f;
        blurRadius = 20f;
        numSamples = 10;

        // 设置视口
        GLES31.glViewport(0, 0, width, height);

        // 创建帧缓冲和渲染缓冲
        GLES31.glGenFramebuffers(1, frameBufferIds, 0);
        GLES31.glGenRenderbuffers(1, renderBufferIds, 0);
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, renderBufferIds[0]);
        GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, GLES31.GL_DEPTH_COMPONENT16, width, height);
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBufferIds[0]);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textureIds[1], 0);
        GLES31.glFramebufferRenderbuffer(GLES31.GL_FRAMEBUFFER, GLES31.GL_DEPTH_ATTACHMENT, GLES31.GL_RENDERBUFFER, renderBufferIds[0]);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);

        // 绘制视频帧
        drawFrame();

        // 进行高斯模糊
        GLES31.glUseProgram(blurProgram);
        GLES31.glUniform1f(blurRadiusLocation, blurRadius);
        for (int i = 0; i < numSamples; i++) {
            // 水平方向模糊
            GLES31.glUniform2f(blurDirectionLocation, 1f / width, 0f);
            blur(textureIds[1], textureIds[2], width, height);

            // 垂直方向模糊
            GLES31.glUniform2f(blurDirectionLocation, 0f, 1f / height);
            blur(textureIds[2], textureIds[1], width, height);
        }

        // 绘制模糊后的纹理
        drawTexture(textureIds[1]);
    }

    private void drawFrame() {
        SurfaceTexture surfaceTexture = mSurface.getSurfaceTexture();
        surfaceTexture.updateTexImage();

        // 获取纹理变换矩阵
        float[] textureTransformMatrix = new float[16];
        surfaceTexture.getTransformMatrix(textureTransformMatrix);

        // 绑定顶点坐标和纹理坐标缓冲区
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vertexBufferId);
        GLES31.glVertexAttribPointer(positionLocation, 2, GLES31.GL_FLOAT, false, 0, 0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, textureBufferId);
        GLES31.glVertexAttribPointer(textureCoordLocation, 2, GLES31.GL_FLOAT, false, 0, 0);

        // 启用顶点坐标和纹理坐标属性
        GLES31.glEnableVertexAttribArray(positionLocation);
        GLES31.glEnableVertexAttribArray(textureCoordLocation);

        // 绑定纹理
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0]);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);

        // 设置纹理变换矩阵
        GLES31.glUniformMatrix4fv(textureMatrixLocation, 1, false, textureTransformMatrix, 0);

        // 绘制
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

        // 禁用顶点坐标和纹理坐标属性
        GLES31.glDisableVertexAttribArray(positionLocation);
        GLES31.glDisableVertexAttribArray(textureCoordLocation);
    }


    private void blur(int inputTextureId, int outputTextureId, int width, int height) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, frameBufferIds[0]);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, outputTextureId, 0);
        drawTexture(inputTextureId);
    }

    private void drawTexture(int textureId) {
        // 绑定顶点坐标和纹理坐标缓冲区
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vertexBufferId);
        GLES31.glVertexAttribPointer(positionLocation, 2, GLES31.GL_FLOAT, false, 0, 0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, textureBufferId);
        GLES31.glVertexAttribPointer(textureCoordLocation, 2, GLES31.GL_FLOAT, false, 0, 0);
        // 启用顶点坐标和纹理坐标属性
        GLES31.glEnableVertexAttribArray(positionLocation);
        GLES31.glEnableVertexAttribArray(textureCoordLocation);

// 绑定纹理
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);

// 绘制
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

// 禁用顶点坐标和纹理坐标属性
        GLES31.glDisableVertexAttribArray(positionLocation);
        GLES31.glDisableVertexAttribArray(textureCoordLocation);

    }


    private int createProgram(String vertexShaderSource, String fragmentShaderSource) {
// 创建顶点着色器
        int vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        GLES31.glShaderSource(vertexShader, vertexShaderSource);
        GLES31.glCompileShader(vertexShader);// 创建片元着色器
        int fragmentShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        GLES31.glShaderSource(fragmentShader, fragmentShaderSource);
        GLES31.glCompileShader(fragmentShader);

// 创建程序对象
        int program = GLES31.glCreateProgram();
        GLES31.glAttachShader(program, vertexShader);
        GLES31.glAttachShader(program, fragmentShader);

// 绑定属性位置
        GLES31.glBindAttribLocation(program, positionLocation, "a_Position");
        GLES31.glBindAttribLocation(program, textureCoordLocation, "a_TexCoord");

// 链接程序
        GLES31.glLinkProgram(program);

        return program;

    }
}