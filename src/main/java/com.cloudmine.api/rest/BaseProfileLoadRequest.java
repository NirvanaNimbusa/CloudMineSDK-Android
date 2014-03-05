package com.cloudmine.api.rest;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.cloudmine.api.CMSessionToken;
import com.cloudmine.api.rest.options.CMServerFunction;
import com.cloudmine.api.rest.response.CMObjectResponse;
import me.cloudmine.annotations.Expand;
import me.cloudmine.annotations.Optional;

/**
 * A Request for loading a logged in user's profile
 * <br>
 * Copyright CloudMine LLC. All rights reserved<br>
 * See LICENSE file included with SDK for details.
 */
public class BaseProfileLoadRequest extends CloudMineRequest<CMObjectResponse> {
    public static final int REQUEST_TYPE = 404;

    /**
     * Create a new BaseProfileLoadRequest
     * @param sessionToken a valid session token for the user whose profile is to be loaded
     * @param serverFunction
     * @param successListener
     * @param errorListener
     */
    @Expand
    public BaseProfileLoadRequest(CMSessionToken sessionToken, @Optional CMServerFunction serverFunction, Response.Listener<CMObjectResponse> successListener, @Optional Response.ErrorListener errorListener) {
        super(Method.GET, "/account/mine", null, sessionToken, serverFunction, successListener,  errorListener);
    }

    @Override
    protected Response<CMObjectResponse> parseNetworkResponse(NetworkResponse networkResponse) {
        return Response.success(new CMObjectResponse(new String(networkResponse.data), networkResponse.statusCode), getCacheEntry());
    }

    @Override
    public int getRequestType() {
        return REQUEST_TYPE;
    }
}
