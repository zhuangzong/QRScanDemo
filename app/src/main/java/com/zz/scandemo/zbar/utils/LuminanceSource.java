package com.zz.scandemo.zbar.utils;

public abstract class LuminanceSource {
    private final int width;
    private final int height;

    protected LuminanceSource(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public abstract byte[] getRow(int var1, byte[] var2);

    public abstract byte[] getMatrix();

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public boolean isCropSupported() {
        return false;
    }

    public LuminanceSource crop(int left, int top, int width, int height) {
        throw new RuntimeException("This luminance source does not support cropping.");
    }

    public boolean isRotateSupported() {
        return false;
    }

    public LuminanceSource rotateCounterClockwise() {
        throw new RuntimeException("This luminance source does not support rotation.");
    }
}
