package online.osslab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * 动感刷新
 * http://jellyrefresh.osslab.online
 */

public class AnimationView extends View {

    private static final String TAG = "AnimationView";

    private int PULL_HEIGHT;
    private int PULL_DELTA;
    private float WIDTH_OFFSET;



    private AnimatorStatus animatorStatus = AnimatorStatus.PULL_DOWN;

    enum AnimatorStatus {
        PULL_DOWN,
        DRAG_DOWN,
        REL_DRAG,
        SPRING_UP, // rebound to up, the position is less than PULL_HEIGHT
        POP_BALL,
        OUTER_CIR,
        REFRESHING,
        DONE,
        STOP;

        @Override
        public String toString() {
            switch (this) {
                case PULL_DOWN:
                    return "pull down";
                case DRAG_DOWN:
                    return "drag down";
                case REL_DRAG:
                    return "release drag";
                case SPRING_UP:
                    return "spring up";
                case POP_BALL:
                    return "pop ball";
                case OUTER_CIR:
                    return "outer circle";
                case REFRESHING:
                    return "refreshing...";
                case DONE:
                    return "done!";
                case STOP:
                    return "stop";
                default:
                    return "unknown state";
            }
        }
    }


    private Paint backPaint;
    private Paint ballPaint;
    private Paint outPaint;
    private Path path;


    public AnimationView(Context context) {
        this(context, null, 0);
    }

