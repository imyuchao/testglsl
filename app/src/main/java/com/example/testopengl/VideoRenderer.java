package com.example.testopengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;

public class VideoRenderer implements GLSurfaceView.Renderer {

    private Context mContext;

    private int mProgram;
    private int aPositionLocation;
    private int aTexCoordLocation;
    private int uTextureMatrixLocation;
    private int uTextureLocation;
    private int uResolutionLocation;
    private int uCenterLocation;
    private int uRadiusLocation;
    private int uSampleCountLocation;

    private int mFboId;
    private int mTextureId;
    private int mVertexBufferId;
    private int mTextureBufferId;
    private int mFrameWidth;
    private int mFrameHeight;
    private float[] mTextureMatrix = new float[16];

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TexCoord;\n" +
                    "uniform mat4 u_TextureMatrix;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = a_Position;\n" +
                    "    v_TexCoord = (u_TextureMatrix * vec4(a_TexCoord, 0, 1)).xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES u_Texture;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(u_Texture, v_TexCoord);\n" +
                    "}\n";

    private static final String BLUR_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TexCoord;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = a_Position;\n" +
                    "    v_TexCoord = a_TexCoord;\n" +
                    "}\n";

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
                    "    vec2 dir = vec2(cos(6.28318531/u_SampleCount), sin(6.28318531/u_SampleCount)) * u_Radius;\n" +
                    "    vec4 color = vec4(0.0);\n" +
                    "    for(float i = 0.0; i < u_SampleCount; i += 1.0) {\n" +
                    "        vec2 offset = dir * i;\n" +
                    "        color += texture2D(u_Texture, v_TexCoord + offset / u_Resolution);\n" +
                    "    }\n" +
                    "    gl_FragColor = color /
        " u_SampleCount;\n"+
                "}\n";

    public VideoRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Load vertex and fragment shaders
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        // Create and link program
        mProgram = createAndLinkProgram(vertexShader, fragmentShader);
        // Get attribute and uniform locations
        aPositionLocation = GLES30.glGetAttribLocation(mProgram, "a_Position");
        aTexCoordLocation = GLES30.glGetAttribLocation(mProgram, "a_TexCoord");
        uTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_TextureMatrix");
        uTextureLocation = GLES30.glGetUniformLocation(mProgram, "u_Texture");

        // Load blur vertex and fragment shaders
        int blurVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, BLUR_VERTEX_SHADER);
        int blurFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, BLUR_FRAGMENT_SHADER);
        // Create and link blur program
        int blurProgram = createAndLinkProgram(blurVertexShader, blurFragmentShader);
        // Get attribute and uniform locations for blur program
        aPositionLocationBlur = GLES30.glGetAttribLocation(blurProgram, "a_Position");
        aTexCoordLocationBlur = GLES30.glGetAttribLocation(blurProgram, "a_TexCoord");
        uTextureLocationBlur = GLES30.glGetUniformLocation(blurProgram, "u_Texture");
        uResolutionLocation = GLES30.glGetUniformLocation(blurProgram, "u_Resolution");
        uCenterLocation = GLES30.glGetUniformLocation(blurProgram, "u_Center");
        uRadiusLocation = GLES30.glGetUniformLocation(blurProgram, "u_Radius");
        uSampleCountLocation = GLES30.glGetUniformLocation(blurProgram, "u_SampleCount");

        // Generate textures
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // Create frame buffer object
        int[] fbos = new int[1];
        GLES30.glGenFramebuffers(1, fbos, 0);
        mFboId = fbos[0];

        // Generate vertex and texture coordinate buffers
        float[] vertices = {
                -1f, -1f, 0f,
                1f, -1f, 0f,
                -1f, 1f, 0f,
                1f, 1f, 0f
        };
        float[] texCoords = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
        };
        int[] buffers = new int[2];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, FloatBuffer.wrap(vertices), GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, texCoords.length * 4, FloatBuffer.wrap(texCoords), GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        mVertexBufferId = buffers[0];
        mTexCoordBufferId = buffers[1];
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // Update texture
        mSurface.updateTexImage();
        // Get texture matrix
        mSurface.getTransformMatrix(textureMatrix);

        // Bind FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFboId);
        // Attach texture to FBO
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId, 0);

        // Set up viewport
        GLES30.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);

        // Clear the screen
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // Draw the texture onto the FBO
        drawTexture(mTextureId, textureMatrix, mProgram);

        // Bind the FBO's texture as input for the blur shader
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        // Draw the texture with a blur effect
        drawFrame(mProgramBlur, mVertexBufferId, mTexCoordBufferId, uTextureLocationBlur, textureMatrix, mSurfaceWidth, mSurfaceHeight);

        // Unbind FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        // Draw the blurred texture onto the screen
        drawTexture(mTextureId, textureMatrix, mProgram);
    }

    private void drawTexture(int textureId, float[] textureMatrix, int program) {
        GLES30.glUseProgram(program);

        // Set vertex attributes
        GLES30.glEnableVertexAttribArray(aPositionLocation);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glEnableVertexAttribArray(aTexCoordLocation);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mTexCoordBufferId);
        GLES30.glVertexAttribPointer(aTexCoordLocation, 2, GLES30.GL_FLOAT, false, 0, 0);

        // Set uniforms
        GLES30.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, textureMatrix, 0);
        GLES30.glUniform1i(uTextureLocation, 0);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Clean up
        GLES30.glDisableVertexAttribArray(aPositionLocation);
        GLES30.glDisableVertexAttribArray(aTexCoordLocation);
    }

}

    private void drawFrame(int program, int vertexBufferId, int texCoordBufferId, int textureLocation, float[] textureMatrix, int width, int height) {
        GLES30.glUseProgram(program);

        // Set vertex attributes
        GLES30.glEnableVertexAttribArray(aPositionLocationBlur);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferId);
        GLES30.glVertexAttribPointer(aPositionLocationBlur, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glEnableVertexAttribArray(aTexCoordLocationBlur);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, texCoordBufferId);
        GLES30.glVertexAttribPointer(aTexCoordLocationBlur, 2, GLES30.GL_FLOAT, false, 0, 0);

        // Set uniforms
        GLES30.glUniformMatrix4fv(uTextureMatrixLocationBlur, 1, false, textureMatrix, 0);
        GLES30.glUniform1i(textureLocation, 0);
        GLES30.glUniform1f(uRadiusLocation, mBlurRadius);
        GLES30.glUniform1f(uAspectRatioLocation, (float) height / (float) width);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Clean up
        GLES30.glDisableVertexAttribArray(aPositionLocationBlur);
        GLES30.glDisableVertexAttribArray(aTexCoordLocationBlur);
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        // Compile shaders
        int vertexShaderId = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShaderId = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Link program
        int programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShaderId);
        GLES30.glAttachShader(programId, fragmentShaderId);
        GLES30.glLinkProgram(programId);

        // Check for errors
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES30.glDeleteProgram(programId);
            throw new RuntimeException("Error creating program: " + GLES30.glGetProgramInfoLog(programId));
        }

        // Clean up
        GLES30.glDeleteShader(vertexShaderId);
        GLES30.glDeleteShader(fragmentShaderId);

        return programId;
    }

    private int compileShader(int type, String shaderCode) {
        int shaderId = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shaderId, shaderCode);
        GLES30.glCompileShader(shaderId);

        // Check for errors
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            GLES30.glDeleteShader(shaderId);
            throw new RuntimeException("Error compiling shader: " + GLES30.glGetShaderInfoLog(shaderId));
        }

        return shaderId;
    }


}