package com.mikescher.xamarinforms.resourcebuilder.converter;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.values.IntRect;
import com.mikescher.xamarinforms.resourcebuilder.util.SVGUtil;
import com.mikescher.xamarinforms.resourcebuilder.values.FittingType;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

@SuppressWarnings({"DuplicatedCode", "WeakerAccess"})
public abstract class VectorToRasterConverter extends AbstractConverter
{
    private static Color TRANSPARENT = new Color(0, 0, 0, 0);

    public int ParameterMargin = 0;
    public FittingType ParameterFitting = FittingType.STRETCH;
    public Color ParameterBackgroundColor = TRANSPARENT;

    @Override
    protected double getInputWidthDouble(File file) throws Exception {
        return SVGUtil.getFloatWidthFromSVG(file);
    }

    @Override
    protected double getInputHeightDouble(File file) throws Exception {
        return SVGUtil.getFloatHeightFromSVG(file);
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
        if (ParameterBackgroundColor != TRANSPARENT)         result.put("background", colorToString(ParameterBackgroundColor));

        return result;
    }

    @Override
    protected File convert(RunEnvironment env) throws Exception
    {

        File f_tmp = File.createTempFile("xfrb_v2r_1_", getOutputExtension());
        f_tmp.deleteOnExit();

        IntRect innerDestination = ParameterFitting.calcInnerRect(ParameterMargin, InputWidth, InputHeight, OutputWidth, OutputHeight);
        if (innerDestination.Width  < 0) throw new Exception("Invalid img width");
        if (innerDestination.Height < 0) throw new Exception("Invalid img height");

        BufferedImage innerImage;
        if (env.SVGRasterizer.equalsIgnoreCase("batik"))
        {
            innerImage = rasterize_batik(new File(InputFile), innerDestination.Width, innerDestination.Height);
        }
        else if (env.SVGRasterizer.equalsIgnoreCase("universe"))
        {
            innerImage = rasterize_universe(new File(InputFile), innerDestination.Width, innerDestination.Height);
        }
        else
        {
            throw new Exception("Unknown rasterizer");
        }

        BufferedImage fullImage = new BufferedImage(OutputWidth, OutputHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = fullImage.createGraphics();
        g2d.setColor(ParameterBackgroundColor);
        g2d.fillRect(0, 0, OutputWidth, OutputHeight);
        g2d.drawImage(innerImage, innerDestination.X, innerDestination.Y, null);

        ImageIO.write(fullImage, getOutputType(), f_tmp);

        return f_tmp;
    }

    private BufferedImage rasterize_universe(File input, int ww, int hh) throws Exception
    {
        SVGDiagram diagram = new SVGUniverse().getDiagram(input.toURI());

        BufferedImage bi = new BufferedImage(ww, hh, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D ig2 = bi.createGraphics();
        ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        diagram.render(ig2);
        ig2.dispose();

        return bi;
    }

    private BufferedImage rasterize_batik(File input, int ww, int hh) throws Exception
    {
        File f_tmp_1 = File.createTempFile("xfrb_v2r_2_", ".png");
        f_tmp_1.deleteOnExit();

        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) ww);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) hh);
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(input));
        OutputStream ostream = new FileOutputStream(f_tmp_1);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();

        BufferedImage img_raster = ImageIO.read(f_tmp_1);
        if (!f_tmp_1.delete()) throw new Exception("Could not delete file");

        return img_raster;
    }

    protected abstract String getOutputType();

    protected abstract String getOutputExtension();
}
