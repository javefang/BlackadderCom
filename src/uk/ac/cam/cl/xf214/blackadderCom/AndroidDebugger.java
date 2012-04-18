package uk.ac.cam.cl.xf214.blackadderCom;

import android.util.Log;
import uk.ac.cam.cl.xf214.DebugTool.Debugger;

public class AndroidDebugger implements Debugger {

	@Override
	public void print(String tag, String msg) {
		Log.i(tag, msg);
	}

	@Override
	public void printe(String tag, String msg) {
		Log.e(tag, msg);
	}
}
