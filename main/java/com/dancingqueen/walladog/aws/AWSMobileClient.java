package com.dancingqueen.walladog.aws;

import android.content.Context;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsConfig;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.EventClient;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.SessionClient;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.regions.Regions;
import com.dancingqueen.walladog.aws.content.ContentManager;
import com.dancingqueen.walladog.aws.content.UserFileManager;
import com.dancingqueen.walladog.aws.push.PushManager;
import com.dancingqueen.walladog.aws.user.IdentityManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by f3rn4nd0 on 11/15/15.
 */
public class AWSMobileClient {

    private final static String LOG_TAG = AWSMobileClient.class.getSimpleName();

    private static AWSMobileClient instance;

    private final Context context;

    private ClientConfiguration clientConfiguration;
    private IdentityManager identityManager;
    private PushManager pushManager;
    private MobileAnalyticsManager mobileAnalyticsManager;
    private CognitoSyncManager syncManager;

    /**
     * Build class used to create the AWS mobile client.
     */
    public static class Builder {

        private Context applicationContext;
        private String  cognitoIdentityPoolID;
        private Regions cognitoRegion;
        private String  mobileAnalyticsAppID;
        private ClientConfiguration clientConfiguration;
        private IdentityManager identityManager;

        /**
         * Constructor.
         * @param context
         */
        public Builder(final Context context) {
            this.applicationContext = context.getApplicationContext();
        };

        /**
         * Provides the Amazon Cognito Identity Pool ID.
         * @param cognitoIdentityPoolID identity pool ID
         * @return builder
         */
        public Builder withCognitoIdentityPoolID(final String cognitoIdentityPoolID) {
            this.cognitoIdentityPoolID = cognitoIdentityPoolID;
            return this;
        };

        /**
         * Provides the Amazon Cognito service region.
         * @param cognitoRegion service region
         * @return builder
         */
        public Builder withCognitoRegion(final Regions cognitoRegion) {
            this.cognitoRegion = cognitoRegion;
            return this;
        }

        /**
         * Provides the Amazon Mobile Analytics App ID.
         * @param mobileAnalyticsAppID application ID
         * @return builder
         */
        public Builder withMobileAnalyticsAppID(final String mobileAnalyticsAppID) {
            this.mobileAnalyticsAppID = mobileAnalyticsAppID;
            return this;
        };

        /**
         * Provides the identity manager.
         * @param identityManager identity manager
         * @return builder
         */
        public Builder withIdentityManager(final IdentityManager identityManager) {
            this.identityManager = identityManager;
            return this;
        }

        /**
         * Provides the client configuration
         * @param clientConfiguration client configuration
         * @return builder
         */
        public Builder withClientConfiguration(final ClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        /**
         * Creates the AWS mobile client instance and initializes it.
         * @return AWS mobile client
         */
        public AWSMobileClient build() {
            return
                    new AWSMobileClient(applicationContext,
                            cognitoIdentityPoolID,
                            cognitoRegion,
                            mobileAnalyticsAppID,
                            identityManager,
                            clientConfiguration);
        }
    }

    private AWSMobileClient(final Context context,
                            final String  cognitoIdentityPoolID,
                            final Regions cognitoRegion,
                            final String mobileAnalyticsAppID,
                            final IdentityManager identityManager,
                            final ClientConfiguration clientConfiguration) {

        this.context = context;
        this.identityManager = identityManager;
        this.clientConfiguration = clientConfiguration;

        try {
            this.mobileAnalyticsManager =
                    MobileAnalyticsManager.
                            getOrCreateInstance(context,
                                    AWSConfiguration.AMAZON_MOBILE_ANALYTICS_APP_ID,
                                    AWSConfiguration.AMAZON_MOBILE_ANALYTICS_REGION,
                                    identityManager.getCredentialsProvider(),
                                    new AnalyticsConfig(clientConfiguration));
        }
        catch (final InitializationException ie) {
            Log.e(LOG_TAG, "Unable to initalize Amazon Mobile Analytics. " + ie.getMessage(), ie);
        }

        this.pushManager =
                new PushManager(context,
                        identityManager.getCredentialsProvider(),
                        AWSConfiguration.GOOGLE_CLOUD_MESSAGING_SENDER_ID,
                        AWSConfiguration.AMAZON_SNS_PLATFORM_APPLICATION_ARN,
                        clientConfiguration);

        final List<String> topics = new ArrayList<String>();
        topics.add(AWSConfiguration.AMAZON_SNS_DEFAULT_TOPIC_ARN);
        topics.addAll(Arrays.asList(AWSConfiguration.AMAZON_SNS_TOPIC_ARNS));

        final String[] topicsArray = topics.toArray(new String[topics.size()]);
        pushManager.setTopics(topicsArray);

        this.syncManager = new CognitoSyncManager(context, AWSConfiguration.AMAZON_COGNITO_REGION,
                identityManager.getCredentialsProvider(), clientConfiguration);
    }

    /**
     * Sets the singleton instance of the AWS mobile client.
     * @param client client instance
     */
    public static void setDefaultMobileClient(AWSMobileClient client) {
        instance = client;
    }

    /**
     * Gets the default singleton instance of the AWS mobile client.
     * @return client
     */
    public static AWSMobileClient defaultMobileClient() {
        return instance;
    }

