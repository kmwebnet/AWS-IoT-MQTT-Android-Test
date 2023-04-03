package com.example.mqtt_test

import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory


fun generateCsrPem(keyPair: KeyPair): String {
    val subject = X500Name("CN=$clientId, O=Example Organization, L=City, ST=State, C=US")
    val csrBuilder = JcaPKCS10CertificationRequestBuilder(subject, keyPair.public)

    val contentSigner = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
    val csr = csrBuilder.build(contentSigner)

    return writeCsrPem(csr)
}

fun writeCsrPem(csr: PKCS10CertificationRequest): String {
    val stringWriter = StringWriter()
    JcaPEMWriter(stringWriter).use { pemWriter ->
        pemWriter.writeObject(csr)
    }
    return stringWriter.toString()
}

fun pemToCertificate(pemString: String): Certificate {
    val cleanedPem = pemString.replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .replace("\n", "")
    val decodedPem = Base64.decode(cleanedPem, Base64.DEFAULT)
    val inputStream = ByteArrayInputStream(decodedPem)
    val certificateFactory = CertificateFactory.getInstance("X.509")
    return certificateFactory.generateCertificate(inputStream)
}

fun createKeyStore(signedCertificate: Certificate, caCertificate: Certificate): KeyStore {
    return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("SignedCertAlias", signedCertificate)
        setCertificateEntry("CACertAlias", caCertificate)
    }
}

fun provideCACertificate(): Certificate {
    val caCertificatePem = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBgzCCASigAwIBAgIQXBTO2eoJ9x2mE5GhAQ61gzAKBggqhkjOPQQDAjApMREw\n" +
            "DwYDVQQKDAh0ZXN0Y29ycDEUMBIGA1UEAwwLdGVzdHByb2plY3QwHhcNMjAxMDA4\n" +
            "MDI0NDA5WhcNNDUxMDA4MDI0NDA5WjApMREwDwYDVQQKDAh0ZXN0Y29ycDEUMBIG\n" +
            "A1UEAwwLdGVzdHByb2plY3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAS2VF/I\n" +
            "Lqi7762N32uGhs4gEz+W294cUhN6nD4a7K91jrboBi5nF+S6rSSpWrG9j4FdR08Y\n" +
            "PCUZ5t1wkWicAsc6ozIwMDAdBgNVHQ4EFgQULWLycP8qydM4BwUGTPkJaQSKkSow\n" +
            "DwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEA9hAO1le9uWxVboTq\n" +
            "1SCX9sb806u3HcyZKR9Hh4bcFZcCIQC2iDENAsOSGCHeHoOr5vh14TXbnsJXwBcA\n" +
            "n3x0XG22YA==\n" +
            "-----END CERTIFICATE-----\n".trimIndent()

    val certificateFactory = CertificateFactory.getInstance("X.509")
    return certificateFactory.generateCertificate(caCertificatePem.byteInputStream())
}

