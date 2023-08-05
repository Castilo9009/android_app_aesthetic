//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.4
//
package com.dancingqueen.walladog.aws.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.dancingqueen.walladog.aws.AWSConfiguration;
import com.dancingqueen.walladog.aws.util.ThreadUtils;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Push Manager registers the app on the device with Google Cloud Messaging (GCM)
 * and registers the resulting device token in Amazon SNS. The result of this
 * registration process is an Amazon SNS Endpoint ARN, which can be used to send
 * push notifications directly to a specific device. The Push Manager also manages
 * Amazon SNS topic subscriptions, allowing the app to subscribe to Amazon SNS topics,
 * which let you target groups of devices with push notifications.
 */
public class PushManager {

    public interface PushStateListener {
        public void onPushStateChange(PushManager pushManager, boolean isEnabled);
    }

    private static final String LOG_TAG = PushManager.class.getSimpleName();

    // Name of the shared preferences
    private static final String SHARED_PREFS_FILE_NAME = PushManager.class.getName();
    // Keys in shared preferences
    private static final String SHARED_PREFS_KEY_DEVICE_TOKEN = "deviceToken";
    private static final String SHARED_PREFS_KEY_ENDPOINT_ARN = "endpointArn";
    private static final String SHARED_PREFS_PUSH_ENABLED = "pushEnabled";
    private static final String SHARED_PREFS_PREVIOUS_PLATFORM_APPLICATION = "previousPlatformApp";
    // Constants for SNS
    private static final String SNS_PROTOCOL_APPLICATION = "application";
    private static final String SNS_ENDPOINT_ATTRIBUTE_ENABLED = "Enabled";

    private static PushStateListener pushStateListener;
    private final GoogleCloudMessaging gcm;
    private final AmazonSNS sns;

    private final String gcmSenderID;
    private final String platformApplicationArn;

    private String deviceToken;
    private String endpointArn;
    private boolean pushEnabled;

    private List<SnsTopic> topics;

    private final SharedPreferences sharedPreferences;

    /**
     * Constructor.
     * @param context application context
     * @param provider credentials provider
     * @param gcmSenderID Google cloud messaging sender identifier
     * @param platformApplicationArn Amazon SNS platform application identifier
     * @param clientConfiguration client configuration for Amazon SNS client
     */
    public PushManager(final Context context,
                       final AWSCredentialsProvider provider,
                       final String gcmSenderID,
                       final String platformApplicationArn,
                       final ClientConfiguration clientConfiguration) {

        if (gcmSenderID == null || gcmSenderID.isEmpty()) {
            throw new IllegalArgumentException("Missing GCM sender ID.");
        }

        this.gcmSenderID = gcmSenderID;
        this.platformApplicationArn = platformApplicationArn;
        gcm = GoogleCloudMessaging.getInstance(context);
        sns = new AmazonSNSClient(provider, clientConfiguration);
        topics = new ArrayList<SnsTopic>();
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_FILE_NAME,
                Context.MODE_PRIVATE);

        // load previously saved device token and endpoint arn
        deviceToken = sharedPreferences.getString(SHARED_PREFS_KEY_DEVICE_TOKEN, "");
        endpointArn = sharedPreferences.getString(SHARED_PREFS_KEY_ENDPOINT_ARN, "");
        pushEnabled = sharedPreferences.getBoolean(SHARED_PREFS_PUSH_ENABLED, false);

        // Avoid the situation where a previous download/build of the sample app has
        // been run in a re-used emulator.
        String previousPlatformApp =
                sharedPreferences.getString(SHARED_PREFS_PREVIOUS_PLATFORM_APPLICATION, "");

        if (!previousPlatformApp.equals("") && !previousPlatformApp.equalsIgnoreCase(platformApplicationArn)) {
            Log.d(LOG_TAG, "App ran previously against a different SNS platform application ARN. Discarding saved settings...");

            // Discard settings because they were stored using a different
            // platform application ID.
            deviceToken = "";
            endpointArn = "";
            pushEnabled = false;
            // Trigger initial registration
            previousPlatformApp = "";
        }

