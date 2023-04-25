package com.example.testopengl.three;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BlurFilter {

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTextureCoord = aTextureCoord.xy;\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform sampler2D uTextureSampler;\n" +
                    "uniform highp float uRadius;\n" +
                    "uniform highp vec2 uTextureOffset;\n" +
                    "void main() {\n" +
                    "    highp vec4 sum = vec4(0.0);\n" +
                    "    highp vec2 textureCoord = vTextureCoord.xy;\n" +
                    "    for (int x = -4; x <= 4; x++) {\n" +
                    "        for (int y = -4; y <= 4; y++) {\n" +
                    "            highp vec2 offset = vec2(float(x), float(y)) * uTextureOffset;\n" +
                    "            sum += texture2D(uTextureSampler, textureCoord + offset);\n" +
                    "        }\n" +
                    "    }\n" +
                    "    gl_FragColor = sum / 81.0;\n" +
                    "}";

    private int mProgram;
    private int mRadiusLocation;
    private int mTextureSamplerLocation;
    private int mTextureOffsetLocation;

    private int mTextureWidth;
    private int mTextureHeight;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureCoordBuffer;

    private int[] mTextures = new int[1];

    public BlurFilter() {
        mVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(new float[]{
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        }).position(0);

        mTextureCoordBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoordBuffer.put(new float[]{
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        }).position(0);

        createProgram();
    }

    public void setTextureSize(int width, int height) {
        mTextureWidth = width;
        mTextureHeight = height;
        GLES30.glViewport(0, 0, width, height);
    }

    public void setRadius(float radius) {
        GLES30.glUseProgram(mProgram);
        GLES30.glUniform1f(mRadiusLocation, radius);
    }

    public void drawTexture(int texture, float[] textureMatrix) {
        GLES30.glUseProgram(mProgram);    // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
        GLES30.glUniform1i(mTextureSamplerLocation, 0);

        // Set texture matrix
        GLES30.glUniformMatrix4fv(mTextureMatrixLocation, 1, false, textureMatrix, 0);

        // Draw texture
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, mVertexBuffer);

        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, mTextureCoordBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    private void createProgram() {
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mRadiusLocation = GLES30.glGetUniformLocation(mProgram, "uRadius");
        mTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "uTextureSampler");
        mTextureOffsetLocation = GLES30.glGetUniformLocation(mProgram, "uTextureOffset");
        mTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix");
    }

    private int createProgram(String vertexShader, String fragmentShader) {
        int vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);
        int programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShaderId);
        GLES30.glAttachShader(programId, fragmentShaderId);
        GLES30.glLinkProgram(programId);
        return programId;
    }

    private int loadShader(int type, String shaderCode) {
        int shaderId = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shaderId, shaderCode);
        GLES30.glCompileShader(shaderId);
        return shaderId;
    }
}