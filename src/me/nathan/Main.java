package me.nathan;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import me.nathan.util.RenderUtil;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static double BALL_DIAMETER = 2.9; // inches
    public static double BLUR_AMOUNT = 25;
    public static double PIXEL_CHANGE_THRESHOLD = 25;
    public static double NON_NOISE_THRESHOLD = 300;

    public static void main (final String args[]) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        final JPanel cameraFeed = new JPanel();
        RenderUtil.createJFrame(cameraFeed);

        final VideoCapture camera = new VideoCapture("C:\\Users\\nathan\\Downloads\\connor.mp4");

       Main.runBallAnalysis(cameraFeed, camera).run();
    }

    public static boolean rectOverlap(Rect r1, ArrayList<Rect> arr) {
        for(Rect r2 : arr) {
            for(int x = r2.x; x < r2.x + r2.width; x++) {
                for(int y = r2.y; y < r2.y + r2.height; y++) {
                    if(r1.contains(new Point(x, y))) return true;
                }
            }
        }
        return false;
    }

    public static int calcSpeed(ArrayList<Integer[]> positions) {
        // The ball isn't recorded traveling the full screen width.
        int widthTraveled_px = positions.getLast()[1] - positions.getFirst()[1];

        // The average difference in pixels the ball traveled between frames.
        int diffSum = 0;
        for(int i = 1; i < positions.size(); i++) {
            diffSum += (positions.get(i)[1] - positions.get(i-1)[1]) /
                    (positions.get(i)[0] - positions.get(i-1)[0]);
        }
        int averageDifference = diffSum / positions.size();

        // The average height, and hopefully apparent diameter of baseball
        int heightTraveled_px = Math.abs(positions.getFirst()[4] - positions.getLast()[4]);
        double theta = Math.atan((double) heightTraveled_px / widthTraveled_px);

        int heightSum = 0;
        for(Integer[] pos : positions) {
            heightSum += pos[2] - (int)Math.floor((pos[3] * Math.tan(theta)));
        }
        int averageHeight = heightSum / positions.size();

        double widthTraveled_ft = ((BALL_DIAMETER / averageHeight) * widthTraveled_px)/12;
        double widthTraveledPerFrame_ft = (widthTraveled_ft * averageDifference) / widthTraveled_px;

        return (int) (widthTraveledPerFrame_ft/(1/60.0) * 0.68);
    }

    /** Uses frame comparison to detect a baseball, and tries to calculate its speed */
    private static Runnable runBallAnalysis(final JPanel cameraFeed, final VideoCapture camera) {
        Mat lastFrame = new Mat();
        Mat finalFrame = new Mat();

        return () -> {
            ArrayList<Integer[]> positions = new ArrayList<>(); // 0 = frame#, 1=posX, 2=height, 3=width, 3=posY

            final Mat unmodifiedFrame = new Mat();
            camera.read(unmodifiedFrame);

            Imgproc.cvtColor(unmodifiedFrame, lastFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(lastFrame, lastFrame, new Size(BLUR_AMOUNT, BLUR_AMOUNT), 0);

            ArrayList<Rect> previousRects = new ArrayList<>();

            int frameNumber = 0;
            while(camera.read(unmodifiedFrame)) {

                Mat currentFrame = new Mat();
                unmodifiedFrame.copyTo(currentFrame);
                frameNumber++;

                Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(currentFrame, currentFrame, new Size(BLUR_AMOUNT, BLUR_AMOUNT), 0);

                Mat frameDifference = new Mat();
                Core.absdiff(lastFrame, currentFrame, frameDifference);
                Imgproc.threshold(frameDifference, frameDifference, PIXEL_CHANGE_THRESHOLD, 255, Imgproc.THRESH_BINARY);

                final List<MatOfPoint> allContours = new ArrayList<>();
                Imgproc.findContours(
                        frameDifference,
                        allContours,
                        new Mat(frameDifference.size(), frameDifference.type()),
                        Imgproc.RETR_EXTERNAL,
                        Imgproc.CHAIN_APPROX_NONE
                );

                final List<MatOfPoint> filteredContours = new ArrayList<>();
                for(MatOfPoint contour: allContours) {
                    final double value = Imgproc.contourArea(contour);
                    final Rect rect = Imgproc.boundingRect(contour);

                    final boolean isNotNoise = value > NON_NOISE_THRESHOLD;

                    if (isNotNoise && !rectOverlap(rect, previousRects) && frameNumber > 10) { // filter out pixel change of small area
                        previousRects.add(rect);
                        positions.add(new Integer[]{
                                frameNumber,
                                rect.x,
                                rect.height,
                                rect.width,
                                rect.y
                        });

                        Imgproc.putText ( // label large pixel change as a ball location
                                unmodifiedFrame,
                                "Baseball",
                                new Point(rect.x + rect.width, rect.y),
                                2,
                                0.5,
                                new Scalar(0, 0, 255),
                                1
                        );

                        Imgproc.rectangle(unmodifiedFrame, rect, new Scalar(0, 255, 0), 2);
                        filteredContours.add(contour);

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                String speed = "NAN";
                if(positions.size() >= 2) {
                    speed = String.valueOf(calcSpeed(positions));
                }

                Imgproc.putText ( // render the average ball speed
                        unmodifiedFrame,
                        "Velo: " + speed,
                        new Point(5, camera.get(Videoio.CAP_PROP_FRAME_HEIGHT) - 220),
                        2,
                        2,
                        new Scalar(255, 255, 255),
                        2
                );

                RenderUtil.drawImage(unmodifiedFrame, cameraFeed);
                Imgcodecs.imwrite("C:\\Users\\nathan\\IdeaProjects\\OpenCV Test\\frame_output"+ "/" + frameNumber +".jpg", frameDifference);
                currentFrame.copyTo(lastFrame);
                unmodifiedFrame.copyTo(finalFrame);
            }
            RenderUtil.drawImage(finalFrame, cameraFeed);
        };
    }
}