package com.github.graphene.util.entity;

import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

public class ClientSettings {

    private String locale;
    private int viewDistance;
    private byte displayedSkinParts;
    private WrapperPlayClientSettings.ChatVisibility chatMode;
    private HumanoidArm mainHand;

    public ClientSettings(String locale, int viewDistance, byte displayedSkinParts, WrapperPlayClientSettings.ChatVisibility chatMode, HumanoidArm mainHand) {
        this.locale = locale;
        this.viewDistance = viewDistance;
        this.displayedSkinParts = displayedSkinParts;
        this.chatMode = chatMode;
        this.mainHand = mainHand;
    }

    public ClientSettings(WrapperPlayClientSettings eventWrapper) {
        this.locale = eventWrapper.getLocale();
        this.viewDistance = eventWrapper.getViewDistance();
        this.displayedSkinParts = eventWrapper.getVisibleSkinSectionMask();
        this.chatMode = eventWrapper.getVisibility();
        this.mainHand = eventWrapper.getMainHand();
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    public byte getDisplayedSkinParts() {
        return displayedSkinParts;
    }

    public void setDisplayedSkinParts(byte displayedSkinParts) {
        this.displayedSkinParts = displayedSkinParts;
    }

    public WrapperPlayClientSettings.ChatVisibility getChatMode() {
        return chatMode;
    }

    public void setChatMode(WrapperPlayClientSettings.ChatVisibility chatMode) {
        this.chatMode = chatMode;
    }

    public HumanoidArm getMainHand() {
        return mainHand;
    }

    public void setMainHand(HumanoidArm mainHand) {
        this.mainHand = mainHand;
    }
}
