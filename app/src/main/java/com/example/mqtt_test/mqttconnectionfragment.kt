package com.example.mqtt_test

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

const val clientId = "test-android-client"

class mqttConnectionFragment : Fragment() {
        private lateinit var mqttClient: MqttAndroidClient
        private lateinit var keyPairAlias: String
        private lateinit var mqttUrl: String

        private var connectButtonEnabled = true
        private val connectButtonHandler = Handler(Looper.getMainLooper())
        private val connectButtonRunnable = Runnable {
            connectButtonEnabled = true
        }

    companion object {
        private const val ARG_PARAM1 :String = "param1"
        private const val ARG_PARAM2 :String= "param2"


        fun newInstance(param1: String, param2: String): mqttConnectionFragment {
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            val fragment = mqttConnectionFragment()
            fragment.arguments = args
            return fragment
        }
    }

        override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mqtt_connection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            keyPairAlias = it.getString(ARG_PARAM1).toString()
            mqttUrl = it.getString(ARG_PARAM2).toString()
        }

        val serverURI = "ssl://$mqttUrl:8883"
        mqttClient = MqttAndroidClient(activity, serverURI, clientId )
        //displays the mqtt url
        val mqttUrlText = view.findViewById<android.widget.TextView>(R.id.textView5)
        mqttUrlText.text = serverURI



        val connectButton: Button = view.findViewById(R.id.connectbutton)
        connectButton.setOnClickListener {
            if (connectButtonEnabled) {
                connectToMqtt(view)
                connectButtonEnabled = false
                connectButtonHandler.postDelayed(connectButtonRunnable, 1000)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            mqttClient.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        connectButtonHandler.removeCallbacks(connectButtonRunnable)
    }

    private fun connectToMqtt(view: View) {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        mqttConnectOptions.isAutomaticReconnect = false

        try {
            //debugging
            //setupPahoLogging()
            val MyCertStore = MyCertStore.getInstance()
            val signedCertificate = MyCertStore.signedCertificate
            val caCertificate = provideCACertificate()

            if (signedCertificate != null) {
                val keyStore = createKeyStore( signedCertificate, caCertificate)
            }
            else {
                Log.d("signedCertificate", "signedCertificate not found")
            }

            val keyManager = CustomX509ExtendedKeyManager(keyPairAlias)

            val caPemStrings = provideRootCertificates()
            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                for ((index, caPemString) in caPemStrings.withIndex()) {
                    val caInputStream: InputStream = ByteArrayInputStream(caPemString.toByteArray(Charsets.UTF_8))
                    val caCert = CertificateFactory.getInstance("X.509").generateCertificate(caInputStream)
                    setCertificateEntry("ca$index", caCert)
                }
            }

            val trustManagerFactory = TrustManagerFactory.getInstance("X509")
            trustManagerFactory.init(caKeyStore)


            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(arrayOf(keyManager), trustManagerFactory.trustManagers, null)
            // Create SniSSLSocketFactory
            val sslSocketFactory = sslContext.socketFactory
            val sniHostname = mqttUrl
            val sniSslSocketFactory = SniSSLSocketFactory(sslSocketFactory, sniHostname)

            // Set SniSSLSocketFactory in MqttConnectOptions
            mqttConnectOptions.socketFactory = sniSslSocketFactory

            mqttClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Connection successful
                    // You can perform your MQTT operations here
                    Log.d("MQTT","MQTT connected successfully!")

                    val connectButton: Button = view.findViewById(R.id.connectbutton)
                    connectButton.isEnabled = false

                    // Subscribe to a topic
                    val subscriptionTopic = "your/subscription/topic"
                    val topictext = view.findViewById(R.id.textView7) as TextView
                    topictext.text = subscriptionTopic

                    mqttClient.subscribe(subscriptionTopic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MQTT", "Subscribed to topic successfully!")
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.d("MQTT", "Failed to subscribe to topic", exception)
                        }
                    }, object : IMqttMessageListener {
                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            Log.d("MQTT", "Message received from topic: $topic, payload: ${message?.toString()}")
                            var subscribed = view.findViewById(R.id.textView9) as TextView
                            subscribed.text = message?.toString()
                        }
                    })

                    val publishButton: Button = view.findViewById(R.id.publish)
                    val publishcontents: EditText = view.findViewById(R.id.textView13)
                    publishButton.setOnClickListener {

                        val topic = "your/subscription/topic"
                        val messagePayload = publishcontents.text.toString()
                        val message = MqttMessage(messagePayload.toByteArray())
                        message.qos = 0
                        try {
                            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    Log.d("MQTT", "Message published successfully!")
                                    publishcontents.text.clear()
                                }

                                override fun onFailure(
                                    asyncActionToken: IMqttToken?,
                                    exception: Throwable?
                                ) {
                                    Log.d("MQTT", "Failed to publish message", exception)
                                }
                            })
                        } catch (e: MqttException) {
                            Log.e("MQTT", "Error publishing message", e)
                        }
                    }

                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    // Connection failed
                    Log.d("MQTT","MQTT connection failed!")
                    exception?.printStackTrace()
                    onDestroyView()
                }
            })


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

