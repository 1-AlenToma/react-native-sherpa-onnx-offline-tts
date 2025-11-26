// TTSManager.js

import { NativeModules, NativeEventEmitter } from 'react-native';

const { TTSManager } = NativeModules;
const ttsManagerEmitter = new NativeEventEmitter(TTSManager);

const initialize = (modelId: string, debug: boolean = false, threadsUsed: number = 1) => {
  TTSManager.initializeTTS(22050, 1, modelId, debug, threadsUsed);
};

const stop = async () => {
  await TTSManager.stop();
}

const generateAndPlay = async (text: any, sid: any, speed: any) => {
  try {
    const result = await TTSManager.generateAndPlay(text, sid, speed);
    if (__DEV__)
      console.log(result);

    return result as "PlaybackFinished" | "overwritten";
  } catch (error) {
    console.error(error);
    return "Error";
  }


};

const deinitialize = () => {
  TTSManager.deinitialize();
};

const addVolumeListener = (callback: any) => {
  const subscription = ttsManagerEmitter.addListener(
    'VolumeUpdate',
    (event) => {
      const { volume } = event;
      callback(volume);
    }
  );
  return subscription;
};

export default {
  initialize,
  generateAndPlay,
  deinitialize,
  addVolumeListener,
  stop
};
