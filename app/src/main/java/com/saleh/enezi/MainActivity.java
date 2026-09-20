package com.saleh.enezi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int GREEN=Color.rgb(30,107,56), GOLD=Color.rgb(230,161,0), BLUE=Color.rgb(42,82,190), RED=Color.rgb(229,57,53);
    DB db; LinearLayout root,content,bottom; TextView pageTitle,totalView; int textSize=17;

    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(21,85,45)); db=new DB(this); home();}

    TextView tv(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.rgb(35,45,38));v.setPadding(18,14,18,14);return v;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setAllCaps(false);b.setPadding(12,4,12,4);return b;}
    EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(textSize);e.setSingleLine(true);e.setPadding(16,8,16,8);e.setBackgroundColor(Color.WHITE);return e;}
    void base(String title){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(247,249,247));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);bar.setPadding(12,4,12,4);bar.setBackgroundColor(GREEN);
        TextView logo=tv("🧺  بقالة العنزي",21);logo.setTextColor(Color.WHITE);logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bar.addView(logo,new LinearLayout.LayoutParams(0,62,1));
        pageTitle=tv(title,16);pageTitle.setTextColor(Color.WHITE);bar.addView(pageTitle);
        root.addView(bar);
        ScrollView sv=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(14,14,14,14);sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setBackgroundColor(Color.WHITE);
        String[] names={"الرئيسية","الحسابات","الفواتير","المخزون","التقارير"};
        for(String n:names){Button b=button(n);b.setTextSize(12);b.setOnClickListener(v->navigate(n));bottom.addView(b,new LinearLayout.LayoutParams(0,64,1));}
        root.addView(bottom);setContentView(root);
    }
    void navigate(String n){if(n.equals("الرئيسية"))home();else if(n.equals("الحسابات"))customers();else if(n.equals("الفواتير"))invoice();else if(n.equals("المخزون"))inventory();else reports();}
    TextView cardTitle(String s){TextView v=tv(s,19);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setTextColor(GREEN);v.setBackgroundColor(Color.WHITE);return v;}
    void home(){
        base("الرئيسية");
        TextView hero=tv("بقالة العنزي\nمرحباً بكم في بقالتكم المفضلة\nنظام الفواتير ودفتر الحسابات — يعمل محلياً بدون إنترنت",22);hero.setTextColor(Color.WHITE);hero.setGravity(Gravity.CENTER);hero.setPadding(20,28,20,28);hero.setBackgroundColor(GREEN);content.addView(hero);
        addSpace();
        addAction("🧾  إنشاء فاتورة مبيعات","إدخال الأصناف والإجمالي وحفظ الفاتورة",v->invoice());
        addAction("👥  العملاء والحسابات","كشف الحساب، عليه، له، الرصيد والعمليات",v->customers());
        addAction("📦  المخزون","الأصناف والكميات والحد الأدنى",v->inventory());
        addAction("📊  التقارير والسجل","ملخص الفواتير والحركة",v->reports());
    }
    void addAction(String a,String sub,View.OnClickListener l){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(8,4,8,4);Button b=button(a);b.setTextSize(18);b.setTextColor(GREEN);b.setOnClickListener(l);c.addView(b);c.addView(tv(sub,13));content.addView(c);addSpace();}
    void addSpace(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,8));}

    void invoice(){
        base("إنشاء فاتورة");
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        EditText no=field("رقم الفاتورة");no.setText(String.valueOf(db.nextInvoice()));top.addView(no,new LinearLayout.LayoutParams(0,58,1));
        EditText customer=field("اسم العميل (اختياري)");top.addView(customer,new LinearLayout.LayoutParams(0,58,1));content.addView(top);
        TextView hint=tv("ترتيب الإدخال: الإجمالي ← الكمية ← اسم الصنف. سعر الوحدة يُحسب تلقائياً.",13);hint.setTextColor(BLUE);content.addView(hint);
        EditText item=field("اسم الصنف");EditText qty=field("الكمية");qty.setText("1");EditText lineTotal=field("الإجمالي");TextView unit=tv("سعر الوحدة: 0",14);unit.setTextColor(Color.GRAY);
        content.addView(item);content.addView(qty);content.addView(lineTotal);content.addView(unit);
        LinearLayout controls=new LinearLayout(this);Button add=button("＋ إضافة الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);controls.addView(add,new LinearLayout.LayoutParams(0,58,1));
        Button clear=button("مسح");controls.addView(clear,new LinearLayout.LayoutParams(0,58,1));content.addView(controls);
        TextView total=tv("الإجمالي: 0 ريال",24);total.setTextColor(GREEN);total.setTypeface(Typeface.DEFAULT,Typeface.BOLD);total.setGravity(Gravity.CENTER);total.setPadding(16,22,16,22);total.setBackgroundColor(Color.rgb(255,247,218));content.addView(total);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);
        final double[] sum={0};
        lineTotal.setOnFocusChangeListener((v,f)->{if(!f)return;});
        add.setOnClickListener(v->{
            try{String n=item.getText().toString().trim();double q=Double.parseDouble(qty.getText().toString()),t=Double.parseDouble(lineTotal.getText().toString());if(n.isEmpty()||q<=0||t<0)throw new Exception();double p=t/q;unit.setText("سعر الوحدة: "+fmt(p)+" ريال");sum[0]+=t;TextView r=tv(n+"   |   "+fmt(q)+"   |   "+fmt(p)+"   |   "+fmt(t),16);rows.addView(r);total.setText("الإجمالي: "+fmt(sum[0])+" ريال");item.setText("");qty.setText("1");lineTotal.setText("");item.requestFocus();}catch(Exception e){Toast.makeText(this,"أدخل اسم الصنف والكمية والإجمالي بشكل صحيح",Toast.LENGTH_SHORT).show();}
        });
        clear.setOnClickListener(v->{rows.removeAllViews();sum[0]=0;total.setText("الإجمالي: 0 ريال");item.setText("");qty.setText("1");lineTotal.setText("");});
        Button save=button("💾 حفظ الفاتورة");save.setTextColor(Color.WHITE);save.setBackgroundColor(GREEN);content.addView(save,new LinearLayout.LayoutParams(-1,60));
        save.setOnClickListener(v->{if(sum[0]<=0){Toast.makeText(this,"أضف صنفاً أولاً",Toast.LENGTH_SHORT).show();return;}String cn=customer.getText().toString().trim();long cid=cn.isEmpty()?-1:db.customer(cn);long inv=db.addInvoice(no.getText().toString(),cn,sum[0]);db.addTransaction(cid,sum[0],"فاتورة مبيعات رقم "+no.getText());Toast.makeText(this,"تم حفظ الفاتورة رقم "+no.getText(),Toast.LENGTH_SHORT).show();clear.performClick();no.setText(String.valueOf(db.nextInvoice()));customer.setText("");});
        Button print=button("🖨 طباعة 80mm");print.setOnClickListener(v->thermal(no.getText().toString(),customer.getText().toString(),sum[0],rows));content.addView(print);
    }
    String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("العملاء والحسابات");
        EditText search=field("بحث بالاسم أو الهاتف");content.addView(search);
        EditText name=field("اسم العميل الجديد");EditText phone=field("رقم الهاتف");content.addView(name);content.addView(phone);
        Button add=button("＋ إضافة عميل");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);content.addView(add);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable refresh=()->{list.removeAllViews();Cursor c=db.customers(search.getText().toString());while(c.moveToNext()){String n=c.getString(1),p=c.getString(2);double bal=db.balance(c.getLong(0));Button b=button(n+"\n"+(p.isEmpty()?"بدون رقم":p)+"    |    الرصيد: "+fmt(bal)+" ريال");b.setGravity(Gravity.RIGHT);b.setOnClickListener(v->account(c.getLong(0),n));list.addView(b);}c.close();};
        add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty())return;db.addCustomer(n,phone.getText().toString().trim());name.setText("");phone.setText("");refresh.run();});
        search.setOnKeyListener((v,k,e)->{refresh.run();return false;});refresh.run();
    }
    void account(long id,String name){
        base("حساب العميل: "+name);
        TextView bal=tv("الرصيد: "+fmt(db.balance(id))+" ريال",23);bal.setTextColor(BLUE);content.addView(bal);
        EditText amount=field("المبلغ");EditText details=field("التفاصيل");content.addView(amount);content.addView(details);
        LinearLayout types=new LinearLayout(this);Button debit=button("عليه");debit.setTextColor(RED);Button credit=button("له / دفعة");credit.setTextColor(GREEN);types.addView(debit,new LinearLayout.LayoutParams(0,58,1));types.addView(credit,new LinearLayout.LayoutParams(0,58,1));content.addView(types);
        LinearLayout history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);content.addView(history);
        Runnable refresh=()->{history.removeAllViews();Cursor c=db.transactions(id);while(c.moveToNext()){String date=c.getString(1),d=c.getString(2);double a=c.getDouble(3);String type=c.getInt(4)==1?"عليه":"له";TextView r=tv(date+"\n"+d+"\n"+type+"  "+fmt(a)+" ريال",15);history.addView(r);}c.close();bal.setText("الرصيد: "+fmt(db.balance(id))+" ريال");};
        View.OnClickListener add=(v)->{try{double a=Double.parseDouble(amount.getText().toString());if(a<=0)throw new Exception();db.addTransaction(id,a,details.getText().toString(),v==debit?1:0);amount.setText("");details.setText("");refresh.run();}catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}};debit.setOnClickListener(add);credit.setOnClickListener(add);refresh.run();
    }

    void inventory(){
        base("المخزون والأصناف");
        EditText name=field("اسم الصنف");EditText qty=field("الكمية");EditText min=field("الحد الأدنى");content.addView(name);content.addView(qty);content.addView(min);
        Button add=button("＋ حفظ الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);content.addView(add);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable refresh=()->{list.removeAllViews();Cursor c=db.items();while(c.moveToNext()){list.addView(tv(c.getString(1)+"    |    الكمية: "+fmt(c.getDouble(2))+"    |    الحد الأدنى: "+fmt(c.getDouble(3)),16));}c.close();};add.setOnClickListener(v->{try{db.addItem(name.getText().toString().trim(),Double.parseDouble(qty.getText().toString()),Double.parseDouble(min.getText().toString()));name.setText("");qty.setText("");min.setText("");refresh.run();}catch(Exception e){Toast.makeText(this,"أدخل بيانات الصنف",Toast.LENGTH_SHORT).show();}});refresh.run();
    }
    void reports(){
        base("التقارير والسجل");
        content.addView(cardTitle("ملخص اليوم"));content.addView(tv("عدد الفواتير: "+db.invoiceCount()+"\nإجمالي المبيعات: "+fmt(db.sales())+" ريال\nعدد العملاء: "+db.customerCount(),19));
        content.addView(cardTitle("آخر الفواتير"));Cursor c=db.invoices();while(c.moveToNext())content.addView(tv("فاتورة "+c.getString(1)+"  |  "+c.getString(2)+"  |  "+fmt(c.getDouble(4))+" ريال",15));c.close();
    }
    void thermal(String no,String customer,double total,LinearLayout rows){
        StringBuilder s=new StringBuilder();s.append("بقالة العنزي\n------------------------------\nفاتورة: ").append(no).append("\n");
        if(!customer.isEmpty())s.append("العميل: ").append(customer).append("\n");
        for(int i=0;i<rows.getChildCount();i++)s.append(((TextView)rows.getChildAt(i)).getText()).append("\n");
        s.append("------------------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\nشكراً لتعاملكم معنا");
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s.toString());startActivity(Intent.createChooser(i,"مشاركة / إرسال الفاتورة"));
    }

    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"enezi.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,customer TEXT,total REAL,date TEXT)");d.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER,amount REAL,details TEXT,type INTEGER,date TEXT)");d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,min_qty REAL)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){}
        long customer(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);return getWritableDatabase().insert("customers",null,v);}
        void addCustomer(String n,String p){ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);getWritableDatabase().insert("customers",null,v);}
        long addInvoice(String no,String c,double total){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",c);v.put("total",total);v.put("date",now());return getWritableDatabase().insert("invoices",null,v);}
        void addTransaction(long cid,double a,String d){addTransaction(cid,a,d,1);}
        void addTransaction(long cid,double a,String d,int type){if(cid<0)return;ContentValues v=new ContentValues();v.put("customer_id",cid);v.put("amount",a);v.put("details",d);v.put("type",type);v.put("date",now());getWritableDatabase().insert("transactions",null,v);}
        double balance(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE -amount END),0) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        Cursor customers(String q){return getReadableDatabase().rawQuery("SELECT id,name,COALESCE(phone,'') FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});}
        Cursor transactions(long id){return getReadableDatabase().rawQuery("SELECT date,details,amount,type FROM transactions WHERE customer_id=? ORDER BY id DESC",new String[]{String.valueOf(id)});}
        Cursor items(){return getReadableDatabase().rawQuery("SELECT id,name,qty,min_qty FROM items ORDER BY name",null);}
        void addItem(String n,double q,double m){if(n.isEmpty())throw new IllegalArgumentException();ContentValues v=new ContentValues();v.put("name",n);v.put("qty",q);v.put("min_qty",m);getWritableDatabase().insert("items",null,v);}
        Cursor invoices(){return getReadableDatabase().rawQuery("SELECT id,no,customer,total,date FROM invoices ORDER BY id DESC LIMIT 100",null);}
        int invoiceCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        int customerCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        double sales(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM invoices",null);double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        int nextInvoice(){return invoiceCount()+1;}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
    }
}
