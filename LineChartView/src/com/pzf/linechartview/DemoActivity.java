package com.pzf.linechartview;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ConcurrentSkipListMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.pzf.linechartview.LineChartView.Mstyle;

/**
 * 动态折线图
 * 
 * @author pangzf
 * 
 */
@SuppressLint("NewApi")
public class DemoActivity extends Activity {
	private static final int UPDATE_VIEW = 0;
	ConcurrentSkipListMap<Double, Double> mDataMap;
	LineChartView mChartView;
	Button addPoint;
	Timer mTimer = new Timer();

	String[] strTimes = new String[] { "1", "", "", "", "2" };
	private long mDelay = 1000;// 延迟ms
	private Thread mThread;

	Handler mHander = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATE_VIEW:
				mChartView.postInvalidate();// 更新view需要手动刷新
				break;
			default:
				break;
			}
		}

	};
	private Button mEndTime;
	private Button mBtnCutWindow;
	private RelativeLayout mRlMainView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);

		addPoint = (Button) findViewById(R.id.bt_add);
		mEndTime = (Button) findViewById(R.id.btn_endtime);
		mBtnCutWindow = (Button) findViewById(R.id.btn_cut_window);
		mRlMainView = (RelativeLayout) findViewById(R.id.rela);
		mChartView = (LineChartView) findViewById(R.id.charview);
		mChartView.SetInitView(mDataMap, 100, 20, "km/h", "", false,
				getResources().getDrawable(R.drawable.chartview_pop));// 设置基本数据
		mDataMap = new ConcurrentSkipListMap<Double, Double>();
		mDataMap.put(0.0, 0.0);// 设置基本数据

		// mCharView.setYTotalvalue(100);//设置y轴最大值,可以手动改写
		// mCharView.setYScaleValue(10);//设置y轴的间距

		mChartView.setMap(mDataMap);// 设置点的数据
		mChartView.setMargint(20);// 设置偏移量
		mChartView.setMarginb(100);
		mChartView.setMstyle(Mstyle.Line);// 设置线的样式，line折线，Curve曲线
		mChartView.setStartTime(System.currentTimeMillis(), mDelay);
		// mCharView.setGridLineColor(Color.BLUE);// 设置网格颜色
		// mCharView.setGridLineSize(4);// 设置网格线的粗细

		// mCharView.setLineSize(4);// 这是折线粗细
		// mCharView.setLineColor(Color.RED);// 设置折线颜色

		// mCharView.setTimeTextColor(Color.RED);// 设置x轴文字颜色
		// mCharView.setTimeTextSize(20);// 设置x轴文字大小

		// mCharView.setYTextColor(Color.RED);// 设置y轴文字颜色
		// mCharView.setYTextSize(20);// 设置y轴文字大小

		// mCharView.setMapHalve(12);// 这里可以设置当多少个点之后。减少map的个数，防止由于点过多，造成重叠问题

		addPoint.setOnClickListener(new click());
		mEndTime.setOnClickListener(new click());
		mBtnCutWindow.setOnClickListener(new click());
	}

	class click implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.bt_add:
				if (mThread == null) {
					mThread = new Thread() {
						public void run() {
							Random radom = new Random();

							for (int i = 20; i < 400; i++) {
								SystemClock.sleep(mDelay);
								int nextInt = radom.nextInt(20) * 4;
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

				break;
			case R.id.btn_endtime:
				String endTime = mChartView.getEndTime();
				Toast.makeText(getApplicationContext(), endTime, 0).show();
				break;
			case R.id.btn_cut_window://截屏
				mRlMainView.setDrawingCacheEnabled(true);
				Bitmap bitMap = mRlMainView.getDrawingCache();
				if(bitMap!=null){
					saveBitToSdCard(bitMap);
				}
				mRlMainView.setDrawingCacheEnabled(false);
				break;

			default:
				break;
			}

		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	/**
	 * 保存图片到sdcard
	 * @param bitMap
	 */
	public void saveBitToSdCard(Bitmap bitMap) {
		File file=new File(Environment.getExternalStorageDirectory()+"/baidu/"+System.currentTimeMillis()+".jpg");
		try {
			BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(file));
			bitMap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
	}

}
