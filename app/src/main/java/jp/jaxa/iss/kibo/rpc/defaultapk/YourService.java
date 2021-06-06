package jp.jaxa.iss.kibo.rpc.defaultapk;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.common.HybridBinarizer;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {
    @Override
    protected void runPlan1(){
        // astrobee is undocked and the mission starts
        api.startMission();

        // astrobee moves to Point A
        moveToWrapper(11.32, -9.8, 4.85, 0, 0, -0.707, 0.707);

        // scans QR code
        Bitmap bitmap = api.getBitmapNavCam();
        Bitmap bitmapCropped = Bitmap.createBitmap(bitmap,400,480,480,480);
        // reads QR code
        String QRinfo = readQR(bitmapCropped);
        // send QR code info to judge
        api.sendDiscoveredQR(QRinfo);

        // interpret info from QR code
        String[] qr_contents = QRinfo.split(",");
        int QRpattern = Integer.valueOf(qr_contents[0].substring(qr_contents[0].length()-1));
        double QRpos_x = Double.valueOf(qr_contents[1].substring(4));
        double QRpos_y = Double.valueOf(qr_contents[2].substring(4));
        double QRpos_z = Double.valueOf(qr_contents[3].substring(4, qr_contents[3].length()-1));

        Log.d("QR info", "pattern: " + QRpattern);
        Log.d("QR info", "pos_x: " + QRpos_x);
        Log.d("QR info", "pos_y: " + QRpos_y);
        Log.d("QR info", "pos_z: " + QRpos_z);

        // TODO: 8 patterns have different movement type, use pattern 2 for now because it's the most straight-forward
        if (QRpattern == 1 || QRpattern == 2 || QRpattern == 3) {
            moveToWrapper(QRpos_x, QRpos_y, QRpos_z, 0, 0, -0.707, 0.707);
        }
        else if (QRpattern == 4) {
            relativeMoveToWrapper(-0.4, 0, 0, 0, 0, 0, 0);
            moveToWrapper(QRpos_x, QRpos_y, QRpos_z, 0, 0, -0.707, 0.707);
        }
        else if (QRpattern == 5) {
            Log.d("moveToDebug", "[moveToDebug] relative move 1 started");
            relativeMoveToWrapper(-0.65, 0, 0, 0, 0, 0, 0);
            Log.d("moveToDebug", "[moveToDebug] relative move 2 started");
            relativeMoveToWrapper(0, 0, 0.6, 0, 0, 0, 0);
            Log.d("moveToDebug", "[moveToDebug] move to final started");
            moveToWrapper(QRpos_x, QRpos_y, QRpos_z, 0, 0, -0.707, 0.707);
            Log.i("moveToDebug", "[moveToDebug] move to final ended");
        }
        // irradiate the laser
        // api.laserControl(true);

        // take snapshots
        // api.takeSnapshot();

        // move to the rear of Bay7
        // moveToWrapper(10.275, -10.314, 4.295, 0, -0.7071068, 0, 0.7071068);

        // Send mission completion
         api.reportMissionCompletion();
    }

    @Override
    protected void runPlan2(){
        // this is unused
        // write here your plan 2
    }

    @Override
    protected void runPlan3(){
        // this is unused
        // write here your plan 3
    }

    // You can add your method
    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w){

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y, (float)qua_z, (float)qua_w);

        Result result = api.moveTo(point, quaternion, true);

        int loopCounter = 0;
        while(!result.hasSucceeded() || loopCounter < LOOP_MAX){
            result = api.moveTo(point, quaternion, true);
            ++loopCounter;
        }
    }

    private void relativeMoveToWrapper(double pos_x, double pos_y, double pos_z,
                                       double qua_x, double qua_y, double qua_z,
                                       double qua_w){

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y, (float)qua_z, (float)qua_w);

        Result result = api.relativeMoveTo(point, quaternion, true);

        int loopCounter = 0;
        while(!result.hasSucceeded() || loopCounter < LOOP_MAX){
            result = api.relativeMoveTo(point, quaternion, true);
            ++loopCounter;
        }
    }

    private String readQR(Bitmap bitmap){
        String result = "";
        // get the size of bitmap and get the pixel data
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        try {
            // convert to binary bitmap that can be handled by Zxing
            LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            // read and analyze image data with Zxing
            Reader reader = new MultiFormatReader();
            com.google.zxing.Result decodeResult = reader.decode(binaryBitmap);

            // get the analysis result
            result = decodeResult.getText();
        } catch (Exception e) {
            Log.e("readQR", "ERROR: " + e.getLocalizedMessage());
        }
        return result;
    }
}

