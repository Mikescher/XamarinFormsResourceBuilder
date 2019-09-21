package com.mikescher.xamarinforms.resourcebuilder.converter;

public class RasterToPNGConverter extends RasterToRasterConverter
{
    @Override
    protected String getDisplayString() {
        return "PNG @ " + OutputWidth + "x" + OutputHeight;
    }

    @Override
    protected String getOutputType() { return "PNG"; }

    @Override
    protected String getOutputExtension() { return ".png"; }
}
