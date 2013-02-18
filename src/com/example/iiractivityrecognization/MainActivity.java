package com.example.iiractivityrecognization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;


public class MainActivity extends Activity {
	private ProgressDialog myDialog;
	svm_model model;
	DateFormat dateFormat;
	TextView tv1;
	private int allcount = 0, activecount=0; 
	private List<String> sportTypeRecorder;   //存動作類別
	private List<Double> hotRecorder;  
	private List<String> timeRecorder; 
	private int[] sportTypeNumber;
	private int weight = 65;
	private double hotSum = 0;
	private Handler mHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv1 = (TextView)findViewById(R.id.textView1);
		modelLoading();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void onSensorChanged(final int sensor, final float[] values) {
		Thread myThread = new Thread() {//每5秒更新一次資料
			public void run() {
				Date date0 = new Date();
				while (true) {
					Date date1 = new Date();
					String startTime = dateFormat.format(date1);
					final List<Float> x_list = new ArrayList<Float>();
					final List<Float> y_list = new ArrayList<Float>();
					final List<Float> z_list = new ArrayList<Float>();
					
					TimerTask GetSensorData = new TimerTask() {
						public void run() {
							if (x_list.size() < 100) {    
								x_list.add(values[0]);
								y_list.add(values[1]);
								z_list.add(values[2]);
							}
						}
					};
					Timer timer = new Timer(true);
					timer.scheduleAtFixedRate(GetSensorData, 0, 50);
					
					try {
						Thread.sleep(5000);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						recognition(x_list, y_list, z_list, startTime);
						timer.cancel();
					}
					
				}
				
			}
		};
		myThread.setDaemon(true);
		myThread.start();
	}
	/**
	 * Loading 機率表
	 * */
	public void modelLoading() {
		myDialog = ProgressDialog.show(MainActivity.this, "Warning...",
				"Model Loading", true);
		new Thread() {
			public void run() {
				try {
			        model = svm.svm_load_model("/sdcard/FitnessSystem/LG/svm18.model");				
				} catch (Exception e) {
					e.printStackTrace();
					Log.i("svm",e.getMessage());
					try {
						copySvm();
						model = svm.svm_load_model("/sdcard/FitnessSystem/LG/svm18.model");
					} catch (IOException e1) {
						
						e1.printStackTrace();
					}
					
				} finally {
					myDialog.dismiss();
				}
			}
		}.start();
	}
	//複製svm18.model到sd目錄
	private void copySvm() throws IOException {
		InputStream myInput;
		File dir = new File("/sdcard/FitnessSystem/LG/");
		if (!dir.exists()) {
		dir.mkdirs();
		}
		File dbf = new File("/sdcard/FitnessSystem/LG/svm18.model");
		if (dbf.exists()) {
		dbf.delete();
		}
		String outFileName = "/sdcard/FitnessSystem/LG/svm18.model";
		OutputStream myOutput = new FileOutputStream(outFileName);
		myInput = this.getAssets().open("svm18.model");
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
		myOutput.write(buffer, 0, length);
		}
		myOutput.flush();
		myInput.close();
		myOutput.close();
		}
	public void recognition(List<Float> x_list, List<Float> y_list, List<Float> z_list, String startTime) {
		 Date date2 = new Date();
		 Features features = new Features(x_list, y_list, z_list);
		 double[] featureSet = features.getFeatureSet();
		 String endTime = dateFormat.format(date2);
		
		 double key = 99;     
		 key=predict("555 1:"+featureSet[0]+" 2:"+featureSet[1]+" 3:"+featureSet[2]+" 4:"+featureSet[3]+
				" 5:"+featureSet[4]+" 6:"+featureSet[5]+" 7:"+featureSet[6]+" 8:"+featureSet[7]+" 9:"+featureSet[8]+
				" 10:"+featureSet[9]+" 11:"+featureSet[10]+" 12:"+featureSet[12]+" 13:"+featureSet[13]+" 14:"+featureSet[14]+
				" 15:"+featureSet[15]+" 16:"+featureSet[16]+" 17:"+featureSet[19]+" 18:"+featureSet[20]+"",model);
		
		// 結合門檻與辨識值
		if ((features.getAmplitude(y_list) > 18.5635)
				&& features.getMeanAmp(x_list, y_list, z_list) <= 67.6307) {
			if (key == 0.0) {
				if(allcount==720 && activecount<60){
					updateView(9);
					sportTypeRecorder.add("static state");   //紀錄動作類型
					sportTypeNumber[0]++;                    //算動作次數
					timeRecorder.add(endTime);               //紀錄時間
					double temp2 = weight * 0.001 * 1.2;     //算每次辨識結果的熱量，體重/時間(小時)*代謝當量
					hotRecorder.add(temp2);                  //分段計熱量
					hotSum += temp2;                         //熱量加總
					allcount=0;
					activecount=0;
					

				}else{
					updateView(0);                           
					sportTypeRecorder.add("static state");          
					sportTypeNumber[0]++;                    
					timeRecorder.add(endTime);              
					double temp2 = weight * 0.001 * 1.2;     
					hotRecorder.add(temp2);                  
					hotSum += temp2;                         
					allcount+=1;
				}

			} else if (key == 1.0) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("static state");
					sportTypeNumber[1]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 1;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=0;
					

				}else{
					updateView(1);     
					sportTypeRecorder.add("static state");
					sportTypeNumber[1]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 1;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
				}

			} else if (key == 7.0) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("static state");
					sportTypeNumber[1]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 1;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=0;
					
				}else{
					updateView(1);     
					sportTypeRecorder.add("static state");
					sportTypeNumber[1]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 1;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;

				}
			} else if (key == 2.0
					&& ((features.getAmplitude(x_list)+features.getAmplitude(y_list)+features.getAmplitude(z_list))/3)<=20) {     //平均振幅小於20
				if(allcount==720 && activecount<60){
					updateView(9);
					sportTypeRecorder.add("walk");
					sportTypeNumber[2]++;
					timeRecorder.add(endTime);
				    double temp2 = weight * 0.001 * 2.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
					
				}else{
					updateView(2);
					sportTypeRecorder.add("walk");
					sportTypeNumber[2]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 2.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 2.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 20
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 23) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(6);
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 2.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 23
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 25) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(6);
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 5.0) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("walk");
					sportTypeNumber[5]++;
					timeRecorder.add(endTime);
					int temp = (int) features.getStep(y_list);
					//stepRecorder.add(temp);
					//stepSum += features.getStep(y_list);
					double temp2 = weight * 0.001 * 8;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(5);
					sportTypeRecorder.add("walk");
					sportTypeNumber[5]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 8;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 6.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 20) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("walk");
					sportTypeNumber[2]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 2.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(2);
					sportTypeNumber[2]++;
					sportTypeRecorder.add("walk");
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 2.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 6.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 20
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 23) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(6);
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 6.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 23
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 25) {
				if(allcount==720 && activecount<60){
					
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(6);
					sportTypeRecorder.add("walk");
					sportTypeNumber[6]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 3;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else {
				
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("unknown");
					timeRecorder.add(endTime);
					hotRecorder.add(0.0);
					allcount=0;
					activecount=0;
				}else{
					updateView(8);
					sportTypeRecorder.add("unknown");
					timeRecorder.add(endTime);
					hotRecorder.add(0.0);
					allcount+=1;
				}
			}
		} else if ((features.getAmplitude(y_list) > 18.5635)
				&& features.getMeanAmp(x_list, y_list, z_list) > 67.6307) {
			if (key == 3.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 45) {
				if(allcount==720 && activecount<60){
					
					sportTypeRecorder.add("slow-run");
					sportTypeNumber[3]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 8.7;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(3);
					sportTypeRecorder.add("slow-run");
					sportTypeNumber[3]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 8.7;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 3.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 45) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("fast-run");
					sportTypeNumber[4]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 12.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(4);
					sportTypeRecorder.add("fast-run");
					sportTypeNumber[4]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 12.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 4.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) <= 45) {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("slow-run");
					sportTypeNumber[3]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 8.7;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(3);
					sportTypeRecorder.add("slow-run");
					sportTypeNumber[3]++;
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 8.7;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			} else if (key == 4.0
					&& ((features.getAmplitude(x_list)
							+ features.getAmplitude(y_list) + features
							.getAmplitude(z_list)) / 3) > 45) {
				if(allcount==720 && activecount<60){
					sportTypeNumber[4]++;
					sportTypeRecorder.add("fast-run");
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 12.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount=0;
					activecount=1;
				}else{
					updateView(4);
					sportTypeNumber[4]++;
					sportTypeRecorder.add("fast-run");
					timeRecorder.add(endTime);
					double temp2 = weight * 0.001 * 12.5;
					hotRecorder.add(temp2);
					hotSum += temp2;
					allcount+=1;
					activecount+=1;
				}
			}  else {
				if(allcount==720 && activecount<60){
					sportTypeRecorder.add("unknown");
					timeRecorder.add(endTime);
					hotRecorder.add(0.0);
					allcount=0;
					activecount=0;
				}else{
					updateView(8);
					sportTypeRecorder.add("unknown");
					timeRecorder.add(endTime);
					hotRecorder.add(0.0);
					allcount+=1;
				}
			}
		} 
	 }
	
	 private static double predict(String input, svm_model model)
	 {
	 	double v=99;

	 	int svm_type=svm.svm_get_svm_type(model);
	 	int nr_class=svm.svm_get_nr_class(model);

 		StringTokenizer st = new StringTokenizer(input," \t\n\r\f:");

		double target = atof(st.nextToken());
		int m = st.countTokens()/2;
		svm_node[] x = new svm_node[m];
		for(int j=0;j<m;j++)
		{
			x[j] = new svm_node();
			x[j].index = atoi(st.nextToken());
			x[j].value = atof(st.nextToken());
		}
			
		v = svm.svm_predict(model,x);
		return v;
	}
	 private static double atof(String s)
	 {
		return Double.valueOf(s).doubleValue();
	 }

	 private static int atoi(String s)
	 {
	 	return Integer.parseInt(s);
	 }
	 
	//動作型態顯示
		public class MyRunnable implements Runnable {
			int checkValue;
			
			public MyRunnable(int input) {
				checkValue = input;
			}
			
			public void run() {
				// TODO Auto-generated method stub
				//imageview1.clearAnimation();     
													

					switch (checkValue) {
						case 0:
							tv1.setText("靜態");
							break;
						case 1:
							tv1.setText("靜態");
							break;
						case 2:
							tv1.setText("走路");
							break;
						case 3:
							tv1.setText("慢跑");
							break;
						case 4:
							tv1.setText("快跑");
							break;
						case 5:
							tv1.setText("走路");
							break;
						case 6:
							tv1.setText("走路");
							break;
						case 7:
							tv1.setText("靜態");
							break;
				
					}			
				
			}
		}
	 
	public void updateView(int check) {
			MyRunnable myRunnable = new MyRunnable(check);
			mHandler.postDelayed(myRunnable, 1000);
	}


}
