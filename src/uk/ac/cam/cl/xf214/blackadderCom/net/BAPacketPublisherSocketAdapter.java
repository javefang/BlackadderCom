package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;


public class BAPacketPublisherSocketAdapter extends BAPacketPublisher {
	public static final String TAG = "BAPacketSenderSocketAdapter";
	public static final String DEFAULT_SOCKET_NAME = "BAVideoSender";
	
	private LocalServerSocket serverSocket;
	private LocalSocket recvSocket;
	private LocalSocket sendSocket;
	private Thread dataTransportThread;
	
	private volatile boolean released;
	
	public BAPacketPublisherSocketAdapter(BAWrapperNB wrapper, HashClassifierCallback classifier, BAItem item) throws IOException {
		super(wrapper, classifier, item);
		
		// Set up Socket->Blackadder bridge
		Log.i(TAG, "Setting up Socket->Blackadder bridge...");
		Log.i(TAG, "Initializing LocalServerSocket...");
		recvSocket = new LocalSocket();
		serverSocket = new LocalServerSocket(DEFAULT_SOCKET_NAME);
		
		Log.i(TAG, "Initializing receiving LocalSocket...");
		recvSocket.connect(new LocalSocketAddress(DEFAULT_SOCKET_NAME));
		recvSocket.setReceiveBufferSize(500000);
		recvSocket.setSendBufferSize(500000);
		
		Log.i(TAG, "Accepting receiving LocalSocket...");
		sendSocket = serverSocket.accept();
		sendSocket.setReceiveBufferSize(500000);
		sendSocket.setSendBufferSize(500000);
		
		Log.i(TAG, "Starting bridging thread...");
		dataTransportThread = new Thread(new Runnable() {
			private InputStream inputStream;
			
			public void run() {
				released = false;
				byte[] buf = new byte[BAWrapperShared.DEFAULT_PKT_SIZE];
				try {
					inputStream = recvSocket.getInputStream();
					
					Log.i(TAG, "Socket->BA bridging loop start!");
					while (!released) {
						// read data packet from socket to buffer
						readFully(buf, 0, buf.length);
						// send data in the buffer via Blackadder
						send(buf, 0);
					}
				} catch (IOException e1) {
					Log.e(TAG, "IOException caught while getting input stream from recvSocket, abort...");
					release();
				}
				
				Log.i(TAG, "Socket->BA transport thread terminated!");
			}
			
			/* fill the byte[] with recorded audio data */
			private void readFully(byte[] data, int off, int length) throws IOException {
				int read;
				while (length > 0) {
					read = inputStream.read(data, off, length);
					if (read == -1) {
						return;
					}
					length -= read;
					off += read;
				}
			}
		});
		dataTransportThread.start();
	}
	
	public OutputStream getOutputStream() throws IOException {
		return sendSocket.getOutputStream();
	}
	
	public FileDescriptor getFileDescriptor() {
		if (!released) {
			return sendSocket.getFileDescriptor();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public synchronized void release() {
		if (!released) {
			Log.i(TAG, "Finishing thread...");
			released = true;
			Log.i(TAG, "Closing socket...");
			try {
				sendSocket.close();	// this causes app failed to write new data into the stream
				recvSocket.close();	// this causes Socket->BA bridge failed to read new data from the stream, IOException caught and thread exit
				serverSocket.close();	// release socket address "BAVideoSender"
			} catch (IOException e) {
				Log.e(TAG, "IOException caught while closing socket!");
			}
			dataTransportThread.interrupt();
			// TODO: join thread here? (avoid calling send() after super class finished)
			super.release();
		}
	}

}