fun provideRootCertificates(): List<String> {
    return  listOf (
        "-----BEGIN CERTIFICATE-----\n" +
                "MIIBtjCCAVugAwIBAgITBmyf1XSXNmY/Owua2eiedgPySjAKBggqhkjOPQQDAjA5\n" +
                "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6b24g\n" +
                "Um9vdCBDQSAzMB4XDTE1MDUyNjAwMDAwMFoXDTQwMDUyNjAwMDAwMFowOTELMAkG\n" +
                "A1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJvb3Qg\n" +
                "Q0EgMzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABCmXp8ZBf8ANm+gBG1bG8lKl\n" +
                "ui2yEujSLtf6ycXYqm0fc4E7O5hrOXwzpcVOho6AF2hiRVd9RFgdszflZwjrZt6j\n" +
                "QjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMB0GA1UdDgQWBBSr\n" +
                "ttvXBp43rDCGB5Fwx5zEGbF4wDAKBggqhkjOPQQDAgNJADBGAiEA4IWSoxe3jfkr\n" +
                "BqWTrBqYaGFy+uGh0PsceGCmQ5nFuMQCIQCcAu/xlJyzlvnrxir4tiz+OpAUFteM\n" +
                "YyRIHN8wfdVoOw==\n" +
                "-----END CERTIFICATE-----\n",


        "-----BEGIN CERTIFICATE-----\n" +
                "MIIDQTCCAimgAwIBAgITBmyfz5m/jAo54vB4ikPmljZbyjANBgkqhkiG9w0BAQsF\n" +
                "ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6\n" +
                "b24gUm9vdCBDQSAxMB4XDTE1MDUyNjAwMDAwMFoXDTM4MDExNzAwMDAwMFowOTEL\n" +
                "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv\n" +
                "b3QgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJ4gHHKeNXj\n" +
                "ca9HgFB0fW7Y14h29Jlo91ghYPl0hAEvrAIthtOgQ3pOsqTQNroBvo3bSMgHFzZM\n" +
                "9O6II8c+6zf1tRn4SWiw3te5djgdYZ6k/oI2peVKVuRF4fn9tBb6dNqcmzU5L/qw\n" +
                "IFAGbHrQgLKm+a/sRxmPUDgH3KKHOVj4utWp+UhnMJbulHheb4mjUcAwhmahRWa6\n" +
                "VOujw5H5SNz/0egwLX0tdHA114gk957EWW67c4cX8jJGKLhD+rcdqsq08p8kDi1L\n" +
                "93FcXmn/6pUCyziKrlA4b9v7LWIbxcceVOF34GfID5yHI9Y/QCB/IIDEgEw+OyQm\n" +
                "jgSubJrIqg0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMC\n" +
                "AYYwHQYDVR0OBBYEFIQYzIU07LwMlJQuCFmcx7IQTgoIMA0GCSqGSIb3DQEBCwUA\n" +
                "A4IBAQCY8jdaQZChGsV2USggNiMOruYou6r4lK5IpDB/G/wkjUu0yKGX9rbxenDI\n" +
                "U5PMCCjjmCXPI6T53iHTfIUJrU6adTrCC2qJeHZERxhlbI1Bjjt/msv0tadQ1wUs\n" +
                "N+gDS63pYaACbvXy8MWy7Vu33PqUXHeeE6V/Uq2V8viTO96LXFvKWlJbYK8U90vv\n" +
                "o/ufQJVtMVT8QtPHRh8jrdkPSHCa2XV4cdFyQzR1bldZwgJcJmApzyMZFo6IQ6XU\n" +
                "5MsI+yMRQ+hDKXJioaldXgjUkK642M4UwtBV8ob2xJNDd2ZhwLnoQdeXeGADbkpy\n" +
                "rqXRfboQnoZsG4q5WTP468SQvvG5\n" +
                "-----END CERTIFICATE-----",

        "-----BEGIN CERTIFICATE-----\n" +
                "MIIDxzCCAq+gAwIBAgITBn+USjDPzE90tfUwblTTt74KwzANBgkqhkiG9w0BAQsF\n" +
                "ADCBmDELMAkGA1UEBhMCVVMxEDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNj\n" +
                "b3R0c2RhbGUxJTAjBgNVBAoTHFN0YXJmaWVsZCBUZWNobm9sb2dpZXMsIEluYy4x\n" +
                "OzA5BgNVBAMTMlN0YXJmaWVsZCBTZXJ2aWNlcyBSb290IENlcnRpZmljYXRlIEF1\n" +
                "dGhvcml0eSAtIEcyMB4XDTE1MDUyNTEyMDAwMFoXDTM3MTIzMTAxMDAwMFowOTEL\n" +
                "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv\n" +
                "b3QgQ0EgMzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABCmXp8ZBf8ANm+gBG1bG\n" +
                "8lKlui2yEujSLtf6ycXYqm0fc4E7O5hrOXwzpcVOho6AF2hiRVd9RFgdszflZwjr\n" +
                "Zt6jggExMIIBLTAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAdBgNV\n" +
                "HQ4EFgQUq7bb1waeN6wwhgeRcMecxBmxeMAwHwYDVR0jBBgwFoAUnF8A36oB1zAr\n" +
                "OIiiuG1KnPIRkYMweAYIKwYBBQUHAQEEbDBqMC4GCCsGAQUFBzABhiJodHRwOi8v\n" +
                "b2NzcC5yb290ZzIuYW1hem9udHJ1c3QuY29tMDgGCCsGAQUFBzAChixodHRwOi8v\n" +
                "Y3J0LnJvb3RnMi5hbWF6b250cnVzdC5jb20vcm9vdGcyLmNlcjA9BgNVHR8ENjA0\n" +
                "MDKgMKAuhixodHRwOi8vY3JsLnJvb3RnMi5hbWF6b250cnVzdC5jb20vcm9vdGcy\n" +
                "LmNybDARBgNVHSAECjAIMAYGBFUdIAAwDQYJKoZIhvcNAQELBQADggEBAG5Z+hfC\n" +
                "ycAuzqKJ4ClKK5VZUqjH4jYSzu3UVOLvHzoYk0rIVqFjjBJwa/L5MreP219CIdIX\n" +
                "di3s9at8ZZR+tKsD4T02ZqO/43FQqnSkzF/G+OxYo3malxhuT9j7bNiA9WkCuqVV\n" +
                "bUncQt79aEjDKht7viKQnoybiHB6dtWAXMNObcCviQMqTcoV+sQOpKJMvQanxUk+\n" +
                "fKQLGKlkpu9zKNr2kWdx874JVpYhDCUzW2RX9TtQ04VT6J0xTEew55OJj02jNxHu\n" +
                "Gijg0YLZtWLNWEXkNDkVpZozXbhuTM6GJKhwLn2rmgRgtFTWUDbeq3YE/7NHu+3a\n" +
                "LOL51JEnEI+4hac=\n" +
                "-----END CERTIFICATE-----\n",

        "-----BEGIN CERTIFICATE-----\n" +
                "MIIEkjCCA3qgAwIBAgITBn+USionzfP6wq4rAfkI7rnExjANBgkqhkiG9w0BAQsF\n" +
                "ADCBmDELMAkGA1UEBhMCVVMxEDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNj\n" +
                "b3R0c2RhbGUxJTAjBgNVBAoTHFN0YXJmaWVsZCBUZWNobm9sb2dpZXMsIEluYy4x\n" +
                "OzA5BgNVBAMTMlN0YXJmaWVsZCBTZXJ2aWNlcyBSb290IENlcnRpZmljYXRlIEF1\n" +
                "dGhvcml0eSAtIEcyMB4XDTE1MDUyNTEyMDAwMFoXDTM3MTIzMTAxMDAwMFowOTEL\n" +
                "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEZMBcGA1UEAxMQQW1hem9uIFJv\n" +
                "b3QgQ0EgMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJ4gHHKeNXj\n" +
                "ca9HgFB0fW7Y14h29Jlo91ghYPl0hAEvrAIthtOgQ3pOsqTQNroBvo3bSMgHFzZM\n" +
                "9O6II8c+6zf1tRn4SWiw3te5djgdYZ6k/oI2peVKVuRF4fn9tBb6dNqcmzU5L/qw\n" +
                "IFAGbHrQgLKm+a/sRxmPUDgH3KKHOVj4utWp+UhnMJbulHheb4mjUcAwhmahRWa6\n" +
                "VOujw5H5SNz/0egwLX0tdHA114gk957EWW67c4cX8jJGKLhD+rcdqsq08p8kDi1L\n" +
                "93FcXmn/6pUCyziKrlA4b9v7LWIbxcceVOF34GfID5yHI9Y/QCB/IIDEgEw+OyQm\n" +
                "jgSubJrIqg0CAwEAAaOCATEwggEtMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/\n" +
                "BAQDAgGGMB0GA1UdDgQWBBSEGMyFNOy8DJSULghZnMeyEE4KCDAfBgNVHSMEGDAW\n" +
                "gBScXwDfqgHXMCs4iKK4bUqc8hGRgzB4BggrBgEFBQcBAQRsMGowLgYIKwYBBQUH\n" +
                "MAGGImh0dHA6Ly9vY3NwLnJvb3RnMi5hbWF6b250cnVzdC5jb20wOAYIKwYBBQUH\n" +
                "MAKGLGh0dHA6Ly9jcnQucm9vdGcyLmFtYXpvbnRydXN0LmNvbS9yb290ZzIuY2Vy\n" +
                "MD0GA1UdHwQ2MDQwMqAwoC6GLGh0dHA6Ly9jcmwucm9vdGcyLmFtYXpvbnRydXN0\n" +
                "LmNvbS9yb290ZzIuY3JsMBEGA1UdIAQKMAgwBgYEVR0gADANBgkqhkiG9w0BAQsF\n" +
                "AAOCAQEAYjdCXLwQtT6LLOkMm2xF4gcAevnFWAu5CIw+7bMlPLVvUOTNNWqnkzSW\n" +
                "MiGpSESrnO09tKpzbeR/FoCJbM8oAxiDR3mjEH4wW6w7sGDgd9QIpuEdfF7Au/ma\n" +
                "eyKdpwAJfqxGF4PcnCZXmTA5YpaP7dreqsXMGz7KQ2hsVxa81Q4gLv7/wmpdLqBK\n" +
                "bRRYh5TmOTFffHPLkIhqhBGWJ6bt2YFGpn6jcgAKUj6DiAdjd4lpFw85hdKrCEVN\n" +
                "0FE6/V1dN2RMfjCyVSRCnTawXZwXgWHxyvkQAiSr6w10kY17RSlQOYiypok1JR4U\n" +
                "akcjMS9cmvqtmg5iUaQqqcT5NJ0hGA==\n" +
                "-----END CERTIFICATE-----\n" ,



        )
}