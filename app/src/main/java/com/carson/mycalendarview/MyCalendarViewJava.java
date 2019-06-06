package com.carson.mycalendarview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MyCalendarViewJava extends View {

    private static final String TAG = "MyCalendarView";

    // 固定的月份信息
    private static final String[] months = new String[]{"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    // 固定的星期信息
    private static final String[] weeks = new String[]{"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    // 选中的日期做处理
    private static final List<Integer> selectedDates = new ArrayList<>();

    // 代表点击时判定是前一个月还是后一个月
    public enum TYPE {BEFORE, AFTER}

    // 定义画笔 这里其实可以优化省略一点
    private Paint mBgPaint;
    private Paint mTextPaint;
    private Paint mPaint;
    private Paint mBorderPaint;

    // 当前月份 日期
    private String currentMonth;
    private int currentDate;

    // 字体大小
    private int mTitleSize;
    private int mWeekSize;
    private int mDateSize;

    // 字体颜色
    private int mColorText;
    private int mSelectedColorText;
    private int mSelectedColor;

    // 阴影 在关闭硬件加速后会显示
    private Shader mSignedShader;
    private Shader mShader;

    // 标题高度
    private float mTitleHeight;
    // 星期几的高度
    private float mWeekHeight;
    // 日期行的高度
    private float mDayHeight;

    private float mTitlePaddingTop;
    private float mWeekPaddingTop;

    // 日期计算的相关参数
    private int rowCount;
    private boolean isCurMonth;
    private int firstIndex;
    private int dayOfMonth;
    private int remains;
    private int lastMonthDays;

    private int perWidth;

    // 月份改变监听器
    private OnChangeMonthListener mListener;

    public MyCalendarViewJava(Context context) {
        this(context, null);
    }

    public MyCalendarViewJava(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyCalendarViewJava(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTitleSize = (int) dp2px(30);
        mWeekSize = (int) dp2px(10);
        mDateSize = (int) dp2px(12);

        mColorText = Color.BLACK;
        mSelectedColorText = Color.WHITE;
        mSelectedColor = getResources().getColor(R.color.colorAccent);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

//        mTextPaint.setColor(Color.BLACK);

        mTextPaint.setTextSize(mTitleSize);
        mTitleHeight = mTextPaint.getFontMetrics().descent - mTextPaint.getFontMetrics().ascent;

        mTextPaint.setTextSize(mWeekSize);
        mWeekHeight = mTextPaint.getFontMetrics().descent - mTextPaint.getFontMetrics().ascent;

        mTextPaint.setTextSize(mDateSize);
        mDayHeight = mTextPaint.getFontMetrics().descent - mTextPaint.getFontMetrics().ascent;

        mBgPaint.setColor(Color.parseColor("#DDDDDD"));
        mBgPaint.setStrokeWidth(1);

        mTitlePaddingTop = dp2px(10);
        mWeekPaddingTop = dp2px(15);

//        setMonth(new Date());
        // 放置在初始化可以避免经常重绘设置 提高性能
        setLayerType(LAYER_TYPE_SOFTWARE, null);

    }

    public void setMonthChangeListener(OnChangeMonthListener listener) {
        this.mListener = listener;
    }

    public void setMonth(Date date) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        String mCurDate = sf.format(date);
        Log.i(TAG, "mCurDate = " + mCurDate);
        String month = mCurDate.substring(4, 6);
        Log.i(TAG, "month = " + month);

        String curDate = sf.format(new Date());
        String curMonth = curDate.substring(4, 6);
        if (curMonth.equals(month)) {
            isCurMonth = true;
        } else {
            isCurMonth = false;
        }

        if (month.startsWith("0")) {
            month = month.substring(1);
        }
        Log.i(TAG, "month = " + month);
        int monthInt = Integer.parseInt(month);
        this.currentMonth = months[monthInt - 1];
        Log.i(TAG, "monthInt = " + monthInt);

        String mDate = mCurDate.substring(6, 8);
        if (mDate.startsWith("0")) {
            mDate = mDate.substring(1);
        }
        // 获取到当前日期
        this.currentDate = Integer.parseInt(mDate);

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        // 当前月份第一天开始是星期几 周一开始算的话要 - 2
        firstIndex = c.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstIndex < 0) {
            firstIndex += 7;
        }

        Log.i(TAG, "firstIndex = " + firstIndex);

        // 当前月份的天数
        dayOfMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int days = dayOfMonth;
        Log.i(TAG, "dayOfMonth = " + dayOfMonth);

        days -= (7 - firstIndex);
        // 最后一行剩余的天数
        remains = days;
        rowCount = 1;
        while (remains >= 7) {
            remains -= 7;
            rowCount++;
        }
        if (remains > 0) rowCount++;

        Log.i(TAG, "rowCount = " + rowCount);
        Log.i(TAG, "remains = " + remains);

        // 模拟添加数据
        selectedDates.add(4);
        selectedDates.add(9);
        selectedDates.add(16);
        selectedDates.add(25);

        // 上一个月
        c.add(Calendar.MONTH, -1);
        // 上一个月总共有多少天
        lastMonthDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec); //布满父布局
        perWidth = width / 7;
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 设置关闭硬件加速 否则不显示阴影 但是在此开启后会很慢 因为会不断重绘
//        setLayerType(LAYER_TYPE_SOFTWARE, null);

        drawMonth(canvas);

        drawWeek(canvas);

        drawDate(canvas);

//        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void drawMonth(Canvas canvas) {
        mTextPaint.setTextSize(mTitleSize);
        mTextPaint.setColor(Color.BLACK);
        int x = (getWidth() - (int) mTextPaint.measureText(currentMonth)) / 2;
        canvas.drawText(currentMonth, x, mTitleHeight, mTextPaint);
    }

    private void drawWeek(Canvas canvas) {
        mTextPaint.setTextSize(mWeekSize);

        mTextPaint.setColor(Color.parseColor("#BBBBBB"));

        for (int i = 0; i < 7; i++) {
            int len = (int) mTextPaint.measureText(weeks[i]);
            int currentX = (int) (i * perWidth + (perWidth - len) / 2);
            canvas.drawText(weeks[i], currentX, mTitleHeight
                    + mWeekPaddingTop + mWeekHeight, mTextPaint);
//            currentX += perWidth;
        }

        canvas.drawLine(0, mTitleHeight
                        + mWeekPaddingTop + mWeekHeight + dp2px(10), getMeasuredWidth(),
                mTitleHeight + mWeekPaddingTop + mWeekHeight + dp2px(10), mBgPaint);
    }

    private void drawDate(Canvas canvas) {
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(mDateSize);

        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(1);

        float rowHeight = mDayHeight + dp2px(30);
        int passDays = 0;
        for (int row = 0; row < rowCount; row++) {
            if (row == 0) {
                // 第一行
                drawDay(canvas, 7, firstIndex, row, rowHeight, passDays);
                passDays += (7 - firstIndex);
                if (firstIndex > 0) {
                    for (int i = 0; i < firstIndex; i++) {
                        mTextPaint.setColor(Color.argb(50,
                                95, 95, 95));
                        String day = String.valueOf(lastMonthDays - firstIndex + i + 1);
                        float len = mTextPaint.measureText(day);
                        float curX = i * perWidth + (perWidth - len) / 2;
                        float curY = mTitleHeight + mWeekPaddingTop + mWeekHeight + (row + 1) * rowHeight;
                        canvas.drawText(day, curX, curY, mTextPaint);
                    }
                    mTextPaint.setColor(Color.BLACK);
                }
            } else if (row == rowCount - 1) {
                // 最后一行
                if (remains > 0) {
                    drawDay(canvas, remains, 0, row, rowHeight, dayOfMonth - remains);
                    for (int i = remains, j = 1; i < 7; i++, j++) {
                        mTextPaint.setColor(Color.argb(50,
                                95, 95, 95));
                        String day = String.valueOf(j);
                        float len = mTextPaint.measureText(day);
                        float curX = i * perWidth + (perWidth - len) / 2;
                        float curY = mTitleHeight + mWeekPaddingTop + mWeekHeight + (row + 1) * rowHeight;
                        canvas.drawText(day, curX, curY, mTextPaint);
                    }
                    mTextPaint.setColor(Color.BLACK);
                } else {
                    // remains = 0的情况就是刚好一周
                    drawDay(canvas, 7, 0, row, rowHeight, dayOfMonth - 7);
                }
            } else {
                drawDay(canvas, 7, 0, row, rowHeight, passDays);
                passDays += 7;
            }
        }

    }

    private void drawDay(Canvas canvas, int endIndex, int startIndex, int rowCount, float rowHeight, int passDay) {
        int date = passDay + 1;
        float originTop = mTitleHeight + mWeekPaddingTop + mWeekHeight;
        for (int column = startIndex; column < endIndex; column++) {
            float len = mTextPaint.measureText(String.valueOf(date));
            float currentX = column * perWidth + (perWidth - len) / 2;

            if (isCurMonth) {
                if (date == this.currentDate) {
                    mBgPaint.setColor(getResources().getColor(R.color.colorAccent));
                    float cx = column * perWidth + (float) perWidth / 2;
                    float cy = originTop + rowHeight * (rowCount + 1) - 12;
                    mBgPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(cx, cy, 40, mBgPaint);
                    mTextPaint.setColor(mSelectedColor);
                }
            }

            if (selectedDates.contains(date)) {
                mPaint.setColor(getResources().getColor(R.color.colorAccent));
                float cx = column * perWidth + (float) perWidth / 2;
                float cy = originTop + rowHeight * (rowCount + 1) - 12;
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setShadowLayer(10, 0, 5, mSelectedColor);
                canvas.drawCircle(cx, cy, 40, mPaint);
                mTextPaint.setColor(mSelectedColorText);
                mPaint.setShadowLayer(0, 0, 0, Color.GRAY);
            } else {
                // 不包含
                if (date < currentDate || !isCurMonth) {
                    mPaint.setColor(Color.argb(100, 228, 0, 0));
                    float cx = column * perWidth + (float) perWidth / 2;
                    float cy = originTop + rowHeight * (rowCount + 1) - 12;
                    mPaint.setStyle(Paint.Style.FILL);
                    mBorderPaint.setStyle(Paint.Style.STROKE);
                    mBorderPaint.setColor(Color.GRAY);
                    mPaint.setShadowLayer(10, 0, 5, Color.GRAY);
                    canvas.drawCircle(cx, cy, 40, mPaint);
                    canvas.drawCircle(cx, cy, 40, mBorderPaint);
                    mTextPaint.setColor(mSelectedColorText);
                    mPaint.setShadowLayer(0, 0, 0, Color.GRAY);
                }
            }

            canvas.drawText(String.valueOf(date), currentX, originTop + (rowCount + 1) * rowHeight, mTextPaint);
            date++;
            mBgPaint.setStyle(Paint.Style.FILL);
            mBgPaint.setColor(Color.parseColor("#DDDDDD"));
            mTextPaint.setColor(Color.BLACK);
            mPaint.setShader(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x;
        float y;
        if (remains > 0 || firstIndex > 0) {
            // 只有满足这两个条件才进行点击事件传递
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = event.getX();
                    y = event.getY();
                    Log.e(TAG, "x = " + x);
                    Log.e(TAG, "y = " + y);
                    if (firstIndex > 0) {
                        // 如果第一行有空隙
                        float judgeYTop = mTitleHeight + mWeekPaddingTop + mWeekHeight + dp2px(30) + 20;
                        float judgeYBottom = mTitleHeight + mWeekPaddingTop + mWeekHeight + mDayHeight + dp2px(30) + 40;
                        float judgeXEnd = firstIndex * perWidth;
                        float judgeXStart = 0;
//                        Log.e(TAG, "judgeYTop = " + judgeYTop);
//                        Log.e(TAG, "judgeYBottom = " + judgeYBottom);
//                        Log.e(TAG, "judgeXStart = " + judgeXStart);
//                        Log.e(TAG, "judgeXEnd = " + judgeXEnd);
                        if (x < judgeXEnd && x > judgeXStart && y > judgeYTop && y < judgeYBottom) {
                            if (mListener != null) {
                                mListener.onChangeMonth(TYPE.BEFORE);
                            }
                        }
                    }
                    if (remains > 0) {
                        // 最后一行有空隙
                        float judgeYTop = mTitleHeight + mWeekPaddingTop + mWeekHeight + mDayHeight * (rowCount + 1)
                                + dp2px(30) + 40 * (rowCount + 1) + 20;
                        float judgeYBottom = mTitleHeight + mWeekPaddingTop + mWeekHeight + mDayHeight * (rowCount + 2)
                                + dp2px(30) + (rowCount + 2) * 40 + 40;
                        float judgeXEnd = getWidth();
                        float judgeXStart = remains * perWidth;
//                        Log.e(TAG, "judgeYTop = " + judgeYTop);
//                        Log.e(TAG, "judgeYBottom = " + judgeYBottom);
//                        Log.e(TAG, "judgeXStart = " + judgeXStart);
//                        Log.e(TAG, "judgeXEnd = " + judgeXEnd);
                        if (x < judgeXEnd && x > judgeXStart && y > judgeYTop && y < judgeYBottom) {
                            if (mListener != null) {
                                mListener.onChangeMonth(TYPE.AFTER);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }


        return true;
    }

    /**
     * 改变月份
     * 以日期为参数 mDate
     */
    public void changeMonth(Date mDate) {
        setMonth(mDate);
        invalidate();
    }

    /**
     * 改变月份
     * 以数字为参数 +1 -1 change
     */
    public void changeMonth(int change) {
        Log.e(TAG, "change = " + change);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MONTH, change);
        setMonth(c.getTime());
//        invalidate();
        postInvalidate();
    }

    @Override
    public void invalidate() {
        requestLayout();
        super.invalidate();
    }

    private float dp2px(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return (dp * density) + 0.5f;
    }

    public interface OnChangeMonthListener {
        void onChangeMonth(TYPE type);
    }
}
