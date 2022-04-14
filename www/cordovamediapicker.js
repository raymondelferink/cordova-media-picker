var exec = require('cordova/exec');

function CordovaMediaPicker() {}

CordovaMediaPicker.prototype.pick = function(options, successCallback, errorCallback) {
    
    options = options || {};

    var picker_options = [0, 0, 0, 0, 0, 0];
    
    if ("all" in options && options.all) {
        picker_options = [1, 1, 1, 1, 1, 1];
    } else {
        picker_options = [
            options.camera?1:0,
            options.gallery?1:0,
            options.video?1:0,
            options.file?1:0,
            options.audiorecorder?1:0,
            options.videorecorder?1:0 
        ];
    }
    
//    var cordovaPluginCameraInstalled = (navigator.camera && navigator.camera.getPicture)?true:false;
//    if (!cordovaPluginCameraInstalled) {
//        //only do this on android
//        picker_options[0] = 0;
//    }
    
    var cameraCallback = function(result) {
        var results = [{
            type: 'image/jpeg',
            base64: result
        }]
        successCallback(results);
    };
    
    var catchCallback = function(result) {
        if (cordovaPluginCameraInstalled && result === 'OPEN_CAMERA') {
            var args = [
                50,         //var quality = getValue(options.quality, 50);
                0,          //var destinationType = getValue(options.destinationType, Camera.DestinationType.FILE_URI);
                1,          //var sourceType = getValue(options.sourceType, Camera.PictureSourceType.CAMERA);
                -1,         //var targetWidth = getValue(options.targetWidth, -1);
                -1,         //var targetHeight = getValue(options.targetHeight, -1);
                0,          //var encodingType = getValue(options.encodingType, Camera.EncodingType.JPEG);
                0,          //var mediaType = getValue(options.mediaType, Camera.MediaType.PICTURE);
                false,      //var allowEdit = !!options.allowEdit;
                true,       //var correctOrientation = !!options.correctOrientation;
                false,      //var saveToPhotoAlbum = !!options.saveToPhotoAlbum;
                null,       //var popoverOptions = getValue(options.popoverOptions, null);
                0           //var cameraDirection = getValue(options.cameraDirection, Camera.Direction.BACK);
            ];
            exec(cameraCallback, errorCallback, 'Camera', 'takePicture', args);
        }
        successCallback(result);
    };
    
    exec(catchCallback, errorCallback, 'CordovaMediaPicker', 'pick', [picker_options]);
    
};

module.exports = new CordovaMediaPicker();