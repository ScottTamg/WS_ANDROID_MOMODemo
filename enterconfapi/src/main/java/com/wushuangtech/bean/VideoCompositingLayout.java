package com.wushuangtech.bean;

/**
 * 画中画布局类
 */
public class VideoCompositingLayout {

    /**
     * 视频背景颜色，RGB值，[0-255 , 0-255 , 0-255]
     */
    public int[] backgroundColor = new int[]{80, 80, 80};
//    public int[] backgroundColor = new int[]{127, 0, 0};
    /**
     * 小视频窗口数据集合
     */
    public VideoCompositingLayout.Region[] regions = null;

    public VideoCompositingLayout() {
    }

    public static class Region {
        /**
         * 小视频的用户ID
         */
        public long mUserID;
        /**
         * 小视频X坐标
         */
        public double x;
        /**
         * 小视频Y坐标
         */
        public double y;
        /**
         * 小视频宽度
         */
        public double width;
        /**
         * 小视频高度
         */
        public double height;
        /**
         * 小视频层级关系
         */
        public int zOrder;

        public Region() {
        }
    }
}