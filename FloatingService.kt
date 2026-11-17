package com.rialz.tka

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var menu: LinearLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val notification=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,"rialz_overlay").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Rialz TKA AI aktif").setContentText("Floating learning menu sedang berjalan").setOngoing(true).build()
            else Notification.Builder(this).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Rialz TKA AI aktif").setContentText("Floating menu berjalan").build()
        startForeground(501,notification)
        showBubble()
    }
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val ch=NotificationChannel("rialz_overlay","Rialz Floating Assistant",NotificationManager.IMPORTANCE_LOW);(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)}}
    private fun lp():WindowManager.LayoutParams {
        return WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=20; y=240 }
    }
    private fun showBubble(){
        val b=TextView(this).apply { text="Rz"; textSize=17f; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setTypeface(null,1); background=shape(0xFFE6324B.toInt(),100); elevation=12f; setPadding(17,17,17,17) }
        bubble=b; bubbleParams=lp(); wm.addView(b,bubbleParams)
        b.setOnTouchListener(object:View.OnTouchListener{
            var sx=0f;var sy=0f;var ox=0;var oy=0;var moved=false
            override fun onTouch(v:View,e:MotionEvent):Boolean {
                val p=bubbleParams ?: return false
                when(e.action){
                    MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;ox=p.x;oy=p.y;moved=false;return true}
                    MotionEvent.ACTION_MOVE->{val dx=e.rawX-sx;val dy=e.rawY-sy;if(kotlin.math.abs(dx)+kotlin.math.abs(dy)>8)moved=true;p.x=ox+dx.toInt();p.y=oy+dy.toInt();wm.updateViewLayout(b,p);return true}
                    MotionEvent.ACTION_UP->{if(!moved)toggleMenu();return true}
                };return false
            }
        })
    }
    private fun shape(color:Int,r:Float)=GradientDrawable().apply{setColor(color);cornerRadius=r}
    private fun toggleMenu(){if(menu==null)showMenu() else hideMenu()}
    private fun showMenu(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,10);background=shape(Color.WHITE,24f);elevation=16f}
        box.addView(TextView(this).apply{text="✨ RIALZ MENU";textSize=15f;setTextColor(0xFF5520B8.toInt());setTypeface(null,1);setPadding(8,8,8,12)})
        addMenuButton(box,"📷  Scan Soal","scan")
        addMenuButton(box,"🤖  Tutor AI","tutor")
        addMenuButton(box,"🧠  Tanya AI","ask")
        addMenuButton(box,"✍️  Cek Jawaban","check")
        addMenuButton(box,"📖  Penjelasan","explain")
        addMenuButton(box,"📚  Ringkas Materi","summary")
        val stop=Button(this).apply{text="STOP FLOATING";setOnClickListener{stopSelf()}}
        box.addView(stop)
        menu=box;menuParams=lp().apply{x=12;y=300}
        wm.addView(box,menuParams)
    }
    private fun addMenuButton(parent:LinearLayout,title:String,mode:String){
        parent.addView(Button(this).apply{text=title;textSize=13f;setOnClickListener{
            val i=Intent(this@FloatingService,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("mode",mode)
            startActivity(i);hideMenu()
        }})
    }
    private fun hideMenu(){try{menu?.let{wm.removeView(it)}}catch(_:Exception){};menu=null}
    override fun onDestroy(){hideMenu();try{bubble?.let{wm.removeView(it)}}catch(_:Exception){};bubble=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()}
}
