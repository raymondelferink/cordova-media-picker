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
    var ios = options.ios?true:false;
    var cordovaPluginAudioInstalled = (navigator.device && navigator.device.capture && navigator.device.capture.captureAudio)?true:false;
    if (ios && !cordovaPluginAudioInstalled) {
        //only do this on android
        picker_options[4] = 0;
    }
    
    var audioCallback = function(result) {
        if (result && result[0] && result[0].localURL) {
            result[0].uri = result[0].localURL;
        }
        successCallback(result);
    };
    
    var catchCallback = function(result) {
        if (cordovaPluginAudioInstalled && result === 'OPEN_AUDIORECORDER') {
            var args = {
                limit: 1, 
                duration: 60
            };
            exec(audioCallback, errorCallback, 'Capture', 'captureAudio', [args]);
        } else {
            successCallback(result);
        }
    };
    
    exec(catchCallback, errorCallback, 'CordovaMediaPicker', 'pick', [picker_options]);
    
};

module.exports = new CordovaMediaPicker();