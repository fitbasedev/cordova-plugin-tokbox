function TokBox() {}

TokBox.prototype.PluginName = "TokBox";

TokBox.prototype.pInvoke = function(method, data, callbackOK, callbackError){
    if (data == null || data === undefined) {
        data = [];
    }
    else if (!Array.isArray(data)) {
        data = [data];
    }
    cordova.exec(callbackOK, callbackError, this.PluginName, method, data);
};

TokBox.prototype.startStream = function(data, callbackOK, callbackError){
    this.pInvoke("startStream", data, callbackOK, callbackError);
};

TokBox.install = function()
{
    if (!window.plugins) {
        window.plugins = {};
    }
    
    window.plugins.tokbox = new TokBox();
    return window.plugins.tokbox;
};

cordova.addConstructor(TokBox.install);
