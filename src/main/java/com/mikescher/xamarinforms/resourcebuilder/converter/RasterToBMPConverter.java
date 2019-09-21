package com.mikescher.xamarinforms.resourcebuilder.converter;

public class RasterToBMPConverter extends RasterToRasterConverter
{
    @Override
    protected String getDisplayString() {
        return "BITMAP @ " + OutputWidth + "x" + OutputHeight;
    }

    @Override
    protected String getOutputType() { return "BMP"; }

    @Override
    protected String getOutputExtension() { return ".bmp"; }
}
