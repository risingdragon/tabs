package jp.webtips.tabs;

import android.util.Log;

public class Common
{
	public static void log(Object msg)
	{
		Log.d("MyDebug", msg == null ? "null" : msg.toString());
	}

}
