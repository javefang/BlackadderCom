package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.Closeable;
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
	public static final String TAG = "BAPacketReceiverSocketAdapter";
	public static final String DEFAULT_SOCKET_NAME = "BAVideoReceiver";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private LocalServerSocket serverSocket;
	private LocalSocket recvSocket;
	private LocalSocket sendSocket;
	private Thread dataTransportThread;
	private Closeable streamWrapper;
	
	private volatile boolean receive;
	private volatile boolean released;
	
	public BAPacketReceiverSocketAdapter(HashClassifierCallback classifier,
			byte[] rid) throws IOException {
		super(classifier, rid);
		String ridStr = BAHelper.byteToHex(rid);
		// Set up Blackadder->Socket bridge
		Log.i(TAG, "Setting up Blackadder->Socket bridge...");
		recvSocket = new LocalSocket();
		serverSocket = new LocalServerSocket(DEFAULT_SOCKET_NAME + ridStr);
		
		recvSocket.connect(new LocalSocketAddress(DEFAULT_SOCKET_NAME + ridStr));
		recvSocket.setReceiveBufferSize(500000);
		recvSocket.setSendBufferSize(500000);
		
		sendSocket = serverSocket.accept();
		sendSocket.setReceiveBufferSize(500000);
		sendSocket.setSendBufferSize(500000);
		
		final Object initWait = new Object();
		
		Log.i(TAG, "Starting bridging thread...");
		dataTransportThread = new Thread(new Runnable() {
			private OutputStream outputStream;

			public void run() {
				released = false;
				receive = false;
				BlockingQueue<BAEvent> dataQueue = getDataQueue();
				byte[] buf;
				BAEvent event = null;
				try {
					outputStream = sendSocket.getOutputStream();
					Log.i(TAG, "BA->Socket bridging loop start!");
					synchronized(initWait) {
						initWait.notify();
					}
					while (!released) {
						if (dataQueue.size() > RESYNC_THRESHOLD) {
							// when queue size > 10, remove old event to keep audio synced
							Log.i(TAG, "Resync video");
							drainDataQueue();
						}
						//Log.i(TAG, "waiting for BA_PKT...");
						// get a data event from BA
						event = dataQueue.take();
						//Log.i(TAG, "got new BA_PKT");
						if (event.getDataLength() == 0) {
							Log.i(TAG, "FIN_PKT received: terminating video " + BAHelper.byteToHex(getRid()));
							event.freeNativeBuffer();
							release();
							break;
						}
						// get the data buffer
						if (receive) {
							buf = event.getData(0, event.getDataLength());
							// write to output stream
							//Log.i(TAG, "Writing BA_PKT to socket... size=" + buf.length);
							outputStream.write(buf);
						}
						event.freeNativeBuffer();
					}
					event = null;
				} catch (IOException e) {
					Log.e(TAG, "IOException caught, abort...");
					release();
				} catch (InterruptedException e) {
					// caused by thread interruption when waiting on BlockingQueue
					Log.e(TAG, "Bridging thread interrupted while waiting for event on BlockingQueue!");
					release();
				} finally {
					if (event != null) {
						event.freeNativeBuffer();
					}
				}
				
				Log.i(TAG, "BA->Socket transport thread terminated!");
			}
		});
		dataTransportThread.start();
		synchronized(initWait) {
			try {
				initWait.wait(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
	
	public void setCloseable(Closeable streamWrapper) {
		this.streamWrapper = streamWrapper;
	}
	
	/* must call setReceive(true) to start receiving data */
	public void setReceive(boolean receive) {
		Log.i(TAG, "Set receive=" + receive);
		this.receive = receive;
	}
	
	public synchronized void release() {
		if (!released) {
			released = true;
			
			if (streamWrapper != null) {
				Log.i(TAG, "Closing stream wrapper...");
				try {
					streamWrapper.close();
				} catch (IOException e) {
					Log.e(TAG, "release(): IOException caught when closing stream wrapper");
				}
				streamWrapper = null;
			}
			
			try {
				Log.i(TAG, "Closing socket...");
				sendSocket.close();
				recvSocket.close();
				serverSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "release(): IOException caught while closing socket!");
			}
			dataTransportThread.interrupt();
			// TODO: join thread here?

			Log.i(TAG, "BAPacketReceiverSocketAdapter released!");
			super.release();
		}
	}

}
