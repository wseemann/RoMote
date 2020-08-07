package wseemann.media.romote.audio;

interface IRemoteAudioInterface {
    void setDevice(String host);
    void toggleRemoteAudio();
    boolean isRemoteAudioActive();
}
