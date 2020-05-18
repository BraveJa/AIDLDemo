package com.self.aidltest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.widget.Toast;

import com.self.aidltest.enty.Message;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

/**
 * android:process=":remote"
 * 管理和提供子进程的连接消息服务
 * aidl子线程池子里 ,
 * oneway 不能有返回值 ,我并不关心远端的执行 ,主程调用不阻塞
 */
public class RemoteService extends Service {
	private volatile boolean isConnect = false;
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(@NonNull android.os.Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			data.setClassLoader(Message.class.getClassLoader());//有可能会反序列化出问题,导致类找不到
			Message message = data.getParcelable("fromParcel");
			Toast.makeText(RemoteService.this, "handleMessage : " + message.getContent(), Toast.LENGTH_SHORT).show();

			//返回一个响应
			Messenger replyTo = msg.replyTo;
			Message replyMessage = Message.CREATOR.createFromParcel(Parcel.obtain());
			replyMessage.setContent("你好我收到了 code = 200");
			Bundle bundle = new Bundle();
			bundle.putParcelable("reply",replyMessage);
			android.os.Message message1 = new android.os.Message();
			message1.setData(bundle);
			try {
				replyTo.send(message1);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	private Messenger messenger = new Messenger(handler);


	/**
	 * 连接服务
	 */
	private IConnectionService iConnectionService = new IConnectionService.Stub() {
		@Override
		public void connect() throws RemoteException {

			try {
				//不设置oneway 主程就会阻塞
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isConnect = true;
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(RemoteService.this, "连接了", Toast.LENGTH_SHORT).show();
				}
			});
			future = scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					/*for (IMessageReceiveListener listener : remoteCallbackList) {
						Message fromParcel = Message.CREATOR.createFromParcel(Parcel.obtain());

						fromParcel.setContent("远程来的消息");
						try {
							listener.onReceiveMessage(fromParcel);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}*/
					int size = remoteCallbackList.beginBroadcast();
					for (int j = 0; j < size; j++) {
						Message fromParcel = Message.CREATOR.createFromParcel(Parcel.obtain());
						fromParcel.setContent("远程来的消息");
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
							IMessageReceiveListener registeredCallbackItem = remoteCallbackList.getRegisteredCallbackItem(j);
							try {
								registeredCallbackItem.onReceiveMessage(fromParcel);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
					remoteCallbackList.finishBroadcast();
				}
			}, 5000, 10000, TimeUnit.MILLISECONDS);
		}

		@Override
		public void disconnect() throws RemoteException {

			isConnect = false;
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(RemoteService.this, "断开了", Toast.LENGTH_SHORT).show();
				}
			});
			//取消定时服务
			future.cancel(true);
		}

		@Override
		public boolean isConnect() throws RemoteException {
			return isConnect;
		}
	};

	//private List<IMessageReceiveListener> iMessageReceiveListeners = new ArrayList<>();
	//RemoteCallbackList 保证了跨进程 ,对象是同一个
	private RemoteCallbackList<IMessageReceiveListener> remoteCallbackList = new RemoteCallbackList<>();
	/**
	 * 消息服务
	 */
	private IMessageService iMessageService = new IMessageService.Stub() {
		@Override
		public void sendMessage(final Message message) throws RemoteException {

			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(RemoteService.this, message.getContent(), Toast.LENGTH_SHORT).show();
				}
			});

			if (isConnect) {
				message.setSendSuccess(true);
			} else {
				message.setSendSuccess(false);
			}
		}

		@Override
		public void registerMessageListener(IMessageReceiveListener messageReceiveListener) throws RemoteException {
			if (messageReceiveListener != null) {

				//iMessageReceiveListeners.add(messageReceiveListener);
				remoteCallbackList.register(messageReceiveListener);
			}
		}

		/**
		 * 注册进来的和需要取消注册的不是同一个 ,进程不一样,不同进程传递数据都是通过序列化和反序列化的 ,反序列化后就和开始传进来的不一样了
		 * @param messageReceiveListener
		 * @throws RemoteException
		 */
		@Override
		public void unRegisterMessageListener(IMessageReceiveListener messageReceiveListener) throws RemoteException {
			if (messageReceiveListener != null) {
				//iMessageReceiveListeners.remove(messageReceiveListener);
				remoteCallbackList.unregister(messageReceiveListener);
			}
		}
	};

	private ScheduledFuture<?> future;

	public RemoteService() {
	}

	/**
	 * 管理多个服务
	 */
	private IServiceManager iServiceManager = new IServiceManager.Stub() {
		@Override
		public IBinder getService(String serviceName) throws RemoteException {
			System.out.println("IServiceManager -- serviceName = " + serviceName);
			if (IConnectionService.class.getSimpleName().equals(serviceName)) {
				return iConnectionService.asBinder();
			} else if (IMessageService.class.getSimpleName().equals(serviceName)) {
				return iMessageService.asBinder();

			} else if (Messenger.class.getSimpleName().equals(serviceName)) {
				return messenger.getBinder();
			} else {
				return null;
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {

		//return iConnectionService.asBinder();//只能返回一个服务 ,无法使用messageservice等

		return iServiceManager.asBinder();
	}

	ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	@Override
	public void onCreate() {
		super.onCreate();
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
	}
}
