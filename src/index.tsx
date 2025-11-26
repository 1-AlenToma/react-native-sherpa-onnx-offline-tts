// TTSManager.js

import { NativeModules, NativeEventEmitter } from 'react-native';

const { TTSManager } = NativeModules;
const ttsManagerEmitter = new NativeEventEmitter(TTSManager);

type MGS = "PlaybackFinished" | "overwritten" | "Stopped" | "Error";

let onDone: ((msg?: MGS) => void) = () => { };

const initialize = (modelId: string, debug: boolean = false, threadsUsed: number = 1) => {
  TTSManager.initializeTTS(22050, 1, modelId, debug, threadsUsed);
};

const stop = async () => {
  await TTSManager.stop();
}

const generateAndPlay = async (item: { text: string, sid?: number, speed?: number, onDone?: typeof onDone }) => {
  try {
    if (item.onDone)
      onDone = item.onDone;
    const result = await TTSManager.generateAndPlay(item.text, item.sid ?? 0, item.speed ?? 1);

    if (__DEV__)
      console.log(result);

    return result as string;
  } catch (error) {
    console.error(error);
    return "Error";
  }
};

const deinitialize = () => {
  TTSManager.deinitialize();
};


const onDoneSubscription = ttsManagerEmitter.addListener(
  'onDone',
  (event) => {
    const { msg } = event;
    onDone?.(msg as MGS)
  }
);


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
  stop,
  onDoneSubscription
};
