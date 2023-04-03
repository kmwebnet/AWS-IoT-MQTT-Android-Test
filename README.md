### AWS IoT MQTT Client mutual tls authentication example on Android

This provides an example of how to use the AWS IoT MQTT Client on Android to connect to AWS IoT using mutual TLS authentication by using Android Keystore.

### Scenario
1, Automated certificate registration using AWS IoT Just In time Registration.  
2, Protect the private key in the Android Keystore and never retrieve and process the private key itself.  
3, Provide an interface that can be provisioned to sign with a self-certificate authority and write the certificate back to the device.  

### Prerequisites
Android Studio Electric Eel (1.0.0) or later

### Setup
1. setup self signed CA certificate and private key
2. register them by using verification code on AWS IoT.
3. activate the CA certificate .please refer [AWS IoT auto registration](https://docs.aws.amazon.com/ja_jp/iot/latest/developerguide/auto-register-device-cert.html)
4. Create Lambda function to register device certificate.
sample code includes as index.mjs
5. Create IoT policy and attach Permissions policies and Permissions boundary - (set) to allow the Lambda function to register device certificate.
sample policy includes as JITRPolicy

### Build
1. Open the project in Android Studio
2. Build the project
3. Install the app on your Android device

### How to use
1. Launch the app
2. Provision the device to access AWS IoT using WEB UI provided by the app
3. Connect to AWS IoT using the provisioned credentials by clicking the Connect button
4. Publish a message to the topic by clicking the Publish button
5. Subscribe to the topic to check the message

### movie
https://youtu.be/p7QMLSj_XoM

### License
If a license is stated in the source code, that license is applied, otherwise the MIT license is applied.






