var exec = require('cordova/exec');

function CordovaMediaPicker() {}

CordovaMediaPicker.prototype.pick = function(options, successCallback, errorCallback) {
    
    var cameraCallback = function(result) {
        successCallback(result);
    };
    
    var interimCallback = function(result) {
        if (result === 'OPEN_CAMERA') {
            var args = [50, 0, 1, -1, -1, 0, 0, false, true, false, null, 0];
            exec(successCallback, errorCallback, 'Camera', 'takePicture', args);
        }
        successCallback(result);
    };
    
    exec(interimCallback, errorCallback, 'CordovaMediaPicker', 'pick', [options]);
    
};

module.exports = new CordovaMediaPicker();