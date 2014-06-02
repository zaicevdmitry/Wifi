/**
 * 
 */
package by.salin.app;

import android.app.Application;

/**
 * @author Alexandr.Salin
 * 
 */
public class WIFIApplication extends Application
{
	private static WIFIApplication mApplication;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mApplication = this;
	}

	public static WIFIApplication getApplication()
	{
		return mApplication;
	}

}
