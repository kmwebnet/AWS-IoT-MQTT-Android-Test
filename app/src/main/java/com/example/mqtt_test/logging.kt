package com.example.mqtt_test

import android.util.Log
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

fun setupPahoLogging() {
    // Paho MQTT client logger
    val logger = Logger.getLogger("org.eclipse.paho.client.mqttv3")

    // Clear existing handlers
    for (handler in logger.handlers) {
        logger.removeHandler(handler)
    }

    // Set logging level
    logger.level = Level.ALL

    // Add custom handler
    val handler = object : java.util.logging.Handler() {
        override fun publish(record: LogRecord) {
            val message = String.format("%s: %s", record.level, record.message)
            Log.d("PahoMqtt", message)
        }

        override fun flush() {}

        override fun close() {}
    }

    handler.level = Level.ALL
    logger.addHandler(handler)
}