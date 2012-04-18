package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

public class AudioHelper {
	public static double getRMSAverage(byte[] audioData) {
		int sum = 0;
		// look at every other value (odd indexed value)
		for (int i = 1; i < audioData.length; i += 2) {
			// 1. square each sample, sum the squared sample
			sum += audioData[i] * audioData[i];
		}
		// 2. divide the sum by the number of samples
		// 3. take the square root of step 2 (ROOT-MEAN-SQUARED RMS value)
		return Math.sqrt(sum / (double)(audioData.length / 2));
	}
}
