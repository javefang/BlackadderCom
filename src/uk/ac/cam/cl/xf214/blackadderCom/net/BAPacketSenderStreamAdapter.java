package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.InputStream;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;

public class BAPacketSenderStreamAdapter extends BAPacketSender {

	public BAPacketSenderStreamAdapter(BAWrapperNB wrapper,
			HashClassifierCallback classifier, BAItem item) {
		super(wrapper, classifier, item);
		
	}
	
}
