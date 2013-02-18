package com.iir.FitnessSystem;
import java.util.Arrays;
import java.util.List;
/** @description 從輸入資料計算特徵值 */
public class Features {
	private List<Float> dataX;
	private List<Float> dataY;
	private List<Float> dataZ;
	private double[] featureSet = new double[21];
	
	public Features(List<Float> x, List<Float> y, List<Float> z) {
			this.dataX = addZero(x);
			this.dataY = addZero(y);
			this.dataZ = addZero(z);
	}
	
	/* 計算頻域特徵時，將資料點補齊至128個點 */
	public List<Float> addZero(List<Float> list) {
  		float zero = 0;
  		while (list.size() < 128) {
  			list.add(zero);
  		}
  		return list;
  	}
	
	/* 原始資料轉頻域  */
	public Complex[] transform(List<Float> data) {
		Complex[] beforeTrans;
		Complex[] afterTrans;
		beforeTrans = new Complex[data.size()];
		for (int i = 0; i < 128; i++) {
			beforeTrans[i] = new Complex(data.get(i), 0);
		}
		afterTrans = FFT.fft(beforeTrans);
		return afterTrans;
	}
	
	public double[] getFeatureSet() {
		featureSet[0] = ((this.getAmplitude(dataX))*0.02264215)-1.00806705;
		featureSet[1] = ((this.getAmplitude(dataY))*0.02700189)-1.2472294;
		featureSet[2] = ((this.getAmplitude(dataZ))*0.02538513)-1.00347381;
		featureSet[3] = ((this.getCorrelation(dataX, dataY))*1.01486293)+0.01361325;
		featureSet[4] = ((this.getCorrelation(dataY, dataZ))*1.02847751)-0.02847751;
		featureSet[5] = ((this.getCorrelation(dataX, dataZ))*1.04103293)+0.03980865;
		featureSet[6] = ((this.getIQR(dataX))*0.06119951)-1;
		featureSet[7] = ((this.getIQR(dataY))*0.08613264)-1;
		featureSet[8] = ((this.getIQR(dataZ))*0.10989011)-1;
		featureSet[9] = ((this.getMean(dataX))*0.15419581)-0.21589012;
		featureSet[10] = ((this.getMean(dataY))*0.09260676)+0.33760926;
		featureSet[11] = this.getMedian(dataX);    
		featureSet[12] = ((this.getMedian(dataY))*0.08915551)+0.36612023;
		featureSet[13] = ((this.getMedian(dataZ))*0.10563966)-0.04676258;
		featureSet[14] = ((this.getStep(dataX))*0.09090909)-1;
		featureSet[15] = ((this.getStep(dataY))*0.06666667)-1;
		featureSet[16] = ((this.getStep(dataZ))*0.08)-1;
		featureSet[17] = this.getPowerSpectral(dataX);    
		featureSet[18] = this.getPowerSpectral(dataZ);    
		featureSet[19] = ((this.getSma(dataX, dataY, dataZ))*0.07858369)-1.78482476;
		featureSet[20] = ((this.getSvm(dataX, dataY, dataZ))*0.00726091)-1.00000292; 
		return featureSet;
	}
	
	
	//振幅
	public double getAmplitude(List<Float> data) {
		List<Float> data2 = addZero(data);
		double tmp = 0.0;
		Complex[] after = transform(data2);
		for (int i = 0; i < after.length; i++)
		{
			tmp += after[i].abs();
		}
		return tmp / data2.size();
	}
	
	
	//相關係數
	public double getCorrelation(List<Float> data1, List<Float> data2) {
		double r;
		int indexX, n;
		double x_sum, y_sum, xx_sum, yy_sum, xy_sum;
		x_sum = y_sum = xx_sum = yy_sum = xy_sum = 0.0;
		indexX = n = data1.size();
		while (indexX-- != 0) {
			x_sum += data1.get(indexX);
			y_sum += data2.get(indexX);
			xx_sum += data1.get(indexX) * data1.get(indexX);
			yy_sum += data2.get(indexX) * data2.get(indexX);
			xy_sum += data1.get(indexX) * data2.get(indexX);
		}
		r = ((n * xy_sum - x_sum * y_sum) / (Math.sqrt((n * xx_sum - x_sum * x_sum) * (n * yy_sum - y_sum * y_sum))));
		return r;
	}
	
	
	//取得四分位差
	public double getIQR(List<Float> data) {
		Float[] data2 = new Float[data.size()];
		int middle = data2.length / 2;
		data.toArray(data2);
		Arrays.sort(data2);
		Float[] Q1 = new Float[middle];
		Float[] Q2 = new Float[middle];
		for (int i = 0; i < middle; i++) {
			Q1[i] = data2[i];
			Q2[i] = data2[data2.length - i - 1];
		}
		return computeMedian(Q2) - computeMedian(Q1);
	}
	
	
	//取得平均值
	public double getMean(List<Float> data) {
		double i = 0.0;
		for (int j = 0; j < data.size(); j++)
		{
			i += data.get(j);
		}
		return i / data.size();
	}
	
