package com.pzf.linechartview;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 动态折线图
 * 
 * @author pangzf
 * 
 */
@SuppressLint("NewApi")
class LineChartView extends LinearLayout {
    Context mContext;
    /**
     * 折线图峰值的小红方块
     */
    public static final int RECT_SIZE = 10;
    protected static final int END_TIME = 0;
    /**
     * 选择的点
     */
    private Point mSelectedPoint;
    /**
     * 延迟多久画一次点
     */
    private long mTimeDelay = 0;
    /**
     * Line:折线 Curve:曲线
     */
    private Mstyle mStyle = Mstyle.Line;
    /**
     * 画布点集合
     */
    private Point[] mPoints = new Point[100];
    /**
     * 数据点集合，存放点的x,y坐标
     */
    ConcurrentSkipListMap<Double, Double> mDataMap;
    /**
     * 网格
     */
    ArrayList<Double> mGrid;
    /**
     * x轴，时间点对应的文字
     */
    // String[] mStrTimes;
    /**
     * 是否显示网格
     */
    Boolean isylineshow;

    private int mMaxValueIndex;// 最大值
    private TextView mTvHeightValue;// textview
    private ImageView mIvPoint;// imageview
    private String[] mStrTimes;

    int mBheight = 0;// 高度，距离x轴的距离
    int mMarginb = 40;// 距离x轴的间距

    int mTotalValue = 30;// y周总刻度
    int mYScale = 5;// y间距
    String mXstr, mYstr = "";// 横纵坐标的属性
    int mMargin = 50;// 偏移量

    int mBackGroundColor = 0;// 背景颜色
    int mBackGroundResourceId = 0;// 设置背景资源

    int mWidthMarginLef = 33;// x线距离左边的距离
    int mWidthMarginRight = 15;// x线距离右边的距离
    int mScaleYTextToLine = 25;// y轴文字到线的距离
    int mGridLineColor = R.color.xytextcolor;// 这是网格线的颜色
    int mGridLineSize = 1;// 网格线的粗细

    int mLineColor = getResources().getColor(R.color.line_blue_color);// 设置折线的颜色
    int mLineSize = 2;// 折纸折线的大小

    int mTimeTextColor = R.color.xytextcolor;// x轴时间颜色值
    int mTimeTextSize = 12;// x轴文字的大小

    int mYTextSize = 12;// y轴文字大小
    int mYTextColor = R.color.line_grid_line;// y轴文字的颜色

    int mMaxIndex = 0;
    /**
     * x轴文字距离x轴的距离
     */
    int mXTextMarginX = 20;
    /**
     * x轴文字最后一个距离右边的距离
     */
    int mXTextMarginRight = 60;

    long mStartTime;// 开始的时间
    long mEndTime;// 停止的时间
    int mDelayCount;// 延迟数
    int mValueTextSize = 6;// 大小sp

    Drawable mTvBackground;// TextView背景图

    // 枚举实现坐标桌面的样式风格
    public static enum Mstyle {
        Line, Curve
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case END_TIME:
                    updateTimes();
                    break;

