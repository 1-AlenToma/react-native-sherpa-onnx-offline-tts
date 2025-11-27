type MGS = "PlaybackFinished" | "overwritten" | "Stopped" | "Error";
declare let onDone: ((msg?: MGS) => void);
declare const _default: {
    initialize: (modelId: string, debug?: boolean, threadsUsed?: number) => void;
    generateAndPlay: (item: {
        text: string;
        nextText?: string;
        sid?: number;
        speed?: number;
        onDone?: typeof onDone;
    }) => Promise<string>;
    deinitialize: () => void;
    addVolumeListener: (callback: any) => import("react-native").EmitterSubscription;
    stop: () => Promise<void>;
    onDoneSubscription: import("react-native").EmitterSubscription;
};
export default _default;
//# sourceMappingURL=index.d.ts.map