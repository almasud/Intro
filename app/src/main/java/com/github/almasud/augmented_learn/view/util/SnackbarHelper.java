package com.github.almasud.augmented_learn.view.util;

import android.app.Activity;

import com.github.almasud.augmented_learn.R;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
public final class SnackbarHelper {
  private static final SnackbarHelper THE_INSTANCE = new SnackbarHelper();
  private Snackbar messageSnackbar;

  private enum DismissBehavior {
    HIDE,
    SHOW,
    FINISH
  };

  public static SnackbarHelper getInstance() {
    return THE_INSTANCE;
  }

  public boolean isShowing() {
    return messageSnackbar != null;
  }

  /** Shows a snackbar with a given message. */
  public void showMessage(Activity activity, String message) {
    show(activity, message, DismissBehavior.HIDE);
  }

  /** Shows a snackbar with a given message, and a dismiss button. */
  public void showMessageWithDismiss(Activity activity, String message) {
    show(activity, message, DismissBehavior.SHOW);
  }

  /**
   * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
   * for notifying errors, where no further interaction with the activity is possible.
   */
  public void showError(Activity activity, String errorMessage) {
    show(activity, errorMessage, DismissBehavior.FINISH);
  }

  /**
   * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
   * call even if snackbar is not shown.
   */
  public void hide(Activity activity) {
    activity.runOnUiThread(() -> {
        if (messageSnackbar != null) {
            messageSnackbar.dismiss();
        }
        messageSnackbar = null;
    });
  }

  private void show(
          final Activity activity, final String message, final DismissBehavior dismissBehavior) {
    activity.runOnUiThread(() -> {
        messageSnackbar =
                Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_LONG);
        messageSnackbar.getView().setBackgroundColor(activity.getResources().getColor(R.color.colorPrimaryTransparent_66));
        if (dismissBehavior != DismissBehavior.HIDE) {
            messageSnackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
            messageSnackbar.setAction(
                    "Dismiss",
                    v -> messageSnackbar.dismiss());
            if (dismissBehavior == DismissBehavior.FINISH) {
                messageSnackbar.addCallback(
                        new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);
                                activity.finish();
                            }
                        });
            }
        }
        messageSnackbar.show();
    });
  }
}
