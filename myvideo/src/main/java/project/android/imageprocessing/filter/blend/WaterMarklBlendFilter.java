package project.android.imageprocessing.filter.blend;

import com.wushuangtech.videocore.WaterMarkPosition;

import project.android.imageprocessing.filter.MultiInputFilter;
import project.android.imageprocessing.input.GLTextureOutputRenderer;

/**
 * Created by root on 17-2-13.
 */

public class WaterMarklBlendFilter extends MultiInputFilter {
    private static final int NUM_OF_INPUTS = 2;

    private boolean bDirtyOne = false;
    private boolean bDirtyTwo = false;

    private float x_scale = 5.0f;
    private float y_scale = 5.0f;
    private float x_soffset = 0.3f;
    private float y_soffset = 0.5f;
    private float x_eoffset = 0.1f;
    private float y_eoffset = 0.3f;
    public WaterMarklBlendFilter(WaterMarkPosition waterMarkPosition) {
        super(NUM_OF_INPUTS);//float xsoffset,float xeoffset,float ysoffset,float yeoffset

        float xoffset = waterMarkPosition.x_eoffset-waterMarkPosition.x_soffset;

        float yoffset = waterMarkPosition.y_eoffset-waterMarkPosition.y_soffset;

        x_soffset = waterMarkPosition.x_soffset;
        x_eoffset = waterMarkPosition.x_eoffset;
        y_soffset = waterMarkPosition.y_soffset;
        y_eoffset = waterMarkPosition.y_eoffset;
        x_scale=1.0f/xoffset;
        y_scale=1.0f/yoffset;
    }

    @Override
    protected String getFragmentShader() {
        return
                "precision mediump float;\n"
                        +"uniform sampler2D "+UNIFORM_TEXTURE0+";\n"
                        +"uniform sampler2D "+UNIFORM_TEXTUREBASE+1+";\n"
                        +"varying vec2 "+VARYING_TEXCOORD+";\n"

                        +"void main(){\n"
                        +"   vec4 color2 = texture2D("+UNIFORM_TEXTURE0+","+VARYING_TEXCOORD+");\n"
                        +"   vec4 color1;\n"
                        +"   vec2 texcoord2;\n"
                        +"  if ("+VARYING_TEXCOORD+".x > "+x_soffset+" && "+VARYING_TEXCOORD+".x <"+x_eoffset+" && "+VARYING_TEXCOORD+".y >"+y_soffset+" &&"+VARYING_TEXCOORD+".y < "+y_eoffset+") {\n"
                        +"   texcoord2.x = ("+VARYING_TEXCOORD+".x - "+x_soffset+") * "+x_scale+";\n"
                        +"   texcoord2.y = ("+VARYING_TEXCOORD+".y - "+y_soffset+") * "+y_scale+";\n"
                        +"    color1 = texture2D("+UNIFORM_TEXTUREBASE+1+",texcoord2);\n"
                        +"  } else \n"
                        +"    color1 = color2;\n"
                        +"   vec4 outputColor;\n"
                        +"   float a = color1.a + color2.a * (1.0 - color1.a);\n"
                        +"   outputColor.r = (color1.r * color1.a + color2.r * color2.a * (1.0 - color1.a))/a;\n"
                        +"   outputColor.g = (color1.g * color1.a + color2.g * color2.a * (1.0 - color1.a))/a;\n"
                        +"   outputColor.b = (color1.b * color1.a + color2.b * color2.a * (1.0 - color1.a))/a;\n"
                        +"   outputColor.a = a;\n"
                        +"   gl_FragColor = outputColor;\n"
                        +"}\n";
    }

    /* (non-Javadoc)
	 * @see project.android.imageprocessing.filter.BasicFilter#newTextureReady(int, project.android.imageprocessing.input.GLTextureOutputRenderer)
	 */
    @Override
    public synchronized void newTextureReady(int texture, GLTextureOutputRenderer source, boolean newData) {
        if(!texturesReceived.contains(source)) {
            texturesReceived.add(source);
        }

        if (texturesReceived.size() == NUM_OF_INPUTS -1) {
            bDirtyOne = newData;
        } else if (texturesReceived.size() == NUM_OF_INPUTS) {
            bDirtyTwo = newData;
        }

        if (bDirtyOne && bDirtyTwo) {
            markAsDirty();
        }

        int pos = filterLocations.lastIndexOf(source);
        if(pos == 0) {
            texture_in = texture;
        } else {
            this.texture[pos-1] = texture;
        }
        if(texturesReceived.size() == NUM_OF_INPUTS) {
            onDrawFrame();
            bDirtyOne = false;
            bDirtyTwo = false;
            texturesReceived.clear();
        }
    }
}
