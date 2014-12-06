package jp.webtips.tabs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
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
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

	public void show(String message)
	{
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private static String convertHTML(String html)
	{
		// 現状はそのまま返却
		return html;
	}

	private void settingWebView(final WebView web, final String targetURL)
	{
		web.getSettings().setJavaScriptEnabled(true);
		final String ua = web.getSettings().getUserAgentString();

		web.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				web.setWebViewClient(new WebViewClient()
				{

					@Override
					public void onPageStarted(WebView view, String url, Bitmap favicon)
					{
						super.onPageStarted(view, url, favicon);
					}

					@Override
					public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl)
					{
						super.onReceivedError(view, errorCode, description, failingUrl);
						show(getResources().getString(R.string.error_networkd));
						finish();
					}

					@Override
					public void onPageFinished(WebView view, String url)
					{
						super.onPageFinished(view, url);
					}

					@Override
					public WebResourceResponse shouldInterceptRequest(WebView view, String url)
					{
						if (
							url.endsWith(".jpg") ||
							url.endsWith(".png") ||
							url.endsWith(".js") ||
							url.endsWith(".css") ||
							url.endsWith(".gif") ||
							url.endsWith(".xml") ||
							url.endsWith(".ico")
						) return null;

						DefaultHttpClient client = new DefaultHttpClient();

						try {
							HttpGet req = new HttpGet(url);
							req.setHeader("User-Agent", ua);
							HttpResponse response = client.execute(req);
							HttpEntity entity = response.getEntity();

							if (entity == null) {
								return null;
							}

							Header contentTypeHeader = entity.getContentType();
							if (contentTypeHeader == null) return null;

							String contentType = contentTypeHeader.getValue();
							if (contentType.startsWith("text/html") == false) return null;

							// Common.log(url);
							// Common.log("Type: " + contentType);
							String encoding = null;

							Pattern pattern = Pattern.compile(".*charset=([a-z0-9\\-]+)", Pattern.CASE_INSENSITIVE);
							Matcher matcher = pattern.matcher(contentType);
							if (matcher.matches() && matcher.groupCount() > 0) {
								encoding = matcher.group(1);
							}

							InputStream is = entity.getContent();

							ByteArrayBuffer arrayBuffer = new ByteArrayBuffer(8192000);
							byte[] buffer = new byte[1024];
							int len = 0;
							while ((len = is.read(buffer)) > 0) {
								arrayBuffer.append(buffer, 0, len);
							}

							return new WebResourceResponse(
								"text/html",
								encoding,
								new ByteArrayInputStream(convertHTML(new String(arrayBuffer.toByteArray())).getBytes())
							);
						} catch (IOException ex) {
							Common.log(ex.getMessage());
						}

						return null;
					}
				});

				web.loadUrl(targetURL);
			}
		});

		web.setOnTouchListener(new OnTouchListener()
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

		web.loadUrl("file:///android_asset/loading.html");
	}

	private void addContentView(Tab tab)
	{
		ViewGroup contentsView = (ViewGroup)findViewById(R.id.contents);

		LinearLayout contentView = (LinearLayout)getLayoutInflater().inflate(R.layout.webview, contentsView, false);
		contentView.setBackgroundColor(tab.color);
		WebView web = (WebView)contentView.findViewById(R.id.web01);
		settingWebView(web, tab.url);

		contents.add(contentView);
	}

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

	private synchronized void switchContent(int no)
	{
		int max = contents.size();

		if (no < 0) no = max - 1;
		if (no >= max) no = 0;

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
			int posx = 0;
			LinearLayout tabsView = (LinearLayout)findViewById(R.id.tabs);
			int size = tabsView.getChildCount();
			for (int i=0;i<size;i++) {
				View child = tabsView.getChildAt(i);
				TextView text = (TextView)child.findViewById(R.id.text);
				LayoutParams params = text.getLayoutParams();
				if (no == i) {
					params.height = (int)((float)tabHeight * 1.2f);
					posx += child.getWidth() / 2;
				} else {
					if (no > i) posx += child.getWidth();
					params.height = tabHeight;
				}
				text.setLayoutParams(params);
			}

			// 中心へ移動
			HorizontalScrollView scroll = (HorizontalScrollView)findViewById(R.id.tabScroll);
			int moveTo = posx - deviceWidth / 2;
			scroll.smoothScrollTo(moveTo, 0);
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
