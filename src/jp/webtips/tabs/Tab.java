package jp.webtips.tabs;

public class Tab
{
	public String name;
	public String url;
	public int color;

	public Tab(String name, String url, int color)
	{
		super();
		this.name = name;
		this.url = url;
		this.color = color;
	}

	public static Tab create(String name, String url, int color)
	{
		return new Tab(name, url, color);
	}
}
