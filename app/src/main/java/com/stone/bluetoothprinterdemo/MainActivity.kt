package com.stone.bluetoothprinterdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

private const val PERMISSION_BLUETOOTH = 1
private const val PERMISSION_BLUETOOTH_ADMIN = 2
private const val PERMISSION_BLUETOOTH_CONNECT = 3
private const val PERMISSION_BLUETOOTH_SCAN = 4

class MainActivity : AppCompatActivity() {

    private val applicationUUID = UUID
        .fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothSocket: BluetoothSocket
    private lateinit var mSelectedDevice: BluetoothDevice
    private lateinit var mBluetoothConnectProgressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    private val registerForBluetoothEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            getDevicesList()
        } else
            Toast.makeText(
                applicationContext,
                "Need bluetooth to connect the printer!!",
                Toast.LENGTH_SHORT
            ).show()
    }

    fun print(v: View) {

        if (this::mBluetoothSocket.isInitialized) {

            thread(name = "print", start = true) {
                try {
                    val printer = mBluetoothSocket.outputStream
                    printer.write("hello\n".toByteArray())
                } catch (e: Exception) {
                    Log.i("Goooo", e.toString())
                }
            }


        } else
            Toast.makeText(
                applicationContext,
                "Please connect a printer first!",
                Toast.LENGTH_SHORT
            ).show()
    }



    private fun closeSocket(nOpenSocket: BluetoothSocket) {
        try {
            nOpenSocket.close()
            Log.d("Goo", "SocketClosed")
        } catch (ex: IOException) {
            Log.d("Goo", "CouldNotCloseSocket")
        }
    }


    @SuppressLint("MissingPermission")
    private fun preparePrinter() {
//        mBluetoothSocket=mSelectedDevice.createRfcommSocketToServiceRecord(applicationUUID)
        mBluetoothConnectProgressDialog = ProgressDialog.show(
            this,
            "Connecting...", mSelectedDevice.name + " : "
                    + mSelectedDevice.address, true, false
        )
       thread(name = "connect", start = true) {
           try {
               mBluetoothSocket = mSelectedDevice
                   .createRfcommSocketToServiceRecord(applicationUUID)
               mBluetoothAdapter.cancelDiscovery()
               mBluetoothSocket.connect()
               mBluetoothConnectProgressDialog.dismiss()

           } catch (eConnectException: IOException) {
               Log.d("Gooo", "CouldNotConnectToSocket", eConnectException)
               closeSocket(mBluetoothSocket)
           }
       }
    }

    fun connect(view: View) {
        if (checkPermission()) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBluetoothAdapter = mBluetoothManager.adapter
            if (!mBluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE
                )
                registerForBluetoothEnable.launch(enableBtIntent)
            } else {
                val deviceList = getDevicesList()
                if (deviceList.isNotEmpty()) {
                    val items = arrayOfNulls<String>(deviceList.size)
                    deviceList.forEachIndexed { index, item ->
                        items[index] = (item.name)
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Select Printer")
                        .setItems(items) { _, i ->
                            mSelectedDevice = deviceList[i]
                            Toast.makeText(applicationContext, "${items[i]}", Toast.LENGTH_SHORT)
                                .show()
                            val button = findViewById<Button>(R.id.connect)
                            button.text = items[i]
                            preparePrinter()
//                            Thread(this).start()
                        }
                        .setCancelable(false)
                        .show()


                } else {
                    Toast.makeText(applicationContext, "No Devices Found!!", Toast.LENGTH_SHORT)
                        .show()
                }


            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDevicesList(): List<BluetoothDevice> {
        val pairedDevices = mBluetoothAdapter.bondedDevices
        val devicesList: MutableList<BluetoothDevice> = mutableListOf()
        if (pairedDevices.size > 0) {
            pairedDevices.forEach { device ->
                devicesList.add(device)
            }
        }
        return devicesList

    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                PERMISSION_BLUETOOTH
            )
            Log.i("Goooo", "denined")
//      ActivityCompat.requestPermissions(this);
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            return true
        }
        return false
    }


}