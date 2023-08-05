//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.4
//
package com.dancingqueen.walladog.aws;

import com.amazonaws.regions.Regions;

/**
 * This class defines constants for the developer's resource
 * identifiers and API keys. It should be kept private.
 */
public class AWSConfiguration {

    // AWS MobileHub user agent string
    public static final String AWS_MOBILEHUB_USER_AGENT =
        "com.dancingqueen.walladog.android";
    // AMAZON COGNITO
    public static final Regions AMAZON_COGNITO_REGION =
        Regions.US_EAST_1;
    public static final String  AMAZON_COGNITO_IDENTITY_POOL_ID =
        "OGNITO_IDENTITY_POOL_ID";
    // Custom Developer Provided Authentication ID
    public static final String DEVELOPER_AUTHENTICATION_PROVIDER_ID =
        "mobilelink.fernandolunamolina.com";
    // Developer Authentication - URL for Create New Account
    public static final String DEVELOPER_AUTHENTICATION_CREATE_ACCOUNT_URL =
        "aws.amazon.com";
    // Developer Authentication - URL for Forgot Password
    public static final String DEVELOPER_AUTHENTICATION_FORGOT_PASSWORD_URL =
        "aws.amazon.com";
    // AMAZON MOBILE ANALYTICS
    public static final String  AMAZON_MOBILE_ANALYTICS_APP_ID =
        "ANALYTICS_APP_ID";
    // Amazon Mobile Analytics region
    public static final Regions AMAZON_MOBILE_ANALYTICS_REGION = Regions.US_EAST_1;
    // GOOGLE CLOUD MESSAGING API KEY
    public static final String GOOGLE_CLOUD_MESSAGING_API_KEY =
        "CLOUD_MESSAGING_API_KEY";
    // GOOGLE CLOUD MESSAGING SENDER ID
    public static final String GOOGLE_CLOUD_MESSAGING_SENDER_ID =
        "CLOUD_MESSAGING_SENDER_ID";
    // SNS PLATFORM APPLICATION ARN
    public static final String AMAZON_SNS_PLATFORM_APPLICATION_ARN =
        "SNS_PLATFORM_APPLICATION_ARN";
    // SNS DEFAULT TOPIC ARN
    public static final String AMAZON_SNS_DEFAULT_TOPIC_ARN =
        "SNS_DEFAULT_TOPIC_ARN";
    // SNS PLATFORM TOPIC ARNS
    public static final String[] AMAZON_SNS_TOPIC_ARNS =
        {};
    public static final String AMAZON_CONTENT_DELIVERY_S3_BUCKET =
        "CONTENT_DELIVERY_S3_BUCKET";
    public static final String AMAZON_CLOUD_FRONT_DISTRIBUTION_DOMAIN =
        "FRONT_DISTRIBUTION_DOMAIN";
    // S3 BUCKET
    public static final String AMAZON_S3_USER_FILES_BUCKET =
        "AMAZON_S3_USER_FILES_BUCKET";
}
