package com.cloudmine.api;

import android.content.Context;
import android.os.Handler;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.cloudmine.api.rest.BaseAddPaymentMethodRequest;
import com.cloudmine.api.rest.BaseChangeUserIdentifierRequest;
import com.cloudmine.api.rest.BaseChangeUserPasswordRequest;
import com.cloudmine.api.rest.BaseLoadPaymentMethodsRequest;
import com.cloudmine.api.rest.BaseLoadUserProfilesRequest;
import com.cloudmine.api.rest.BaseProfileLoadRequest;
import com.cloudmine.api.rest.BaseProfileUpdateRequest;
import com.cloudmine.api.rest.BaseRemovePaymentMethodRequest;
import com.cloudmine.api.rest.BaseUserCreationRequest;
import com.cloudmine.api.rest.BaseUserLoginRequest;
import com.cloudmine.api.rest.BaseUserLogoutRequest;
import com.cloudmine.api.rest.CloudMineRequest;
import com.cloudmine.api.rest.SharedRequestQueueHolders;
import com.cloudmine.api.rest.response.CMObjectResponse;
import com.cloudmine.api.rest.response.CMResponse;
import com.cloudmine.api.rest.response.CreationResponse;
import com.cloudmine.api.rest.response.LoginResponse;
import com.cloudmine.api.rest.response.PaymentResponse;

import java.util.Collection;
import java.util.Collections;

import static com.cloudmine.api.rest.SharedRequestQueueHolders.getRequestQueue;

/**
 * Android specific CMUser. Should be used over JavaCMUser unless running server side code
 * <br>
 * Copyright CloudMine LLC. All rights reserved<br>
 * See LICENSE file included with SDK for details.
 */
public class CMUser extends JavaCMUser {

