package com.tjeannin.apprate;

import com.facebook.UiLifecycleHelper;

/**
 * Must be implemented by activity which launcher AppRater in order to post a recommendation on Facebook.
 */
public interface IHasUiHelper {

    public UiLifecycleHelper getUiHelper();
}
