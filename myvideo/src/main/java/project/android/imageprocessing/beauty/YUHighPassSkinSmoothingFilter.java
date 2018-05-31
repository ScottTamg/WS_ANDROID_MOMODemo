package project.android.imageprocessing.beauty;

import android.graphics.Point;
import android.opengl.GLES20;

import project.android.imageprocessing.filter.BasicFilter;
import project.android.imageprocessing.filter.GroupFilter;
import project.android.imageprocessing.filter.MultiInputFilter;
import project.android.imageprocessing.filter.colour.ExposureFilter;
import project.android.imageprocessing.filter.colour.ToneCurveFilter;
import project.android.imageprocessing.filter.processing.GaussianBlurFilter;
import project.android.imageprocessing.input.GLTextureOutputRenderer;

public class YUHighPassSkinSmoothingFilter extends GroupFilter {
    String TAG="YUHighPassFilter";
    private class YUGreenAndBlueChannelOverlayFilter  extends BasicFilter {
        @Override
        protected String getFragmentShader() {
            return "precision mediump float;\n"
                    + "uniform sampler2D " + UNIFORM_TEXTURE0 + ";\n"
                    + "varying vec2 " + VARYING_TEXCOORD + ";\n"

                    + "void main() {\n"
                    + "   vec4 image = texture2D(" + UNIFORM_TEXTURE0 + "," + VARYING_TEXCOORD + ");\n" +
                    "     vec4 base = vec4(image.g,image.g,image.g,1.0);\n" +
                    "     vec4 overlay = vec4(image.b,image.b,image.b,1.0);\n" +
                    "     float ba = 2.0 * overlay.b * base.b + overlay.b * (1.0 - base.a) + base.b * (1.0 - overlay.a);\n" +
                    "     gl_FragColor = vec4(ba,ba,ba,1.0);\n" +
                    "}\n";
        }
        /*@Override
        protected void drawFrame() {
            destroyFrameBuffer=false;
            super.drawFrame();
        }*/
    }

    private class YUSkinSmoothingMaskBoostFilter extends BasicFilter {
        @Override
        protected String getFragmentShader() {
            return "precision mediump float;\n"
                    + "uniform sampler2D " + UNIFORM_TEXTURE0 + ";\n"
                    + "varying vec2 " + VARYING_TEXCOORD + ";\n"

                    + "void main() {\n"
                    + "   vec4 final = texture2D(" + UNIFORM_TEXTURE0 + "," + VARYING_TEXCOORD + ");\n" +
                    "     float ba = 0.0;\n" +
                    "     lowp vec4 hardLightColor = vec4(vec3(final.b), 1.0);\n" +
                    "     for (int i =0; i < 3; i++)\n" +
                    "     {\n" +
                    "         if (hardLightColor.b < 0.5) {\n" +
                    "             ba = hardLightColor.b  * hardLightColor.b * 2.;\n" +
                    "         } else {\n" +
                    "             ba = 1. - (1. - hardLightColor.b) * (1. - hardLightColor.b) * 2.;\n" +
                    "         }\n" +
                    "         hardLightColor = vec4(vec3(ba), 1.0);\n" +
                    "     }\n" +
                    "     \n" +
                    "     float k = 255.0 / (164.0 - 75.0);\n" +
                    "     hardLightColor.r = (hardLightColor.r - 75.0 / 255.0) * k;\n" +
                    "     hardLightColor.g = (hardLightColor.g - 75.0 / 255.0) * k;\n" +
                    "     hardLightColor.b = (hardLightColor.b - 75.0 / 255.0) * k;\n" +
                    "     \n" +
                    "     gl_FragColor = hardLightColor;\n" +
                    "}\n";
        }
    }

    private class YUStillImageHighPassFilter extends MultiInputFilter {
        public YUStillImageHighPassFilter() {
            super(2);
        }

        @Override
        protected String getFragmentShader() {
            return "precision mediump float;\n"
                    + "uniform sampler2D " + UNIFORM_TEXTURE0 + ";\n"
                    + "uniform sampler2D " + UNIFORM_TEXTUREBASE + 1 + ";\n"
                    + "varying vec2 " + VARYING_TEXCOORD + ";\n"
                    + "void main(){\n"
                    + "   vec4 image = texture2D(" + UNIFORM_TEXTURE0 + "," + VARYING_TEXCOORD + ");\n"
                    + "   vec4 blurredImage = texture2D(" + UNIFORM_TEXTUREBASE + 1 + "," + VARYING_TEXCOORD + ");\n"
                    + "   gl_FragColor = vec4(image.rgb - blurredImage.rgb + vec3(0.5,0.5,0.5),image.a);\n"
                    + "}\n";
        }

    }
    private class YUHighpassSkinSmoothingCompositingFilter extends MultiInputFilter {

        private static final String UNIFORM_AMOUNT = "amountPercent";
        private int amountSlot;
        private float amount;

        private YUHighpassSkinSmoothingCompositingFilter(float amount) {
            super(3);
            this.amount = amount;
        }

        public void setAmount(float amount) {
            synchronized (getLockObject()) {
                this.amount = amount;
            }
        }