    /**
     * Load all of the user profiles. Requires the use of your master key
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public static CloudMineRequest loadAllUserProfiles(Context context, Response.Listener<CMObjectResponse> successListener, Response.ErrorListener errorListener) {
        BaseLoadUserProfilesRequest loadUserProfilesRequest = new BaseLoadUserProfilesRequest(null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(loadUserProfilesRequest);
        return loadUserProfilesRequest;
    }

    /**
     * Search for user profiles matching a given searchString
     * @param context
     * @param searchString Use {@link com.cloudmine.api.SearchQuery} to construct this search string
     * @param successListener
     * @param errorListener
     * @return
     */
    public static CloudMineRequest searchUserProfiles(Context context, String searchString, Response.Listener<CMObjectResponse> successListener, Response.ErrorListener errorListener) {
        BaseLoadUserProfilesRequest loadUserProfilesRequest = new BaseLoadUserProfilesRequest(searchString, null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(loadUserProfilesRequest);
        return loadUserProfilesRequest;
    }

    /**
     * Create a new CMUser with the given username and password
     * @param userName
     * @param password
     * @return
     */
    public static CMUser CMUserWithUserName(String userName, String password) {
        return new CMUser(null, userName, password);
    }

    /**
     * Create a new CMUser with the given username and blank password
     * @param userName
     * @return
     */
    public static CMUser CMUserWithUserName(String userName) {
        return new CMUser(null, userName, "");
    }

    /**
     * Create a new CMUser with the given email and password
     * @param email
     * @param password
     * @return
     */
    public static CMUser CMUserWithEmail(String email, String password) {
        return new CMUser(email, password);
    }

    /**
     * Create a new CMUser with the given email and blank password
     * @param email
     * @return
     */
    public static CMUser CMUserWithEmail(String email) {
        return new CMUser(email, "");
    }

    protected CMUser() {}

    public CMUser(String email, String userName, String password) {
        super(email, userName, password);
    }

    public CMUser(String email, String password) {
        super(email, password);
    }

    /**
     * Create a user asynchronously
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest create(Context context, Response.Listener<CreationResponse> successListener, Response.ErrorListener errorListener) {
        RequestQueue queue = getRequestQueue(context);
        CloudMineRequest request = new BaseUserCreationRequest(this, null, successListener,  errorListener);
        queue.add(request);
        return request;
    }

    public CloudMineRequest create(Context context, Handler handler) {
        CloudMineRequest request = new BaseUserCreationRequest(this, null, null, null);
        request.setHandler(handler);
        getRequestQueue(context).add(request);
        return request;
    }

    /**
     * Login a user asynchronously, set their session token and profile information on this object, and clears their password
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest login(Context context, Response.Listener<LoginResponse> successListener, Response.ErrorListener errorListener) {
        return login(context, getPassword(), successListener, errorListener);
    }

    /**
     * Login a user asynchronously, set their session token and profile information on this object, and clears their password
     * @param context
     * @param password the password to use to login
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest login(Context context, String password, final Response.Listener<LoginResponse> successListener, Response.ErrorListener errorListener) {
        RequestQueue queue = getRequestQueue(context);

        CloudMineRequest request = new BaseUserLoginRequest(getUserIdentifier(), password, null, new Response.Listener<LoginResponse>() {
            @Override
            public void onResponse(LoginResponse response) {
                try {
                    setLoggedInUser(response);
                } finally {
                    successListener.onResponse(response);
                }
            }
        }, errorListener);
        queue.add(request);
        return request;
    }

    /**
     * Logs the user out, invalidating their session token. If successful, clears the session token on this object
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest logout(Context context, final Response.Listener<CMResponse> successListener, Response.ErrorListener errorListener) {
        BaseUserLogoutRequest request = new BaseUserLogoutRequest(getSessionToken(), null, new Response.Listener<CMResponse>() {
            @Override
            public void onResponse(CMResponse response) {
                try {
                    if(response.wasSuccess()) {
                        setSessionToken(null);
                    }
                }finally {
                    successListener.onResponse(response);
                }
            }
        }, errorListener);
        getRequestQueue(context).add(request);
        return request;
    }

    /**
     * Save this user's profile. A user must be logged in to save their profile.
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest saveProfile(Context context, Response.Listener<CreationResponse> successListener, Response.ErrorListener errorListener) {
        RequestQueue queue = getRequestQueue(context);
        CloudMineRequest request = new BaseProfileUpdateRequest(profileTransportRepresentation(), getSessionToken(), null, successListener, errorListener);
        queue.add(request);
        return request;
    }

    /**
     * Load this user's profile. A user must be logged in to load their profile
     * @param context
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest loadProfile(Context context, Response.Listener<CMObjectResponse> successListener, Response.ErrorListener errorListener) {
        RequestQueue queue = getRequestQueue(context);
        CloudMineRequest request = new BaseProfileLoadRequest(getSessionToken(), null, successListener, errorListener);
        queue.add(request);
        return request;
    }

    /**
     * Change this user's password
     * @param context
     * @param currentPassword
     * @param newPassword
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest changePassword(Context context, String currentPassword, String newPassword, Response.Listener<CMResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseChangeUserPasswordRequest(getUserIdentifier(), currentPassword, newPassword, null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        setPassword(newPassword);
        return request;
    }

    /**
     * Change this user's userName
     * @param context
     * @param newUserName
     * @param currentPassword
     * @param successListener
     * @param errorListener
     * @return
     */
    public CloudMineRequest changeUserName(Context context, String newUserName, String currentPassword, Response.Listener<CMResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseChangeUserIdentifierRequest(getUserIdentifier(), currentPassword, null, newUserName, null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        setUserName(newUserName);
        return request;
    }


    public CloudMineRequest changeEmail(Context context, String newEmail, String currentPassword, Response.Listener<CMResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseChangeUserIdentifierRequest(getUserIdentifier(), currentPassword, newEmail, null, null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        setEmail(newEmail);
        return request;
    }

    /**
     * See {@link com.cloudmine.api.rest.BaseAddPaymentMethodRequest}. User must be logged in
     * @param context
     * @param creditCard
     * @param successListener
     * @param errorListener
     * @return The request that was added to the queue
     */
    public CloudMineRequest addPaymentMethod(Context context, CMCreditCard creditCard, Response.Listener<PaymentResponse> successListener, Response.ErrorListener errorListener) {
        return addPaymentMethod(context, Collections.singleton(creditCard), successListener, errorListener);
    }

    /**
     * See {@link com.cloudmine.api.rest.BaseRemovePaymentMethodRequest}. User must be logged in
     * @param context
     * @param index
     * @param successListener
     * @param errorListener
     * @return The request that was added to the queue
     */
    public CloudMineRequest removePaymentMethodAtIndex(Context context, int index, Response.Listener<PaymentResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseRemovePaymentMethodRequest(index, getSessionToken(), null,successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        return request;
    }

    /**
     * See {@link com.cloudmine.api.rest.BaseAddPaymentMethodRequest}. User must be logged in
     * @param context
     * @param creditCards
     * @param successListener
     * @param errorListener
     * @return The request that was added to the queue
     */
    public CloudMineRequest addPaymentMethod(Context context, Collection<CMCreditCard> creditCards, Response.Listener<PaymentResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseAddPaymentMethodRequest(creditCards, getSessionToken(), null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        return request;
    }

    /**
     * See {@link com.cloudmine.api.rest.BaseLoadPaymentMethodsRequest}. User must be logged in
     * @param context
     * @param successListener
     * @param errorListener
     * @return The request that was added to the queue
     */
    public CloudMineRequest loadPaymentMethods(Context context, Response.Listener<PaymentResponse> successListener, Response.ErrorListener errorListener) {
        CloudMineRequest request = new BaseLoadPaymentMethodsRequest(getSessionToken(), null, successListener, errorListener);
        SharedRequestQueueHolders.getRequestQueue(context).add(request);
        return request;
    }
}