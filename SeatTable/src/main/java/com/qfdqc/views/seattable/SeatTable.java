package com.qfdqc.views.seattable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by baoyunlong on 16/6/16.
 */
public class SeatTable extends View {
    private static final String TAG = SeatTable.class.getName();
    boolean isDebug;

    Paint paint = new Paint();
    Matrix matrix = new Matrix();
    int spacing = 6;
    int verSpacing = 10;
    int numberWidth = 20;

    int row = 0;
    int column = 0;

    Bitmap seatBitmap;
    Bitmap checkedSeatBitmap;
    Bitmap seatSoldBitmap;

    Bitmap overviewBitmap;

    Bitmap seat;
    int lastX;
    int lastY;

    int seatBitmapWidth;
    int seatBitmapHeight;
    boolean isNeedDrawSeatBitmap = true;

    float rectSize = 10;
    float rectWidth = 10;
    float overviewSpacing = 5;
    float overviewVerSpacing = 5;
    float overviewScale = 4.8f;

    float screenHeight;
    float screenWidthScale = 0.5f;
    int defaultScreenWidth;

    boolean isScaling;
    float scaleX, scaleY;
    boolean firstScale = true;

    public void setMaxSelected(int maxSelected) {
        this.maxSelected = maxSelected;
    }

    int maxSelected=Integer.MAX_VALUE;

    public void setSeatChecker(SeatChecker seatChecker) {
        this.seatChecker = seatChecker;
        invalidate();
    }

    private SeatChecker seatChecker;

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    private String screenName = "";

    ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            isScaling = true;
            float scaleFactor = detector.getScaleFactor();
            if (getMatrixScaleY() * scaleFactor > 3) {
                scaleFactor = 3 / getMatrixScaleY();
            }
            if (firstScale) {
                scaleX = detector.getCurrentSpanX();
                scaleY = detector.getCurrentSpanY();
                firstScale = false;
            }

