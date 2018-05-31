package project.android.imageprocessing.output;import com.wushuangtech.library.Constants;import com.wushuangtech.utils.PviewLog;import project.android.imageprocessing.FastImageProcessingPipeline;import project.android.imageprocessing.GLRenderer;import project.android.imageprocessing.input.GLTextureOutputRenderer;/** * A screen renderer extension of GLRenderer. * This class accepts a texture as input and renders it to the screen. * * @author Chris Batt */public class ScreenEndpoint extends GLRenderer implements GLTextureInputRenderer {    private int rawWidth;    private int rawHeight;    private int encWidth;    private int encHeight;    private boolean bPreView = false;    private int scale_mode = Constants.RENDER_MODE_HIDDEN;    private FastImageProcessingPipeline rendererContext;    /**     * Creates a GLTextureToScreenRenderer.     * If it is not set to full screen mode, the reference to the render context is allowed to be null.     *     * @param rendererContext A reference to the GLSurfaceView.Renderer that contains the OpenGL context.     *                        <p>     *                        Whether or not to use the input filter size as the render size or to render full screen.     */    public ScreenEndpoint(FastImageProcessingPipeline rendererContext) {        super();        this.rendererContext = rendererContext;    }    @Override    protected void initWithGLContext() {        int prevWidth = rendererContext.getWidth();        int prevHeight = rendererContext.getHeight();        float preRate = (float) prevWidth / (float) prevHeight; // 屏幕宽高        float capRate = (float) rawWidth / (float) rawHeight; //预览宽高        // ******************* 苑克敬修改本地显示比例与远端显示比例相匹配 ****************        float encRate = 0;        if (encHeight != 0 && encHeight != 0) {            encRate = (float) encWidth / (float) encHeight;        }        int renderWidth = prevWidth;        int renderHeight = prevHeight;        if (scale_mode == Constants.RENDER_MODE_FIT) {            if (preRate >= capRate) {                renderWidth = (int) (renderHeight * capRate);                startX = (prevWidth - renderWidth) / 2;                startY = 0;            } else {                renderHeight = (int) (renderWidth / capRate);                startX = 0;                startY = (prevHeight - renderHeight) / 2;            }        } else {            if (encRate == 0) {                if (preRate >= capRate) {                    renderHeight = (int) (prevWidth / capRate);//调整render高度，使比例相同，宽度填满view，高度延伸到view外面。                    startY = (prevHeight - renderHeight) / 2;                    startX = 0;                } else {                    renderWidth = (int) (prevHeight * capRate);//调整render宽度，使比例相同，高度填满view，宽度延伸到view外面。                    startX = (prevWidth - renderWidth) / 2;                    startY = 0;                }            } else {                float ampH = 1;                float ampW = 1;                if (preRate >= capRate) {                    renderHeight = (int) (prevWidth / capRate);//调整render高度，使比例相同，宽度填满view，高度延伸到view外面。                    ampH = preRate / capRate;//调整比例导致宽放大倍数                    ampW = 1;                } else {                    renderWidth = (int) (prevHeight * capRate);//调整render宽度，使比例相同，高度填满view，宽度延伸到view外面。                    ampW = capRate / preRate;//调整比例导致高放大倍数                    ampH = 1;                }                float ampRate = 1;                if (preRate >= encRate) {                    ampRate = ((float) rawWidth / (float) encWidth);//放大render，使得放大后编码宽度填满view，高度延伸到view外，                    // 目标放大倍数                    ampRate /= ampW;//调整比例基础上的放大倍数                } else {                    ampRate = ((float) rawHeight / (float) encHeight) / ampH;//放大render，使放大后编码高度填满view，宽度延伸到view外                    // 目标放大倍数                    ampRate /= ampH;                }                renderHeight *= ampRate;                renderWidth *= ampRate;                startY = (prevHeight - renderHeight) / 2;                startX = (prevWidth - renderWidth) / 2;            }        }        // ******************* 苑克敬修改本地显示比例与远端显示比例相匹配 ****************        PviewLog.w(PviewLog.TAG + 1, "prevWidth : " + prevWidth + " | prevHeight : " + prevHeight);        PviewLog.w(PviewLog.TAG + 1, "rawWidth : " + rawWidth + " | rawHeight : " + rawHeight);        PviewLog.w(PviewLog.TAG + 1, "renderWidth : " + renderWidth + " | renderHeight : " + renderHeight);        PviewLog.w(PviewLog.TAG + 1, "startX : " + startX + " | startY : " + startY         + " | bPreView : " + bPreView);        setRenderSize(renderWidth, renderHeight);        super.initWithGLContext();    }    /* (non-Javadoc)     * @see project.android.imageprocessing.output.GLTextureInputRenderer#newTextureReady(int, project.android.imageprocessing.input.GLTextureOutputRenderer)     */    @Override    public void newTextureReady(int texture, GLTextureOutputRenderer source, boolean newData) {        if (!bPreView) {            return;        }        texture_in = texture;        int w = source.getWidth();        int h = source.getHeight();        setWidth(w);        setHeight(h);        onDrawFrame();    }    public int getScaleMode() {        return scale_mode;    }    public void SetRawSize(int width, int heigt) {        rawWidth = width;        rawHeight = heigt;    }    public void SetEncodeSize(int width, int heigt) {        encWidth = width;        encHeight = heigt;    }    public void setPreView(boolean bPreView) {        this.bPreView = bPreView;    }    public void setScaleMode(int mode) {        scale_mode = mode;    }}