package com.github.graphene.wrapper.play.server;

import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.gson.JsonObject;

public class WrapperStatusServerResponse extends PacketWrapper<WrapperStatusServerResponse> {
    public static boolean HANDLE_JSON = true;
    private String componentJson;
    private JsonObject component;

    public WrapperStatusServerResponse(PacketSendEvent event) {
        super(event);
    }

    public WrapperStatusServerResponse(JsonObject component) {
        super(PacketType.Status.Server.RESPONSE);
        this.component = component;
    }

    public WrapperStatusServerResponse(String componentJson) {
        super(PacketType.Status.Server.RESPONSE);
        this.componentJson = componentJson;
    }

    @Override
    public void readData() {
        componentJson = readString();
        if (HANDLE_JSON) {
            component = AdventureSerializer.GSON.serializer().fromJson(componentJson, JsonObject.class);
        }
    }

    @Override
    public void readData(WrapperStatusServerResponse wrapper) {
        componentJson = wrapper.componentJson;
        component = wrapper.component;
    }

    @Override
    public void writeData() {
        if (HANDLE_JSON) {
            componentJson = component.toString();
        }
        writeString(componentJson);
    }

    public JsonObject getComponent() {
        return component;
    }

    public void setComponent(JsonObject component) {
        this.component = component;
    }

    public String getComponentJson() {
        return componentJson;
    }

    public void setComponentJson(String componentJson) {
        this.componentJson = componentJson;
    }
}
