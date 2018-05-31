package project.android.imageprocessing.beauty;

import project.android.imageprocessing.filter.BasicFilter;

public class FUBeautifyFilter extends BasicFilter {

    @Override
    protected void initWithGLContext() {
        curRotation = 1;
        super.initWithGLContext();
    }
}
