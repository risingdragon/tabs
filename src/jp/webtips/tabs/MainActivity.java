package jp.webtips.tabs;

import java.util.ArrayList;
import java.util.Iterator;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity
{
	private ArrayList<Tab> tabs;
	private ArrayList<View> contents;
	private static float initX = 0.0f;
	private float fromX;
	private float currentX;
	private int deviceWidth;
	private int leftPoint;
	private int rightPoint;
	private int tabHeight = 0;
	final private static float[] tabradiis = { 10.0f, 10.0f, 10.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Point point = new Point();
		getWindowManager().getDefaultDisplay().getSize(point);

		deviceWidth = point.x;
		leftPoint = deviceWidth / 5;
		rightPoint = deviceWidth - leftPoint;

		tabs = new ArrayList<Tab>();
		tabs.add(Tab.create("C4 coin", "http://c4coin.jp/views/news", 0xffff0000));
		tabs.add(Tab.create("C4 Study", "http://c4study.jp/views/news", 0xff006600));
		tabs.add(Tab.create("Google", "https://www.google.co.jp/", 0xff0000ff));
		/*
		 * tabs.add(Tab.create("Yahoo", "http://www.yahoo.co.jp/", 0xff00ff00));
		 * tabs.add(Tab.create("ぐるなび", "http://www.gnavi.co.jp/", 0xff0000ff));
		 * tabs.add(Tab.create("Google", "https://www.google.co.jp/",
		 * 0xffff0000)); tabs.add(Tab.create("Yahoo", "http://www.yahoo.co.jp/",
		 * 0xff00ff00)); tabs.add(Tab.create("ぐるなび", "http://www.gnavi.co.jp/",
		 * 0xff0000ff)); tabs.add(Tab.create("Google",
		 * "https://www.google.co.jp/", 0xffff0000));
		 * tabs.add(Tab.create("Yahoo", "http://www.yahoo.co.jp/", 0xff00ff00));
		 * tabs.add(Tab.create("ぐるなび", "http://www.gnavi.co.jp/", 0xff0000ff));
		 * tabs.add(Tab.create("Google", "https://www.google.co.jp/",
		 * 0xffff0000)); tabs.add(Tab.create("Yahoo", "http://www.yahoo.co.jp/",
		 * 0xff00ff00)); tabs.add(Tab.create("ぐるなび", "http://www.gnavi.co.jp/",
		 * 0xff0000ff)); tabs.add(Tab.create("Google",
		 * "https://www.google.co.jp/", 0xffff0000));
		 * tabs.add(Tab.create("Yahoo", "http://www.yahoo.co.jp/", 0xff00ff00));
		 * tabs.add(Tab.create("ぐるなび", "http://www.gnavi.co.jp/", 0xff0000ff));
		 */

		contents = new ArrayList<View>();

		final LinearLayout tabsView = (LinearLayout)findViewById(R.id.tabs);

		Iterator<Tab> iterator = tabs.iterator();
		while (iterator.hasNext()) {
			Tab tab = iterator.next();
			final LinearLayout tabView = (LinearLayout)getLayoutInflater().inflate(R.layout.tab, tabsView, false);
			final TextView textview = (TextView)tabView.findViewById(R.id.text);

			GradientDrawable drawable = new GradientDrawable();
			drawable.setCornerRadii(tabradiis);
			drawable.setColor(tab.color);
			textview.setBackground(drawable);
			textview.setText(tab.name);
			tabsView.addView(tabView);
			tabView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					int no = tabsView.indexOfChild(v);
					switchContent(no);
				}
			});

			ViewTreeObserver obsever =  tabView.getViewTreeObserver();
			obsever.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{

				@Override
				public void onGlobalLayout()
				{
					if (tabHeight == 0) {
						tabHeight = textview.getHeight();
						switchContent(0);
					}
				}
			});

			addContentView(tab);
		}
	}

	private void addContentView(Tab tab)
	{
		ViewGroup contentsView = (ViewGroup)findViewById(R.id.contents);

		LinearLayout contentView = (LinearLayout)getLayoutInflater().inflate(R.layout.webview, contentsView, false);
		WebView webview = (WebView)contentView.findViewById(R.id.web01);
		webview.getSettings().setJavaScriptEnabled(true);
		contentView.setBackgroundColor(tab.color);
		webview.loadUrl(tab.url);
		webview.setOnTouchListener(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				final ViewGroup contentsView = (ViewGroup)findViewById(R.id.contents);
				final View contentView = (View)v.getParent();
				float x = event.getRawX();
				int centerX = (int)currentX + contentView.getWidth() / 2;

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						fromX = x;
						currentX = initX;
						break;
					case MotionEvent.ACTION_MOVE:
						currentX += x - fromX;

						if (contents.size() >=3) {
							boolean isLeft = centerX <= (deviceWidth / 2);
							// タブが3枚以上の場合はスワイプ方向によって表示切替
							contentsView.getChildAt(1).setVisibility(isLeft ? View.VISIBLE : View.INVISIBLE);
							contentsView.getChildAt(0).setVisibility(isLeft ? View.INVISIBLE : View.VISIBLE);
						}

						contentView.setX(currentX);
						fromX = x;

						break;
					case MotionEvent.ACTION_UP:
						if (contentsView.getChildCount() <= 1) {
							// 動かしていない場合
							return false;
						}
						if (centerX > rightPoint || centerX < leftPoint) {
							// 画面外に追いやる
							int move;
							if  (centerX > rightPoint) {
								// 右へスワイプ(前へ)
								move = deviceWidth;
							} else {
								// 左へへスワイプ(次へ)
								move = -contentView.getWidth();
							}

							final int switchTo = contents.indexOf(contentView) + (move < 0 ? 1 : -1);

							ObjectAnimator.ofFloat(contentView, "x", (float)move).setDuration(200).start();
							Handler handler = new Handler();
							handler.postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									switchContent(switchTo);
								}
							}, 200);
							return false;
						}

						// 元の位置に戻す
						ObjectAnimator.ofFloat(contentView, "x", initX).setDuration(500).start();
						break;
				}

				return true;
			}
		});

		contents.add(contentView);
	}

	private synchronized void switchContent(int no)
	{
		int max = contents.size();

		if (no < 0) no = max - 1;
		if (no >= max) no = 0;

		Common.log("switch:" + no);

		ViewGroup contentsView = (ViewGroup)findViewById(R.id.contents);

		// 最前面
		View _view = contents.get(no);
		_view.setVisibility(View.VISIBLE);
		_view.setX(0);
		int index = contentsView.indexOfChild(_view);
		if (index < 0) {
			// 表示中のViewに存在しない
			contentsView.removeAllViews();
			contentsView.addView(_view, 0);
		} else {
			Iterator<View> iterator = contents.iterator();
			while (iterator.hasNext()) {
				View _v = iterator.next();
				if (_view != _v) contentsView.removeView(_v);
			}
		}

		// 次画面
		_view = contents.get((no + 1) >= max ? 0 : (no + 1));
		if (_view != null) {
			_view.setX(0);
			_view.setVisibility(View.VISIBLE);
			contentsView.addView(_view, 0);
		}

		// 前画面
		_view = contents.get((no - 1) < 0 ? (max - 1) : (no - 1));
		if (_view != null) {
			_view.setX(0);
			_view.setVisibility(View.VISIBLE);
			contentsView.addView(_view, 0);
		}

		if (tabHeight > 0) {
			LinearLayout tabsView = (LinearLayout)findViewById(R.id.tabs);
			int size = tabsView.getChildCount();
			for (int i=0;i<size;i++) {
				View child = tabsView.getChildAt(i);
				TextView text = (TextView)child.findViewById(R.id.text);
				LayoutParams params = text.getLayoutParams();
				if (no == i) {
					params.height = (int)((float)tabHeight * 1.2f);
				} else {
					params.height = tabHeight;
				}
				text.setLayoutParams(params);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
