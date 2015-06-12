package net.wessendorf;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jboss.aerogear.simplepush.SimplePushClient;
import org.jboss.aerogear.unifiedpush.PushConfig;
import org.jboss.aerogear.unifiedpush.UnifiedPushClient;

import java.util.Date;
import java.util.logging.Logger;

public class Main {

    static final String UPS_URL = "http://192.168.178.24:8080/ag-push/rest/registry/device";
    static final String UPS_VARIANT_ID = "4ad8c6c8-ffb1-40d1-9a71-d0d3e70155fc";
    static final String UPS_VARIANT_SECRET = "859cf94c-a625-4859-a24a-e111a71be327";

    static final String SPS_URL = "";

    static final String MQTT_URL = "tcp://iot.eclipse.org:1883";



    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String... args) throws Exception {
        final UnifiedPushClient unifiedPushClient = new UnifiedPushClient(UPS_URL, UPS_VARIANT_ID, UPS_VARIANT_SECRET);
        final SimplePushClient simplePushClient = new SimplePushClient(SPS_URL);

        // 1) connect to WSS based Push Network
        simplePushClient.connect();
        // 2) register on push network (for channel)
        simplePushClient.register((channelId, simplePushEndPoint) -> {
            LOGGER.info("URI for PUT updates: " + simplePushEndPoint);

            // 3) returned payload from Push Network is stored on PUS
            final PushConfig config = new PushConfig();
            config.setDeviceToken(simplePushEndPoint);
            unifiedPushClient.register(config);

        });

        simplePushClient.addMessageListener(ack -> {
            LOGGER.info("got message " + ack);

            try {
                final MqttClient client = new MqttClient(MQTT_URL, "publisher", new MemoryPersistence());

                Date datum = new Date();
                client.connect();
                client.publish("", (datum + " Raspberry Pi got message from SimplePush!").getBytes(), 1, false);

                LOGGER.info("Delivered message to MQTT broker");


                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });

        try {
            Thread.currentThread().join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



}
