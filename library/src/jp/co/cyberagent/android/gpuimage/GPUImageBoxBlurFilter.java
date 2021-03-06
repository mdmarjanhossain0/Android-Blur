/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

import java.util.Arrays;

/**
 * A hardware-accelerated 9-hit box blur of an image
 * <p/>
 * scaling: for the size of the applied blur, default of 1.0
 */
public class GPUImageBoxBlurFilter extends GPUImageTwoPassTextureSamplingFilter {


    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec2 inputTextureCoordinate;\n" +
                    "\n" +
                    "uniform float texelWidthOffset; \n" +
                    "uniform float texelHeightOffset; \n" +
                    "\n" +
                    "varying vec2 centerTextureCoordinate;\n" +
                    "varying vec2 oneStepLeftTextureCoordinate;\n" +
                    "varying vec2 twoStepsLeftTextureCoordinate;\n" +
                    "varying vec2 oneStepRightTextureCoordinate;\n" +
                    "varying vec2 twoStepsRightTextureCoordinate;\n" +
                    "varying float log;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "log = -10.0;\n" +
                    "gl_Position = position;\n" +
                    "vec2 firstOffset;\n" +
                    "vec2 secondOffset;\n" +
                    "firstOffset = vec2(1.5 * texelWidthOffset, 1.5 * texelHeightOffset);\n" +
                    "secondOffset = vec2(3.5 * texelWidthOffset, 3.5 * texelHeightOffset);\n" +
                    "\n" +
                    "centerTextureCoordinate = inputTextureCoordinate;\n" +
                    "oneStepLeftTextureCoordinate = inputTextureCoordinate - firstOffset;\n" +
                    "twoStepsLeftTextureCoordinate = inputTextureCoordinate - secondOffset;\n" +
                    "oneStepRightTextureCoordinate = inputTextureCoordinate + firstOffset;\n" +
                    "twoStepsRightTextureCoordinate = inputTextureCoordinate + secondOffset;\n" +
                    "}\n";


    public static final String FRAGMENT_SHADER =
            "precision highp float;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "uniform float texelWidthOffset; \n" +
                    "uniform float texelHeightOffset; \n" +
                    "uniform float screenRatio; \n" +
                    "uniform vec2 focusLocation; \n" +
                    "\n" +
                    "varying vec2 centerTextureCoordinate;\n" +
                    "varying vec2 oneStepLeftTextureCoordinate;\n" +
                    "varying vec2 twoStepsLeftTextureCoordinate;\n" +
                    "varying vec2 oneStepRightTextureCoordinate;\n" +
                    "varying vec2 twoStepsRightTextureCoordinate;\n" +

                    "\n" +
                    "vec2 oneStepLeftTextureCoordinateLocal;\n" +
                    "vec2 twoStepsLeftTextureCoordinateLocal;\n" +
                    "vec2 oneStepRightTextureCoordinateLocal;\n" +
                    "vec2 twoStepsRightTextureCoordinateLocal;\n" +
                    "varying float log;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "if (log != -10.0) {\n" +
                    "gl_FragColor = vec4(log, 0.0, 0.0, 1.0);\n" +
                    "return;\n" +
                    "}\n" +

                    "lowp vec4 fragmentColor;\n" +
                    "float caliY =1.0/screenRatio*(gl_PointCoord.y - focusLocation.y); \n" +
                    "float radius = sqrt(pow(abs(gl_PointCoord.x - focusLocation.x), 2.0) + pow(abs(caliY), 2.0));\n" +
                    "float centerRadius = 0.2;\n" +
                    "if (radius < centerRadius) {\n" +
                    "fragmentColor = texture2D(inputImageTexture, centerTextureCoordinate);\n" +
                    "} else {\n" +
                    "float weight = min(pow(radius/1.0, 2.0) * 1.4, 2.0);\n" +
                    "vec2 firstOffset = vec2(1.5 * weight * texelWidthOffset, 1.5 * weight * texelHeightOffset);\n" +
                    "vec2 secondOffset = vec2(3.5 * weight * texelWidthOffset, 3.5 * weight * texelHeightOffset);\n" +

                    "oneStepLeftTextureCoordinateLocal = centerTextureCoordinate - firstOffset;\n" +
                    "twoStepsLeftTextureCoordinateLocal = centerTextureCoordinate - secondOffset;\n" +
                    "oneStepRightTextureCoordinateLocal = centerTextureCoordinate + firstOffset;\n" +
                    "twoStepsRightTextureCoordinateLocal = centerTextureCoordinate + secondOffset;\n" +

                    "fragmentColor = texture2D(inputImageTexture, centerTextureCoordinate) * 0.2;\n" +
                    "fragmentColor += texture2D(inputImageTexture, oneStepLeftTextureCoordinateLocal) * 0.2;\n" +
                    "fragmentColor += texture2D(inputImageTexture, oneStepRightTextureCoordinateLocal) * 0.2;\n" +
                    "fragmentColor += texture2D(inputImageTexture, twoStepsLeftTextureCoordinateLocal) * 0.2;\n" +
                    "fragmentColor += texture2D(inputImageTexture, twoStepsRightTextureCoordinateLocal) * 0.2;\n" +
                    "}\n" +
                    "gl_FragColor = fragmentColor;\n" +
                    "}\n";

    private float blurSize = 1f;

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        float ratio = (float) mOutputWidth / mOutputHeight;

        GPUImageFilter filter = mFilters.get(0);
        int screenRatioLocation = GLES20.glGetUniformLocation(filter.getProgram(), "screenRatio");
        filter.setFloat(screenRatioLocation, ratio);

        filter = mFilters.get(1);
        screenRatioLocation = GLES20.glGetUniformLocation(filter.getProgram(), "screenRatio");
        filter.setFloat(screenRatioLocation, ratio);
    }

    /**
     * Construct new BoxBlurFilter with default blur size of 1.0.
     */
    public GPUImageBoxBlurFilter() {
        this(1f);
    }


    public GPUImageBoxBlurFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        this.blurSize = blurSize;
    }

    /**
     * A scaling for the size of the applied blur, default of 1.0
     *
     * @param blurSize
     */
    public void setBlurSize(float blurSize) {
        this.blurSize = blurSize;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return blurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return blurSize;
    }

    public void focus(final float x, final float y) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                float[] focusLocation = new float[]{normalize(x, mOutputWidth), -normalize(y, mOutputHeight)};
                GPUImageFilter filter = mFilters.get(0);
                int screenRatioLocation = GLES20.glGetUniformLocation(filter.getProgram(), "focusLocation");
                filter.setFloatVec2(screenRatioLocation, focusLocation);

                filter = mFilters.get(1);
                screenRatioLocation = GLES20.glGetUniformLocation(filter.getProgram(), "focusLocation");
                filter.setFloatVec2(screenRatioLocation, focusLocation);
                initTexelOffsets();
            }
        });
    }

    private float normalize(float x, int outputWidth) {
        return x / outputWidth * 2 - 1;
    }
}