                default:
                    break;
            }
        }

    };
    private SimpleDateFormat mForMat;
    private ArrayList<Double> mDataMapValueList;
    private ScheduledExecutorService mSheduled;

    /**
     * @param map
     *            需要的数据，虽然key是double，但是只用于排序和显示，与横向距离无关
     * @param totalvalue
     *            Y轴的最大值
     * @param pjvalue
     *            Y平均值
     * @param xstr
     *            X轴的单位
     * @param ystr
     *            Y轴的单位
     * @param isylineshow
     *            是否显示纵向网格
     * @return
     */
    public void SetInitView(ConcurrentSkipListMap<Double, Double> map,
            int totalvalue, int pjvalue, String xstr, String ystr,
            Boolean isylineshow, Drawable drawable) {
        this.mDataMap = map;
        this.mTotalValue = totalvalue;
        this.mYScale = pjvalue;
        this.mXstr = xstr;
        this.mYstr = ystr;
        this.isylineshow = isylineshow;
        this.mTvBackground = drawable;
        initAddView();// 添加view
        // 屏幕横向
        // act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * 添加的view
     */
    private void initAddView() {
        mForMat = new SimpleDateFormat("HH:mm:ss ");
        mTvHeightValue = new TextView(mContext);
        mTvHeightValue.setBackgroundDrawable(mTvBackground);
        mTvHeightValue.setTextSize(dip2px(mContext, mValueTextSize));
        mTvHeightValue.setTextColor(Color.WHITE);
        mTvHeightValue.setGravity(Gravity.CENTER_HORIZONTAL);
        mTvHeightValue.setVisibility(View.GONE);

        mIvPoint = new ImageView(mContext);
        mIvPoint.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.chartview_point));
        mIvPoint.setVisibility(View.GONE);
        this.addView(mIvPoint);
        this.addView(mTvHeightValue);
    }

    /**
     * 更新时间
     */
    protected void updateTimes() {
        mStrTimes = new String[2];
        mStrTimes[0] = getTimes(mStartTime);
        mStrTimes[1] = getTimes(mEndTime);

    }

    /**
     * 获取当前时间
     * 
     * @param startTime
     * @return
     */
    private String getTimes(long time) {
        Date curDate = new Date(time);// 获取当前时间
        return mForMat.format(curDate);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBackGroundColor != 0)
            this.setBackgroundColor(mBackGroundColor);
        if (mBackGroundResourceId != 0)
            this.setBackgroundResource(mBackGroundResourceId);
        mGrid = getInitFromMap(mDataMap);
        int height = getHeight();
        if (mBheight == 0) {
            mBheight = height - mMarginb;
        }

        int width = getWidth();
        int marinLeftSize = dip2px(mContext, mWidthMarginLef);
        int marginRightSize = dip2px(mContext, mWidthMarginRight);
        int ySpaceSize = mTotalValue / mYScale;// 界面布局的尺寸的比例

        // set up paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mGridLineColor); // 横线
        paint.setStrokeWidth(mGridLineSize);
        paint.setStyle(Style.STROKE);
        for (int i = 0; i < ySpaceSize + 1; i++) {
            // if(i==pjsize)//将顶点的线变为红色的 警戒线
            // paint.setColor(Color.RED);//最大值线

            // if(i==0){
            // canvas.drawLine(blwidh,bheight-(bheight/pjsize)*i+margint,width,bheight-(bheight/pjsize)*i+margint,
            // paint);//Y坐标
            // //画y轴直线
            // canvas.drawLine(blwidh+(width-blwidh)/dlk.size()*i,margint,blwidh+(width-blwidh)/dlk.size()*i,bheight+margint,
            // paint);
            // }
            // 画x轴直线
            canvas.drawLine(marinLeftSize, mBheight - (mBheight / ySpaceSize)
                    * i + mMargin, width - marginRightSize, mBheight
                    - (mBheight / ySpaceSize) * i + mMargin, paint);
            // Y坐标
            drawline(mYScale * i + mYstr, marinLeftSize / 2, mBheight
                    - (mBheight / ySpaceSize) * i + mMargin, canvas);

        }
        ArrayList<Integer> xlist = new ArrayList<Integer>();// 记录每个x的值
        // 画直线（纵向）
        paint.setColor(mGridLineColor);
        if (mGrid == null)
            return;
        for (int i = 0; i < mGrid.size(); i++) {
            xlist.add(marinLeftSize + (width - marinLeftSize) / mGrid.size()
                    * i);// 修改widh可以设置x轴顶头位置//修改x轴的宽度，让它实时缩小
            if (isylineshow) {
                canvas.drawLine(
                        marinLeftSize + (width - marinLeftSize) / mGrid.size()
                                * i, mMargin, marinLeftSize
                                + (width - marinLeftSize) / mGrid.size() * i,
                        mBheight + mMargin, paint);
            }

        }

        // 点的操作设置
        mPoints = getpoints(mGrid, mDataMap, xlist, mTotalValue, mBheight);
        mMaxValueIndex = getMaxPoint(mPoints, canvas);

        paint.setColor(mLineColor); // 设置折线的颜色
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(mLineSize);

        if (mStyle == Mstyle.Curve)
            drawscrollline(mPoints, canvas, paint);
        else
            drawline(mPoints, canvas, paint);

        // 写时间值
        int tempscale = (this.getWidth() - marinLeftSize) / 2;
        int scale = (tempscale + tempscale / 2) - 4;
        paint.setColor(mTimeTextColor);
        paint.setStyle(Style.FILL);
        paint.setTextSize(dip2px(mContext, mTimeTextSize));
        if (mStrTimes != null && mStrTimes.length > 0) {

            canvas.drawText(mStrTimes[0], scale * 0 + marinLeftSize, mBheight
                    + dip2px(mContext, mXTextMarginX), paint);
            // 最右边
            int cWidth = canvas.getWidth();
            canvas.drawText(mStrTimes[1],
                    cWidth - dip2px(mContext, mXTextMarginRight), mBheight
                            + dip2px(mContext, 20), paint);

        }

        int maxIndex = getMapHalve();
        if (maxIndex > 0) {
            optionMapHalve();
        }

    }

    private int getMaxPoint(Point[] point, Canvas canvas) {
        int min = 0;
        int index = 0;
        int max = canvas.getHeight();
        for (int i = 0; i < point.length - 1; i++) {
            if (canvas.getHeight() - point[i].y > canvas.getHeight() - max) {
                max = point[i].y;
                index = i;
            }

        }
        return index;
    }

    public void setStartTime(long startTime, final long delay) {
        this.mStartTime = startTime;
        mSheduled = Executors.newScheduledThreadPool(1);
        mSheduled.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                mDelayCount++;
                mEndTime = mStartTime + delay * mDelayCount;
                mHandler.sendMessage(mHandler.obtainMessage(END_TIME));
            }
        }, delay / 1000, delay / 1000, TimeUnit.SECONDS);
    }

    public void stopTime() {
        if (mSheduled != null && !mSheduled.isShutdown()) {
            mSheduled.shutdown();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < mPoints.length; i++) {
                    if (pointToRect(mPoints[i]).contains(event.getX(),
                            event.getY())) {
                        System.out.println("-yes-" + i);
                        mSelectedPoint = mPoints[i];
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (null != mSelectedPoint) {
                    // mSelectedPoint.x = (int) event.getX();
                    mSelectedPoint.y = (int) event.getY();
                    // invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mSelectedPoint = null;
                break;
            default:
                break;
        }
        return true;

    }

    /**
     * 折线峰值画矩形
     * 
     * @param p
     * @return
     */
    private RectF pointToRect(Point p) {
        return new RectF(p.x - RECT_SIZE / 2, p.y - RECT_SIZE / 2, p.x
                + RECT_SIZE / 2, p.y + RECT_SIZE / 2);
    }

    /**
     * 折线峰值画圆形
     * 
     * @param p
     * @return
     */
    private RectF pointToCircle(Point p) {
        return new RectF(p.x - RECT_SIZE / 2, p.y - RECT_SIZE / 2, p.x
                + RECT_SIZE / 2, p.y + RECT_SIZE / 2);
    }

    /**
     * 暂时无用，此处是可以画曲线，如使用需要将Mstyle设置成Curve
     * 
     * @param ps
     * @param canvas
     * @param paint
     */
    private void drawscrollline(Point[] ps, Canvas canvas, Paint paint) {
        Point startp = new Point();
        Point endp = new Point();
        for (int i = 0; i < ps.length - 1; i++) {
            startp = ps[i];
            endp = ps[i + 1];
            int wt = (startp.x + endp.x) / 2;
            Point p3 = new Point();
            Point p4 = new Point();
            p3.y = startp.y;
            p3.x = wt;
            p4.y = endp.y;
            p4.x = wt;

            Path path = new Path();
            path.moveTo(startp.x, startp.y);
            path.cubicTo(p3.x, p3.y, p4.x, p4.y, endp.x, endp.y);
            canvas.drawPath(path, paint);

        }
    }

    /**
     * 画点集合的折线
     * 
     * @param ps
     * @param canvas
     * @param paint
     */
    @SuppressLint("NewApi")
    private void drawline(Point[] ps, Canvas canvas, Paint paint) {
        Point startp = new Point();
        Point endp = new Point();
        for (int i = 0; i < ps.length - 1; i++) {
            startp = ps[i];
            endp = ps[i + 1];

            // 画圆点
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(mLineColor);
            paint.setStrokeWidth(dip2px(mContext, mLineSize));
            if (mMaxValueIndex != 0 && i == mMaxValueIndex) {

                Object value = mDataMap.values().toArray()[i];

                if (endp.y > startp.y) {
                    canvas.drawLine(startp.x, startp.y, endp.x, endp.y, paint);
                    // canvas.drawCircle(startp.x, startp.y, radius, paint);
                    // 提示pop
                    mTvHeightValue.setVisibility(View.VISIBLE);
                    mTvHeightValue.setText(value + mXstr);
                    mTvHeightValue.setX(startp.x - mTvHeightValue.getWidth()
                            / 2);
                    mTvHeightValue.setY(startp.y - mTvHeightValue.getHeight()
                            - dip2px(mContext, 5));

                    // 提示圆点
                    mIvPoint.setVisibility(View.VISIBLE);
                    mIvPoint.setX(startp.x - mIvPoint.getWidth() / 2);
                    mIvPoint.setY(startp.y - mIvPoint.getHeight() / 2);
                } else {
                    canvas.drawLine(startp.x, startp.y, endp.x, endp.y, paint);
                }

            } else {
                canvas.drawLine(startp.x, startp.y, endp.x, endp.y, paint);
            }

        }

    }

    /**
     * 点集合
     * 
     * @param dlk
     * @param map
     * @param xlist
     * @param max
     * @param h
     * @return
     */
    private Point[] getpoints(ArrayList<Double> dlk,
            ConcurrentSkipListMap<Double, Double> map,
            ArrayList<Integer> xlist, int max, int h) {
        Point[] points = new Point[dlk.size()];
        for (int i = 0; i < dlk.size(); i++) {
            int ph = h - (int) (h * (map.get(dlk.get(i)) / max));
            // if(i==0){
            // points[i]=new Point(xlist.get(i),ph+margint);
            // }else{
            // points[i]=new Point(xlist.get(i),ph+margint);
            // }
            // if(i==0){
            // points[i]=new Point(xlist.get(i)+100,ph+margint);
            // }else {
            // points[i]=new Point(xlist.get(i)-100,ph+margint);
            // }

            points[i] = new Point(xlist.get(i), ph + mMargin);
        }
        return points;
    }

    /**
     * 画y线重的文字
     * 
     * @param text
     * @param x
     * @param y
     * @param canvas
     */
    private void drawline(String text, int x, int y, Canvas canvas) {
        Paint p = new Paint();
        p.setTextSize(dip2px(mContext, mYTextSize));
        p.setColor(mYTextColor);
        canvas.drawText(text, x, y, p);
    }

    // private void drawline(String text, int x, int y, Canvas canvas) {
    // Paint p = new Paint();
    // p.setAlpha(0x0000ff);
    // p.setTextSize(20);
    // String familyName = "宋体";
    // Typeface font = Typeface.create(familyName, Typeface.ITALIC);
    // p.setTypeface(font);
    // p.setTextAlign(Paint.Align.CENTER);
    // canvas.drawText(text, x, y, p);
    // }

    /**
     * 画x轴线的值和圆点
     * 
     * @param text
     * @param x
     * @param y
     * @param canvas
     * @param cirX
     *            圆的x
     * @param cirY
     *            圆的y
     */
    private void drawLineAndCircle(String text, int x, int y, Canvas canvas,
            int cirX, int cirY, int cRadius, int redColor) {
        Paint p = new Paint();
        p.setAlpha(0x0000ff);
        p.setTextSize(10);
        String familyName = "宋体";
        Typeface font = Typeface.create(familyName, Typeface.ITALIC);
        p.setTypeface(font);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y, p);
        p.setColor(redColor);
        canvas.drawCircle(cirX, cirY, cRadius, p);
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 画x,y轴之前的准备
     * 
     * @param map
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ArrayList<Double> getInitFromMap(
            ConcurrentSkipListMap<Double, Double> map) {
        ArrayList<Double> dlk = new ArrayList<Double>();
        mDataMapValueList = new ArrayList<Double>();
        int position = 0;
        if (map == null) {
            return null;
        }

        Set set = map.entrySet();
        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {
            @SuppressWarnings("rawtypes")
            Map.Entry mapentry = (Map.Entry) iterator.next();
            dlk.add((Double) mapentry.getKey());
            mDataMapValueList.add((Double) mapentry.getValue());

        }

        for (int i = 0; i < dlk.size(); i++) {
            int j = i + 1;
            position = i;
            Double temp = dlk.get(i);
            for (; j < dlk.size(); j++) {
                if (dlk.get(j) < temp) {
                    temp = dlk.get(j);
                    position = j;
                }
            }

            dlk.set(position, dlk.get(i));
            dlk.set(i, temp);
        }
        return dlk;

    }

    public void setMapHalve(int index) {
        this.mMaxIndex = index;
    }

    public int getMapHalve() {
        return this.mMaxIndex;
    }

    /**
     * 防止点过多，设置map到size值之后，总点数减半
     * 
     * @param index
     *            临界点
     */
    protected void optionMapHalve() {
        int index = this.mMaxIndex;
        if (null != mDataMap && mDataMap.size() % index == 0) {
            List<Double> list = new ArrayList<Double>();
            LinkedHashMap<Double, Double> mLinkedHashMap = new LinkedHashMap<Double, Double>();
            Set<Double> keySet = mDataMap.keySet();
            Iterator<Double> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                Double next = iterator.next();
                list.add(next);
            }

            // 获得不需要删除
            List<Double> mList = new ArrayList<Double>();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (i % 2 == 0) {
                        mList.add(list.get(i));
                    }
                }
            }
            // 获取不需要删除的值放到新的列表中
            for (int i = 0; i < mList.size(); i++) {
                Double dKey = mList.get(i);
                Double dValue = mDataMap.get(dKey);
                mLinkedHashMap.put(dKey, dValue);
            }

            if (mLinkedHashMap != null && mLinkedHashMap.size() > 0) {
                mDataMap.clear();
                mDataMap.putAll(mLinkedHashMap);
            }
            mTvHeightValue.setVisibility(View.GONE);
            mIvPoint.setVisibility(View.GONE);

        }

    }

    public ConcurrentSkipListMap<Double, Double> getMap() {
        return mDataMap;
    }

    /**
     * 设置数据
     * 
     * @param map
     */
    public void setMap(ConcurrentSkipListMap<Double, Double> map) {
        this.mDataMap = map;
    }

    public int getYTotalvalue() {
        return mTotalValue;
    }

    /**
     * 设置纵向最大值
     * 
     * @param totalvalue
     */
    public void setYTotalvalue(int totalvalue) {
        this.mTotalValue = totalvalue;
    }

    public int getYScaleValue() {
        return mYScale;
    }

    /**
     * 设置纵向间距
     * 
     * @return
     */
    public void setYScaleValue(int pjvalue) {
        this.mYScale = pjvalue;
    }

    public String getXstr() {
        return mXstr;
    }

    public void setXstr(String xstr) {
        this.mXstr = xstr;
    }

    public String getYstr() {
        return mYstr;
    }

    public void setYstr(String ystr) {
        this.mYstr = ystr;
    }

    public int getMargint() {
        return mMargin;
    }

    /**
     * 设置横向坐标轴，文字与轴的距离
     * 
     * @param margint
     */
    public void setMargint(int margint) {
        this.mMargin = margint;
    }

    public Boolean getIsylineshow() {
        return isylineshow;
    }

    public void setIsylineshow(Boolean isylineshow) {
        this.isylineshow = isylineshow;
    }

    public int getMarginb() {
        return mMarginb;
    }

    public void setMarginb(int marginb) {
        this.mMarginb = marginb;
    }

    public Mstyle getMstyle() {
        return mStyle;
    }

    /**
     * 设置样式
     * 
     * @param mstyle
     */
    public void setMstyle(Mstyle mstyle) {
        this.mStyle = mstyle;
    }

    public int getBheight() {
        return mBheight;
    }

    public void setBheight(int bheight) {
        this.mBheight = bheight;
    }

    public int getBackGroundColor() {
        return mBackGroundColor;
    }

    /**
     * 设置背景根据颜色
     * 
     * @param c
     */
    public void setBackGroundColor(int c) {
        this.mBackGroundColor = c;
    }

    public int getBackGroundResourceId() {
        return mBackGroundResourceId;
    }

    /**
     * 设置背景，使用资源分拣
     * 
     * @param resid
     */
    public void setBackGroundResourceId(int resid) {
        this.mBackGroundResourceId = resid;
    }

    /**
     * 设置网格线的颜色
     * 
     * @return
     */
    public void setGridLineColor(int mGridLineColor) {
        this.mGridLineColor = mGridLineColor;
    }

    /**
     * 设置网格线的粗细
     * 
     * @return
     */
    public int getGridLineSize() {
        return mGridLineSize;
    }

    public void setGridLineSize(int mGridLineSize) {
        this.mGridLineSize = mGridLineSize;
    }

    public int getLineSize() {
        return mLineSize;
    }

    public void setLineSize(int mLineSize) {
        this.mLineSize = mLineSize;
    }

    public int getLineColor() {
        return mLineColor;
    }

    /**
     * 设置折线的颜色
     * 
     * @param mLineColor
     */
    public void setLineColor(int mLineColor) {
        this.mLineColor = mLineColor;
    }

    /**
     * 设置x轴，时间的文字颜色
     * 
     * @param mTimeTextColor
     */
    public void setTimeTextColor(int mTimeTextColor) {
        this.mTimeTextColor = mTimeTextColor;
    }

    /**
     * 设置x轴，时间的文字大小
     * 
     * @param mTimeTextSize
     */
    public void setTimeTextSize(int mTimeTextSize) {
        this.mTimeTextSize = mTimeTextSize;
    }

    public void setYTextSize(int mYTextSize) {
        this.mYTextSize = mYTextSize;
    }

    public void setYTextColor(int mYTextColor) {
        this.mYTextColor = mYTextColor;
    }

    public LineChartView(Context ct) {
        super(ct);
        this.mContext = ct;
    }

    public LineChartView(Context ct, AttributeSet attrs) {
        super(ct, attrs);
        this.mContext = ct;
    }

    /**
     * 返回最后的时间
     * 
     * @return
     */
    public String getEndTime() {
        if (mStrTimes != null && mStrTimes.length > 0) {
            return mStrTimes[1];
        }
        return "";
    }
}
