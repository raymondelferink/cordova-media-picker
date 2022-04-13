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
    public static final int ACTION_FILE = 1;
    public static final int ACTION_IMAGE = 2;
    public static final int ACTION_VIDEO = 3;
    
    private int currentAction;              // Current action
    private String applicationId;

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
        try {
            if (!args.isNull(0) && args.get(0) == Boolean.TRUE) {
                optionlist.add("Camera");
                optionCount++;
            }
            if (!args.isNull(1) && args.get(1) == Boolean.TRUE) {
                optionlist.add("Gallery");
                optionCount++;
            }
            if (!args.isNull(2) && args.get(2) == Boolean.TRUE) {
                optionlist.add("Video");
                optionCount++;
            }
            if (!args.isNull(3) && args.get(3) == Boolean.TRUE) {
                optionlist.add("File");
                optionCount++;
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
            optionCount = 4;
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
                callbackContext.success("OPEN_CAMERA");
                JSONArray data = new JSONArray();
                /*
                data.put(50);   //var quality = getValue(options.quality, 50);
                data.put(0);   //var destinationType = getValue(options.destinationType, Camera.DestinationType.FILE_URI);
                data.put(1);   //var sourceType = getValue(options.sourceType, Camera.PictureSourceType.CAMERA);
                data.put(-1);   //var targetWidth = getValue(options.targetWidth, -1);
                data.put(-1);   //var targetHeight = getValue(options.targetHeight, -1);
                data.put(0);   //var encodingType = getValue(options.encodingType, Camera.EncodingType.JPEG);
                data.put(0);   //var mediaType = getValue(options.mediaType, Camera.MediaType.PICTURE);
                data.put(false);   //var allowEdit = !!options.allowEdit;
                data.put(true);   //var correctOrientation = !!options.correctOrientation;
                data.put(false);   //var saveToPhotoAlbum = !!options.saveToPhotoAlbum;
                data.put(null);   //var popoverOptions = getValue(options.popoverOptions, null);
                data.put(0);   //var cameraDirection = getValue(options.cameraDirection, Camera.Direction.BACK);
                try {
                    CameraLauncher Camera = new CameraLauncher();
                    CallbackContext newCallbackContext = callbackContext;

                    Camera.execute("takePicture", data, newCallbackContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("error", "onActivityResult: " + e.toString());
                    callbackContext.error("Call CameraLauncher failed");
                }
                */
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
            case "Cancel": // Cancel
                callbackContext.error("Action cancelled");
                break;
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
            intent.setType("*/*");
            String[] mimetypes = { "image/*", "video/*", "audio/*", "application/pdf" };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
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
        }
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        

        if (resultCode == Activity.RESULT_OK) {

            switch(this.currentAction){
                case ACTION_IMAGE:
                case ACTION_FILE:
                case ACTION_VIDEO:
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
