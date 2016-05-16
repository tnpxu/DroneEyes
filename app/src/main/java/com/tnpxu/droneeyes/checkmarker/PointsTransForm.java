package com.tnpxu.droneeyes.checkmarker;

import com.tnpxu.droneeyes.FPVTutorialActivity;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tnpxu on 3/11/2558.
 */
public class PointsTransForm{

//    public Mat inputMat;
//
//    public PointsTransForm(Mat inputMat) {
//        this.inputMat = inputMat;
//    }

    public boolean checkTransform(Mat inputMat) {

        Mat inPerspec = new Mat();
        inputMat.copyTo(inPerspec);
        Imgproc.cvtColor(inPerspec, inPerspec, Imgproc.COLOR_BGR2HSV);
        Mat outPerspec = new Mat();
        boolean check = false;

        Mat yellowMat = new Mat();
        inPerspec.copyTo(yellowMat);
        Mat greenMat = new Mat();
        inPerspec.copyTo(greenMat);
        Mat redMat = new Mat();
        inPerspec.copyTo(redMat);
        Mat blueMat = new Mat();
        inPerspec.copyTo(blueMat);

        //4 point detect
        //green
        Core.inRange(greenMat, new Scalar(60, 150, 150), new Scalar(80, 255, 255), greenMat);
        //yellow
        Core.inRange(yellowMat, new Scalar(90, 150, 150), new Scalar(100, 255, 255), yellowMat);
        //blue
        Core.inRange(blueMat, new Scalar(30, 150, 150), new Scalar(40, 255, 255), blueMat);
        //pink
        Core.inRange(redMat, new Scalar(140, 150, 150), new Scalar(160, 255, 255), redMat);

        //Gaussian noise
//        Imgproc.GaussianBlur(inPerspec, inPerspec, new Size(5, 5), 8);

        //dilate to remove noise
        Imgproc.dilate (greenMat, greenMat, new Mat());
        Imgproc.dilate (yellowMat, yellowMat, new Mat());
        Imgproc.dilate (blueMat, blueMat, new Mat());
        Imgproc.dilate (redMat, redMat, new Mat());


        Mat cannyYellow = new Mat();
        Mat cannyRed = new Mat();
        Mat cannyGreen = new Mat();
        Mat cannyBlue = new Mat();
        ArrayList<MatOfPoint> contourYellow = new ArrayList<>();
        ArrayList<MatOfPoint> contourRed = new ArrayList<>();
        ArrayList<MatOfPoint> contourGreen = new ArrayList<>();
        ArrayList<MatOfPoint> contourBlue = new ArrayList<>();
        MatOfInt4 hierarchy = new MatOfInt4();

//      Imgproc.Canny(detectedEdges, detectedEdges, this.threshold.getValue(), this.threshold.getValue() * 3, 3, false);
        Imgproc.Canny(yellowMat, cannyYellow, 100, 200, 3, false);
        Imgproc.Canny(blueMat, cannyBlue, 100, 200, 3, false);
        Imgproc.Canny(redMat, cannyRed, 100, 200, 3, false);
        Imgproc.Canny(greenMat, cannyGreen, 100, 200, 3, false);

        Imgproc.findContours(cannyYellow, contourYellow, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(cannyBlue, contourBlue, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(cannyRed, contourRed, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(cannyGreen, contourGreen, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        if(contourBlue.size() > 0 && contourGreen.size() > 0 && contourRed.size() > 0 && contourYellow.size() > 0) {

//            for (int contourIdx = 0; contourIdx < contourRed.size(); contourIdx++) {
//                Imgproc.drawContours(inputMat, contourRed, contourIdx, new Scalar(0, 0, 255), 1);
//            }
//            for (int contourIdx = 0; contourIdx < contourGreen.size(); contourIdx++) {
//                Imgproc.drawContours(inputMat, contourGreen, contourIdx, new Scalar(0, 0, 255), 1);
//            }
//            for (int contourIdx = 0; contourIdx < contourYellow.size(); contourIdx++) {
//                Imgproc.drawContours(inputMat, contourYellow, contourIdx, new Scalar(0, 0, 255), 1);
//            }
//            for (int contourIdx = 0; contourIdx < contourBlue.size(); contourIdx++) {
//                Imgproc.drawContours(inputMat, contourBlue, contourIdx, new Scalar(0, 0, 255), 1);
//            }
            check = true;

        } else {
            check = false;
        }
        return check;
    }

//
//    @Override
//    public void run() {
//        FPVTutorialActivity.markerCheck = checkTransform(inputMat);
//    }
}
