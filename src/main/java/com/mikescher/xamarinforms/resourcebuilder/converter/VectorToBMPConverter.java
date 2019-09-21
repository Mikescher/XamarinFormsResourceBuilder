package com.mikescher.xamarinforms.resourcebuilder.converter;

public class VectorToBMPConverter extends VectorToRasterConverter
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
