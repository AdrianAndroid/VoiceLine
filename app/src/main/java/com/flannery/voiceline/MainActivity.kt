package com.flannery.voiceline

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), Runnable {
    private var mMediaRecorder: MediaRecorder? = null
    private var isAlive = true
    private var voiceLineView: VoiceLineView? = null
    private var stopVoice: Button? = null
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (mMediaRecorder == null) return
            val ratio =
                mMediaRecorder!!.maxAmplitude.toDouble() / 100 //mMediaRecorder.getMaxAmplitude()获取在前一次调用此方法之后录音中出现的最大振幅。
            var db = 0.0 // 分贝
            //默认的最大音量是100,可以修改，但其实默认的，在测试过程中就有不错的表现
            //你可以传自定义的数字进去，但需要在一定的范围内，比如0-200，就需要在xml文件中配置maxVolume
            //同时，也可以配置灵敏度sensibility
            if (ratio > 1) db = 20 * Math.log10(ratio)
            //只要有一个线程，不断调用这个方法，就可以使波形变化
            //主要，这个方法必须在ui线程中调用
//            Log.i("=======", db+"");
            voiceLineView!!.setVolume(db.toInt()) //这里穿进去的就是分贝
            Log.i("MainActivity", "handleMessage: $db")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        voiceLineView = findViewById<VoiceLineView>(R.id.voicLine)

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            //        voiceLineView.startWave();
            if (mMediaRecorder == null) mMediaRecorder = MediaRecorder(this)
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            val file = File(cacheDir.absolutePath, "hello.log")
            file.deleteOnExit()
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            stopVoice = findViewById<Button>(R.id.stopVoice)
            mMediaRecorder!!.setOutputFile(file.absolutePath)
            mMediaRecorder!!.setMaxDuration(1000 * 60 * 10)
            mMediaRecorder!!.prepare()
            mMediaRecorder!!.start()
            val thread = Thread(this)
            thread.start()
            stopVoice!!.setOnClickListener {
                if (voiceLineView!!.isShow) {
                    voiceLineView!!.stopWave()
                    stopVoice!!.text = "开始"
                } else {
                    voiceLineView!!.startWave()
                    stopVoice!!.text = "停止"
                }
            }
        }
        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 0)
    }

    override fun onDestroy() {
        isAlive = false
        mMediaRecorder!!.release()
        mMediaRecorder = null
        super.onDestroy()
    }

    override fun run() {
        while (isAlive) {
            handler.sendEmptyMessage(0)
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