    /**
     * Gets the identity manager.
     * @return identity manager
     */
    public IdentityManager getIdentityManager() {
        return this.identityManager;
    }

    /**
     * Gets the push notifications manager.
     * @return push manager
     */
    public PushManager getPushManager() {
        return this.pushManager;
    }

    /**
     * Gets the Amazon Cognito Sync Manager, which is responsible for saving and
     * loading user profile data, such as game state or user settings.
     * @return sync manager
     */
    public CognitoSyncManager getSyncManager() {
        return syncManager;
    }

    /**
     * Creates and initialize the default AWSMobileClient if it doesn't already
     * exist using configuration constants from {@link AWSConfiguration}.
     *
     * @param context an application context.
     */
    public static void initializeMobileClientIfNecessary(final Context context) {
        if (AWSMobileClient.defaultMobileClient() == null) {
            Log.d(LOG_TAG, "Initializing AWS Mobile Client...");
            final ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setUserAgent(AWSConfiguration.AWS_MOBILEHUB_USER_AGENT);
            final IdentityManager identityManager = new IdentityManager(context, clientConfiguration);
            final AWSMobileClient awsClient =
                    new AWSMobileClient.Builder(context)
                            .withCognitoRegion(AWSConfiguration.AMAZON_COGNITO_REGION)
                            .withCognitoIdentityPoolID(AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID)
                            .withMobileAnalyticsAppID(AWSConfiguration.AMAZON_MOBILE_ANALYTICS_APP_ID)
                            .withIdentityManager(identityManager)
                            .withClientConfiguration(clientConfiguration)
                            .build();

            AWSMobileClient.setDefaultMobileClient(awsClient);
        }
        Log.d(LOG_TAG, "AWS Mobile Client is OK");
    }
    /**
     * Gets the Amazon Mobile Analytics Manager, which allows you to submit
     * custom and monetization events to the Amazon Mobile Analytics system. It
     * also handles recording user session data events.
     * @return mobile analytics manager
     */
    public MobileAnalyticsManager getMobileAnalyticsManager() {
        return this.mobileAnalyticsManager;
    }

    /**
     * This method should be invoked when each activity is paused. It is used
     * to assist in tracking user session data in Amazon Mobile Analytics system.
     */
    public void handleOnPause() {

        SessionClient sessionClient = null;
        EventClient eventClient = null;

        try {
            if (mobileAnalyticsManager != null &&
                    (sessionClient = mobileAnalyticsManager.getSessionClient()) != null &&
                    (eventClient = mobileAnalyticsManager.getEventClient()) != null) {
                sessionClient.pauseSession();
                eventClient.submitEvents();
            }
        }
        catch (final Exception e) {
            Log.w(LOG_TAG, "Unable to report analytics. " + e.getMessage(), e);
        }
    }

    /**
     * This method should be called whenever any activity resumes. It assists in
     * tracking user session data in the Amazon Mobile Analytics system.
     */
    public void handleOnResume() {
        SessionClient sessionClient = null;

        try {
            if (mobileAnalyticsManager != null &&
                    (sessionClient = mobileAnalyticsManager.getSessionClient()) != null) {
                sessionClient.resumeSession();
            }
        }
        catch (final Exception e) {
            Log.w(LOG_TAG, "Unable to resume analytics. " + e.getMessage(), e);
        }
    }

    /**
     * Creates a User File Manager instance, which facilitates file transfers
     * between the device and the specified Amazon S3 (Simple Storage Service) bucket.
     *
     * @param s3Bucket Amazon S3 bucket
     * @param s3FolderPrefix Folder pre-fix for files affected by this user file
     *                       manager instance
     * @param resultHandler handles the resulting UserFileManager instance
     */
    public void createUserFileManager(final String s3Bucket,
                                      final String s3FolderPrefix,
                                      final UserFileManager.BuilderResultHandler resultHandler) {

        new UserFileManager.Builder().withContext(context)
                .withIdentityManager(getIdentityManager())
                .withS3Bucket(s3Bucket)
                .withS3ObjectDirPrefix(s3FolderPrefix)
                .withLocalBasePath(context.getFilesDir().getAbsolutePath())
                .withClientConfiguration(clientConfiguration)
                .build(resultHandler);
    }

    /**
     * Creates the default Content Manager, which allows files to be downloaded from
     * the Amazon S3 (Simple Storage Service) bucket associated with the App Content
     * Delivery feature (optionally through Amazon CloudFront if Multi-Region CDN option
     * was selected).
     * @param resultHandler handles the resulting ContentManager instance
     */
    public void createDefaultContentManager(final ContentManager.BuilderResultHandler resultHandler) {
        new ContentManager.Builder()
                .withContext(context)
                .withIdentityManager(identityManager)
                .withS3Bucket(AWSConfiguration.AMAZON_CONTENT_DELIVERY_S3_BUCKET)
                .withLocalBasePath(context.getFilesDir().getAbsolutePath())
                .withCloudFrontDomainName(AWSConfiguration.AMAZON_CLOUD_FRONT_DISTRIBUTION_DOMAIN)
                .withClientConfiguration(clientConfiguration)
                .build(resultHandler);
    }
}
