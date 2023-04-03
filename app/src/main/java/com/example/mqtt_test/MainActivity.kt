package com.example.mqtt_test

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.TextView
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import java.security.KeyPair
import java.security.cert.Certificate
import java.util.regex.Pattern

class MyCertStore : Application(){
    var signedCertificate: Certificate? = null

    companion object {
        private var instance : MyCertStore? = null
        fun  getInstance(): MyCertStore {
            if (instance == null)
                instance = MyCertStore()
            return instance!!
        }
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var httpServer: AndroidHttpServer
    private lateinit var inputEditText: TextView
    private lateinit var pubKeyText: TextView
    private lateinit var statusText: TextView
    private val keyPairAlias = "ec_keypair"

    private val port = 8080
    private lateinit var mqttUrl: String
    private lateinit var keypair: KeyPair

    private val keyPairGenerator: KeyPairGenerator by lazy {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            keyPairAlias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .build()
        keyPairGenerator.initialize(spec)
        keyPairGenerator
    }

    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_drawer)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_certificate_provisioning -> {
                    closeCurrentFragment()
                }
                R.id.menu_mqtt_connection -> {
                    if (mqttUrl.isEmpty()) {
                        return@setNavigationItemSelectedListener false
                    }
                    val mqttFragment = mqttConnectionFragment.newInstance(keyPairAlias, mqttUrl)
                    replaceFragment(mqttFragment)
                }

            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        inputEditText = findViewById(R.id.textedit1)
        val networkMonitor = NetworkMonitor(this) { ipAddress ->
            inputEditText.text = buildString {
        append(ipAddress)
        append(":")
        append(port)
    }
        }
        // Register the network monitor to notify the self IP address to Android device screen
        networkMonitor.register()

        // Start the HTTP server on port 8080
        httpServer = AndroidHttpServer(this, GenKeyfunction, GenCertfunction, SubmitUrl, port)
        httpServer.startServer()
    }

    private val GenKeyfunction: ClientGenKeyCallbackFunction = {

        statusText = findViewById(R.id.textedit3)
        keypair = keyPairGenerator.generateKeyPair()

        val CertificateRequest = generateCsrPem(keypair).
        replace(Regex("[\u0000-\u0019]+"), "")

        pubKeyText = findViewById(R.id.textedit2)
        pubKeyText.text = CertificateRequest

        "{\"result\":\"$CertificateRequest\"}"
    }

    private val GenCertfunction: ClientCertCallbackFunction  = GenCertfunction@{ Cert ->

        val MyCertStore = MyCertStore.getInstance()
        val tempCert = pemToCertificate(Cert)
        MyCertStore.signedCertificate = tempCert
        val caCertificate = provideCACertificate()

        createKeyStore( tempCert, caCertificate)


        val androidKeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        androidKeyStore.setCertificateEntry("SignedCertAlias", pemToCertificate(Cert))
        androidKeyStore.setCertificateEntry("CACertAlias",provideCACertificate())

        statusText = findViewById(R.id.textedit3)
        statusText.text = "Certificate is stored"

        return@GenCertfunction "{\"result\":\"OK\"}"
    }

    private val SubmitUrl: submitUrlFunction  = submitUrlFunction@{ url ->

        Log.d("Url", url)
        val hostnameRegex = "^(?=.{1,253})(?:(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.?)+[A-Za-z]{2,6}$"
        val pattern = Pattern.compile(hostnameRegex)
        val matcher = pattern.matcher(url)
        if (matcher.matches()) {
            mqttUrl = url
            return@submitUrlFunction "{\"result\":\"OK\"}"
        }
        else
            return@submitUrlFunction "{\"result\":\"NG\"}"
    }



    override fun onDestroy() {
        super.onDestroy()
        // Stop the HTTP server when the activity is destroyed
        httpServer.stopServer()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun closeCurrentFragment() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}

