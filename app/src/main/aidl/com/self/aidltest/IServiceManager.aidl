// IServiceManager.aidl
package com.self.aidltest;

// Declare any non-default types here with import statements

interface IServiceManager {
   IBinder getService(String serviceName);
}
