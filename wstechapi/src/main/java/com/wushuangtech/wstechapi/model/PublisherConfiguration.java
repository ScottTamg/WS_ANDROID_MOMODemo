package com.wushuangtech.wstechapi.model;

/**
 * 配置旁路直播推流的类
 */
public class PublisherConfiguration {

    private Builder mBuilder;

    public Builder Builder() {
        mBuilder = new Builder();
        return mBuilder;
    }

    public String getPushUrl() {
        if (mBuilder == null) {
            return null;
        }
        return mBuilder.mPushUrl;
    }

    class Builder {

        private String mPushUrl;
        private int mBiteRate;

        /**
         * Description: 设置流码率.
         *
         * @param bitrate 流码率
         * @return the builder
         */
        public Builder biteRate(int bitrate) {
            this.mBiteRate = mBiteRate;
            return this;
        }

        /**
         * Description: 设置推流的地址.
         *
         * @param url 推流的地址
         * @return the builder
         */
        public Builder publishUrl(String url) {
            this.mPushUrl = mPushUrl;
            return this;
        }

        public PublisherConfiguration build() {
            return PublisherConfiguration.this;
        }
    }

}
