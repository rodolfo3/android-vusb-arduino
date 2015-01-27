package rodolfo.xyz.myusbtest;

import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = "Meh";

    protected UsbDevice device;
    protected UsbDeviceConnection connection;
    protected UsbRequest request;
    protected TextView tv;
    protected UsbManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.log);

        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceConnect();
            }
        });

        Button btnOn = (Button) findViewById(R.id.btnOn);
        btnOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeDevice((byte) 1);
            }
        });

        Button btnOff = (Button) findViewById(R.id.btnOff);
        btnOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeDevice( (byte)0 );
            }
        });

    }

    protected void log(String txt){
        tv.setText(
            tv.getText() +
            "\n" +
            txt
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void writeDevice(byte value) {
        UsbInterface ui = device.getInterface(0x0);

        if (connection != null) {
            // TODO check this magic numbers
            connection.claimInterface(ui, true);
            connection.controlTransfer(32, 9, 768, value, null, 0, 1000);
            log("write: " + value);
        } else {
            log("write error: connection is null");
        }
    }

    public void deviceConnect() {

        log("connect");

        if (connection == null) {

            // Find all available drivers from attached devices.
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);

            HashMap<String, UsbDevice> devs = manager.getDeviceList();

            if (devs.isEmpty()) {
                log("devs.isEmpty()");
                return;
            } else {
                for (String d: devs.keySet()) {
                    UsbDevice dev = (UsbDevice) devs.get(d);
                    log(
                            "  dev (" + d + "): " +
                                    Integer.toHexString(dev.getVendorId()) +
                                    ":" +
                                    Integer.toHexString(dev.getProductId()));
                    log("  permission: " + manager.hasPermission(dev));

                    if (dev.getVendorId() == 0x16c0 && dev.getProductId() == 0x05df) {
                        device = dev;
                    }
                }
            }

            if (device == null) {
                log("device not found... :(");
                return;
            }

            // Ask USB Permission from user
            // http://arduino-er.blogspot.com.br/2013/03/request-permission-to-access-usb.html
            Intent intent_UsbPermission = new Intent("permission");
            PendingIntent PendingIntent_UsbPermission = PendingIntent.getBroadcast(
                    this,      //context
                    0, // RQS_USB_PERMISSION,  //request code
                    intent_UsbPermission, //intent
                    0);      //flags
            IntentFilter intentFilter_UsbPermission = new IntentFilter("permission");
            BroadcastReceiver myUsbPermissionReceiver = new BroadcastReceiver(){

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if(action.equals("permission")){

                        synchronized(this) {
                            connection = manager.openDevice(device);
                            log("permission granted!");
                            log("connection: " + connection);
                        }
                    }
                }

            };
            registerReceiver(myUsbPermissionReceiver, intentFilter_UsbPermission);
            manager.requestPermission(device, PendingIntent_UsbPermission);
            // end permission request

            log("interface counter:" + device.getInterfaceCount());
            if (connection == null) {
                log("but connection is null :(");
            } else {
                log("connection: " + connection);
            }

        } else {
            log("using same connection!");
        }

    }
}
