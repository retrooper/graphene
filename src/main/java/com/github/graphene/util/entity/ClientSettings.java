package com.github.graphene.util.entity;

import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.player.SkinSection;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

import java.util.Set;

public class ClientSettings {

    private String locale;
    private int viewDistance;
    private Set<SkinSection> skinSections;
    private WrapperPlayClientSettings.ChatVisibility chatMode;
    private HumanoidArm mainHand;

    public ClientSettings(String locale, int viewDistance, Set<SkinSection> skinSections, WrapperPlayClientSettings.ChatVisibility chatMode, HumanoidArm mainHand) {
        this.locale = locale;
        this.viewDistance = viewDistance;
        this.skinSections = skinSections;
        this.chatMode = chatMode;
        this.mainHand = mainHand;
    }

    public ClientSettings(WrapperPlayClientSettings eventWrapper) {
        this.locale = eventWrapper.getLocale();
        this.viewDistance = eventWrapper.getViewDistance();
        this.skinSections = eventWrapper.getVisibleSkinSections();
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

    public Set<SkinSection> getVisibleSkinSections() {
        return skinSections;
    }

    public void setVisibleSkinSections(Set<SkinSection> skinSections) {
        this.skinSections = skinSections;
    }

    public WrapperPlayClientSettings.ChatVisibility getChatVisibility() {
        return chatMode;
    }

    public void setChatVisibility(WrapperPlayClientSettings.ChatVisibility chatMode) {
        this.chatMode = chatMode;
    }

    public HumanoidArm getMainHand() {
        return mainHand;
    }

    public void setMainHand(HumanoidArm mainHand) {
        this.mainHand = mainHand;
    }
}
