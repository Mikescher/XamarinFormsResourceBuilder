package com.mikescher.xamarinforms.resourcebuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class PNGUtil {

    public static int getWidthFromPNG(String path) throws Exception
    {
        return ImageIO.read(new File(path)).getWidth();
    }

    public static int getHeightFromPNG(String path) throws Exception
    {
        return ImageIO.read(new File(path)).getHeight();
    }

    public static int calcAutoWidthFromPNG(String path, int height) throws Exception
    {
        double png_width = getWidthFromPNG(path);
        double png_height = getHeightFromPNG(path);

        return (int)Math.round((png_width/png_height) * height);
    }

    public static int calcAutoHeightFromPNG(String path, int width) throws Exception
    {
        double png_width = getWidthFromPNG(path);
        double png_height = getHeightFromPNG(path);

        return (int)Math.round((png_height/png_width) * width);
    }
}
