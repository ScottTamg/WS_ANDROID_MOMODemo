package com.wushuangtech.library;

public class UserDeviceConfig {

    private long mUerID;
    private String mDeviceID;
    private boolean mIsUse;

    /**
     * Construct EVIDEODEVTYPE_VIDEO device
     *
     * @param mUerID
     * @param mDeviceID
     */
    public UserDeviceConfig(long mUerID,
                            String mDeviceID, boolean mIsUse) {
        this.mUerID = mUerID;
        this.mDeviceID = mDeviceID;
        this.mIsUse = mIsUse;
    }

    public String getDeviceID() {
        return mDeviceID;
    }

    public boolean isUse() {
        return mIsUse;
    }

    public void setIsUse(boolean mIsUse) {
        this.mIsUse = mIsUse;
    }

    public long getUerID() {
        return mUerID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mDeviceID == null) ? 0 : mDeviceID.hashCode());
        result = prime * result + (int) (mUerID ^ (mUerID >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserDeviceConfig other = (UserDeviceConfig) obj;
        if (mDeviceID == null) {
            if (other.mDeviceID != null)
                return false;
        } else if (!mDeviceID.equals(other.mDeviceID))
            return false;
        if (mUerID != other.mUerID)
            return false;
        return true;
    }
}
