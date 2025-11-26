"use strict";

// TTSManager.js

import { NativeModules, NativeEventEmitter } from 'react-native';
const {
  TTSManager
} = NativeModules;
const ttsManagerEmitter = new NativeEventEmitter(TTSManager);
const initialize = (modelId, debug = false, threadsUsed = 1) => {
  TTSManager.initializeTTS(22050, 1, modelId, debug, threadsUsed);
};
const stop = async () => {
  await TTSManager.stop();
};
const generateAndPlay = async (text, sid, speed) => {
  try {
    const result = await TTSManager.generateAndPlay(text, sid, speed);
    if (__DEV__) console.log(result);
    return result;
  } catch (error) {
    console.error(error);
    return "Error";
  }
};
const deinitialize = () => {
  TTSManager.deinitialize();
};
const addVolumeListener = callback => {
  const subscription = ttsManagerEmitter.addListener('VolumeUpdate', event => {
    const {
      volume
    } = event;
    callback(volume);
  });
  return subscription;
};
export default {
  initialize,
  generateAndPlay,
  deinitialize,
  addVolumeListener,
  stop
};
//# sourceMappingURL=index.js.map