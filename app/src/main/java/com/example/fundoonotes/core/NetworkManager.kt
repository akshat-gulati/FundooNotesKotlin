package com.example.fundoonotes.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkManager(private val context: Context) {
    private val _networkState = MutableStateFlow(isOnline())
    val networkState: StateFlow<Boolean> = _networkState.asStateFlow()

    init {
        setupNetworkMonitoring()
    }

    private fun setupNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = true
            }

            override fun onLost(network: Network) {
                _networkState.value = false
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    // Keeping this for one-time checks if I need it
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        return capabilities?.let {
            it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }
}