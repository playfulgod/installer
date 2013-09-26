package com.techygeek.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends Activity {

    private ProgressBar mProgress;
    private Process mProcess;
    private Handler mProgressBarHandler = new Handler();

    public enum StreamLoggerTypes {InfoLog, ErrorLog}

    ;

    public class StreamLogger extends Thread {

        private InputStream mStream = null;
        private StreamLoggerTypes mLoggerType = StreamLoggerTypes.InfoLog;

        public StreamLogger(InputStream streamToLog, StreamLoggerTypes typeOfLog) {
            this.mStream = streamToLog;
            this.mLoggerType = typeOfLog;
        }

        @Override
        public void run() {
            try {
                if (this.mStream != null) {
                    java.io.BufferedReader reader = new BufferedReader(new InputStreamReader(this.mStream));

                    if (reader != null) {
                        String sLine = "";

                        Log.i("Installer", "Starting " + this.mLoggerType.toString() + " buffer handler");

                        while ((sLine = reader.readLine()) != null) {
                            if (this.mLoggerType == StreamLoggerTypes.InfoLog)
                                Log.i("Installer", sLine);

                            else if (this.mLoggerType == StreamLoggerTypes.ErrorLog)
                                Log.e("Installer", sLine);
                        }
                    } else Log.e("Installer", "Could not create buffer");
                } else Log.e("Installer", "Cannot buffer on a non-existing stream");
            } catch (IOException ex) {
                Log.e("Installer", ex.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Is this application running on a LG Motion l0?
        String line = "";
        String CorrectDevice = "l0_MPCS_US";
        String CorrectDevice2 = "l0";

        // Get property and save to String line, then compare with StringDevice
        // If the output from getprop doesnt't match CorrectDevice, display a warning.
        try {
            Process ifc = Runtime.getRuntime().exec("getprop ro.product.name");
            BufferedReader bis = new BufferedReader(new InputStreamReader(ifc.getInputStream()));
            line = bis.readLine();
        } catch (java.io.IOException e) {
        }

        if (!line.equals(CorrectDevice)) {
            if (!line.equals(CorrectDevice2)) {
                AlertDialog.Builder IncorrectDevice = new AlertDialog.Builder(this);
                IncorrectDevice.setTitle("Wrong Device!");
                IncorrectDevice.setMessage("You are probably not running this application on the LG Motion 4G. Please note that this application is intended to run on the LG Motion 4G. If you are using a LG Motion 4G, please contact your ROM developer and tell him, that the \nro.product.name should be l0_MPCS_US.");
                IncorrectDevice.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                IncorrectDevice.show();
            }
        }


        //set the progressbar
        mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mProgress.setVisibility(View.GONE);

        // Backup button
        final Button button_backup = (Button) findViewById(R.id.button_backup);
        button_backup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File directory = new File("/sdcard/external_sd/MyDataBackup");
                directory.mkdirs();
                Runtime runtime = Runtime.getRuntime();
                OutputStreamWriter osw = null;

                String command = "tar -cf /sdcard/external_sd/MyDataBackup/data.tar /data"; //$(date +%Y%m%d_%H%M%S)-

                try { // Run Script

                    mProcess = runtime.exec("su");
                    osw = new OutputStreamWriter(mProcess.getOutputStream());
                    osw.write(command);
                    osw.flush();
                    osw.close();

                    Context context = getApplicationContext();
                    CharSequence text = "Backing Data up!";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();

                    //this sets the progressbar to show
                    mProgress.setVisibility(View.VISIBLE);

                    //set a thread to wait for the process to finish and when it does
                    //set the progressbar to invisible
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.i("Installer", "Starting wait on process thread...");

                                //handle both input and error streams
                                StreamLogger loggerInput = new StreamLogger(mProcess.getInputStream(), StreamLoggerTypes.InfoLog);
                                StreamLogger loggerError = new StreamLogger(mProcess.getErrorStream(), StreamLoggerTypes.ErrorLog);

                                //process the buffers and only then wait
                                loggerInput.start();
                                loggerError.start();

                                //wait for the process to finish running
                                mProcess.waitFor();

                                //when it does post a handler message to the progressbar to disappear
                                mProgressBarHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i("Installer", "Process done - hiding progressbar");
                                        mProgress.setVisibility(View.GONE);
                                    }
                                });
                            } catch (Exception ex) {
                                Log.e("Installer", ex.getMessage());
                            }
                        }
                    }).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Restore button
        final Button button_restore = (Button) findViewById(R.id.button_restore);
        button_restore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v == button_restore) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to restore data?? Your Device will reboot after data has been restored!!!");
                    builder.setTitle("Confirmation Dialog");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // do something after confirm
                            Runtime runtime = Runtime.getRuntime();
                            OutputStreamWriter osw = null;

                            String command = "tar -xf /sdcard/external_sd/MyDataBackup/data.tar -C / && /system/bin/reboot";

                            try { // Run Script

                                mProcess = runtime.exec("su");
                                osw = new OutputStreamWriter(mProcess.getOutputStream());
                                osw.write(command);
                                osw.flush();
                                osw.close();

                                Context context = getApplicationContext();
                                CharSequence text = "Restoring Data Backup!";
                                int duration = Toast.LENGTH_LONG;

                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();

                                //this sets the progressbar to show
                                mProgress.setVisibility(View.VISIBLE);

                                //set a thread to wait for the process to finish and when it does
                                //set the progressbar to invisible
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.i("Installer", "Starting wait on process thread...");

                                            //handle both input and error streams
                                            StreamLogger loggerInput = new StreamLogger(mProcess.getInputStream(), StreamLoggerTypes.InfoLog);
                                            StreamLogger loggerError = new StreamLogger(mProcess.getErrorStream(), StreamLoggerTypes.ErrorLog);

                                            //process the buffers and only then wait
                                            loggerInput.start();
                                            loggerError.start();

                                            //wait for the process to finish running
                                            mProcess.waitFor();

                                            //when it does post a handler message to the progressbar to disappear
                                            mProgressBarHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.i("Installer", "Process done - hiding progressbar");
                                                    mProgress.setVisibility(View.GONE);
                                                }
                                            });
                                        } catch (Exception ex) {
                                            Log.e("Installer", ex.getMessage());
                                        }
                                    }
                                }).start();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.create().show();
                }
            }
        });

        // Install Recovery Button
        final Button button_installrec = (Button) findViewById(R.id.button_installrec);
        button_installrec.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v == button_installrec) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to download & install CWM Recovery??? A network connection (WiFi or Data) is required!!!");
                    builder.setTitle("Confirmation Dialog");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // do something after confirm
                            Runtime runtime = Runtime.getRuntime();
                            OutputStreamWriter osw = null;

                            String command = "curl http://unleashedprepaids.com/upload/devs/playfulgod/phones/LG/MS770/l0-cwm.lok > /data/local/tmp/l0-cwm.lok  && dd if=/data/local/tmp/l0-cwm.lok of=/dev/block/platform/msm_sdcc.1/by-name/recovery && rm /data/local/tmp/l0-cwm.lok";

                            try { // Run Script

                                mProcess = runtime.exec("su");
                                osw = new OutputStreamWriter(mProcess.getOutputStream());
                                osw.write(command);
                                osw.flush();
                                osw.close();

                                Context context = getApplicationContext();
                                CharSequence text = "Downloading and Installing Recovery";
                                int duration = Toast.LENGTH_SHORT;

                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                                //this sets the progressbar to show
                                mProgress.setVisibility(View.VISIBLE);

                                //set a thread to wait for the process to finish and when it does
                                //set the progressbar to invisible
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.i("Installer", "Starting wait on process thread...");

                                            //handle both input and error streams
                                            StreamLogger loggerInput = new StreamLogger(mProcess.getInputStream(), StreamLoggerTypes.InfoLog);
                                            StreamLogger loggerError = new StreamLogger(mProcess.getErrorStream(), StreamLoggerTypes.ErrorLog);

                                            //process the buffers and only then wait
                                            loggerInput.start();
                                            loggerError.start();

                                            //wait for the process to finish running
                                            mProcess.waitFor();

                                            //when it does post a handler message to the progressbar to disappear
                                            mProgressBarHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.i("Installer", "Process done - hiding progressbar");
                                                    mProgress.setVisibility(View.GONE);
                                                }
                                            });
                                        } catch (Exception ex) {
                                            Log.e("Installer", ex.getMessage());
                                        }
                                    }
                                }).start();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.create().show();
                }
            }
        });

        // Reboot to Recovery Button
        final Button button_rebootrec = (Button) findViewById(R.id.button_rebootrec);
        button_rebootrec.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v == button_rebootrec) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to reboot to recovery? A Factory Reset will occur, make sure to have made a backup!!! This will also move this app to system/app so it remains installed. Manual removal will be required!!!");
                    builder.setTitle("Confirmation Dialog");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // do something after confirm
                            Runtime runtime = Runtime.getRuntime();
                            Process proc = null;
                            OutputStreamWriter osw = null;

                            String command = "mount -o remount,rw -t ext4 /dev/block/platform/msm_sdcc.1/by-name/system /system && cp /data/app/com.techygeek.installer-*.apk /system/app/ && /system/bin/reboot recovery";

                            try { // Run Script

                                proc = runtime.exec("su");
                                osw = new OutputStreamWriter(proc.getOutputStream());
                                osw.write(command);
                                osw.flush();
                                osw.close();

                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Rebooting to Recovery", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.create().show();
                }
            }
        });

        // Donate Button
        final Button button_donate = (Button) findViewById(R.id.button_donate);
        button_donate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Runtime runtime = Runtime.getRuntime();
                Process proc = null;
                OutputStreamWriter osw = null;

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.techygeek.installer.donate"));
                startActivity(intent);

                Context context = getApplicationContext();
                CharSequence text = "linking to donate version";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(this, About.class));
                return true;
            case R.id.disclaimer:
                startActivity(new Intent(this, Disclaimer.class));
                return true;
            case R.id.credits:
                startActivity(new Intent(this, Credits.class));
                return true;
            case R.id.quit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}