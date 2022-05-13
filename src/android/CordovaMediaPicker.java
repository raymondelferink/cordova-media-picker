package com.raycom.cordova.plugin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.util.Base64;
import android.util.Log;

// Cordova-required packages
import org.apache.cordova.BuildHelper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import org.apache.cordova.camera.CameraLauncher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CordovaMediaPicker extends CordovaPlugin {
    public CallbackContext callbackContext;
    public Context context = null;
    private static final boolean IS_AT_LEAST_LOLLIPOP = Build.VERSION.SDK_INT >= 21;

    public static final int REQUEST_CODE = 1;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int CHOOSE_FILE_SEC = 1;
    public static final int CHOOSE_IMAGE_SEC = 2;
    public static final int CHOOSE_VIDEO_SEC = 3;
    public static final int CHOOSE_AUDIORECORDER_SEC = 4;
    public static final int CHOOSE_CAMERA_SEC = 5;
    public static final int CHOOSE_VIDEORECORDER_SEC = 6;
    public static final int ACTION_FILE = 1;
    public static final int ACTION_IMAGE = 2;
    public static final int ACTION_VIDEO = 3;
    public static final int ACTION_AUDIORECORDER = 4;
    public static final int ACTION_CAMERA = 5;
    public static final int ACTION_VIDEORECORDER = 6;

    public static final long AUDIO_MAX_BYTES = (long) 1024L * 1024L; // max 1MB, roughly 1 minute
    public static final long VIDEO_MAX_DURATION = 30; // max 30 seconds
    public static final long VIDEO_QUALITY = 1; // video quality between 0 and 1
    
    private int currentAction;              // Current action
    private String applicationId;
    private String[] mimetypes = { "image/*", "video/*", "audio/*", "application/pdf", "text/plain" };

    @Override
    public boolean execute(String action, JSONArray args,
      final CallbackContext callbackContext) {
        /* Verify that the user sent a 'pick' action */
        if (!action.equals("pick")) {
            callbackContext.error("\"" + action + "\" is not a supported action.");
            return false;
        }
        this.applicationId = (String) BuildHelper.getBuildConfigValue(cordova.getActivity(), "APPLICATION_ID");
        this.applicationId = preferences.getString("applicationId", this.applicationId);

        this.callbackContext = callbackContext;

        context = IS_AT_LEAST_LOLLIPOP ? cordova.getActivity().getWindow().getContext() : cordova.getActivity().getApplicationContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        List<String> optionlist = new ArrayList<String>();
        
        int optionCount = 0;
        int mimeCount = 0;
        try {
            JSONObject argoptions = args.getJSONObject(0);
            if (argoptions.has("camera") && argoptions.getInt("camera") == 1) {
                optionlist.add("Camera");
                optionCount++;
            }
            if (argoptions.has("gallery") && argoptions.getInt("gallery") == 1) {
                optionlist.add("Gallery");
                optionCount++;
            }
            if (argoptions.has("video") && argoptions.getInt("video") == 1) {
                optionlist.add("Video");
                optionCount++;
            }
            if (argoptions.has("file") && argoptions.getInt("file") == 1) {
                optionlist.add("File");
                optionCount++;
            }
            if (argoptions.has("audiorecorder") && argoptions.getInt("audiorecorder") == 1) {
                optionlist.add("Audio Recorder");
                optionCount++;
            }
            if (argoptions.has("videorecorder") && argoptions.getInt("videorecorder") == 1) {
                optionlist.add("Video Recorder");
                optionCount++;
            }
            if (argoptions.has("filetypes")) {
                JSONObject filetypeoptions = argoptions.getJSONObject("filetypes");
                List<String> mimelist = new ArrayList<String>();
                if (filetypeoptions.has("photo") && filetypeoptions.getInt("photo") == 1) {
                    mimelist.add("image/*");
                    mimeCount++;
                }
                if (filetypeoptions.has("video") && filetypeoptions.getInt("video") == 1) {
                    mimelist.add("video/*");
                    mimeCount++;
                }
                if (filetypeoptions.has("audio") && filetypeoptions.getInt("audio") == 1) {
                    mimelist.add("audio/*");
                    mimeCount++;
                }
                if (filetypeoptions.has("file") && filetypeoptions.getInt("file") == 1) {
                    mimelist.add("application/pdf");
                    mimelist.add("text/plain");
                    mimeCount++;
                }
                if (mimeCount > 0) {
                    this.mimetypes = mimelist.toArray(new String[0]);
                }
            }
        } catch (JSONException e) {
            // do nothing, this results in all options being active
            // e.printStackTrace();
        }
        if (optionCount == 0) {
            optionlist.add("Camera");
            optionlist.add("Gallery");
            optionlist.add("Video");
            optionlist.add("File");
            optionlist.add("Audio Recorder");
            optionlist.add("Video Recorder");
            optionCount = 6;
        }
        if (optionCount > 1) {
            optionlist.add("Cancel");
        }
        
        String[] options = optionlist.toArray(new String[0]);

        if (optionCount == 1) {
             handleOption(options[0]);
        } else {
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handleOption(options[which]);
                }
            });
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();

        }
        
        return true;
        
    }

    public void handleOption (String option) {
        switch (option) {
            case "Camera":
                // callbackContext.success("OPEN_CAMERA");
                chooseCamera(callbackContext);
                break;
            case "Gallery": // Gallery
                chooseImage(callbackContext);
                break;
            case "Video": // Video
                chooseVideo(callbackContext);
                break;
            case "File": // File
                chooseFile(callbackContext);
                break;
            case "Audio Recorder": // Audio Recorder
                chooseAudioRecorder(callbackContext);
                break;
            case "Video Recorder": // Video Recorder
                chooseVideoRecorder(callbackContext);
                break;
            case "Cancel": // Cancel
                callbackContext.error("Action cancelled");
                break;
        }
    }

    public void chooseCamera (CallbackContext callbackContext) {
        this.currentAction = ACTION_CAMERA;

        if(!PermissionHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            PermissionHelper.requestPermission(this, CHOOSE_CAMERA_SEC, Manifest.permission.CAMERA);
        } else {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cordova.startActivityForResult(this, intent, REQUEST_CODE);
        }

    }

    public void chooseImage (CallbackContext callbackContext) {
        this.currentAction = ACTION_IMAGE;

        if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionHelper.requestPermission(this, CHOOSE_IMAGE_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            Intent chooser = Intent.createChooser(intent, "Select Image");
            cordova.startActivityForResult(this, chooser, REQUEST_CODE);
        }

    }

    public void chooseFile (CallbackContext callbackContext) {
        this.currentAction = ACTION_FILE;
        if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionHelper.requestPermission(this, CHOOSE_FILE_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(this.mimetypes[0]);
            
            intent.putExtra(Intent.EXTRA_MIME_TYPES, this.mimetypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            Intent chooser = Intent.createChooser(intent, "Select File");
            cordova.startActivityForResult(this, chooser, REQUEST_CODE);
        }
    }

    public void chooseVideo (CallbackContext callbackContext) {
        this.currentAction = ACTION_VIDEO;

        if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionHelper.requestPermission(this, CHOOSE_VIDEO_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            Intent chooser = Intent.createChooser(intent, "Select Video");
            cordova.startActivityForResult(this, chooser, REQUEST_CODE);
        }

    }

    public void chooseAudioRecorder (CallbackContext callbackContext) {
        this.currentAction = ACTION_AUDIORECORDER;

        if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionHelper.requestPermission(this, CHOOSE_AUDIORECORDER_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            intent.putExtra("android.provider.MediaStore.extra.MAX_BYTES", AUDIO_MAX_BYTES);
            cordova.startActivityForResult(this, intent, REQUEST_CODE);
        }

    }

    public void chooseVideoRecorder (CallbackContext callbackContext) {
        this.currentAction = ACTION_VIDEORECORDER;

        if(!PermissionHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            PermissionHelper.requestPermission(this, CHOOSE_VIDEORECORDER_SEC, Manifest.permission.CAMERA);
        } else {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra("android.intent.extra.durationLimit", VIDEO_MAX_DURATION);
            intent.putExtra("android.intent.extra.videoQuality", VIDEO_QUALITY);
            cordova.startActivityForResult(this, intent, REQUEST_CODE);
        }

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case CHOOSE_FILE_SEC:
                chooseFile(this.callbackContext);
                break;
            case CHOOSE_IMAGE_SEC:
                chooseImage(this.callbackContext);
                break;
            case CHOOSE_VIDEO_SEC:
                chooseVideo(this.callbackContext);
                break;
            case CHOOSE_AUDIORECORDER_SEC:
                chooseAudioRecorder(this.callbackContext);
                break;
            case CHOOSE_CAMERA_SEC:
                chooseCamera(this.callbackContext);
                break;
        }
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        

        if (resultCode == Activity.RESULT_OK) {

            switch(this.currentAction){
                case ACTION_IMAGE:
                case ACTION_FILE:
                case ACTION_VIDEO:
                case ACTION_AUDIORECORDER:
                case ACTION_CAMERA:
                case ACTION_VIDEORECORDER:
                    handleFile(data);
                    break;
                default:
                    this.callbackContext.error("Execute failed");
            }
            
        } else {
            this.callbackContext.error("Execute failed");
        }
    }

    private void handleFile(Intent data){
        JSONArray results = new JSONArray();
        Uri uri = null;
        ClipData clipData = null;

        if (data != null) {
            uri = data.getData();
            clipData = data.getClipData();
        }
        if (uri != null) {
            results.put(getMetadata(uri));
        } else if (clipData != null && clipData.getItemCount() > 0) {
            final int length = clipData.getItemCount();
            for (int i = 0; i < length; ++i) {
                ClipData.Item item = clipData.getItemAt(i);
                results.put(getMetadata(item.getUri()));
            }
        }
        this.callbackContext.success(results.toString());
    }

    private Object getMetadata(Uri uri) {
        try {
            JSONObject result = new JSONObject();


            String uriString = uri.toString();
            Log.d("data", "onActivityResult: uri"+uriString);

            result.put("uri", uriString);
            ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();

            String typeString = contentResolver.getType(uri);
            

            Cursor cursor = contentResolver.query(uri, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (!cursor.isNull(displayNameIndex)) {
                    result.put("name", cursor.getString(displayNameIndex));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                    if (!cursor.isNull(mimeIndex)) {
                        typeString = cursor.getString(mimeIndex);
                    }
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    result.put("size", cursor.getInt(sizeIndex));
                }
            }
            
            result.put("type", typeString);
            if (typeString.startsWith("image") || typeString.startsWith("application")) {
                try {
                    InputStream in = contentResolver.openInputStream(uri);
                    byte[] bytes = getBytes(in);
                    Log.d("data", "onActivityResult: bytes size="+bytes.length);
                    Log.d("data", "onActivityResult: Base64string="+Base64.encodeToString(bytes,Base64.DEFAULT));
                    String ansValue = Base64.encodeToString(bytes,Base64.DEFAULT);
                    result.put("base64", ansValue);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    Log.d("error", "onActivityResult: " + e.toString());
                }
            }
            
            return result;
        } catch (JSONException err) {
            return "Error";
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
