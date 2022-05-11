var exec = require('cordova/exec');

function CordovaMediaPicker() {}

CordovaMediaPicker.prototype.pick = function(options, successCallback, errorCallback) {
    options = options || {};

    var default_filetypes = {
        photo: 1,
        video: 1,
        audio: 1,
        file: 1
    };

    var pick_options = {};
    pick_options.camera = (options.camera || options.all)?1:0;
    pick_options.gallery = (options.gallery || options.all)?1:0;
    pick_options.video = (options.video || options.all)?1:0;
    pick_options.file = (options.file || options.all)?1:0;
    pick_options.audiorecorder = (options.audiorecorder || options.all)?1:0;
    pick_options.videorecorder = (options.camera || options.all)?1:0;
    pick_options.filetypes = options.filetypes || default_filetypes;
    
    var ios = options.ios?true:false;
    var cordovaPluginAudioInstalled = (navigator.device && navigator.device.capture && navigator.device.capture.captureAudio)?true:false;
    if (ios && !cordovaPluginAudioInstalled) {
        //only do this on ios
        pick_options.audiorecorder = 0;
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
    
    exec(catchCallback, errorCallback, 'CordovaMediaPicker', 'pick', [pick_options]);
    
};

module.exports = new CordovaMediaPicker();