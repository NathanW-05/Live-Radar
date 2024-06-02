package me.nathan.util;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

public class RenderUtil {

    public static void createJFrame(final JPanel... panels) {
        final JFrame window = new JFrame("Live Radar");
        window.setSize(new Dimension(panels.length * 1920, 1080));
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setLayout(new GridLayout(1, panels.length));

        for (final JPanel panel : panels) {
            window.add(panel);
        }

        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void drawImage(final Mat mat, final JPanel panel) {
        final BufferedImage image = convertMatToBufferedImage(mat);

        final Graphics graphics = panel.getGraphics();
        graphics.drawImage(image, 0, 0, panel);
    }

    private static BufferedImage convertMatToBufferedImage(final Mat mat) {

        final BufferedImage bufferedImage = new BufferedImage(
                mat.width(),
                mat.height(),
                mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR
        );

        final WritableRaster raster = bufferedImage.getRaster();
        final DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        mat.get(0, 0, dataBuffer.getData());

        return bufferedImage;
    }
}
