package com.jakewharton.telecine;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

import static android.view.View.ALPHA;
import static android.view.View.TRANSLATION_Y;

public final class TelecineActivity extends Activity {
  private static final int CREATE_SCREEN_CAPTURE = 4242;

  @InjectView(R.id.main) View mainView;
  @InjectView(R.id.tutorial) View tutorialView;

  private float defaultActionBarElevation;
  private int primaryColor;
  private int primaryColorDark;
  private int mainAnimationTranslationY;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.inject(this);

    defaultActionBarElevation = getActionBar().getElevation();

    TypedValue value = new TypedValue();
    Resources.Theme theme = getTheme();
    theme.resolveAttribute(android.R.attr.colorPrimary, value, true);
    primaryColor = value.data;
    theme.resolveAttribute(android.R.attr.colorPrimaryDark, value, true);
    primaryColorDark = value.data;

    mainAnimationTranslationY =
        getResources().getDimensionPixelSize(R.dimen.main_animation_translation_y);

    tutorialView.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override public void onGlobalLayout() {
            tutorialView.setTranslationY(tutorialView.getHeight());
            tutorialView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  @Override public void onBackPressed() {
    if (tutorialView.getVisibility() != View.VISIBLE) {
      super.onBackPressed();
    }
    hideTutorial();
  }

  @OnClick(R.id.view_tutorial) void showTutorial() {
    // Animate status bar color to primary.
    Animator statusBar = ObjectAnimator.ofArgb(getWindow(), "statusBarColor", primaryColor);

    // Animate tutorial up.
    tutorialView.setVisibility(View.VISIBLE);
    Animator tutorial = ObjectAnimator.ofFloat(tutorialView, TRANSLATION_Y, 0);
    tutorial.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        getActionBar().setTitle("Welcome to Telecine");
        // TODO show "Skip" action item.
      }
    });

    // Animate main down and alpha out.
    PropertyValuesHolder main1 =
        PropertyValuesHolder.ofFloat(TRANSLATION_Y, mainAnimationTranslationY);
    PropertyValuesHolder main2 = PropertyValuesHolder.ofFloat(ALPHA, 0);
    Animator main = ObjectAnimator.ofPropertyValuesHolder(mainView, main1, main2);

    // Animate elevation of action bar to 0.
    Animator actionBar = ObjectAnimator.ofFloat(getActionBar(), "elevation", 0);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(statusBar, tutorial, main, actionBar);
    set.setDuration(300);
    set.start();
  }

  private void hideTutorial() {
    // Animate status bar color to primary dark.
    Animator statusBar = ObjectAnimator.ofArgb(getWindow(), "statusBarColor", primaryColorDark);

    // Animate tutorial down.
    int tutorialHeight = tutorialView.getHeight();
    Animator tutorial = ObjectAnimator.ofFloat(tutorialView, TRANSLATION_Y, tutorialHeight);
    tutorial.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(@NonNull Animator animation) {
        tutorialView.setVisibility(View.INVISIBLE);
      }
    });

    // Animate main up and alpha in.
    PropertyValuesHolder main1 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, 0);
    PropertyValuesHolder main2 = PropertyValuesHolder.ofFloat(ALPHA, 1);
    Animator main = ObjectAnimator.ofPropertyValuesHolder(mainView, main1, main2);

    // Animate elevation of action bar back to normal.
    Animator actionBar =
        ObjectAnimator.ofFloat(getActionBar(), "elevation", defaultActionBarElevation);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(statusBar, tutorial, main, actionBar);
    set.setDuration(300);
    set.start();
  }

  @OnClick(R.id.launch) void launchScreenCapture() {
    Timber.d("Attempting to acquire permission to screen capture.");

    MediaProjectionManager manager =
        (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    Intent intent = manager.createScreenCaptureIntent();
    startActivityForResult(intent, CREATE_SCREEN_CAPTURE);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case CREATE_SCREEN_CAPTURE:
        if (resultCode == 0) {
          Timber.d("Failed to acquire permission to screen capture.");
        } else {
          Timber.d("Acquired permission to screen capture. Starting service.");
          startService(TelecineService.newIntent(this, resultCode, data));
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