        if (previousPlatformApp.equals("")) {
            // Initial state: Push settings have never been saved.

            // Register device for push and subscribe to default topic
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(LOG_TAG, "Initial App Startup - Registering for Push Notifications...");

                        registerDevice();

                        final List<SnsTopic> topics = getTopics();

                        // Make sure the default topic is intact, before we try to subscribe to it.
                        if (topics.size() > 0
                                && AWSConfiguration.AMAZON_SNS_DEFAULT_TOPIC_ARN.equalsIgnoreCase(topics.get(0).getTopicArn())) {
                            subscribeToTopic(topics.get(0));
                            Log.d(LOG_TAG, "Push Notifications - Registered with default all-devices topic.");
                        }

                        Log.d(LOG_TAG, "Push Notifications - OK");
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "Failed to complete initial Push Notication setup : " + e, e);
                    }
                }
            }).start();
        } else {
            informStateListener();
        }
    }

    /**
     * Registers this application on this device to receive push notifications
     * from Google cloud messaging and registers the resulting device token with
     * Amazon SNS. This creates an Amazon SNS platform endpoint, which can be used
     * to send push notifications directly to this device.
     */
    public void registerDevice() throws IOException {

        // GCM throws a NullPointerException in some failure cases.
        try {
            deviceToken = gcm.register(gcmSenderID);
        }
        catch (final RuntimeException re) {
            final String error = "Unable to register with GCM. " + re.getMessage();
            Log.e(LOG_TAG, error, re);
            throw new RuntimeException(error, re);
        }

        Log.d(LOG_TAG, "registrationId:" + deviceToken);

        final CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();
        request.setPlatformApplicationArn(platformApplicationArn);
        request.setToken(deviceToken);
        final CreatePlatformEndpointResult result = sns.createPlatformEndpoint(request);
        endpointArn = result.getEndpointArn();
        Log.d(LOG_TAG, "endpoint arn: " + endpointArn);

        pushEnabled = true;
        informStateListener();

        sharedPreferences.edit()
                .putString(SHARED_PREFS_KEY_DEVICE_TOKEN, deviceToken)
                .putString(SHARED_PREFS_KEY_ENDPOINT_ARN, endpointArn)
                .putBoolean(SHARED_PREFS_PUSH_ENABLED, true)
                .putString(SHARED_PREFS_PREVIOUS_PLATFORM_APPLICATION, platformApplicationArn)
                .commit();
    }

    /**
     * Associates Amazon SNS topic ARNs to this push manager.
     *
     * @param topicArns a list of topic ARNs
     */
    public void setTopics(final String[] topicArns) {
        topics.clear();
        for (String topicArn : topicArns) {
            topics.add(new SnsTopic(topicArn, sharedPreferences.getString(topicArn, "")));
        }
    }

    /**
     * Subscribes to a given Amazon SNS topic.
     *
     * @param topic topic to subscribe to
     */
    public void subscribeToTopic(final SnsTopic topic) {
        final SubscribeRequest request = new SubscribeRequest();
        request.setEndpoint(endpointArn);
        request.setTopicArn(topic.getTopicArn());
        request.setProtocol(SNS_PROTOCOL_APPLICATION);
        final SubscribeResult result = sns.subscribe(request);

        // update topic and save subscription in shared preferences
        final String subscriptionArn = result.getSubscriptionArn();
        topic.setSubscriptionArn(subscriptionArn);
        sharedPreferences.edit().putString(topic.getTopicArn(), subscriptionArn).commit();
    }

    /**
     * Unsubscribes from a given Amazon SNS topic.
     *
     * @param topic topic to unsubscribe from
     */
    public void unsubscribeFromTopic(final SnsTopic topic) {
        // Rely on the status stored locally even though it's likely that the device is
        // subscribed to a topic, but the subscription arn is lost, say due to clearing app data.
        if (!topic.isSubscribed()) {
            return;
        }

        final UnsubscribeRequest request = new UnsubscribeRequest();
        request.setSubscriptionArn(topic.getSubscriptionArn());
        sns.unsubscribe(request);

        // update topic and save subscription in shared preferences
        topic.setSubscriptionArn("");
        sharedPreferences.edit().putString(topic.getTopicArn(), "").commit();
    }

    /**
     * Gets whether the device has been registered. If not registered,
     * registerDevice() should be invoked.
     *
     * @return true if registered, false otherwise
     */
    public boolean isRegistered() {
        return endpointArn != null && !endpointArn.isEmpty();
    }

    /**
     * Changes push notification Amazon SNS endpoint status.
     *
     * @param enabled whether to enable the push notification endpoint
     */
    public void setPushEnabled(boolean enabled) {
        Map<String, String> attr = new HashMap<String, String>();
        attr.put(SNS_ENDPOINT_ATTRIBUTE_ENABLED, String.valueOf(enabled));
        SetEndpointAttributesRequest request = new SetEndpointAttributesRequest();
        request.setEndpointArn(endpointArn);
        request.setAttributes(attr);
        sns.setEndpointAttributes(request);
        Log.d(LOG_TAG, String.format("Set push %s for endpoint arn: %s",
                enabled ? "enabled" : "disabled", endpointArn));

        this.pushEnabled = enabled;
        informStateListener();

        sharedPreferences.edit()
                .putBoolean(SHARED_PREFS_PUSH_ENABLED, enabled)
                .putString(SHARED_PREFS_PREVIOUS_PLATFORM_APPLICATION, platformApplicationArn)
                .commit();
    }

    /**
     * Gets whether the device is enabled for push notification.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isPushEnabled() {
        return pushEnabled;
    }

    /**
     * Gets a list of topics that this push manager has.
     *
     * @return a list of SNS topics
     */
    public List<SnsTopic> getTopics() {
        return Collections.unmodifiableList(topics);
    }

    /**
     * Gets the device's Amazon SNS endpoint ARN. This endpoint identifier can be
     * used to send push notifications directly to this device, from the Amazon SNS
     * console, or from another mobile app, if the app has permissions in IAM to
     * publish messages to Amazon SNS.
     */
    public String getEndpointArn() {
        return endpointArn;
    }

    /**
     * Sets a listener to be informed when push notifications are enabled or disabled.
     * @param listener listener
     */
    public static void setPushStateListener(final PushStateListener listener) {
        PushManager.pushStateListener = listener;
    }

    private void informStateListener() {
        if (pushStateListener == null) {
            return;
        } else {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG, "PushStateListener: State changed to : " +
                            (pushEnabled ? "PUSH ENABLED" : "PUSH DISABLED"));

                    try {
                        pushStateListener.onPushStateChange(PushManager.this, pushEnabled);
                        Log.d(LOG_TAG, "PushStateListener:onPushStateChange ok");
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "PushStateListener:onPushStateChange Failed : " + e.getMessage(), e);
                    }
                }
            });
        }
    }
}
