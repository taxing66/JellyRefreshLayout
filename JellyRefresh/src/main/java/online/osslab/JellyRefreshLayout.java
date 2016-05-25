package online.osslab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;


/**
 * 动感刷新
 * http://jellyrefresh.osslab.online
 */

public class JellyRefreshLayout extends FrameLayout {

    private static String TAG = "RippleRefreshLayout";

    private static final long BACK_TOP_DUR = 600;
    private static final long REL_DRAG_DUR = 200;

    private int headerBackColor = 0xff8b90af;
    private int headerForeColor = 0xffffffff;
    private int headerJellySmaller = 6;


    private float pullHeight;
    private float headerHeight;
    private View childView;
    private AnimationView animationView;

    private boolean isRefreshing;

    private float touchStartY;

    private float touchCurY;

    private ValueAnimator upBackAnimator;
    private ValueAnimator upTopAnimator;

    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(10);

    public JellyRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public JellyRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JellyRefreshLayout(Context context, AttributeSet attrs, int styleAttr) {
        super(context, attrs, styleAttr);
        init(context, attrs, styleAttr);
    }

    private void init(Context context, AttributeSet attrs, int styleAttr) {

        if (getChildCount() > 1) {
            throw new RuntimeException("you can only attach one child");
        }
        setAttrs(attrs);
        pullHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
        headerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());

        this.post(new Runnable() {
            @Override
            public void run() {
                childView = getChildAt(0);
                addHeaderView();
            }
        });

    }

    private void setAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.JellyRefreshLayout);

        headerBackColor = typedArray.getColor(R.styleable.JellyRefreshLayout_backColor, headerBackColor);
        headerForeColor = typedArray.getColor(R.styleable.JellyRefreshLayout_foreColor, headerForeColor);
        headerJellySmaller = typedArray.getInt(R.styleable.JellyRefreshLayout_smaller, headerJellySmaller);

        typedArray.recycle();
    }

    private void addHeaderView() {
        animationView = new AnimationView(getContext());
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        params.gravity = Gravity.TOP;
        animationView.setLayoutParams(params);

        addViewInternal(animationView);
        animationView.setBackColor(headerBackColor);
        animationView.setForeColor(headerForeColor);
        animationView.setRadius(headerJellySmaller);

        setUpChildAnimation();
    }

    private void setUpChildAnimation() {
        if (childView == null) {
            return;
        }
        upBackAnimator = ValueAnimator.ofFloat(pullHeight, headerHeight);
        upBackAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                if (childView != null) {
                    childView.setTranslationY(val);
                }
            }
        });
        upBackAnimator.setDuration(REL_DRAG_DUR);
        upTopAnimator = ValueAnimator.ofFloat(headerHeight, 0);
        upTopAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                val = decelerateInterpolator.getInterpolation(val / headerHeight) * val;
                if (childView != null) {
                    childView.setTranslationY(val);
                }
                animationView.getLayoutParams().height = (int) val;
                animationView.requestLayout();
            }
        });
        upTopAnimator.setDuration(BACK_TOP_DUR);

        animationView.setOnViewAnimatorDone(new AnimationView.OnViewAnimatorDone() {
            @Override
            public void viewAnimatorDone() {
//                Log.i(TAG, "should invoke");
                upTopAnimator.start();
            }
        });


    }

    private void addViewInternal(@NonNull View child) {
        super.addView(child);
    }

    @Override
    public void addView(View child) {
        if (getChildCount() >= 1) {
            throw new RuntimeException("you can only attach one child");
        }

        childView = child;
        super.addView(child);
        setUpChildAnimation();
    }

    private boolean canChildScrollUp() {
        if (childView == null) {
            return false;
        }


        return ViewCompat.canScrollVertically(childView, -1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isRefreshing) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = event.getY();
                touchCurY = touchStartY;
                break;
            case MotionEvent.ACTION_MOVE:
                float curY = event.getY();
                float dropY = curY - touchStartY;
                if (dropY > 0 && !canChildScrollUp()) {
                    return true;
                }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRefreshing) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                touchCurY = event.getY();
                float dropY = touchCurY - touchStartY;
                dropY = Math.min(pullHeight * 2, dropY);
                dropY = Math.max(0, dropY);


                if (childView != null) {
                    float offsetY = decelerateInterpolator.getInterpolation(dropY / 2 / pullHeight) * dropY / 2;
                    childView.setTranslationY(offsetY);

                    animationView.getLayoutParams().height = (int) offsetY;
                    animationView.requestLayout();
                }


                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (childView != null) {
                    if (childView.getTranslationY() >= headerHeight) {
                        upBackAnimator.start();
                        animationView.releaseDrag();
                        isRefreshing = true;
                        if (onJellyRefreshListener !=null) {
                            onJellyRefreshListener.refreshing();
                        }

                    } else {
                        float height = childView.getTranslationY();
                        ValueAnimator backTopAni = ValueAnimator.ofFloat(height, 0);
                        backTopAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float val = (float) animation.getAnimatedValue();
                                val = decelerateInterpolator.getInterpolation(val / headerHeight) * val;
                                if (childView != null) {
                                    childView.setTranslationY(val);
                                }
                                animationView.getLayoutParams().height = (int) val;
                                animationView.requestLayout();
                            }
                        });
                        backTopAni.setDuration((long) (height * BACK_TOP_DUR / headerHeight));
                        backTopAni.start();
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public void finishRefreshing() {
        if (onJellyRefreshListener != null) {
            onJellyRefreshListener.completeRefresh();
        }
        isRefreshing = false;
        animationView.setRefreshing(false);
    }

    private OnJellyRefreshListener onJellyRefreshListener;

    public void setOnRefreshListener(OnJellyRefreshListener onJellyRefreshListener) {
        this.onJellyRefreshListener = onJellyRefreshListener;
    }

    public interface OnJellyRefreshListener {
        void completeRefresh();
        void refreshing();
    }
}
