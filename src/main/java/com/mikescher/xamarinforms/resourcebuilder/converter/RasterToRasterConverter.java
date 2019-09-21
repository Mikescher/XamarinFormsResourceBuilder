package com.mikescher.xamarinforms.resourcebuilder.converter;

import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.util.IntRect;
import com.mikescher.xamarinforms.resourcebuilder.values.FittingType;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

@SuppressWarnings("WeakerAccess")
public abstract class RasterToRasterConverter extends AbstractConverter
{
    private static Color TRANSPARENT = new Color(0, 0, 0, 0);

    public int ParameterMargin = 0;
    public FittingType ParameterFitting = FittingType.STRETCH;
    public Color ParameterBackgroundColor = TRANSPARENT;

    @Override
    protected double getInputWidthDouble(File file) throws Exception {
        return ImageIO.read(file).getWidth();
    }

    @Override
    protected double getInputHeightDouble(File file) throws Exception {
        return ImageIO.read(file).getHeight();
    }

    @Override
    protected void setMargin(int value) {
        ParameterMargin = value;
    }

    @Override
    protected void setFitting(FittingType value) {
        ParameterFitting = value;
    }

    @Override
    protected void setBackgroundColor(Color value) {
        ParameterBackgroundColor = value;
    }

    @Override
    protected HashMap<String, String> listParameterForChecksum()
    {
        HashMap<String, String> result = super.listParameterForChecksum();

        if (ParameterMargin          != 0)                   result.put("margin",     Integer.toString(ParameterMargin));
        if (ParameterFitting         != FittingType.STRETCH) result.put("fitting",    ParameterFitting.toString());
        if (ParameterBackgroundColor != TRANSPARENT)         result.put("background", ParameterBackgroundColor.toString());

        return result;
    }

    @Override
    protected File convert(RunEnvironment env) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_r2r_", getOutputExtension());
        f_tmp.deleteOnExit();

        BufferedImage img = ImageIO.read(new File(InputFile));

        IntRect innerDestination = ParameterFitting.calcInnerRect(ParameterMargin, InputWidth, InputHeight, OutputWidth, OutputHeight);
        if (innerDestination.Width  < 0) throw new Exception("Invalid img width");
        if (innerDestination.Height < 0) throw new Exception("Invalid img height");

        BufferedImage scaledImage = scaleImage(img, innerDestination.Width, innerDestination.Height);

        BufferedImage fullImage = new BufferedImage(OutputWidth, OutputHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = fullImage.createGraphics();
        g2d.setColor(ParameterBackgroundColor);
        g2d.fillRect(0, 0, OutputWidth, OutputHeight);
        g2d.drawImage(scaledImage, innerDestination.X, innerDestination.Y, null);

        ImageIO.write(fullImage, getOutputType(), f_tmp);

        return f_tmp;
    }

    private BufferedImage scaleImage(BufferedImage src, int w, int h)
    {
        if (src.getWidth() == w && src.getHeight() == h) return src;

        ResampleOp resizeOp = new ResampleOp(OutputWidth, OutputHeight);
        resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
        return resizeOp.filter(src, null);
    }

    protected abstract String getOutputType();

    protected abstract String getOutputExtension();
}
