package com.mikescher.xamarinforms.resourcebuilder.converter;

public class RasterToJPEGConverter extends RasterToRasterConverter
{
    @Override
    protected String getDisplayString() {
        return "JPEG @ " + OutputWidth + "x" + OutputHeight;
    }

    @Override
    protected String getOutputType() { return "JPEG"; }

    @Override
    protected String getOutputExtension() { return ".jpeg"; }
}