        @Override
        protected String getFragmentShader() {
            return "precision mediump float;\n"
                            + "uniform sampler2D " + UNIFORM_TEXTURE0 + ";\n"
                            + "uniform sampler2D " + UNIFORM_TEXTUREBASE + 1 + ";\n"
                            + "uniform sampler2D " + UNIFORM_TEXTUREBASE + 2 + ";\n"
                            + "varying vec2 " + VARYING_TEXCOORD + ";\n"
                            + "uniform float " + UNIFORM_AMOUNT + ";\n"
                            + "void main(){\n"
                            + "   vec4 image = texture2D(" + UNIFORM_TEXTURE0 + "," + VARYING_TEXCOORD + ");\n"
                            + "   vec4 toneCurvedImage = texture2D(" + UNIFORM_TEXTUREBASE + 1 + "," + VARYING_TEXCOORD + ");\n"
                            + "   vec4 mask = texture2D(" + UNIFORM_TEXTUREBASE + 2 + "," + VARYING_TEXCOORD + ");\n"
                            + "    gl_FragColor = vec4(mix(image.rgb,toneCurvedImage.rgb,1.0 - mask.b*" + UNIFORM_AMOUNT + "),1.0);\n"
                            + "}\n";
        }

        @Override
        protected void initShaderHandles() {
            super.initShaderHandles();
            amountSlot = GLES20.glGetUniformLocation(programHandle, UNIFORM_AMOUNT);
        }

        @Override
        protected void passShaderValues() {
            super.passShaderValues();
            GLES20.glUniform1f(amountSlot, amount);
        }
    }

    private class YUHighPassSkinSmoothingMaskGenerator extends GroupFilter {
        ExposureFilter input = new ExposureFilter(0);
        YUGreenAndBlueChannelOverlayFilter channelOverlayFilter = new YUGreenAndBlueChannelOverlayFilter();
        YUStillImageHighPassFilter highpassFilter = new YUStillImageHighPassFilter();
        GaussianBlurFilter gaussianBlur = new GaussianBlurFilter(4.0f);
        YUSkinSmoothingMaskBoostFilter maskBoostFilter = new YUSkinSmoothingMaskBoostFilter();

        private YUHighPassSkinSmoothingMaskGenerator() {
            input.addTarget(channelOverlayFilter);
            channelOverlayFilter.addTarget(gaussianBlur);
            channelOverlayFilter.addTarget(highpassFilter);
            gaussianBlur.addTarget(highpassFilter);
            highpassFilter.registerFilterLocation(channelOverlayFilter, 0);
            highpassFilter.registerFilterLocation(gaussianBlur, 1);
            highpassFilter.addTarget(maskBoostFilter);

            maskBoostFilter.addTarget(this);

            registerInitialFilter(input);
            registerFilter(gaussianBlur);
            registerFilter(highpassFilter);
            registerFilter(channelOverlayFilter);
            registerTerminalFilter(maskBoostFilter);
        }
        @Override
        public void newTextureReady(int texture, GLTextureOutputRenderer source, boolean newData) {
            super.newTextureReady(texture,source,newData);
        }
    }
    private YUHighpassSkinSmoothingCompositingFilter composeFilter;
    ExposureFilter inputFilter;
    ToneCurveFilter skinToneCurveFilter;
    YUHighPassSkinSmoothingMaskGenerator highPassSkinSmoothingMaskGenerator;
    private float amount = 0.0f;

    public YUHighPassSkinSmoothingFilter() {
        Point controlPoint0 = new Point(0, 0);
        Point controlPoint1 = new Point(120, 146);
        Point controlPoint2 = new Point(255, 255);
        Point[] rgbSingle = new Point[]{controlPoint0, controlPoint2};
        Point[] rgbComposite = new Point[]{controlPoint0, controlPoint1, controlPoint2};
        inputFilter = new ExposureFilter(0.0f);
        skinToneCurveFilter = new ToneCurveFilter(rgbSingle, rgbSingle, rgbSingle, rgbComposite);
        highPassSkinSmoothingMaskGenerator = new YUHighPassSkinSmoothingMaskGenerator();
        composeFilter = new YUHighpassSkinSmoothingCompositingFilter(0.8f);

        inputFilter.addTarget(skinToneCurveFilter);
        inputFilter.addTarget(highPassSkinSmoothingMaskGenerator);

        inputFilter.addTarget(composeFilter);
        skinToneCurveFilter.addTarget(composeFilter);
        highPassSkinSmoothingMaskGenerator.addTarget(composeFilter);

        composeFilter.registerFilterLocation(inputFilter, 0);
        composeFilter.registerFilterLocation(skinToneCurveFilter, 1);
        composeFilter.registerFilterLocation(highPassSkinSmoothingMaskGenerator, 2);

        composeFilter.addTarget(this);
        registerInitialFilter(inputFilter);
        registerFilter(skinToneCurveFilter);
        registerFilter(highPassSkinSmoothingMaskGenerator);
        registerTerminalFilter(composeFilter);
    }


    public void setAmount(float amount) {
        synchronized (getLockObject()) {
            composeFilter.setAmount(amount);
        }
    }
    public float getAmount(){
        return amount;
    }

    @Override
    public void SetFrameAvaliableListener(FrameAvaliableListener listener) {
        composeFilter.SetFrameAvaliableListener(listener);
    }
}

