package com.raycom.cordova.plugin;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
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
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CordovaMediaPicker extends CordovaPlugin implements MediaScannerConnectionClient {
    private CallbackContext callback;
    private boolean base64;
    private int mQuality;                   // Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
    protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int CAMERA = 1;                // Take picture from camera
    private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)
    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                  // Take a picture of type PNG
    private static final String JPEG_TYPE = "jpg";
    private static final String PNG_TYPE = "png";
    private static final String JPEG_EXTENSION = "." + JPEG_TYPE;
    private static final String PNG_EXTENSION = "." + PNG_TYPE;
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    public static final int REQUEST_CODE = 1;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int CHOOSE_CAMERA_SEC = 0;
    public static final int CHOOSE_FILE_SEC = 1;
    public static final int CHOOSE_IMAGE_SEC = 2;
    public static final int CHOOSE_VIDEO_SEC = 3;
    public static final int ACTION_CAMERA = 0;
    public static final int ACTION_FILE = 1;
    public static final int ACTION_IMAGE = 2;
    public static final int ACTION_VIDEO = 3;

    private Uri imageUri;                   // Uri of captured image
    private int currentAction;              // Current action

    @Override
    public boolean execute(String action, JSONArray args,
      final CallbackContext callbackContext) {
        /* Verify that the user sent a 'pick' action */
        if (!action.equals("pick")) {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }
        this.mQuality = 50;
        this.callbackContext = callbackContext;
        chooseFile(callbackContext);
        
        return true;
        
    }

    public void chooseCamera (CallbackContext callbackContext) {
        this.currentAction = ACTION_CAMERA;
        List<String> permissions = new ArrayList<String>();
        boolean setPermissions = false;
        if (!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PermissionHelper.hasPermission(this, Manifest.permission.CAMERA) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (permissions.size()) {
            String[] simpleArray = new String[ permissions.size() ];
            permissions.toArray( simpleArray );
            PermissionHelper.requestPermissions(this, CHOOSE_CAMERA_SEC, permissions);

        } else {
            //todo camera stuff
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Specify file so that large image is captured and returned
            File photo = createCaptureFile(JPEG, "");
            this.imageFilePath = photo.getAbsolutePath();
            this.imageUri = FileProvider.getUriForFile(cordova.getActivity(),
                    applicationId + ".cordova.media.picker",
                    photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            //We can write to this URI, this will hopefully allow us to write files to get to the next step
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            PackageManager mPm = this.cordova.getActivity().getPackageManager();
            if(intent.resolveActivity(mPm) != null) {
                cordova.startActivityForResult((CordovaPlugin) this, intent, (CAMERA + 1) * 16 + DATA_URL + 1);
            } else {
                LOG.d("CameraLauncher", "Error: You don't have a default camera.  Your device may not be CTS complaint.");
            }
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
            case CHOOSE_CAMERA_SEC:
                chooseCamera(this.callbackContext);
                break;
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
                case ACTION_CAMERA:
                    handleCamera(data);
                    break;
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

    private void handleCamera(Intent data){
        int rotate = 0;
        String sourcePath = this.imageFilePath;

        // Create an ExifHelper to save the exif data that is lost during compression
        ExifHelper exif = new ExifHelper();

        try {
            exif.createInFile(sourcePath);
            exif.readExifData();
            rotate = exif.getOrientation();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = null;

        bitmap = getScaledAndRotatedBitmap(sourcePath);

        if (bitmap == null) {
            // Try to get the bitmap from intent.
            bitmap = (Bitmap) data.getExtras().get("data");
        }

        // Double-check the bitmap.
        if (bitmap == null) {
            this.callbackContext.error("Unable to create bitmap!");
            return;
        }

        this.processPicture(bitmap, this.encodingType);

        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();

        try {
            if (bitmap.compress(CompressFormat.JPEG, mQuality, jpeg_data)) {
                byte[] code = jpeg_data.toByteArray();
                // byte[] output = Base64.encode(code, Base64.NO_WRAP);
                // String js_out = new String(output);
                String ansValue = Base64.encodeToString(code,Base64.DEFAULT);

                JSONArray results = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("base64", ansValue);
                result.put("name", System.currentTimeMillis() + "." + JPEG_TYPE;
                result.put("type", JPEG_MIME_TYPE);
                results.put(result);

                this.callbackContext.success(results.toString());
                code = null;
            }
        } catch (Exception e) {
            this.callbackContext.error("Error compressing image: "+e.getLocalizedMessage());
        }
        jpeg_data = null;


        // check this out:
        // checkForDuplicateImage(DATA_URL);
        
        // cleanup
        if (bitmap != null) {
            bitmap.recycle();
        }
        // Clean up initial camera-written image file.
        (new File(FileHelper.stripFileProtocol(this.imageUri.toString()))).delete();

        System.gc();
        bitmap = null;

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

            try {
                Context context = cordova.getContext();
                InputStream in = context.getContentResolver().openInputStream(uri);
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

            result.put("uri", uri.toString());
            ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();

            result.put("type", contentResolver.getType(uri));
            Cursor cursor = contentResolver.query(uri, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (!cursor.isNull(displayNameIndex)) {
                    result.put("name", cursor.getString(displayNameIndex));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                    if (!cursor.isNull(mimeIndex)) {
                        result.put("type", cursor.getString(mimeIndex));
                    }
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    result.put("size", cursor.getInt(sizeIndex));
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
    
    private File createCaptureFile(int encodingType, String fileName) {
        if (fileName.isEmpty()) {
            fileName = ".Pic";
        }

        if (encodingType == JPEG) {
            fileName = fileName + JPEG_EXTENSION;
        } else if (encodingType == PNG) {
            fileName = fileName + PNG_EXTENSION;
        } else {
            throw new IllegalArgumentException("Invalid Encoding Type: " + encodingType);
        }

        return new File(getTempDirectoryPath(), fileName);
    }

    private String getTempDirectoryPath() {
        File cache = cordova.getActivity().getCacheDir();
        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }
}
