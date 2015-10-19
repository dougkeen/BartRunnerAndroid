/*
 * Copyright 2012 Doug Keen
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ADAPTED FROM https://github.com/JakeWharton/SwipeToDismissNOA
 */

package com.dougkeen.bart.controls;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.dougkeen.bart.model.Constants;

/**
 * A utility class for animating the dismissal of a view (with 'onDismiss'
 * callback). Also, a {@link android.view.View.OnTouchListener} that makes any
 * {@link View} dismissable when the user swipes (drags her finger) horizontally
 * across the view.
 * <p>
 * <p>
 * <em>For {@link android.widget.ListView} list items that don't manage their own touch events
 * (i.e. you're using
 * {@link android.widget.ListView#setOnItemClickListener(android.widget.AdapterView.OnItemClickListener)}
 * or an equivalent listener on {@link android.app.ListActivity} or
 * {@link android.app.ListFragment}, use {@link SwipeDismissListViewTouchListener} instead.</em>
 * </p>
 * <p>
 * <p>
 * Example usage:
 * </p>
 * <p>
 * <pre>
 * view.setOnTouchListener(new SwipeDismisser(view, null, // Optional
 *                                                         // token/cookie
 *                                                         // object
 *         new SwipeDismisser.OnDismissCallback() {
 *             public void onDismiss(View view, Object token) {
 *                 parent.removeView(view);
 *            }
 *        }));
 * </pre>
 *
 * @see SwipeDismissListViewTouchListener
 */
public class SwipeHelper implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private View mView;
    private OnDismissCallback mCallback;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private float mDownX;
    private boolean mSwiping;
    private Object mToken;
    private VelocityTracker mVelocityTracker;
    private float mTranslationX;

    /**
     * The callback interface used by {@link SwipeHelper} to inform its client
     * about a successful dismissal of the view for which it was created.
     */
    public interface OnDismissCallback {
        /**
         * Called when the user has indicated they she would like to dismiss the
         * view.
         *
         * @param view  The originating {@link View} to be dismissed.
         * @param token The optional token passed to this object's constructor.
         */
        void onDismiss(View view, Object token);
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given view.
     *
     * @param view     The view to make dismissable.
     * @param token    An optional token/cookie object to be passed through to the
     *                 callback.
     * @param callback The callback to trigger when the user has indicated that she
     *                 would like to dismiss this view.
     */
    public SwipeHelper(View view, Object token, OnDismissCallback callback) {
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = view.getContext().getResources()
                .getInteger(android.R.integer.config_shortAnimTime);
        mView = view;
        mToken = token;
        mCallback = callback;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.v(Constants.TAG, "onTouch()");
        // offset because the view is translated during swipe
        motionEvent.offsetLocation(mTranslationX, 0);

        if (mViewWidth < 2) {
            mViewWidth = mView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // TODO: ensure this is a finger, and set a flag
                mDownX = motionEvent.getRawX();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);
                view.onTouchEvent(motionEvent);
                return false;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                float velocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= velocityX
                        && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
                    dismiss = true;
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    dismissWithAnimation(dismissRight);
                } else {
                    // cancel
                    mView.animate().translationX(0).alpha(1)
                            .setDuration(mAnimationTime).setListener(null);
                }
                mVelocityTracker = null;
                mTranslationX = 0;
                mDownX = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true;
                    mView.getParent().requestDisallowInterceptTouchEvent(true);

                    // Cancel listview's touch
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent
                            .setAction(MotionEvent.ACTION_CANCEL
                                    | (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mView.onTouchEvent(cancelEvent);
                }

                if (mSwiping) {
                    mTranslationX = deltaX;
                    mView.setTranslationX(deltaX);
                    // TODO: use an ease-out interpolator or such
                    mView.setAlpha(
                            Math.max(
                                    0f,
                                    Math.min(1f, 1f - 2f * Math.abs(deltaX)
                                            / mViewWidth)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void dismissWithAnimation(boolean dismissRight) {
        mView.animate().translationX(dismissRight ? mViewWidth : -mViewWidth)
                .alpha(0).setDuration(mAnimationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        performDismiss();
                    }
                });
    }

    private void performDismiss() {
        // Animate the dismissed view to zero-height and then fire the dismiss
        // callback.
        // This triggers layout on each animation frame; in the future we may
        // want to do something
        // smarter and more performant.

        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
        final int originalHeight = mView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1)
                .setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCallback.onDismiss(mView, mToken);
                // Reset view presentation

                /*
                 * Alpha stays at 0, otherwise Android 2.x leaves weird
                 * artifacts
                 */
                // setAlpha(mView, 1f);

                mView.setTranslationX(0);
                lp.height = originalHeight;
                mView.setLayoutParams(lp);
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                mView.setLayoutParams(lp);
            }
        });

        animator.start();
    }

    public void showWithAnimation() {
        final int measureSpec = MeasureSpec.makeMeasureSpec(
                ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
        mView.measure(measureSpec, measureSpec);
        mViewWidth = mView.getMeasuredWidth();
        final int viewHeight = mView.getMeasuredHeight();
        mView.setAlpha(0f);

        final ViewGroup.LayoutParams lp = mView.getLayoutParams();
        final int originalHeight = lp.height;

        mView.setTranslationX(mViewWidth);

        // Grow space
        ValueAnimator animator = ValueAnimator.ofInt(1, viewHeight)
                .setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset view presentation
                lp.height = originalHeight;
                mView.setLayoutParams(lp);

                // Swipe view into space that opened up
                mView.animate().translationX(0).alpha(1)
                        .setDuration(mAnimationTime)
                        // Dummy listener so the default doesn't run
                        .setListener(new AnimatorListenerAdapter() {
                        });
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                mView.setLayoutParams(lp);
            }
        });

        animator.start();
    }
}
