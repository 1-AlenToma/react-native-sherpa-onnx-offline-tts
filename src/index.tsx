// TTSManager.js

import { NativeModules, NativeEventEmitter } from 'react-native';

const { TTSManager } = NativeModules;
const ttsManagerEmitter = new NativeEventEmitter(TTSManager);

type MGS = "PlaybackFinished" | "overwritten" | "Stopped" | "Error";

let onDone: ((msg?: MGS) => void) = () => { };

const initialize = async (modelId: string, debug: boolean = false, threadsUsed: number = 1) => {
  try {
    return await TTSManager.initializeTTS(22050, 1, modelId, debug, threadsUsed) as boolean;
  } catch (e) {
    console.error(e);
    return false;
  }
};

const stop = async () => {
  try {
    return await TTSManager.stop() as boolean;
  } catch (e) {
    console.error(e);
    return false;
  }
}

const generateAndPlay = async (item: { text: string, nextText?: string, sid?: number, speed?: number, onDone?: typeof onDone }) => {
  try {
    if (item.onDone)
      onDone = item.onDone;
    const result = await TTSManager.generateAndPlay(item.text, item.nextText ?? "", item.sid ?? 0, item.speed ?? 1);

    if (__DEV__)
      console.log(result);

    return result as string;
  } catch (error) {
    console.error(error);
    return "Error";
  }
};

const deinitialize = async () => {
  try {
    return await TTSManager.deinitialize() as boolean;
  } catch (e) {
    console.error(e);
    return false;
  }
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
