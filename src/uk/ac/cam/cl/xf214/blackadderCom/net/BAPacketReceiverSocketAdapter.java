package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;


public class BAPacketReceiverSocketAdapter extends BAPacketReceiver {
	public static final String TAG = "BAPacketRecieverSocketAdapter";
	public static final String DEFAULT_SOCKET_NAME = "BAVideoReceiver";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private LocalServerSocket serverSocket;
	private LocalSocket recvSocket;
	private LocalSocket sendSocket;
	private Thread dataTransportThread;
	
	private volatile boolean released;
	
	public BAPacketReceiverSocketAdapter(HashClassifierCallback classifier,
			byte[] rid, StreamFinishedListener streamFinishedListener) throws IOException {
		super(classifier, rid, streamFinishedListener);
		
		// Set up Blackadder->Socket bridge
		Log.i(TAG, "Setting up Blackadder->Socket bridge...");
		recvSocket = new LocalSocket();
		serverSocket = new LocalServerSocket(DEFAULT_SOCKET_NAME);
		
		recvSocket.connect(new LocalSocketAddress(DEFAULT_SOCKET_NAME));
		recvSocket.setReceiveBufferSize(500000);
		recvSocket.setSendBufferSize(500000);
		
		sendSocket = serverSocket.accept();
		sendSocket.setReceiveBufferSize(500000);
		sendSocket.setSendBufferSize(500000);
		
		Log.i(TAG, "Starting bridging thread...");
		dataTransportThread = new Thread(new Runnable() {
			private OutputStream outputStream;

			public void run() {
				released = false;
				BlockingQueue<BAEvent> dataQueue = getDataQueue();
				byte[] buf;
				BAEvent event = null;
				try {
					
					outputStream = sendSocket.getOutputStream();
					Log.i(TAG, "BA->Socket bridging loop start!");
					while (!released) {
						if (dataQueue.size() > RESYNC_THRESHOLD) {
							// when queue size > 10, remove old event to keep audio synced
							Log.i(TAG, "Resync video");
							drainDataQueue();
						}
						// get a data event from BA
						event = dataQueue.take();
						if (event.getDataLength() == 0) {
							Log.i(TAG, "FIN_PKT received: terminating video " + BAHelper.byteToHex(getRid()));
							event.freeNativeBuffer();
							release();
							break;
						}
						// get the data buffer
						buf = event.getData(0, event.getDataLength());
						// write to output stream
						outputStream.write(buf);
						event.freeNativeBuffer();
					}
					event = null;
				} catch (IOException e) {
					Log.e(TAG, "IOException caught, abort...");
					release();
				} catch (InterruptedException e) {
					// caused by thread interruption when waiting on BlockingQueue
					Log.e(TAG, "Bridging thread interrupted while waiting for event on BlockingQueue!");
				} finally {
					if (event != null) {
						event.freeNativeBuffer();
					}
				}
				
				
				Log.i(TAG, "BA->Socket transport thread terminated!");
			}
		});
		dataTransportThread.start();
	}
	
	public InputStream getInputStream() throws IOException {
		return recvSocket.getInputStream();
	}
	
	public FileDescriptor getFileDescriptor() {
		if (!released) {
			return recvSocket.getFileDescriptor();
		} else {
			return null;
		}
	}
	
	public synchronized void release() {
		if (!released) {
			released = true;
			Log.i(TAG, "Closing socket...");
			try {
				sendSocket.close();
				recvSocket.close();
				serverSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException caught while closing socket!");
			}
			dataTransportThread.interrupt();
			// TODO: join thread here?
			super.release();
		}
	}

}
