package com.saleh.enezi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextPaint;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.database.sqlite.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_CONTACTS=4101, PICK_CONTACT=4102;
    EditText customerNameInput, customerPhoneInput;
    static final int GREEN=Color.rgb(24,112,61), DARK=Color.rgb(20,70,40), GOLD=Color.rgb(232,169,45);
    static final int BG=Color.rgb(246,248,246), TEXT=Color.rgb(32,43,36), MUTED=Color.rgb(105,116,108), CARD=Color.WHITE;
    DB db; LinearLayout root,content,bottom; TextView pageTitle; int textSize=16;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        db=new DB(this); home();
    }

    GradientDrawable rounded(int color,float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); return g; }
    GradientDrawable outlined(int color,int stroke,float radius){ GradientDrawable g=rounded(color,radius); g.setStroke(stroke,Color.rgb(224,230,225)); return g; }
    TextView tv(String s,float z){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(z); v.setTextColor(TEXT);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setPadding(dp(12),dp(7),dp(12),dp(7));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); v.setTextDirection(View.TEXT_DIRECTION_RTL); return v;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(0);
        b.setMinimumHeight(0); b.setPadding(dp(10),dp(3),dp(10),dp(3)); b.setGravity(Gravity.CENTER); b.setStateListAnimator(null);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return b;
    }
    EditText field(String h){
        EditText e=new EditText(this); e.setHint(h); e.setTextSize(textSize); e.setSingleLine(true);
        e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setPadding(14,7,14,7); e.setBackground(outlined(CARD,1,14)); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setSelectAllOnFocus(true); e.setOnClickListener(v -> e.selectAll());
        e.setOnFocusChangeListener((v,has)->{ if(has) e.postDelayed(() -> { e.selectAll(); },60); });
        return e;
    }
    void addField(EditText e){content.addView(e,new LinearLayout.LayoutParams(-1,dp(56))); addSpace(6);}
    void addSpace(int h){Space s=new Space(this); content.addView(s,new LinearLayout.LayoutParams(1,h));}
    TextView section(String s){TextView v=tv(s,14);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(4,12,4,6);content.addView(v);return v;}

    void base(String title){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(12,6,12,6); bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        TextView logo=tv("بقالة العزي",20); logo.setTextColor(Color.WHITE); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bar.addView(logo,new LinearLayout.LayoutParams(0,dp(64),1));
        TextView pt=tv(title,15); pt.setTextColor(Color.WHITE); pt.setGravity(Gravity.CENTER); bar.addView(pt,new LinearLayout.LayoutParams(dp(120),dp(64))); root.addView(bar);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.setClipToPadding(false);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(12),dp(12),dp(12),dp(22)); content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER); bottom.setPadding(dp(4),dp(4),dp(4),dp(4)); bottom.setBackground(outlined(CARD,1,20)); bottom.setElevation(8);
        String[] ns={"الرئيسية","العملاء","الفواتير","المخزون","التقارير"};
        for(String n:ns){Button b=button(n); b.setTextSize(12); b.setTextColor(n.equals(title)?GREEN:MUTED); b.setBackgroundColor(Color.TRANSPARENT); b.setOnClickListener(v->navigate(n)); bottom.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}
        root.addView(bottom); setContentView(root);
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{Rect rr=new Rect();root.getWindowVisibleDisplayFrame(rr);int diff=root.getRootView().getHeight()-rr.bottom;if(bottom!=null)bottom.setVisibility(diff>root.getRootView().getHeight()*0.18?View.GONE:View.VISIBLE);});
    }
    void navigate(String n){hideKeyboard(); if(n.equals("الرئيسية"))home();else if(n.equals("العملاء")||n.equals("الحسابات"))customers();else if(n.equals("الفواتير"))invoice();else if(n.equals("المخزون"))inventory();else reports();}
    void importContact(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.READ_CONTACTS")!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{"android.permission.READ_CONTACTS"},REQ_CONTACTS); return; }
        try{ Intent i=new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI); startActivityForResult(i,PICK_CONTACT); }catch(Exception e){ Toast.makeText(this,"تعذر فتح جهات الاتصال",Toast.LENGTH_SHORT).show(); }
    }
    // Permission handling is implemented below for contacts and Bluetooth printing.
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_CONTACTS){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)importContact();
            else Toast.makeText(this,"يلزم السماح بالوصول إلى جهات الاتصال لاستيراد الاسم والرقم",Toast.LENGTH_LONG).show();
        }else if(requestCode==5101 && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED && pendingPrintLines!=null){
            printInvoiceBluetooth(pendingPrintNo,pendingPrintCustomer,pendingPrintLines,pendingPrintTotal);
        }
    }

    void printToBluetooth(BluetoothDevice device,String no,String customer,ArrayList<Line> lines,double total){
        final String text=receiptTextFromLines(no,customer,lines,total,customer.isEmpty()?-1:db.customer(customer));
        final Bitmap bitmap=receiptBitmap(text);
        new Thread(()->{
            BluetoothSocket socket=null; OutputStream out=null;
            try{
                UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                socket=device.createRfcommSocketToServiceRecord(uuid);
                socket.connect(); out=socket.getOutputStream();
                out.write(new byte[]{0x1B,0x40});
                byte[] data=rasterBytes(bitmap);
                out.write(data); out.write(new byte[]{0x0A,0x0A,0x0A});
                out.flush();
                runOnUiThread(()->Toast.makeText(this,"تم إرسال الفاتورة إلى الطابعة",Toast.LENGTH_SHORT).show());
            }catch(Exception e){
                runOnUiThread(()->Toast.makeText(this,"تعذر الاتصال بالطابعة: "+(e.getMessage()==null?"تحقق من الاقتران":e.getMessage()),Toast.LENGTH_LONG).show());
            }finally{try{if(out!=null)out.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}
        }).start();
    }

    byte[] rasterBytes(Bitmap bitmap){
        int width=bitmap.getWidth(), height=bitmap.getHeight(), bytesPerRow=(width+7)/8;
        byte[] out=new byte[8+bytesPerRow*height];
        out[0]=0x1D;out[1]=0x76;out[2]=0x30;out[3]=0x00;
        out[4]=(byte)(bytesPerRow&0xFF);out[5]=(byte)((bytesPerRow>>8)&0xFF);
        out[6]=(byte)(height&0xFF);out[7]=(byte)((height>>8)&0xFF);
        int p=8;
        for(int y=0;y<height;y++){
            for(int xb=0;xb<bytesPerRow;xb++){
                int value=0;
                for(int bit=0;bit<8;bit++){
                    int x=xb*8+bit;
                    if(x<width){
                        int pixel=bitmap.getPixel(x,y);
                        int gray=(Color.red(pixel)+Color.green(pixel)+Color.blue(pixel))/3;
                        if(gray<180)value|=(1<<(7-bit));
                    }
                }
                out[p++]=(byte)value;
            }
        }
        return out;
    }

    void thermalPreview(String no,String customer,LinearLayout rows,double total){
        String s=receiptText(no,customer,rows,total,customer.isEmpty()?-1:db.customer(customer));
        TextView v=tv(s,12);v.setTypeface(Typeface.MONOSPACE);v.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this).setTitle("معاينة 58mm").setView(v)
            .setPositiveButton("مشاركة",(d,w)->shareWhatsApp(s))
            .setNeutralButton("طباعة",(d,w)->printInvoiceBluetooth(no,customer,new ArrayList<Line>(),total))
            .setNegativeButton("إغلاق",null).show();
    }
    String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("الحسابات");
        section("حسابات العملاء");

        EditText search=field("بحث بالاسم أو الهاتف"); addField(search);

        LinearLayout addBox=new LinearLayout(this);
        addBox.setOrientation(LinearLayout.VERTICAL);
        addBox.setPadding(dp(12),dp(10),dp(12),dp(10));
        addBox.setBackground(outlined(CARD,1,16));

        TextView addTitle=tv("إضافة عميل جديد",17);
        addTitle.setTextColor(GREEN); addTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        addBox.addView(addTitle,new LinearLayout.LayoutParams(-1,dp(38)));

        EditText name=field("اسم العميل");
        EditText phone=field("رقم الهاتف");
        customerNameInput=name; customerPhoneInput=phone;
        addBox.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(7)));
        addBox.addView(phone,new LinearLayout.LayoutParams(-1,dp(56)));
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(8)));

        LinearLayout contactActions=new LinearLayout(this);
        contactActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("👤 جهات الاتصال"); pick.setTextColor(GREEN); pick.setOnClickListener(v->importContact());
        Button add=button("＋ إضافة العميل"); add.setTextColor(Color.WHITE); add.setBackgroundColor(GREEN);
        contactActions.addView(pick,new LinearLayout.LayoutParams(0,dp(52),1));
        contactActions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1));
        addBox.addView(contactActions);
        content.addView(addBox,new LinearLayout.LayoutParams(-1,-2));
        addSpace(12);

        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);

        Runnable refresh=()->{
            list.removeAllViews();
            Cursor c=db.customers(search.getText().toString());
            while(c.moveToNext()){
                long id=c.getLong(0); String n=c.getString(1), p=c.getString(2);
                double bal=db.balance(id);
                LinearLayout card=new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14),dp(10),dp(14),dp(10));
                card.setBackground(outlined(CARD,1,16));
                card.setOnClickListener(v->account(id,n));

                TextView title=tv(n,18);
                title.setTextColor(GREEN); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                card.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));

                TextView sub=tv((p==null||p.isEmpty()?"بدون رقم":p)+"   •   "+db.transactionCount(id)+" عملية",12);
                sub.setTextColor(MUTED);
                card.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));

                TextView balance=tv(balanceText(bal),15);
                balance.setTextColor(bal>0?Color.rgb(180,55,45):GREEN);
                balance.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                balance.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                card.addView(balance,new LinearLayout.LayoutParams(-1,dp(34)));

                list.addView(card,new LinearLayout.LayoutParams(-1,dp(108)));
                addSpaceTo(list,8);
            }
            c.close();
        };

        add.setOnClickListener(v->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()){Toast.makeText(this,"اكتب اسم العميل",Toast.LENGTH_SHORT).show();return;}
            db.addCustomer(n,phone.getText().toString().trim());
            name.setText(""); phone.setText(""); refresh.run();
        });
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}
            public void afterTextChanged(android.text.Editable e){}
        });
        refresh.run();
    }
    void addSpaceTo(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,h));}
    void account(long id,String name){
        base("حساب العميل");
        section(name);

        LinearLayout summary=card();
        summary.setPadding(dp(14),dp(10),dp(14),dp(10));
        TextView bal=tv(balanceText(db.balance(id)),22);
        bal.setTextColor(GREEN); bal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bal.setGravity(Gravity.CENTER);
        summary.addView(bal,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView hint=tv("العمليات مرتبة من الأحدث إلى الأقدم، ويظهر الرصيد بعد كل عملية.",11);
        hint.setTextColor(MUTED); hint.setGravity(Gravity.CENTER);
        summary.addView(hint,new LinearLayout.LayoutParams(-1,dp(30)));
        addCard(summary,94);

        EditText amount=field("المبلغ"); addField(amount);
        EditText details=field("التفاصيل"); addField(details);

        LinearLayout acts=new LinearLayout(this);
        acts.setOrientation(LinearLayout.HORIZONTAL);
        Button debit=button("عليه"); Button credit=button("له / دفعة");
        debit.setTextColor(Color.RED); credit.setTextColor(GREEN);
        acts.addView(debit,new LinearLayout.LayoutParams(0,dp(50),1));
        acts.addView(credit,new LinearLayout.LayoutParams(0,dp(50),1));
        content.addView(acts); addSpace(8);

        Button share=button("📲 مشاركة كشف الحساب");
        share.setTextColor(GREEN); share.setBackground(outline(CARD,14));
        content.addView(share,new LinearLayout.LayoutParams(-1,dp(50)));
        share.setOnClickListener(v->shareWhatsApp(statement(id,name)));
        addSpace(10);

        section("سجل العمليات");
        LinearLayout history=new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        content.addView(history);

        Runnable refresh=()->{
            history.removeAllViews();
            double runningAfter=db.balance(id);
            Cursor c=db.transactions(id);
            while(c.moveToNext()){
                long txId=c.getLong(0);
                String date=c.getString(1), detailsText=c.getString(2);
                double amountValue=c.getDouble(3);
                int type=c.getInt(4);
                double balanceAfter=runningAfter;

                LinearLayout r=new LinearLayout(this);
                r.setOrientation(LinearLayout.VERTICAL);
                r.setPadding(dp(12),dp(8),dp(12),dp(8));
                r.setBackground(outlined(CARD,1,14));

                TextView dateView=tv(date,11);
                dateView.setTextColor(MUTED);
                dateView.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                r.addView(dateView,new LinearLayout.LayoutParams(-1,dp(26)));

                TextView detailView=tv(detailsText==null||detailsText.trim().isEmpty()?"عملية مالية":detailsText,13);
                detailView.setTextColor(TEXT);
                detailView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                r.addView(detailView,new LinearLayout.LayoutParams(-1,dp(34)));

                TextView amountView=tv((type==1?"عليه: ":"له: ")+fmt(amountValue)+" ريال",13);
                amountView.setTextColor(type==1?Color.rgb(190,55,45):GREEN);
                r.addView(amountView,new LinearLayout.LayoutParams(-1,dp(30)));

                TextView balView=tv("الرصيد بعد العملية: "+balanceText(balanceAfter),12);
                balView.setTextColor(GREEN);
                r.addView(balView,new LinearLayout.LayoutParams(-1,dp(30)));

                // إذا كانت العملية فاتورة، نعرض تفاصيل أصنافها داخل الحساب بخط صغير ومضغوط.
                String invNo=db.invoiceNoFromTransaction(detailsText);
                if(!TextUtils.isEmpty(invNo)){
                    TextView invoiceInfo=tv(db.invoiceCompactDetails(invNo),10);
                    invoiceInfo.setTextColor(MUTED);
                    r.addView(invoiceInfo,new LinearLayout.LayoutParams(-1,dp(42)));
                }

                history.addView(r,new LinearLayout.LayoutParams(-1,-2));
                addSpaceTo(history,8);

                double signed=(type==1?amountValue:-amountValue);
                runningAfter-=signed;
            }
            c.close();
            bal.setText(balanceText(db.balance(id)));
        };

        View.OnClickListener add=v->{
            try{
                double a=Double.parseDouble(amount.getText().toString());
                if(a<=0)throw new Exception();
                db.addTransaction(id,a,details.getText().toString().trim(),v==debit?1:0,db.now());
                amount.setText(""); details.setText(""); refresh.run();
            }catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}
        };
        debit.setOnClickListener(add); credit.setOnClickListener(add);
        refresh.run();
    }

    String statement(long id,String name){
        StringBuilder s=new StringBuilder("بقالة العزي\nكشف حساب العميل: ").append(name).append("\n");
        double runningAfter=db.balance(id);
        Cursor c=db.transactions(id);
        while(c.moveToNext()){
            String date=c.getString(1), d=c.getString(2);
            double a=c.getDouble(3); int type=c.getInt(4);
            s.append(date).append(" | ").append(d==null?"":d).append(" | ")
             .append(type==1?"عليه":"له").append(": ").append(fmt(a)).append(" ريال")
             .append(" | ").append(balanceText(runningAfter)).append("\n");
            runningAfter-=(type==1?a:-a);
        }
        c.close();
        s.append("--------------------\n").append(balanceText(db.balance(id)));
        return s.toString();
    }

    void inventory(){
        base("المخزون");section("إضافة صنف");
        EditText name=field("اسم الصنف");EditText qty=field("الكمية");EditText min=field("الحد الأدنى");addField(name);addField(qty);addField(min);
        Button add=button("＋ حفظ الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);content.addView(add,new LinearLayout.LayoutParams(-1,48));addSpace(8);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable refresh=()->{list.removeAllViews();Cursor c=db.items();while(c.moveToNext()){double q=c.getDouble(2),m=c.getDouble(3);TextView r=tv(c.getString(1)+"\nالكمية: "+fmt(q)+"   •   الحد الأدنى: "+fmt(m)+(q<=m?"   ⚠ منخفض":""),
                14);r.setBackgroundColor(CARD);r.setTextColor(q<=m?Color.rgb(170,75,35):TEXT);list.addView(r,new LinearLayout.LayoutParams(-1,68));addSpaceTo(list,5);}c.close();};
        add.setOnClickListener(v->{try{db.addItem(name.getText().toString().trim(),Double.parseDouble(qty.getText().toString()),Double.parseDouble(min.getText().toString()));name.setText("");qty.setText("");min.setText("");refresh.run();}catch(Exception e){Toast.makeText(this,"أدخل بيانات الصنف بشكل صحيح",Toast.LENGTH_SHORT).show();}});refresh.run();
    }
    void reports(){base("التقارير");section("ملخص سريع");cardTitle("المبيعات","عدد الفواتير: "+db.invoiceCount()+"   •   إجمالي المبيعات: "+fmt(db.sales())+" ريال");cardTitle("العملاء","عدد العملاء: "+db.customerCount());section("آخر الفواتير");Cursor c=db.invoices();while(c.moveToNext())content.addView(tv("فاتورة "+c.getString(1)+"  •  "+c.getString(2)+"\n"+fmt(c.getDouble(4))+" ريال   •   "+c.getString(3),14));c.close();}

    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"enezi.db",null,5);}
        public void onCreate(SQLiteDatabase d){create(d);}
        void create(SQLiteDatabase d){d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,customer TEXT,total REAL,date TEXT)");d.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER,amount REAL,details TEXT,type INTEGER,date TEXT)");d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,min_qty REAL)");d.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<2){try{d.execSQL("ALTER TABLE invoices ADD COLUMN date TEXT");}catch(Exception ignored){}}if(o<5){d.execSQL("CREATE TABLE IF NOT EXISTS invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");}}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
        long customer(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);return getWritableDatabase().insert("customers",null,v);}
        void addCustomer(String n,String p){ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);getWritableDatabase().insert("customers",null,v);}
        long addInvoice(String no,String c,double t,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",c);v.put("total",t);v.put("date",date);return getWritableDatabase().insert("invoices",null,v);}
        void addTransaction(long id,double a,String d,int type,String date){if(id<1)return;ContentValues v=new ContentValues();v.put("customer_id",id);v.put("amount",a);v.put("details",d);v.put("type",type);v.put("date",date);getWritableDatabase().insert("transactions",null,v);}
        double balance(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE -amount END),0) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        Cursor customers(String q){return getReadableDatabase().rawQuery("SELECT id,name,COALESCE(phone,'') FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});}
        Cursor transactions(long id){return getReadableDatabase().rawQuery("SELECT id,date,details,amount,type FROM transactions WHERE customer_id=? ORDER BY id DESC",new String[]{String.valueOf(id)});}
        Cursor items(){return getReadableDatabase().rawQuery("SELECT id,name,qty,min_qty FROM items ORDER BY name",null);}
        void addItem(String n,double q,double m){if(n.isEmpty()||q<0||m<0)throw new IllegalArgumentException();ContentValues v=new ContentValues();v.put("name",n);v.put("qty",q);v.put("min_qty",m);getWritableDatabase().insert("items",null,v);}
        int transactionCount(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        String invoiceNoFromTransaction(String details){
            if(details==null)return "";
            String p="فاتورة مبيعات رقم ";
            return details.startsWith(p)?details.substring(p.length()).trim():"";
        }
        String invoiceCompactDetails(String no){
            Cursor c=getReadableDatabase().rawQuery("SELECT name,qty,total FROM invoice_items WHERE invoice_id=(SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1) ORDER BY id",new String[]{no});
            StringBuilder s=new StringBuilder("تفاصيل الفاتورة: ");
            int count=0;
            while(c.moveToNext() && count<6){
                if(count>0)s.append("  •  ");
                s.append(c.getString(0)).append(" × ").append(fmt(c.getDouble(1))).append(" = ").append(fmt(c.getDouble(2)));
                count++;
            }
            c.close();
            return count==0?"تفاصيل الفاتورة غير متاحة":s.toString();
        }
        Cursor invoices(){return getReadableDatabase().rawQuery("SELECT id,no,customer,total,date FROM invoices ORDER BY id DESC LIMIT 100",null);}
        int invoiceCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        int customerCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        double sales(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM invoices",null);double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        String[] customerNames(){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM customers ORDER BY name",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
        String phoneByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(phone,'') FROM customers WHERE name=? LIMIT 1",new String[]{n});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        long customer(String n,String p){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();ContentValues v=new ContentValues();v.put("phone",p);getWritableDatabase().update("customers",v,"id=?",new String[]{String.valueOf(x)});return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);return getWritableDatabase().insert("customers",null,v);}
        double balanceByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=? LIMIT 1",new String[]{n});if(!c.moveToFirst()){c.close();return 0;}long id=c.getLong(0);c.close();return balance(id);}
        String invoiceNo(long id){Cursor c=getReadableDatabase().rawQuery("SELECT no FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        String invoiceCustomer(long id){Cursor c=getReadableDatabase().rawQuery("SELECT customer FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        void updateInvoice(long id,String no,String customer,double total,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",customer);v.put("total",total);v.put("date",date);getWritableDatabase().update("invoices",v,"id=?",new String[]{String.valueOf(id)});}
        Cursor invoiceLines(long id){return getReadableDatabase().rawQuery("SELECT id,name,qty,total FROM invoice_items WHERE invoice_id=? ORDER BY id",new String[]{String.valueOf(id)});}
        void replaceInvoiceLines(long id,ArrayList<Line> ls){SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});for(Line l:ls){ContentValues v=new ContentValues();v.put("invoice_id",id);v.put("name",l.name);v.put("qty",l.qty);v.put("total",l.total);d.insert("invoice_items",null,v);}}
        void deleteInvoice(long id){String no=invoiceNo(id);deleteInvoiceTransaction(no);SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});d.delete("invoices","id=?",new String[]{String.valueOf(id)});}
        void deleteInvoiceTransaction(String no){getWritableDatabase().delete("transactions","details=?",new String[]{"فاتورة مبيعات رقم "+no});}
        void addTransactionOnce(long id,double a,String details,String date){if(id>0)addTransaction(id,a,details,1,date);}
        int nextInvoice(){return invoiceCount()+1;}
    }
}
