package project.android.imageprocessing.beauty;

import android.opengl.GLES20;
import project.android.imageprocessing.filter.BasicFilter;

public class FUBeautifyFilter extends BasicFilter {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private int matrixHandle;
    private float[] matrix = new float[16];

    @Override
    protected String getFragmentShader() {
        return
                "precision mediump float;\n"
                        +"uniform sampler2D "+UNIFORM_TEXTURE0+";\n"
                        +"varying vec2 "+VARYING_TEXCOORD+";\n"

                        + "void main() {\n"
                        + "   gl_FragColor = texture2D("+UNIFORM_TEXTURE0+", "+VARYING_TEXCOORD+");\n"
                        + "}\n";
    }

    @Override
    protected String getVertexShader() {
        return
                "uniform mat4 "+UNIFORM_CAM_MATRIX+";\n"
                        + "attribute vec4 "+ATTRIBUTE_POSITION+";\n"
                        + "attribute vec2 "+ATTRIBUTE_TEXCOORD+";\n"
                        + "varying vec2 "+VARYING_TEXCOORD+";\n"

                        + "void main() {\n"
                        + "   vec4 texPos = "+UNIFORM_CAM_MATRIX+" * vec4("+ATTRIBUTE_TEXCOORD+", 1, 1);\n"
                        + "   "+VARYING_TEXCOORD+" = texPos.xy;\n"
                        + "   gl_Position = "+ATTRIBUTE_POSITION+";\n"
                        + "}\n";
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        matrixHandle = GLES20.glGetUniformLocation(programHandle, UNIFORM_CAM_MATRIX);
    }

    @Override
    protected void passShaderValues() {
        super.passShaderValues();
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrix, 0);
    }

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    @Override
    protected void handleSizeChange() {
        super.InitSuperFBO();
    }
}
