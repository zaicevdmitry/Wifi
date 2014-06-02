/*
 * ECE 480 Spring 2011
 * Team 2 Design Project
 * Matt Gottshall
 * Jake D'Onofrio
 * Gordie Stein
 * Andrew Kling
 */
package by.salin.app.utils.wifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;


/** An object to add up and average the AP locations */
public class ScanCounter implements Runnable
{
	private static Map<String,String> macRegistry = new HashMap<String, String>();
	static{
		macRegistry.put("60:a1:0a:f2:0c:b6", "HTC One");
		
		macRegistry.put("c4:43:8f:47:68:13", "Nuxes 4");
		
		macRegistry.put("78:1d:ba:27:7b:0b", "Tamogoch2");  
		
		macRegistry.put("78:1d:ba:27:7b:03", "Tamogoch");
		
	}
	private static Map<String,Integer> levelRegistry = new HashMap<String, Integer>();
	
	private List<ScanResult> scans;
	private WeightedScanFactory wsFactory;
	private ScanResultReceiver SRR;

	private float sumX = 0;
	private float sumY = 0;
	private float sumZ = 0;
	private int count = 0;

	private static int x = 0;
	private static int y = 1;
	private static int z = 2;
	float[] output;
	float[] outputAbs;

	String outputLevel = "";

	public ScanCounter(ScanResultReceiver scanResultReceiver, List<ScanResult> s, WeightedScanFactory wsF)
	{
		scans = s;
		wsFactory = wsF;
		SRR = scanResultReceiver;
	}

	/**
	 * This is run when the thread is started. It runs through the list of APs
	 * in range and determines the location of the user.
	 */
	@Override
	public void run()
	{
		if(scans != null && !scans.isEmpty())
		{
			// Go through the list of scans, give them a value from 1-20 based
			// on strength, then
			// create a weighted scan and save it in the list
			int reqScan = 0;
			levelRegistry.clear();
			List<String> levelList = new ArrayList<String>();
			for(ScanResult scan : scans)
			{
				int level = WifiManager.calculateSignalLevel(scan.level, 20);
				
				WeightedScan wScan = wsFactory.Create(scan.BSSID);
				
				if(wScan != null)
				{
					wScan.SetLevel(level);
					sumX += level * wScan.GetPos().get(x);
					sumY += level * wScan.GetPos().get(y);
					sumZ += level * wScan.GetPos().get(z);
					count += level;
					//outputLevel += macRegistry.get(scan.BSSID) + " - level:" + level + ",\n";
					levelRegistry.put(scan.BSSID, level);
					reqScan++;
				}
				
			}
			if (!levelRegistry.isEmpty()){
				levelList.addAll(levelRegistry.keySet());
				
				}
				for (String macdevice: levelList){
					outputLevel += macRegistry.get(levelRegistry.get(macdevice)) + " - Device: " + macdevice + ",\n";
				}
				if(reqScan > 0)
			{
				float dLevel = count / reqScan;
				float dX = Float.valueOf((sumX / reqScan) / 20);
				float dY = Float.valueOf((sumY / reqScan) / 20);
				float dZ = Float.valueOf((sumZ / reqScan) / 20);
				float[] output = { dX, dY, dZ };
				this.output = output;

				float drX = Float.valueOf((sumX / reqScan) / dLevel);
				float drY = Float.valueOf((sumY / reqScan) / dLevel);
				float drZ = Float.valueOf((sumZ / reqScan) / dLevel);
				float[] other = { drX, drY, drZ };
				this.outputAbs = other;

				updateInfoMainThread();
			}

		}
	}

	private void updateInfoMainThread()
	{
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				float[] normalMas = LocationNormalizer.Normalize(sumX, sumY, sumZ);
				float nX = normalMas[0];
				float nY = normalMas[1];
				float nZ = normalMas[2];
				MainActivity.getHolder().setStatusText(",sumX = " + outputAbs[0] + "\n,sumY = " + outputAbs[1] + "\n,symZ = " + outputAbs[2] + "\n,count = " + count+"\n_______________");
				
				MainActivity.getHolder().setCoordsText(",sumX = " + output[0] + "\n,sumY = " + output[1] + "\n,symZ = " + output[2] + "\n,count = " + count);
				
				Phone phone = new Phone(levelRegistry);
				Gson gson = new Gson();
		        String jsonData = gson.toJson(phone);

		        HttpClient httpClient = new DefaultHttpClient();

		        try {
		            HttpPost request = new HttpPost("http://192.168.0.185:8081/service/api/sendData");
		            StringEntity params =new StringEntity(jsonData);
		            request.addHeader("content-type", "application/json");
		            request.setEntity(params);
		            HttpResponse response = httpClient.execute(request);
		        }catch(Exception e){}
				
		        MainActivity.getHolder().setChangesText(outputLevel);
				// MainActivity.getHolder().setCoordsText(",sumX = " +
				// sumX/count + "\n,sumY = " + sumY/count + "\n,symZ = " +
				// sumZ/count + "\n,count = " + count);
				// MainActivity.getHolder().setCoordsText(",sumX = " + nX +
				// "\n,sumY = " + nY + "\n,symZ = " + nZ + "\n,count = " +
				// count);

			}
		});
		
	}
	
	 public class Phone   {
	        double x;
	        double y;
	        private Map<String, Integer> levelRegistry;

	        public Phone(Map<String, Integer> levelRegistry) {
	        	this.setLevelRegistry(levelRegistry);
	        }

	       
			public double getX() {
	            return x;
	        }

	        public void setX(double x) {
	            this.x = x;
	        }

	        public double getY() {
	            return y;
	        }

	        public void setY(double y) {
	            this.y = y;
	        }


			public Map<String, Integer> getLevelRegistry() {
				return levelRegistry;
			}


			public void setLevelRegistry(Map<String, Integer> levelRegistry) {
				this.levelRegistry = levelRegistry;
			}
	    }
}
