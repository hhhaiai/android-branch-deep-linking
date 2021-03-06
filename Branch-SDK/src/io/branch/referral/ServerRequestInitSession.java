package io.branch.referral;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Abstract for Session init request. All request which do initilaise session should extend from this.
 * </p>
 */
abstract class ServerRequestInitSession extends ServerRequest {
    protected static final String ACTION_OPEN = "open";
    protected static final String ACTION_INSTALL = "install";

    public ServerRequestInitSession(Context context, String requestPath) {
        super(context, requestPath);
    }

    protected ServerRequestInitSession(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    /**
     * Check if there is a valid callback to return init session result
     *
     * @return True if a valid call back is present.
     */
    public abstract boolean hasCallBack();

    @Override
    public boolean isGAdsParamsRequired() {
        return true; //Session start requests need GAds params
    }

    public abstract String getRequestActionName();

    public static boolean isInitSessionAction(String actionName) {
        boolean isInitSessionAction = false;
        if (actionName != null) {
            isInitSessionAction = (actionName.equalsIgnoreCase(ACTION_OPEN) || actionName.equalsIgnoreCase(ACTION_INSTALL));
        }
        return isInitSessionAction;
    }

    public boolean handleBranchViewIfAvailable(ServerResponse resp) {
        boolean isBranchViewShowing = false;
        if (resp != null && resp.getObject() != null && resp.getObject().has(Defines.Jsonkey.BranchViewData.getKey())) {
            try {
                JSONObject branchViewJsonObj = resp.getObject().getJSONObject(Defines.Jsonkey.BranchViewData.getKey());
                String actionName = getRequestActionName();
                if ((Branch.getInstance().currentActivityReference_ != null && Branch.getInstance().currentActivityReference_.get() != null)) {
                    Activity currentActivity = Branch.getInstance().currentActivityReference_.get();
                    boolean isActivityEnabledForBranchView = true;
                    if (currentActivity instanceof Branch.IBranchViewControl) {
                        isActivityEnabledForBranchView = !((Branch.IBranchViewControl) currentActivity).skipBranchViewsOnThisActivity();
                    }
                    if (isActivityEnabledForBranchView) {
                        isBranchViewShowing = BranchViewHandler.getInstance().showBranchView(branchViewJsonObj, actionName, currentActivity, Branch.getInstance());
                    } else {
                        isBranchViewShowing = BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                    }
                } else {
                    isBranchViewShowing = BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                }
            } catch (JSONException ignore) {
            }
        }
        return isBranchViewShowing;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        // Check for any Third party SDK for data handling
        try {
            // Provide data to Fabric answers
            if (response.getObject() != null && response.getObject().has(Defines.Jsonkey.Data.getKey())) {
                String eventName = (this instanceof ServerRequestRegisterInstall) ? ExtendedAnswerProvider.KIT_EVENT_INSTALL : ExtendedAnswerProvider.KIT_EVENT_OPEN;
                JSONObject linkDataJsonObj = new JSONObject(response.getObject().getString(Defines.Jsonkey.Data.getKey()));
                new ExtendedAnswerProvider().provideData(eventName, linkDataJsonObj, prefHelper_.getIdentityID());
            }
        } catch (JSONException ignore) {
        }
    }

    public void updateLinkClickIdentifier() {
        if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            } catch (JSONException ignore) {
            }
        }
    }

    @Override
    public void onPreExecute() {
        JSONObject post = getPost();
        try {
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            }
            if (!prefHelper_.getAppLink().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.AndroidAppLinkURL.getKey(), prefHelper_.getAppLink());
            }
            if (!prefHelper_.getPushIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.AndroidPushIdentifier.getKey(), prefHelper_.getPushIdentifier());
            }
            // External URI or Extras if exist
            if (!prefHelper_.getExternalIntentUri().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.External_Intent_URI.getKey(), prefHelper_.getExternalIntentUri());
            }
            if (!prefHelper_.getExternalIntentExtra().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.External_Intent_Extra.getKey(), prefHelper_.getExternalIntentExtra());
            }
        } catch (JSONException ignore) {

        }

    }
}
