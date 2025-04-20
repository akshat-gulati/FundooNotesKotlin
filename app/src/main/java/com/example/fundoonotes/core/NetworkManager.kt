package com.example.fundoonotes.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

class NetworkManager(context: Context) {

    fun isOnline(context: Context): Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capabilities?.let {
            when{
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Toast.makeText(context, "Connected To Mobile Data", Toast.LENGTH_SHORT).show()
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Toast.makeText(context, "Connected To Wifi", Toast.LENGTH_SHORT).show()
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Toast.makeText(context, "Connected To Ethernet", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return false
    }
}