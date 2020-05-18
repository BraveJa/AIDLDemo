// IMessageService.aidl
package com.self.aidltest;
import com.self.aidltest.enty.Message;
import com.self.aidltest.IMessageReceiveListener;

// Declare any non-default types here with import statements
//消息服务
interface IMessageService{

    void sendMessage(in Message message);

    void registerMessageListener(IMessageReceiveListener messageReceiveListener);

    void unRegisterMessageListener(IMessageReceiveListener messageReceiveListener);
}
