package com.example.testopengl.three;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BlurFilter {

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "void main() {\n" +
                    "    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
                    "    gl_Position = aPosition;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform sampler2D uTextureSampler;\n" +
                    "uniform float uRadius;\n" +
                    "uniform vec2 uTextureOffset;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    vec4 color = vec4(0.0);\n" +
                    "    float totalWeight = 0.0;\n" +
                    "    for (float dx = -uRadius; dx <= uRadius; dx += uTextureOffset.x) {\n" +
                    "        for (float dy = -uRadius; dy <= uRadius; dy += uTextureOffset.y) {\n" +
                    "            vec2 offset = vec2(dx, dy);\n" +
                    "            float weight = exp(-(dx * dx + dy * dy) / (2.0 * uRadius * uRadius));\n" +
                    "            color += texture2D(uTextureSampler, vTextureCoord + offset);\n" +
                    "            totalWeight += weight;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    gl_FragColor = color / totalWeight;\n" +
                    "}\n";

    private int mProgram;
    private int mRadiusLocation;
    private int mTextureSamplerLocation;
    private int mTextureOffsetLocation;
    private int mTextureMatrixLocation;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureCoordBuffer;

    private float mRadius;
    private float[] mTextureOffset = new float[2];

    public BlurFilter() {
        mRadius = 10f;
        mTextureOffset[0] = 0f;
        mTextureOffset[1] = 0f;

        // Set up vertex buffer
        float[] vertexData = {
                -1f, 1f,    // top left
                -1f, -1f,   // bottom left
                1f, 1f,     // top right
                1f, -1f     // bottom right
        };
        mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(vertexData);
        mVertexBuffer.position(0);

        // Set up texture coordinate buffer
        float[] textureCoordData = {
                0f, 0f,     // top left
                0f, 1f,     // bottom left
                1f, 0f,     // top right
                1f, 1f      // bottom right
        };
        mTextureCoordBuffer = ByteBuffer.allocateDirect(textureCoordData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureCoordBuffer.put(texture,CoordData)
        CoordData);
        mTextureCoordBuffer.position(0);

        // Create shader program
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        // Get location of shader uniform variables
        mRadiusLocation = GLES30.glGetUniformLocation(mProgram, "uRadius");
        mTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "uTextureSampler");
        mTextureOffsetLocation = GLES30.glGetUniformLocation(mProgram, "uTextureOffset");
        mTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix");
    }

    public void setRadius(float radius) {
        mRadius = radius;
    }

    public void setTextureOffset(float x, float y) {
        mTextureOffset[0] = x;
        mTextureOffset[1] = y;
    }

    public void draw(FrameBufferObject fbo, int textureId, float[] textureMatrix) {
        GLES30.glUseProgram(mProgram);

        // Set up vertex and texture coordinate buffers
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, mTextureCoordBuffer);
        GLES30.glEnableVertexAttribArray(1);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(mTextureSamplerLocation, 0);

        // Set up shader uniforms
        GLES30.glUniform1f(mRadiusLocation, mRadius);
        GLES30.glUniform2fv(mTextureOffsetLocation, 1, mTextureOffset, 0);
        GLES30.glUniformMatrix4fv(mTextureMatrixLocation, 1, false, textureMatrix, 0);

        // Draw to FBO
        fbo.bind();
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        fbo.unbind();
    }

    private static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            String error = GLES30.glGetProgramInfoLog(program);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Error linking program: " + error);
        }

        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] != GLES30.GL_TRUE) {
            String error = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader: " + error);
        }

        return shader;
    }
}

/*
FrameBufferObject 不是 Android 系统自带的类，它是开发者自己实现的一个类，通常用于 OpenGL ES 中离屏渲染的场景，可以将渲染结果保存在一个纹理（texture）中，而不是直接渲染到屏幕上。在 Android 中，这个类通常会被定义在开发者自己的项目包下。
 */