            if (getMatrixScaleY() * scaleFactor < 0.5) {
                scaleFactor = 0.5f / getMatrixScaleY();
            }
            matrix.postScale(scaleFactor, scaleFactor, scaleX, scaleY);
            invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
            firstScale = true;
        }
    });

    boolean isOnClick;
    GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            isOnClick = true;
            int x = (int) e.getX();
            int y = (int) e.getY();

            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    int tempX = (int) ((j * seatBitmap.getWidth() + j * spacing) * getMatrixScaleX() + getTranslateX());
                    int maxTemX = (int) (tempX + seatBitmap.getWidth() * getMatrixScaleX());

                    int tempY = (int) ((i * seatBitmap.getHeight() + i * verSpacing) * getMatrixScaleY() + getTranslateY());
                    int maxTempY = (int) (tempY + seatBitmap.getHeight() * getMatrixScaleY());

                    if (seatChecker != null && seatChecker.isValidSeat(i, j) && !seatChecker.isSold(i, j)) {
                        if (x >= tempX && x <= maxTemX && y >= tempY && y <= maxTempY) {
                            String seat = i + "," + j;
                            if (isHave(seat)) {
                                remove(seat);
                                if (seatChecker != null) {
                                    seatChecker.unCheck(i, j);
                                }
                            } else {
                                if(selects.size()>=maxSelected){
                                    Toast.makeText(getContext(),"最多只能选择"+maxSelected+"个",Toast.LENGTH_SHORT).show();
                                    return super.onSingleTapConfirmed(e);
                                }else {
                                    selects.add(seat);
                                    if (seatChecker != null) {
                                        seatChecker.checked(i, j);
                                    }
                                }
                            }
                            isNeedDrawSeatBitmap = true;
                            isDrawOverviewBitmap = true;
                            float currentScaleY = getMatrixScaleY();

                            if (currentScaleY < 1.7) {
                                postDelayed(new AnimationScaleRunnable(x, y, 1.9f), SCALE_TIME);
                            }

                            invalidate();
                            break;
                        }
                    }
                }
            }

            return super.onSingleTapConfirmed(e);
        }
    });

    public SeatTable(Context context) {
        super(context);
    }

    int overview_checked;
    int overview_sold;
    int seatCheckedResID;
    int seatSoldResID;
    int seatAvailableResID;

    public SeatTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SeatTableView);
        overview_checked = typedArray.getColor(R.styleable.SeatTableView_overview_checked, Color.parseColor("#5A9E64"));
        overview_sold = typedArray.getColor(R.styleable.SeatTableView_overview_sold, Color.RED);
        seatCheckedResID = typedArray.getResourceId(R.styleable.SeatTableView_seat_checked, R.drawable.seat_green);
        seatSoldResID = typedArray.getResourceId(R.styleable.SeatTableView_overview_sold, R.drawable.seat_sold);
        seatAvailableResID = typedArray.getResourceId(R.styleable.SeatTableView_seat_available, R.drawable.seat_gray);

        init();
    }

    public SeatTable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init() {
        spacing = (int) dip2Px(5);
        verSpacing = (int) dip2Px(10);
        defaultScreenWidth = (int) dip2Px(80);

        seatBitmap = BitmapFactory.decodeResource(getResources(), seatAvailableResID);
        checkedSeatBitmap = BitmapFactory.decodeResource(getResources(), seatCheckedResID);
        seatSoldBitmap = BitmapFactory.decodeResource(getResources(), seatSoldResID);

        seatBitmapWidth = column * seatBitmap.getWidth() + (column - 1) * spacing;
        seatBitmapHeight = row * seatBitmap.getHeight() + (row - 1) * verSpacing;
        paint.setColor(Color.RED);
        numberWidth = (int) dip2Px(20);

        screenHeight = dip2Px(20);
        headHeight = dip2Px(30);
        if (seatBitmapWidth > 0 && seatBitmapHeight > 0) {
            seat = Bitmap.createBitmap(seatBitmapWidth, seatBitmapHeight, Bitmap.Config.ARGB_4444);
            seatCanvas = new Canvas(seat);
        }
        headPaint = new Paint();
        headPaint.setStyle(Paint.Style.FILL);
        headPaint.setTextSize(24);
        headPaint.setColor(Color.WHITE);
        headPaint.setAntiAlias(true);

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setColor(Color.parseColor("#e2e2e2"));

        redBorderPaint = new Paint();
        redBorderPaint.setAntiAlias(true);
        redBorderPaint.setColor(Color.RED);
        redBorderPaint.setStyle(Paint.Style.STROKE);
        redBorderPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 1);

        rectF = new RectF();

        rectSize = seatBitmap.getHeight() / overviewScale;
        rectWidth = seatBitmap.getWidth() / overviewScale;
        overviewSpacing = spacing / overviewScale;
        overviewVerSpacing = verSpacing / overviewScale;

        rectW = column * rectWidth + (column - 1) * overviewSpacing + overviewSpacing * 2;
        rectH = row * rectSize + (row - 1) * overviewVerSpacing + overviewVerSpacing * 2;
        overviewBitmap = Bitmap.createBitmap((int) rectW, (int) rectH, Bitmap.Config.ARGB_4444);
    }

    float rectW;
    float rectH;

    Paint headPaint;
    Bitmap headBitmap;
    boolean isFirstDraw = true;
    boolean isDrawOverview = false;
    boolean isDrawOverviewBitmap = true;

    @Override
    protected void onDraw(Canvas canvas) {
        if (row <= 0 || column == 0) {
            return;
        }
        if (isNeedDrawSeatBitmap) {
            drawSeat();
        }

        if (isFirstDraw) {
            isFirstDraw = false;
            matrix.postTranslate(getWidth() / 2 - seatBitmapWidth / 2, headHeight + screenHeight + borderHeight + verSpacing);
        }

        canvas.drawBitmap(seat, matrix, paint);
        drawNumber(canvas);

        if (headBitmap == null) {
            headBitmap = drawHeadInfo();
        }
        canvas.drawBitmap(headBitmap, 0, 0, null);

        drawScreen(canvas);

        if (isDrawOverview) {
            if (isDrawOverviewBitmap) {
                drawOverview();
            }
            canvas.drawBitmap(overviewBitmap, 0, 0, null);
            drawOverview(canvas);
        }
    }

    private int downX, downY;
    private boolean pointer;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();
        int x = (int) event.getX();
        super.onTouchEvent(event);

        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        int pointerCount = event.getPointerCount();
        if (pointerCount > 1) {
            pointer = true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pointer = false;
                downX = x;
                downY = y;
                isDrawOverview = true;
                handler.removeCallbacks(hideOverviewRunnable);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isScaling && !isOnClick) {
                    int downDX = Math.abs(x - downX);
                    int downDY = Math.abs(y - downY);
                    if ((downDX > 10 || downDY > 10) && !pointer) {
                        int dx = x - lastX;
                        int dy = y - lastY;
                        matrix.postTranslate(dx, dy);
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.postDelayed(hideOverviewRunnable, 1500);

                autoScale();
                int downDX = Math.abs(x - downX);
                int downDY = Math.abs(y - downY);
                if ((downDX > 10 || downDY > 10) && !pointer) {
                    autoScroll();
                }

                break;
        }
        isOnClick = false;
        lastY = y;
        lastX = x;

        return true;
    }

    private Runnable hideOverviewRunnable = new Runnable() {
        @Override
        public void run() {
            isDrawOverview = false;
            invalidate();
        }
    };

    float headHeight;
    int borderHeight = 1;

    Bitmap drawHeadInfo() {
        String txt = "已售";
        float txtY = getBaseLine(headPaint, 0, headHeight);
        int txtWidth = (int) headPaint.measureText(txt);
        float spacing = dip2Px(10);
        float spacing1 = dip2Px(5);
        float y = (headHeight - seatBitmap.getHeight()) / 2;

        float width = seatBitmap.getWidth() + spacing1 + txtWidth + spacing + seatSoldBitmap.getWidth() + txtWidth + spacing1 + spacing + checkedSeatBitmap.getHeight() + spacing1 + txtWidth;
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), (int) headHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        //绘制背景
        canvas.drawRect(0, 0, getWidth(), headHeight, headPaint);
        headPaint.setColor(Color.BLACK);

        float startX = (getWidth() - width) / 2;

        canvas.drawBitmap(seatBitmap, startX, (headHeight - seatBitmap.getHeight()) / 2, headPaint);
        canvas.drawText("可选", startX + seatBitmap.getWidth() + spacing1, txtY, headPaint);

        float soldSeatBitmapY = startX + seatBitmap.getWidth() + spacing1 + txtWidth + spacing;
        canvas.drawBitmap(seatSoldBitmap, soldSeatBitmapY, (headHeight - seatBitmap.getHeight()) / 2, headPaint);
        canvas.drawText("已售", soldSeatBitmapY + seatSoldBitmap.getWidth() + spacing1, txtY, headPaint);

        float checkedSeatBitmapX = soldSeatBitmapY + seatSoldBitmap.getWidth() + spacing1 + txtWidth + spacing;
        canvas.drawBitmap(checkedSeatBitmap, checkedSeatBitmapX, y, headPaint);
        canvas.drawText("已选", checkedSeatBitmapX + spacing1 + checkedSeatBitmap.getWidth(), txtY, headPaint);

        //绘制分割线
        headPaint.setStrokeWidth(1);
        headPaint.setColor(Color.GRAY);
        canvas.drawLine(0, headHeight, getWidth(), headHeight, headPaint);
        return bitmap;

    }

    Paint pathPaint;

    /**
     * 绘制中间屏幕
     */
    void drawScreen(Canvas canvas) {
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setColor(Color.parseColor("#e2e2e2"));
        float startY = headHeight + borderHeight;

        float centerX = seatBitmapWidth * getMatrixScaleX() / 2 + getTranslateX();
        float screenWidth = seatBitmapWidth * screenWidthScale * getMatrixScaleX();
        if (screenWidth < defaultScreenWidth) {
            screenWidth = defaultScreenWidth;
        }

        Path path = new Path();
        path.moveTo(centerX, startY);
        path.lineTo(centerX - screenWidth / 2, startY);
        path.lineTo(centerX - screenWidth / 2 + 20, screenHeight * getMatrixScaleY() + startY);
        path.lineTo(centerX + screenWidth / 2 - 20, screenHeight * getMatrixScaleY() + startY);
        path.lineTo(centerX + screenWidth / 2, startY);

        canvas.drawPath(path, pathPaint);

        pathPaint.setColor(Color.BLACK);
        pathPaint.setTextSize(20 * getMatrixScaleX());

        canvas.drawText(screenName, centerX - pathPaint.measureText(screenName) / 2, getBaseLine(pathPaint, startY, startY + screenHeight * getMatrixScaleY()), pathPaint);
    }

    private static final int SEAT_TYPE_SOLD = 1;
    private static final int SEAT_TYPE_SELECTED = 2;
    private static final int SEAT_TYPE_AVAILABLE = 3;
    private static final int SEAT_TYPE_NOT_AVAILABLE = 4;
    Canvas seatCanvas;

    void drawSeat() {

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {

                int left = j * seatBitmap.getWidth() + j * spacing;
                int top = i * seatBitmap.getHeight() + i * verSpacing;

                int seatType = getSeatType(i, j);
                switch (seatType) {
                    case SEAT_TYPE_AVAILABLE:
                        seatCanvas.drawBitmap(seatBitmap, left, top, paint);
                        break;
                    case SEAT_TYPE_NOT_AVAILABLE:

                        break;
                    case SEAT_TYPE_SELECTED:
                        seatCanvas.drawBitmap(checkedSeatBitmap, left, top, paint);

                        break;
                    case SEAT_TYPE_SOLD:
                        seatCanvas.drawBitmap(seatSoldBitmap, left, top, paint);
                        break;
                }

            }
        }
        isNeedDrawSeatBitmap = false;

    }

    private int getSeatType(int row, int column) {
        String seat = row + "," + column;
        int seatType = SEAT_TYPE_AVAILABLE;
        if (seatChecker != null) {
            if (!seatChecker.isValidSeat(row, column)) {
                seatType = SEAT_TYPE_NOT_AVAILABLE;
            } else if (seatChecker.isSold(row, column)) {
                seatType = SEAT_TYPE_SOLD;
            }
        }

        if (isHave(seat)) {
            seatType = SEAT_TYPE_SELECTED;
        }
        return seatType;
    }

    RectF rectF;

    /**
     * 绘制行号
     */
    void drawNumber(Canvas canvas) {

        paint.setColor(Color.parseColor("#7e000000"));
        paint.setTextSize(getResources().getDisplayMetrics().density * 16);
        paint.setAntiAlias(true);
        int translateY = (int) getTranslateY();
        float scaleY = getMatrixScaleY();

        rectF.top = translateY - paint.measureText("4") / 2;
        rectF.bottom = translateY + (seatBitmapHeight * scaleY) + paint.measureText("4") / 2;
        rectF.left = 0;
        rectF.right = numberWidth;
        canvas.drawRoundRect(rectF, numberWidth / 2, numberWidth / 2, paint);

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();

        paint.setColor(Color.WHITE);

        paint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < row; i++) {

            int top = (int) ((i * seatBitmap.getHeight() + i * verSpacing) * scaleY) + translateY;
            int bottom = (int) ((i * seatBitmap.getHeight() + i * verSpacing + seatBitmap.getHeight()) * scaleY) + translateY;
            int baseline = (int) ((bottom + top - fontMetrics.bottom - fontMetrics.top) / 2);

            canvas.drawText((i + 1) + "", numberWidth / 2, baseline, paint);
        }
    }

    Paint redBorderPaint;

    /**
     * 绘制概览图
     */
    void drawOverview(Canvas canvas) {

        //绘制红色框
        int left = (int) -getTranslateX();
        if (left < 0) {
            left = 0;
        }
        left /= overviewScale;
        left /= getMatrixScaleX();

        int currentWidth = (int) (getTranslateX() + (column * seatBitmap.getWidth() + spacing * (column - 1)) * getMatrixScaleX());
        if (currentWidth > getWidth()) {
            currentWidth = currentWidth - getWidth();
        } else {
            currentWidth = 0;
        }
        int right = (int) (rectW - currentWidth / overviewScale / getMatrixScaleX());

        float top = -getTranslateY()+headHeight;
        if (top < 0) {
            top = 0;
        }
        top /= overviewScale;
        top /= getMatrixScaleY();
        if (top > 0) {
            top += overviewVerSpacing;
        }

        int currentHeight = (int) (getTranslateY() + (row * seatBitmap.getHeight() + verSpacing * (row - 1)) * getMatrixScaleY());
        if (currentHeight > getHeight()) {
            currentHeight = currentHeight - getHeight();
        } else {
            currentHeight = 0;
        }
        int bottom = (int) (rectH - currentHeight / overviewScale / getMatrixScaleY());

        canvas.drawRect(left, top, right, bottom, redBorderPaint);
    }

    Bitmap drawOverview() {
        isDrawOverviewBitmap = false;

        int bac = Color.parseColor("#7e000000");
        paint.setColor(bac);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas(overviewBitmap);
        //绘制透明灰色背景
        canvas.drawRect(0, 0, rectW, rectH, paint);

        paint.setColor(Color.WHITE);
        for (int i = 0; i < row; i++) {
            float top = i * rectSize + i * overviewVerSpacing + overviewVerSpacing;
            for (int j = 0; j < column; j++) {

                int seatType = getSeatType(i, j);
                switch (seatType) {
                    case SEAT_TYPE_AVAILABLE:
                        paint.setColor(Color.WHITE);
                        break;
                    case SEAT_TYPE_NOT_AVAILABLE:
                        continue;
                    case SEAT_TYPE_SELECTED:
                        paint.setColor(overview_checked);
                        break;
                    case SEAT_TYPE_SOLD:
                        paint.setColor(overview_sold);
                        break;
                }

                float left;

                left = j * rectWidth + j * overviewSpacing + overviewSpacing;
                canvas.drawRect(left, top, left + rectWidth, top + rectSize, paint);
            }
        }

        return overviewBitmap;
    }

    /**
     * 自动回弹
     * 整个大小不超过控件大小的时候:
     * 往左边滑动,自动回弹到行号右边
     * 往右边滑动,自动回弹到右边
     * 往上,下滑动,自动回弹到顶部
     * <p>
     * 整个大小超过控件大小的时候:
     * 往左侧滑动,回弹到最右边,往右侧滑回弹到最左边
     * 往上滑动,回弹到底部,往下滑动回弹到顶部
     */
    private void autoScroll() {
        float currentSeatBitmapWidth = seatBitmapWidth * getMatrixScaleX();
        float currentSeatBitmapHeight = seatBitmapHeight * getMatrixScaleY();
        float moveYLength = 0;
        float moveXLength = 0;

        //处理左右滑动的情况
        if (currentSeatBitmapWidth < getWidth()) {
            if (getTranslateX() < 0 || getMatrixScaleX() < numberWidth + spacing) {
                //计算要移动的距离

                if (getTranslateX() < 0) {
                    moveXLength = (-getTranslateX()) + numberWidth + spacing;
                } else {
                    moveXLength = numberWidth + spacing - getTranslateX();
                }

            }
        } else {

            if (getTranslateX() < 0 && getTranslateX() + currentSeatBitmapWidth > getWidth()) {

            } else {
                //往左侧滑动
                if (getTranslateX() + currentSeatBitmapWidth < getWidth()) {
                    moveXLength = getWidth() - (getTranslateX() + currentSeatBitmapWidth);
                } else {
                    //右侧滑动
                    moveXLength = -getTranslateX() + numberWidth + spacing;
                }
            }

        }

        float startYPosition = screenHeight * getMatrixScaleY() + verSpacing * getMatrixScaleY() + headHeight + borderHeight;

        //处理上下滑动
        if (currentSeatBitmapHeight < getHeight()) {

            if (getTranslateY() < startYPosition) {
                moveYLength = startYPosition - getTranslateY();
            } else {
                moveYLength = -(getTranslateY() - (startYPosition));
            }

        } else {

            if (getTranslateY() < 0 && getTranslateY() + currentSeatBitmapHeight > getHeight()) {

            } else {
                //往上滑动
                if (getTranslateY() + currentSeatBitmapHeight < getHeight()) {
                    moveYLength = getHeight() - (getTranslateY() + currentSeatBitmapHeight);
                } else {
                    moveYLength = -(getTranslateY() - (startYPosition));
                }
            }
        }

        Message message = Message.obtain();
        MoveInfo moveInfo = new MoveInfo();
        moveInfo.moveXLength = moveXLength;
        moveInfo.moveYLength = moveYLength;
        message.obj = moveInfo;
        handler.sendMessageDelayed(message, time);
    }

    class MoveInfo {
        public float moveXLength;
        public float moveYLength;
    }

    private void autoScale() {

        if (getMatrixScaleX() > 2.2) {
            postDelayed(new AnimationScaleRunnable(scaleX, scaleY, 2.0f), SCALE_TIME);
        } else if (getMatrixScaleX() < 0.98) {
            postDelayed(new AnimationScaleRunnable(scaleX, scaleY, 1.0f), SCALE_TIME);
        }
    }

    int FRAME_COUNT = 10;
    int time = 15;
    int count;
    int SCALE_TIME = 15;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            if (count < FRAME_COUNT) {
                count++;
                MoveInfo moveInfo = (MoveInfo) msg.obj;
                float moveXLength = moveInfo.moveXLength;
                float moveYLength = moveInfo.moveYLength;
                float xValue = moveXLength / FRAME_COUNT;
                float yValue = moveYLength / FRAME_COUNT;

                matrix.postTranslate(xValue, yValue);
                invalidate();
                Message message = Message.obtain();
                message.obj = msg.obj;
                handler.sendMessageDelayed(message, time);
            } else {
                count = 0;
            }

            return true;
        }
    });


    ArrayList<String> selects = new ArrayList<>();

    public ArrayList<String> getSelectedSeats() {
        return selects;
    }

    private boolean isHave(String seat) {
        for (String item : selects) {
            if (seat.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private void remove(String seat) {

        for (int i = 0; i < selects.size(); i++) {
            String item = selects.get(i);
            if (seat.equals(item)) {
                selects.remove(i);
                break;
            }
        }
    }

    float[] m = new float[9];

    private float getTranslateX() {

        matrix.getValues(m);
        return m[2];
    }

    private float getTranslateY() {
        float[] m = new float[9];
        matrix.getValues(m);
        return m[5];
    }

    private float getMatrixScaleY() {
        matrix.getValues(m);
        return m[4];
    }

    private float getMatrixScaleX() {
        matrix.getValues(m);
        return m[0];
    }

    private float dip2Px(float value) {
        return getResources().getDisplayMetrics().density * value;
    }

    private float getBaseLine(Paint p, float top, float bottom) {
        Paint.FontMetrics fontMetrics = p.getFontMetrics();
        int baseline = (int) ((bottom + top - fontMetrics.bottom - fontMetrics.top) / 2);
        return baseline;
    }

    public class AnimationScaleRunnable implements Runnable {
        private float x, y;//缩放的中心点

        private float targetScale;

        private float tempScale;

        public AnimationScaleRunnable(float x, float y, float targetScale) {
            float currentScale = getMatrixScaleX();
            this.x = x;
            this.y = y;
            this.targetScale = targetScale;

            if (currentScale < targetScale) {
                tempScale = 1.06f;
            } else {
                tempScale = 0.95f;
            }
        }

        @Override
        public void run() {
            matrix.postScale(tempScale, tempScale, x, y);
            invalidate();
            float currentScale = getMatrixScaleX();

            if (tempScale > 1 && currentScale < targetScale) {

                postDelayed(this, SCALE_TIME);
            } else if (tempScale < 1 && currentScale > targetScale) {
                postDelayed(this, SCALE_TIME);
            }
        }
    }

    public void setData(int row, int column) {
        this.row = row;
        this.column = column;
        init();
        invalidate();
    }

    public interface SeatChecker {
        /**
         * 是否可用座位
         *
         * @param row
         * @param column
         * @return
         */
        boolean isValidSeat(int row, int column);

        /**
         * 是否已售
         *
         * @param row
         * @param column
         * @return
         */
        boolean isSold(int row, int column);

        void checked(int row, int column);

        void unCheck(int row, int column);

    }

}
