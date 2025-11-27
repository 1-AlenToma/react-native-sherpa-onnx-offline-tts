type MGS = "PlaybackFinished" | "overwritten" | "Stopped" | "Error";
declare let onDone: ((msg?: MGS) => void);
declare const _default: {
    initialize: (modelId: string, sampleRate?: number, debug?: boolean, threadsUsed?: number) => Promise<boolean>;
    generateAndPlay: (item: {
        text: string;
        nextText?: string;
        sid?: number;
        speed?: number;
        onDone?: typeof onDone;
    }) => Promise<string>;
    deinitialize: () => Promise<boolean>;
    addVolumeListener: (callback: any) => import("react-native").EmitterSubscription;
    stop: () => Promise<boolean>;
    onDoneSubscription: import("react-native").EmitterSubscription;
};
export default _default;
//# sourceMappingURL=index.d.ts.map