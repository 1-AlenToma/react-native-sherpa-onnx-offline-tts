declare const _default: {
    initialize: (modelId: string, debug?: boolean, threadsUsed?: number) => void;
    generateAndPlay: (text: any, sid: any, speed: any) => Promise<"PlaybackFinished" | "overwritten" | "Error">;
    deinitialize: () => void;
    addVolumeListener: (callback: any) => import("react-native").EmitterSubscription;
    stop: () => Promise<void>;
};
export default _default;
//# sourceMappingURL=index.d.ts.map