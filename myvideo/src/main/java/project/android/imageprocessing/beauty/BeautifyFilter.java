package project.android.imageprocessing.beauty;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

import project.android.imageprocessing.filter.BasicFilter;

/**
 * Created by Administrator on 2017-08-16.
 */

public class BeautifyFilter extends BasicFilter {

    @Override
    protected String getFragmentShader() {
        return "   varying highp vec2 " + VARYING_TEXCOORD + ";\n" +
                "\n" +
                "    uniform sampler2D " + UNIFORM_TEXTURE0 + ";\n" +
                "\n" +
                "    const highp vec2 singleStepOffset = vec2(0.0018518518, 0.0012722646);\n" +
                "    uniform highp vec4 params;\n" +
                "    uniform highp float brightness;\n" +
                "\n" +
                "    const highp vec3 W = vec3(0.299, 0.587, 0.114);\n" +
                "    const highp mat3 saturateMatrix = mat3(\n" +
                "        1.1102, -0.0598, -0.061,\n" +
                "        -0.0774, 1.0826, -0.1186,\n" +
                "        -0.0228, -0.0228, 1.1772);\n" +
                "    highp vec2 blurCoordinates[24];\n" +
                "\n" +
                "    highp float hardLight(highp float color) {\n" +
                "    if (color <= 0.5)\n" +
                "        color = color * color * 2.0;\n" +
                "    else\n" +
                "        color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
                "    return color;\n" +
                "}\n" +
                "\n" +
                "    void main(){\n" +
                "    highp vec3 centralColor = texture2D(" + UNIFORM_TEXTURE0 + ", " + VARYING_TEXCOORD + ").rgb;\n" +
                "    blurCoordinates[0] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(0.0, -10.0);\n" +
                "    blurCoordinates[1] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(0.0, 10.0);\n" +
                "    blurCoordinates[2] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
                "    blurCoordinates[3] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(10.0, 0.0);\n" +
                "    blurCoordinates[4] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(5.0, -8.0);\n" +
                "    blurCoordinates[5] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(5.0, 8.0);\n" +
                "    blurCoordinates[6] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
                "    blurCoordinates[7] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
                "    blurCoordinates[8] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(8.0, -5.0);\n" +
                "    blurCoordinates[9] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(8.0, 5.0);\n" +
                "    blurCoordinates[10] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-8.0, 5.0);\n" +
                "    blurCoordinates[11] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
                "    blurCoordinates[12] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(0.0, -6.0);\n" +
                "    blurCoordinates[13] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(0.0, 6.0);\n" +
                "    blurCoordinates[14] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(6.0, 0.0);\n" +
                "    blurCoordinates[15] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
                "    blurCoordinates[16] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
                "    blurCoordinates[17] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
                "    blurCoordinates[18] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(4.0, -4.0);\n" +
                "    blurCoordinates[19] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(4.0, 4.0);\n" +
                "    blurCoordinates[20] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-2.0, -2.0);\n" +
                "    blurCoordinates[21] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(-2.0, 2.0);\n" +
                "    blurCoordinates[22] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(2.0, -2.0);\n" +
                "    blurCoordinates[23] = " + VARYING_TEXCOORD + ".xy + singleStepOffset * vec2(2.0, 2.0);\n" +
                "\n" +
                "    highp float sampleColor = centralColor.g * 22.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[0]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[1]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[2]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[3]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[4]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[5]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[6]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[7]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[8]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[9]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[10]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[11]).g;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[12]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[13]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[14]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[15]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[16]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[17]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[18]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[19]).g * 2.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[20]).g * 3.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[21]).g * 3.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[22]).g * 3.0;\n" +
                "    sampleColor += texture2D(" + UNIFORM_TEXTURE0 + ", blurCoordinates[23]).g * 3.0;\n" +
                "\n" +
                "    sampleColor = sampleColor / 62.0;\n" +
                "\n" +
                "    highp float highPass = centralColor.g - sampleColor + 0.5;\n" +
                "\n" +
                "    for (int i = 0; i < 5; i++) {\n" +
                "        highPass = hardLight(highPass);\n" +
                "    }\n" +
                "    highp float lumance = dot(centralColor, W);\n" +
                "\n" +
                "    highp float alpha = pow(lumance, params.r);\n" +
                "\n" +
                "    highp vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;\n" +
                "\n" +
                "    smoothColor.r = clamp(pow(smoothColor.r, params.g), 0.0, 1.0);\n" +
                "    smoothColor.g = clamp(pow(smoothColor.g, params.g), 0.0, 1.0);\n" +
                "    smoothColor.b = clamp(pow(smoothColor.b, params.g), 0.0, 1.0);\n" +
                "\n" +
                "    highp vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
                "    highp vec3 bianliang = max(smoothColor, centralColor);\n" +
                "    highp vec3 rouguang = 2.0*centralColor*smoothColor + centralColor*centralColor - 2.0*centralColor*centralColor*smoothColor;\n" +
                "\n" +
                "    gl_FragColor = vec4(mix(centralColor, lvse, alpha), 1.0);\n" +
                "    gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, alpha);\n" +
                "    gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, params.b);\n" +
                "\n" +
                "    highp vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
                "    gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, params.a);\n" +
                "    gl_FragColor.rgb = vec3(gl_FragColor.rgb + vec3(brightness));\n" +
                "}";
    }


    private float[] params;
    private int paramsHandle;
    private int brightnessHandle;
    private float mBrightLevel;

    public BeautifyFilter() {
        setBrightLevel(0.56f);
        setAmount(0.24f);
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        paramsHandle = GLES20.glGetUniformLocation(programHandle, "params");
        brightnessHandle = GLES20.glGetUniformLocation(programHandle, "brightness");
    }

    @Override
    protected void passShaderValues() {
        super.passShaderValues();
        GLES20.glUniform4fv(paramsHandle, 1, FloatBuffer.wrap(params));
        GLES20.glUniform1f(brightnessHandle, mBrightLevel);
    }

    public void setBrightLevel(float brightLevel) {
        mBrightLevel = 0.6f * (-0.5f + brightLevel);
    }

    public void setAmount(float beauty) {
        params = new float[4];
        params[0] = 1.0f - 0.6f * beauty;
        params[1] = 1.0f - 0.3f * beauty;
        params[2] = 0.1f + 0.3f * 0.47f;
        params[3] = 0.1f + 0.3f * 0.47f;
    }

}
