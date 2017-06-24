package com.xpreesbees.pda.Networking;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.xpreesbees.pda.R;
import com.xpreesbees.pda.utils.AppConstants;
import com.xpreesbees.pda.utils.AppUtils;
import com.xpreesbees.pda.utils.DialogHelper;
import com.xpreesbees.pda.utils.LoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by user on 18/4/16.
 */
public abstract class AbstractNetworking implements Response.ErrorListener, Response.Listener<String> {



    public static final boolean isLive = false;


    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";


    protected static final String RETURN_MSG = "ReturnMessage";
    protected static final String SUCCESS = "Successful";


    protected JSONObject params;
    protected boolean isForeground;
    protected Context context;
    protected int type;
    protected String url;
    private ProgressDialog dialog;
    protected boolean hasError;

    public AbstractNetworking(boolean isForeground, Context context, int type, String url) {
        this.isForeground = isForeground;
        this.context = context;
        this.type = type;
        this.url = url;
    }

    abstract void setParams(Object object) throws JSONException;

    public void makeRequestAndInsert(Object obj) throws JSONException {
        setParams(obj);
        Log.d(AbstractNetworking.class.getSimpleName(), "making req");
        Log.e(AbstractNetworking.class.getSimpleName() + " URL ", url);
        if (isForeground)
            dialog = ProgressDialog.show(context, context.getString(R.string.loading), context.getString(R.string.wait), true, false);
        VolleySingleton.getInstance(this.context).addToRequestQueue(buildRequest(this, type, url));

    }

    protected abstract void parseJsonAndInsert(String response) throws Exception;

    protected StringRequest buildRequest(Response.Listener<String> listner, int type, String url) {
        Log.d("params", params.toString());
        StringRequest request = new StringRequest(type, url, listner, this) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                super.getBody();
                return params.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                super.getBodyContentType();
                return CONTENT_TYPE;
            }

            /*@Override
            public RetryPolicy getRetryPolicy() {
                return new DefaultRetryPolicy(AppConstants.TIMEOUT,AppConstants.MAX_RETRIRES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            }*/
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                AppConstants.TIMEOUT,
                AppConstants.MAX_RETRIRES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        return request;
    }

    @Override
    public void onResponse(String response) {
        Log.d("response", response);
        LoggerUtils.LOG(url, params.toString(), response, context);
        AppUtils.trackEvent(context.getString(R.string.ga_event_category_api) ,
                url,
                AppUtils.getUserDetailandTime(context) +"\n params = "+params +"| \n response = " + response ,
                context);

        try {
            parseJsonAndInsert(response);
        } catch (Exception e) {
            e.printStackTrace();
            if (isForeground) {
                DialogHelper.showDialog(context, context.getString(R.string.error), e.getLocalizedMessage(), context.getString(R.string.ok), null, null,false,true);
            }
//18409 213
            hasError = true;
        }
        if (isForeground)
            dialog.dismiss();
    }

    @Override
    public void onErrorResponse(VolleyError error) {

        Log.d("error", "error = " + error);
        if (isForeground) {
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                DialogHelper.showDialog(context, context.getString(R.string.error), context.getString(R.string.no_internet_connection), context.getString(R.string.ok), null, null,false,false);
                LoggerUtils.LOG(url, params.toString(), "Network time out", context);
                AppUtils.trackEvent(context.getString(R.string.ga_event_category_service_fail),
                        "Time out",
                        url,
                        context);

            } else if (error != null) {
                DialogHelper.showDialog(context, context.getString(R.string.error), error.getLocalizedMessage(), context.getString(R.string.ok), null, null,false,false);
                LoggerUtils.LOG(url, params.toString(), error.getLocalizedMessage(), context);
                AppUtils.trackEvent(context.getString(R.string.ga_event_category_service_fail),
                        error.getLocalizedMessage(),
                        url,
                        context);

            }
        }
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

}