	//取得中位數
	public double getMedian(List<Float> data) {
		// sort
		Float[] data2 = new Float[data.size()];
		data.toArray(data2);
		Arrays.sort(data2);
		// median
		return computeMedian(data2);
	}
	
	//計算中位數
	public double computeMedian(Float[] data) {
		int middle = data.length / 2;
		if (data.length % 2 == 1) {
			return data[middle];
		} else {
			return (data[middle] + data[middle - 1]) / 2.0; 
		}
	}
	
	
	//peak數
	public double getStep(List<Float> data) {
		DataPreProcess dataPX = new DataPreProcess(data);
		List<Float> data2 = dataPX.getMOA();
		 double result = 0.0;
		 for (int i = 1; i < data2.size() - 1; i++) {
			 if(data2.get(i - 1) - data2.get(i) < 0 && data2.get(i + 1) - data2.get(i) < 0){
				 result++;
			 }
		 }
		 return result;
	}
	
	//SMA
	public double getSma(List<Float> data1, List<Float> data2, List<Float> data3) {
		double sumX = 0;
		double sumY = 0;
		double sumZ = 0;
		for(int i = 0; i < data1.size(); i++) {
			sumX += Math.abs(data1.get(i));
			sumY += Math.abs(data2.get(i));
			sumZ += Math.abs(data3.get(i));
		}
		return 0.01 * (sumX + sumY + sumZ);
	}
	
	
	//三軸的平均振幅
	public double getMeanAmp(List<Float> data1, List<Float> data2, List<Float> data3) {
		List<Float> data11 = addZero(data1);
		List<Float> data22 = addZero(data2);
		List<Float> data33 = addZero(data3);
		double tmp1 = 0.0;
		double tmp2 = 0.0;
		double tmp3 = 0.0;
		Complex[] after1 = transform(data11);
		Complex[] after2 = transform(data22);
		Complex[] after3 = transform(data33);
		for (int i = 0; i < after1.length; i++)
		{
			tmp1 += after1[i].abs();
			tmp2 += after2[i].abs();
			tmp3 += after3[i].abs();
		}
		return (tmp1 + tmp2 + tmp3) / 128;
	}
	
	
	//取得標準差
	public double getStd(List<Float> data) {
		return Math.sqrt(this.getVariance(data));
	}
	
	//取得變異數
	public double getVariance(List<Float> data) {
		double result = 0.0;
		double x = 0.0;
		for (int i = 0; i < data.size(); i++ )
		{
			x += Math.pow(data.get(i), 2);
		}
		result = (x - data.size() * Math.pow(getMean(data), 2)) / (data.size() - 1);
		return result;
	}
	
	//取得SVM
	public double getSvm(List<Float> data1, List<Float> data2, List<Float> data3) {
		double varX = getVariance(data1);
		double varY = getVariance(data2);
		double varZ = getVariance(data3);
		return Math.sqrt(Math.pow(varX, 2) + Math.pow(varY, 2) + Math.pow(varZ, 2));
	}
	
	//取得Power Spectral
	public double getPowerSpectral(List<Float> data) {
		// 將樣本點擴充到128點，以利於做傅立葉轉換
		List<Float> data2 = addZero(data);
		double tmp = 0.0;
		Complex[] after = transform(data2);
		for (int i = 0; i < after.length; i++)
		{
			tmp += Math.pow(after[i].abs(), 2);
		}
		return tmp / data2.size();
	}
	
}
