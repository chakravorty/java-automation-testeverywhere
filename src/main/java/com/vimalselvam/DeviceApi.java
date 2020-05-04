package com.vimalselvam;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * This class provides the capability to connect or disconnect device.
 */
public class DeviceApi {
    private OkHttpClient client;
    private JsonParser jsonParser;
    private STFService stfService;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Logger LOGGER = Logger.getLogger(Class.class.getName());

    public DeviceApi(STFService stfService) {
        this.client = new OkHttpClient();
        this.jsonParser = new JsonParser();
        this.stfService = stfService;
    }

    public String connectDevice(String deviceSerial) {
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + stfService.getAuthToken())
                .url(stfService.getStfUrl() + "devices/" + deviceSerial)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
            JsonObject jsonObject = jsonParser.parse(response.body().string()).getAsJsonObject();

            if (!isDeviceFound(jsonObject)) {
                throw new RuntimeException("Device not found");
            }

            JsonObject deviceObject = jsonObject.getAsJsonObject("device");
            boolean present = deviceObject.get("present").getAsBoolean();
            boolean ready = deviceObject.get("ready").getAsBoolean();
            boolean using = deviceObject.get("using").getAsBoolean();
            JsonElement ownerElement = deviceObject.get("owner");
            boolean owner = !(ownerElement instanceof JsonNull);
            String url = deviceObject.getAsJsonObject("display").get("url").getAsString();

            if (!present || !ready || using || owner) {
                LOGGER.severe("Device is in use");
                throw new RuntimeException("Device is in use");
            }

            if (addDeviceToUser(deviceSerial)) {
                return addRemoteConntectionForUser(deviceSerial);
            } else {
                throw new RuntimeException("Error in adding device to USER");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("STF service is unreachable", e);
        }
    }

    private boolean isDeviceFound(JsonObject jsonObject) {
        if (!jsonObject.get("success").getAsBoolean()) {
            LOGGER.severe("Device not found");
            return false;
        }
        return true;
    }

    private String addRemoteConntectionForUser(String deviceSerial) {
        RequestBody requestBody = RequestBody.create(JSON, "{}");
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + stfService.getAuthToken())
                .url(stfService.getStfUrl() + "/user/devices/"+ deviceSerial +"/remoteConnect")
                .post(requestBody)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
            JsonObject jsonObject = jsonParser.parse(response.body().string()).getAsJsonObject();
            System.out.println("=====================setting remote connection=====================");
            System.out.println(jsonObject.toString());
            System.out.println("=====================setting remote connection=====================");
            if (!isDeviceFound(jsonObject)) {
                return "";
            }

            return jsonObject.get("remoteConnectUrl").getAsString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Not able to setup remote connection", e);
        }
    }

    private boolean addDeviceToUser(String deviceSerial) {
        RequestBody requestBody = RequestBody.create(JSON, "{\"serial\": \"" + deviceSerial + "\"}");
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + stfService.getAuthToken())
                .url(stfService.getStfUrl() + "user/devices")
                .post(requestBody)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
            JsonObject jsonObject = jsonParser.parse(response.body().string()).getAsJsonObject();
            System.out.println("=====================addDeviceToUser=====================");
            System.out.println(jsonObject.toString());
            System.out.println("=====================addDeviceToUser=====================");

            if (!isDeviceFound(jsonObject)) {
                return false;
            }

            LOGGER.info("The device <" + deviceSerial + "> is locked successfully");
            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException("STF service is unreachable", e);
        }
    }

    public boolean releaseDevice(String deviceSerial) {
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + stfService.getAuthToken())
                .url(stfService.getStfUrl() + "user/devices/" + deviceSerial)
                .delete()
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
            JsonObject jsonObject = jsonParser.parse(response.body().string()).getAsJsonObject();

            if (!isDeviceFound(jsonObject)) {
                return false;
            }

            LOGGER.info("The device <" + deviceSerial + "> is released successfully");
            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException("STF service is unreachable", e);
        }
    }
}