class SniSSLSocketFactory(private val sslSocketFactory: SSLSocketFactory, private val sniHostname: String) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String>  {
        return sslSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> = sslSocketFactory.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val sslSocket = sslSocketFactory.createSocket(socket, sniHostname, port, autoClose) as SSLSocket
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
            serverNames = listOf(SNIHostName(sniHostname))
        }
        return sslSocket
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val sslSocket = sslSocketFactory.createSocket( host, port) as SSLSocket
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
        serverNames = listOf(SNIHostName(sniHostname))
    }
    return sslSocket
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        val sslSocket = sslSocketFactory.createSocket( host, port, localHost, localPort) as SSLSocket
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
        serverNames = listOf(SNIHostName(sniHostname))
    }
    return sslSocket
    }

    @Throws(IOException::class)
        override fun createSocket(host: InetAddress, port: Int): Socket {
        val sslSocket = sslSocketFactory.createSocket( host, port) as SSLSocket
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
        serverNames = listOf(SNIHostName(sniHostname))
        }
    return sslSocket
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
       val sslSocket = sslSocketFactory.createSocket( address, port, localAddress,  localPort) as SSLSocket
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
        serverNames = listOf(SNIHostName(sniHostname))
    }
    return sslSocket
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        val sslSocket = sslSocketFactory.createSocket() as SSLSocket
        val defaultCipherSuites = sslSocket.enabledCipherSuites
        sslSocket.enabledCipherSuites = defaultCipherSuites.filterNot { it.contains("RSA") }.toTypedArray()
        sslSocket.sslParameters = sslSocket.sslParameters.apply {
            serverNames = listOf(SNIHostName(sniHostname)
            )
        }
        sslSocket.enabledProtocols = arrayOf("TLSv1.2")
        return sslSocket
    }
}

class CustomX509ExtendedKeyManager(private val alias: String) : X509ExtendedKeyManager() {

    private val androidKeyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    val MyCertStore = com.example.mqtt_test.MyCertStore.getInstance()
    private var signedClientCertificate = MyCertStore.signedCertificate as X509Certificate
    private var caCertificate = provideCACertificate() as X509Certificate

    private val privateKeyEntry = androidKeyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry

    override fun chooseClientAlias(keyType: Array<String>?, issuers: Array<Principal>?, socket: Socket?): String {
        return alias
    }

    override fun chooseServerAlias(keyType: String?, issuers: Array<Principal>?, socket: Socket?): String? {
        return null
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        return arrayOf( signedClientCertificate ,caCertificate )
    }

    override fun getClientAliases(keyType: String?, issuers: Array<Principal>?): Array<String> {
        return arrayOf(alias)
    }

    override fun getServerAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? {
        return null
    }

    override fun getPrivateKey(alias: String?): PrivateKey {
        return privateKeyEntry.privateKey
    }
}

