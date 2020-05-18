package com.self.aidltest;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

import com.self.aidltest.enty.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

	private Handler handler = new Handler();
	private IConnectionService connectionServiceProxy;
	private IMessageService messageService;

	private IServiceManager iServiceManager;

	private Messenger messengerProxy;
	@SuppressLint("HandlerLeak")
	private Handler clientHandler = new Handler() {
		@Override
		public void handleMessage(@NonNull android.os.Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			data.setClassLoader(Message.class.getClassLoader());//有可能会反序列化出问题,导致类找不到
			final Message message = data.getParcelable("reply");
			postDelayed(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "handleMessage : " + message.getContent(), Toast.LENGTH_SHORT).show();
				}
			}, 3000);

		}
	};
	private Messenger clientMessenger = new Messenger(clientHandler);

	private IMessageReceiveListener iMessageReceiveListener = new IMessageReceiveListener.Stub() {
		@Override
		public void onReceiveMessage(final Message message) throws RemoteException {
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "MainActivity收到消息 :" + message.getContent(), Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Intent intent = new Intent(this, RemoteService.class);

		findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (connectionServiceProxy != null) {
					try {
						connectionServiceProxy.connect();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});

		findViewById(R.id.disconnect).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (connectionServiceProxy != null) {
					try {
						connectionServiceProxy.disconnect();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});

		findViewById(R.id.isconnect).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (connectionServiceProxy != null) {
					try {
						boolean connect = connectionServiceProxy.isConnect();
						Toast.makeText(MainActivity.this, "连接状态 : " + connect, Toast.LENGTH_SHORT).show();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});

		findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Message fromParcel = Message.CREATOR.createFromParcel(Parcel.obtain());
				fromParcel.setContent("MainActivity发送的消息");
				try {
					messageService.sendMessage(fromParcel);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		findViewById(R.id.regise).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					messageService.registerMessageListener(iMessageReceiveListener);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		findViewById(R.id.unregise).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					messageService.unRegisterMessageListener(iMessageReceiveListener);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		findViewById(R.id.messengerProxy).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Message fromParcel = Message.CREATOR.createFromParcel(Parcel.obtain());
					fromParcel.setContent("messengerProxy发送的消息");
					android.os.Message message = new android.os.Message();
					message.replyTo = clientMessenger;//用于接收远端进程返回的结果
					Bundle bundle = new Bundle();
					bundle.putParcelable("fromParcel", fromParcel);
					message.setData(bundle);
					messengerProxy.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		bindService(intent, new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {

				//	connectionServiceProxy = IConnectionService.Stub.asInterface(service);
				iServiceManager = IServiceManager.Stub.asInterface(service);
				try {
					IBinder connectionServiceIBinder = iServiceManager.getService(IConnectionService.class.getSimpleName());
					IBinder messageServiceIBinder = iServiceManager.getService(IMessageService.class.getSimpleName());
					IBinder messengerIBinder = iServiceManager.getService(Messenger.class.getSimpleName());
					connectionServiceProxy = IConnectionService.Stub.asInterface(connectionServiceIBinder);
					messageService = IMessageService.Stub.asInterface(messageServiceIBinder);
					messageService = IMessageService.Stub.asInterface(messageServiceIBinder);
					messengerProxy = new Messenger(messengerIBinder);

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {

			}
		}, BIND_AUTO_CREATE);
	}
}
