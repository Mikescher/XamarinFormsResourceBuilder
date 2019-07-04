package com.mikescher.xamarinforms.resourcebuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class SVGUtil {

    public static double getFloatWidthFromSVG(String path) throws Exception
    {
        File fXmlFile = new File(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        if (root.hasAttribute("width")) return NumberFormat.getInstance(Locale.US).parse(root.getAttribute("width")).doubleValue();

        String[] s = root.getAttribute("viewBox").split(" ");
        if (s.length != 4) throw new Exception("Invalid viewBox value");

        return NumberFormat.getInstance(Locale.US).parse(s[3]).doubleValue();
    }

    public static double getFloatHeightFromSVG(String path) throws Exception
    {
        File fXmlFile = new File(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        if (root.hasAttribute("height")) return NumberFormat.getInstance(Locale.US).parse(root.getAttribute("height")).doubleValue();

        String[] s = root.getAttribute("viewBox").split(" ");
        if (s.length != 4) throw new Exception("Invalid viewBox value");

        return NumberFormat.getInstance(Locale.US).parse(s[3]).doubleValue();
    }

    public static double getRoundedHeightFromSVG(String path) throws Exception
    {
        return (int)Math.round(getFloatHeightFromSVG(path));
    }

    public static double getRoundedWidthFromSVG(String path) throws Exception
    {
        return (int)Math.round(getFloatWidthFromSVG(path));
    }

    public static int calcAutoWidthFromSVG(String path, int height) throws Exception
    {
        double svg_width = getFloatWidthFromSVG(path);
        double svg_height = getFloatHeightFromSVG(path);

        return (int)Math.round((svg_width/svg_height) * height);
    }

    public static int calcAutoHeightFromSVG(String path, int width) throws Exception
    {
        double svg_width = getFloatWidthFromSVG(path);
        double svg_height = getFloatHeightFromSVG(path);

        return (int)Math.round((svg_height/svg_width) * width);
    }

    public static String un_dp(String in) throws Exception
    {
        if (!in.endsWith("dp")) throw new Exception("value must end in 'dp'");
        if (in.length() == 2) throw new Exception("value must end in 'dp'");
        return in.substring(0, in.length()-2);
    }
}
