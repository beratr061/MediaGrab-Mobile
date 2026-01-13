package com.berat.mediagrab.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Utility class for network-related operations.
 */
object NetworkUtils {

    /**
     * Check if device has active network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Check if device is connected via WiFi.
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }

    /**
     * Check if device is connected via mobile data.
     */
    fun isMobileDataConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }

    /**
     * Get current connection type as string.
     */
    fun getConnectionType(context: Context): String {
        return when {
            isWifiConnected(context) -> "WiFi"
            isMobileDataConnected(context) -> "Mobile Data"
            isNetworkAvailable(context) -> "Other"
            else -> "None"
        }
    }

    /**
     * Check if download is allowed based on settings.
     * @param context Application context
     * @param wifiOnly If true, only allow downloads on WiFi
     */
    fun isDownloadAllowed(context: Context, wifiOnly: Boolean): Boolean {
        if (!isNetworkAvailable(context)) return false
        if (wifiOnly && !isWifiConnected(context)) return false
        return true
    }
}