    public AnimationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimationView(Context context, AttributeSet attrs, int styleAttr) {
        super(context, attrs, styleAttr);
        initView(context, attrs, styleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int styleAttr) {

        PULL_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        PULL_DELTA = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        WIDTH_OFFSET = 0.5f;
        backPaint = new Paint();
        backPaint.setAntiAlias(true);
        backPaint.setStyle(Paint.Style.FILL);
        backPaint.setColor(0xff8b90af);

        ballPaint = new Paint();
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(0xffffffff);
        ballPaint.setStyle(Paint.Style.FILL);

        outPaint = new Paint();
        outPaint.setAntiAlias(true);
        outPaint.setColor(0xffffffff);
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setStrokeWidth(5);


        path = new Path();

    }

    private int radius;
    private int width;
    private int height;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (height > PULL_DELTA + PULL_HEIGHT) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(PULL_DELTA + PULL_HEIGHT, MeasureSpec.getMode(heightMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            radius = getHeight() / 6;
            width = getWidth();
            height = getHeight();

            if (height < PULL_HEIGHT) {
                animatorStatus = AnimatorStatus.PULL_DOWN;
            }


            switch (animatorStatus) {
                case PULL_DOWN:
                    if (height >= PULL_HEIGHT) {
                        animatorStatus = AnimatorStatus.DRAG_DOWN;
                    }
                    break;
                case REL_DRAG:
                    break;
            }

        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (animatorStatus) {
            case PULL_DOWN:
                canvas.drawRect(0, 0, width, height, backPaint);
                break;
            case REL_DRAG:
            case DRAG_DOWN:
                drawDrag(canvas);
                break;
            case SPRING_UP:
                drawSpring(canvas, getSpringDelta());
                invalidate();
                break;
            case POP_BALL:
                drawPopBall(canvas);
                invalidate();
                break;
            case OUTER_CIR:
                drawOutCircle(canvas);
                invalidate();
                break;
            case REFRESHING:
                drawRefreshing(canvas);
                invalidate();
                break;
            case DONE:
                drawDone(canvas);
                invalidate();
                break;
            case STOP:
                drawDone(canvas);
                break;

        }

        if (animatorStatus == AnimatorStatus.REL_DRAG) {
            ViewGroup.LayoutParams params = getLayoutParams();
            int height;
            // NOTICE: If the height equals lastHeight, then the requestLayout() will not work correctly
            do {
                height = getRelHeight();
            } while (height == lastHeight && getRelRatio() != 1);
            lastHeight = height;
            params.height = PULL_HEIGHT + height;
            requestLayout();
        }


    }

    private void drawDrag(Canvas canvas) {
        canvas.drawRect(0, 0, width, PULL_HEIGHT, backPaint);

        path.reset();
        path.moveTo(0, PULL_HEIGHT);
        path.quadTo(WIDTH_OFFSET * width, PULL_HEIGHT + (height - PULL_HEIGHT) * 2,
                width, PULL_HEIGHT);
        canvas.drawPath(path, backPaint);
    }

    private void drawSpring(Canvas canvas, int springDelta) {
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(0, PULL_HEIGHT);
        path.quadTo(width / 2, PULL_HEIGHT - springDelta,
                width, PULL_HEIGHT);
        path.lineTo(width, 0);
        canvas.drawPath(path, backPaint);

        int currentHeight = PULL_HEIGHT - springDelta / 2;

        if (currentHeight > PULL_HEIGHT - PULL_DELTA / 2) {
            int leftX = (int) (width / 2 - 2 * radius + getSprRatio() * radius);
            path.reset();
            path.moveTo(leftX, currentHeight);
            path.quadTo(width / 2, currentHeight - radius * getSprRatio() * 2,
                    width - leftX, currentHeight);
            canvas.drawPath(path, ballPaint);
        } else {
            canvas.drawArc(new RectF(width / 2 - radius, currentHeight - radius, width / 2 + radius, currentHeight + radius),
                    180, 180, true, ballPaint);
        }

    }

    private void drawPopBall(Canvas canvas) {
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(0, PULL_HEIGHT);
        path.quadTo(width / 2, PULL_HEIGHT - PULL_DELTA,
                width, PULL_HEIGHT);
        path.lineTo(width, 0);
        canvas.drawPath(path, backPaint);

        int cirCentStart = PULL_HEIGHT - PULL_DELTA / 2;
        int cirCenY = (int) (cirCentStart - radius * 2 * getPopRatio());

        canvas.drawArc(new RectF(width / 2 - radius, cirCenY - radius, width / 2 + radius, cirCenY + radius),
                180, 360, true, ballPaint);

        if (getPopRatio() < 1) {
            drawTail(canvas, cirCenY, cirCentStart + 1, getPopRatio());
        } else {
            canvas.drawCircle(width / 2, cirCenY, radius, ballPaint);
        }


    }

    private void drawTail(Canvas canvas, int centerY, int bottom, float fraction) {
        int bezier1w = (int) (width / 2 + (radius * 3 / 4) * (1 - fraction));
        PointF start = new PointF(width / 2 + radius, centerY);
        PointF bezier1 = new PointF(bezier1w, bottom);
        PointF bezier2 = new PointF(bezier1w + radius / 2, bottom);

        path.reset();
        path.moveTo(start.x, start.y);
        path.quadTo(bezier1.x, bezier1.y,
                bezier2.x, bezier2.y);
        path.lineTo(width - bezier2.x, bezier2.y);
        path.quadTo(width - bezier1.x, bezier1.y,
                width - start.x, start.y);
        canvas.drawPath(path, ballPaint);
    }

    private void drawOutCircle(Canvas canvas) {
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(0, PULL_HEIGHT);
        path.quadTo(width / 2, PULL_HEIGHT - (1 - getOutRatio()) * PULL_DELTA,
                width, PULL_HEIGHT);
        path.lineTo(width, 0);
        canvas.drawPath(path, backPaint);
        int innerY = PULL_HEIGHT - PULL_DELTA / 2 - radius * 2;
        canvas.drawCircle(width / 2, innerY, radius, ballPaint);
    }

    private int refreshStart = 90;
    private int refreshStop = 90;
    private int TARGET_DEGREE = 270;
    private boolean isStart = true;
    private boolean isRefreshing = true;

    private void drawRefreshing(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backPaint);
        int innerY = PULL_HEIGHT - PULL_DELTA / 2 - radius * 2;
        canvas.drawCircle(width / 2, innerY, radius, ballPaint);
        int outerR = radius + 10;

        refreshStart += isStart ? 3 : 10;
        refreshStop += isStart ? 10 : 3;
        refreshStart = refreshStart % 360;
        refreshStop = refreshStop % 360;

        int swipe = refreshStop - refreshStart;
        swipe = swipe < 0 ? swipe + 360 : swipe;

        canvas.drawArc(new RectF(width / 2 - outerR, innerY - outerR, width / 2 + outerR, innerY + outerR),
                refreshStart, swipe, false, outPaint);
        if (swipe >= TARGET_DEGREE) {
            isStart = false;
        } else if (swipe <= 10) {
            isStart = true;
        }
        if (!isRefreshing) {
            applyDone();

        }

    }

    // stop refreshing
    public void setRefreshing(boolean isFresh) {
        isRefreshing = isFresh;
    }

    private void drawDone(Canvas canvas) {


        int beforeColor = outPaint.getColor();
        if (getDoneRatio() < 0.3) {
            canvas.drawRect(0, 0, width, height, backPaint);
            int innerY = PULL_HEIGHT - PULL_DELTA / 2 - radius * 2;
            canvas.drawCircle(width / 2, innerY, radius, ballPaint);
            int outerR = (int) (radius + 10 + 10 * getDoneRatio() / 0.3f);
            int afterColor = Color.argb((int) (0xff * (1 - getDoneRatio() / 0.3f)), Color.red(beforeColor),
                    Color.green(beforeColor), Color.blue(beforeColor));
            outPaint.setColor(afterColor);
            canvas.drawArc(new RectF(width / 2 - outerR, innerY - outerR, width / 2 + outerR, innerY + outerR),
                    0, 360, false, outPaint);
        }
        outPaint.setColor(beforeColor);


        if (getDoneRatio() >= 0.3 && getDoneRatio() < 0.7) {
            canvas.drawRect(0, 0, width, height, backPaint);
            float fraction = (getDoneRatio() - 0.3f) / 0.4f;
            int startCentY = PULL_HEIGHT - PULL_DELTA / 2 - radius * 2;
            int curCentY = (int) (startCentY + (PULL_DELTA / 2 + radius * 2) * fraction);
            canvas.drawCircle(width / 2, curCentY, radius, ballPaint);
            if (curCentY >= PULL_HEIGHT - radius * 2) {
                drawTail(canvas, curCentY, PULL_HEIGHT, (1 - fraction));
            }
        }

        if (getDoneRatio() >= 0.7 && getDoneRatio() <= 1) {
            float fraction = (getDoneRatio() - 0.7f) / 0.3f;
            canvas.drawRect(0, 0, width, height, backPaint);
            int leftX = (int) (width / 2 - radius - 2 * radius * fraction);
            path.reset();
            path.moveTo(leftX, PULL_HEIGHT);
            path.quadTo(width / 2, PULL_HEIGHT - (radius * (1 - fraction)),
                    width - leftX, PULL_HEIGHT);
            canvas.drawPath(path, ballPaint);
        }

    }

    private int lastHeight;

    private int getRelHeight() {
        return (int) (spring * (1 - getRelRatio()));
    }

    private int getSpringDelta() {
        return (int) (PULL_DELTA * getSprRatio());
    }


    private static long REL_DRAG_DUR = 200;

    private long start;
    private long stop;
    private int spring;

    public void releaseDrag() {
        start = System.currentTimeMillis();
        stop = start + REL_DRAG_DUR;
        animatorStatus = AnimatorStatus.REL_DRAG;
        spring = height - PULL_HEIGHT;
        requestLayout();
    }

    private float getRelRatio() {
        if (System.currentTimeMillis() >= stop) {
            springUp();
            return 1;
        }
        float ratio = (System.currentTimeMillis() - start) / (float) REL_DRAG_DUR;
        return Math.min(ratio, 1);
    }

    private static long SPRING_DUR = 200;
    private long sprStart;
    private long sprStop;


    private void springUp() {
        sprStart = System.currentTimeMillis();
        sprStop = sprStart + SPRING_DUR;
        animatorStatus = AnimatorStatus.SPRING_UP;
        invalidate();
    }


    private float getSprRatio() {
        if (System.currentTimeMillis() >= sprStop) {
            popBall();
            return 1;
        }
        float ratio = (System.currentTimeMillis() - sprStart) / (float) SPRING_DUR;
        return Math.min(1, ratio);
    }

    private static final long POP_BALL_DUR = 300;
    private long popStart;
    private long popStop;

    private void popBall() {
        popStart = System.currentTimeMillis();
        popStop = popStart + POP_BALL_DUR;
        animatorStatus = AnimatorStatus.POP_BALL;
        invalidate();
    }

    private float getPopRatio() {
        if (System.currentTimeMillis() >= popStop) {
            startOutCir();
            return 1;
        }

        float ratio = (System.currentTimeMillis() - popStart) / (float) POP_BALL_DUR;
        return Math.min(ratio, 1);
    }

    private static final long OUTER_DUR = 200;
    private long outStart;
    private long outStop;

    private void startOutCir() {
        outStart = System.currentTimeMillis();
        outStop = outStart + OUTER_DUR;
        animatorStatus = AnimatorStatus.OUTER_CIR;
        refreshStart = 90;
        refreshStop = 90;
        TARGET_DEGREE = 270;
        isStart = true;
        isRefreshing = true;
        invalidate();
    }

    private float getOutRatio() {
        if (System.currentTimeMillis() >= outStop) {
            animatorStatus = AnimatorStatus.REFRESHING;
            isRefreshing = true;
            return 1;
        }
        float ratio = (System.currentTimeMillis() - outStart) / (float) OUTER_DUR;
        return Math.min(ratio, 1);
    }

    private static final long DONE_DUR = 1000;
    private long doneStart;
    private long doneStop;

    private void applyDone() {
        doneStart = System.currentTimeMillis();
        doneStop = doneStart + DONE_DUR;
        animatorStatus = AnimatorStatus.DONE;
    }

    private float getDoneRatio() {
        if (System.currentTimeMillis() >= doneStop) {
            animatorStatus = AnimatorStatus.STOP;
            if (onViewAnimatorDone != null) {
                onViewAnimatorDone.viewAnimatorDone();
            }
            return 1;
        }

        float ratio = (System.currentTimeMillis() - doneStart) / (float) DONE_DUR;
        return Math.min(ratio, 1);
    }


    private OnViewAnimatorDone onViewAnimatorDone;

    public void setOnViewAnimatorDone(OnViewAnimatorDone onViewAnimatorDone) {
        this.onViewAnimatorDone = onViewAnimatorDone;
    }

    interface OnViewAnimatorDone {
        void viewAnimatorDone();
    }


    public void setBackColor(int color) {
        backPaint.setColor(color);
    }

    public void setForeColor(int color) {
        ballPaint.setColor(color);
        outPaint.setColor(color);
        setBackgroundColor(color);
    }

    // the height of view is smallTimes times of circle radius
    public void setRadius(int smallTimes) {
        radius = height / smallTimes;
    }


}
