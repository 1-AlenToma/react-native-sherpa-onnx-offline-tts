"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _reactNative = require("react-native");
// TTSManager.js

const {
  TTSManager
} = _reactNative.NativeModules;
const ttsManagerEmitter = new _reactNative.NativeEventEmitter(TTSManager);
let onDone = () => {};
//sampleRate could be found in modol_card
const initialize = async (modelId, sampleRate = 22050, debug = false, threadsUsed = 1) => {
  try {
    return await TTSManager.initializeTTS(sampleRate, 1, modelId, debug, threadsUsed);
  } catch (e) {
    console.error(e);
    return false;
  }
};
const stop = async () => {
  try {
    return await TTSManager.stop();
  } catch (e) {
    console.error(e);
    return false;
  }
};
const generateAndPlay = async item => {
  try {
    if (item.onDone) onDone = item.onDone;
    const result = await TTSManager.generateAndPlay(item.text, item.nextText ?? "", item.sid ?? 0, item.speed ?? 1);
    if (__DEV__) console.log(result);
    return result;
  } catch (error) {
    console.error(error);
    return "Error";
  }
};
const deinitialize = async () => {
  try {
    return await TTSManager.deinitialize();
  } catch (e) {
    console.error(e);
    return false;
  }
};
const onDoneSubscription = ttsManagerEmitter.addListener('onDone', event => {
  const {
    msg
  } = event;
  onDone?.(msg);
});
const addVolumeListener = callback => {
  const subscription = ttsManagerEmitter.addListener('VolumeUpdate', event => {
    const {
      volume
    } = event;
    callback(volume);
  });
  return subscription;
};
var _default = exports.default = {
  initialize,
  generateAndPlay,
  deinitialize,
  addVolumeListener,
  stop,
  onDoneSubscription
};
//# sourceMappingURL=index.js.map