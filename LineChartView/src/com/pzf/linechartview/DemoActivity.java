package com.pzf.linechartview;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Timer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.pzf.linechartview.LineChartView.Mstyle;

/**
 * 动态折线图
 * 
 * @author pangzf
 * 
 */

public class DemoActivity extends Activity {
    private static final int UPDATE_VIEW = 0;

    LinkedHashMap<Double, Double> mDataMap;
    LineChartView mCharView;
    Button addPoint;
    Timer mTimer = new Timer();

    String[] strTimes = new String[] { "0", "1", "2", "3", "4" };
    private long mDelay = 2000;// 延迟ms
    private Thread mThread;

    Handler mHander = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_VIEW:
                    mCharView.postInvalidate();// 更新view需要手动刷新
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        addPoint = (Button) findViewById(R.id.bt_add);
        mCharView = (LineChartView) findViewById(R.id.charview);
        mCharView.SetInitView(mDataMap, 100, 10, "s", "m/s", false);// 设置基本数据
        mDataMap = new LinkedHashMap<Double, Double>();
        mDataMap.put(0.0, 25.0);// 设置基本数据
        mDataMap.put(20.0, 32.0);

        // mCharView.setYTotalvalue(100);//设置y轴最大值,可以手动改写
        // mCharView.setYScaleValue(10);//设置y轴的间距

        mCharView.setMap(mDataMap);// 设置点的数据
        mCharView.setMargint(20);// 设置偏移量
        mCharView.setMarginb(100);
        mCharView.setMstyle(Mstyle.Line);// 设置线的样式，line折线，Curve曲线
        mCharView.setStrTimes(strTimes, mDelay);// 设置x轴时间的文字，和延迟

        mCharView.setGridLineColor(Color.BLUE);// 设置网格颜色
        mCharView.setGridLineSize(4);// 设置网格线的粗细

        mCharView.setLineSize(4);// 这是折线粗细
        mCharView.setLineColor(Color.RED);// 设置折线颜色

        mCharView.setTimeTextColor(Color.RED);// 设置x轴文字颜色
        mCharView.setTimeTextSize(20);// 设置x轴文字大小

        mCharView.setYTextColor(Color.RED);// 设置y轴文字颜色
        mCharView.setYTextSize(20);// 设置y轴文字大小

        // mCharView.setMapHalve(44);//这里可以设置当多少个点之后。减少map的个数，防止由于点过多，造成重叠问题

        addPoint.setOnClickListener(new click());
    }

    class click implements OnClickListener {
        @Override
        public void onClick(View v) {
            mThread = new Thread() {
                public void run() {
                    Random radom = new Random();

                    for (int i = 20; i < 123; i++) {
                        SystemClock.sleep(mDelay);
                        int nextInt = radom.nextInt(4) * 4;
                        if (nextInt < 0) {
                            nextInt = 0;
                        }
                        mDataMap.put((double) i, (double) nextInt);
                        mHander.sendEmptyMessage(UPDATE_VIEW);

                    }
                };
            };
            mThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

}
