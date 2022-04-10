var exec = require('cordova/exec');

function CordovaMediaPicker() {}

CordovaMediaPicker.prototype.pick = function(options, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'CordovaMediaPicker', 'pick', [options]);
};

module.exports = new CordovaMediaPicker();