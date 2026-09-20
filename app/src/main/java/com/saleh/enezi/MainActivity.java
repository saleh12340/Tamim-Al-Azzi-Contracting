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
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==PICK_CONTACT&&resultCode==RESULT_OK&&data!=null){ Cursor c=null; try{ c=getContentResolver().query(data.getData(),new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null); if(c!=null&&c.moveToFirst()){ String n=c.getString(0),p=c.getString(1); if(customerNameInput!=null)customerNameInput.setText(n==null?"":n); if(customerPhoneInput!=null)customerPhoneInput.setText(p==null?"":p); if(customerNameInput!=null)customerNameInput.requestFocus(); Toast.makeText(this,"تم استيراد اسم العميل ورقم الهاتف",Toast.LENGTH_SHORT).show(); } }catch(Exception e){Toast.makeText(this,"تعذر قراءة بيانات جهة الاتصال",Toast.LENGTH_SHORT).show();}finally{if(c!=null)c.close();} } }
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}

    TextView cardTitle(String title,String sub){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(14,8,14,8);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        TextView a=tv(title,17);a.setTextColor(GREEN);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(a);
        TextView b=tv(sub,12);b.setTextColor(MUTED);c.addView(b);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(72)); cp.setMargins(0,0,0,7); content.addView(c,cp);return a;
    }
    void addAction(String a,String sub,View.OnClickListener l){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,5,12,5);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        Button b=button(a);b.setTextSize(16);b.setTextColor(TEXT);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setOnClickListener(l);c.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView s=tv(sub,12);s.setTextColor(MUTED);c.addView(s,new LinearLayout.LayoutParams(-1,30));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(82)); ap.setMargins(0,0,0,7); content.addView(c,ap);
    }

    void home(){
        base("الرئيسية");

        LinearLayout hero=card(); hero.setPadding(dp(20),dp(16),dp(20),dp(16));
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        TextView h=tv("بقالة العزي",26);h.setTextColor(Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        hero.addView(h,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView hs=tv("المبيعات • حسابات العملاء • المخزون • التقارير\nيعمل محلياً بدون إنترنت ويحفظ بياناتك على الجهاز",14);
        hs.setTextColor(Color.WHITE);hero.addView(hs,new LinearLayout.LayoutParams(-1,dp(58)));
        addCard(hero,120);

        LinearLayout quick=card();
        quick.addView(tv("إجراء سريع",17),new LinearLayout.LayoutParams(-1,dp(34)));
        Button ni=action("＋  إضافة فاتورة جديدة",GOLD);ni.setTextSize(18);ni.setOnClickListener(v->invoice());
        quick.addView(ni,new LinearLayout.LayoutParams(-1,dp(58)));
        addCard(quick,108);

        section("الأقسام الرئيسية");

        // بطاقات متجاورة: بطاقتان في كل صف، ثم الصف التالي.
        String[] names={"👥 العملاء والحسابات","🧾 الفواتير","📦 المخزون والأصناف","📊 التقارير"};
        String[] subs={"العملاء، الأرصدة والحركات","سجل الفواتير والتعديل والحذف","الأصناف والكميات والتنبيهات","ملخص المبيعات والحركة"};
        View.OnClickListener[] actions={
            v->customers(), v->invoiceHistory(), v->inventory(), v->reports()
        };
        for(int i=0;i<names.length;i+=2){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            for(int j=i;j<i+2&&j<names.length;j++){
                LinearLayout c=card();
                c.setPadding(dp(10),dp(9),dp(10),dp(8));
                TextView a=tv(names[j],16);a.setTextColor(GREEN);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                c.addView(a,new LinearLayout.LayoutParams(-1,dp(38)));
                TextView b=tv(subs[j],11);b.setTextColor(MUTED);
                c.addView(b,new LinearLayout.LayoutParams(-1,dp(42)));
                c.setOnClickListener(actions[j]);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(92),1);
                if(j==i) cp.setMargins(0,0,dp(5),0); else cp.setMargins(dp(5),0,0,0);
                row.addView(c,cp);
            }
            content.addView(row,new LinearLayout.LayoutParams(-1,dp(92)));
            addSpace(8);
        }

        // زر عام صغير يفتح أهم العمليات من أي وقت في الشاشة الرئيسية.
        Button general=button("＋  زر عام: إضافة عملية");
        general.setTextColor(GREEN);general.setTextSize(14);general.setBackground(outline(CARD,14));
        general.setOnClickListener(v->showGeneralActions());
        content.addView(general,new LinearLayout.LayoutParams(-1,dp(50)));
        addSpace(8);
    }

    void showGeneralActions(){
        String[] choices={"🧾 فاتورة جديدة","👥 إضافة عميل","📦 إضافة صنف","📊 التقارير"};
        new AlertDialog.Builder(this).setTitle("إجراء عام").setItems(choices,(d,w)->{
            if(w==0) invoice();
            else if(w==1) customers();
            else if(w==2) inventory();
            else reports();
        }).setNegativeButton("إغلاق",null).show();
    }

    void invoice(){invoice(false,-1);}
    void invoice(boolean edit,long invoiceId){
        base(edit?"تعديل الفاتورة":"فاتورة جديدة");
        section("بيانات الفاتورة");

        // صف واحد مضغوط: رقم الفاتورة + اسم العميل + التاريخ والوقت.
        LinearLayout metaRow=new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView no=tv(edit?db.invoiceNo(invoiceId):String.valueOf(db.nextInvoice()),15);
        no.setTextColor(GREEN);no.setTypeface(Typeface.DEFAULT,Typeface.BOLD);no.setGravity(Gravity.CENTER);
        no.setBackground(outline(Color.rgb(248,250,248),14));
        no.setContentDescription("رقم الفاتورة");
        metaRow.addView(no,new LinearLayout.LayoutParams(0,dp(54),0.72f));

        AutoCompleteTextView customer=new AutoCompleteTextView(this);
        customer.setHint("اسم العميل");customer.setTextSize(14);customer.setSingleLine(true);
        customer.setTextColor(TEXT);customer.setHintTextColor(MUTED);
        customer.setPadding(dp(10),dp(6),dp(10),dp(6));customer.setBackground(outline(CARD,14));
        customer.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        customer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);customer.setTextDirection(View.TEXT_DIRECTION_RTL);
        customer.setThreshold(1);customer.setSelectAllOnFocus(true);
        customer.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,db.customerNames()));
        metaRow.addView(customer,new LinearLayout.LayoutParams(0,dp(54),1.35f));

        TextView dt=tv(db.now(),12);dt.setTextColor(MUTED);dt.setGravity(Gravity.CENTER);
        dt.setBackground(outline(Color.rgb(248,250,248),14));
        metaRow.addView(dt,new LinearLayout.LayoutParams(0,dp(54),1.15f));
        content.addView(metaRow);space(8);

        if(edit) customer.setText(db.invoiceCustomer(invoiceId));

        section("إدخال الصنف");
        LinearLayout entry=card();entry.setPadding(dp(10),dp(10),dp(10),dp(10));
        LinearLayout line=new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText total=field("الإجمالي");
        EditText qty=field("الكمية");
        EditText item=field("اسم الصنف / التفاصيل");
        total.setInputType(2|8192);qty.setInputType(2|8192);qty.setText("1");

        // لا يوجد سعر وحدة في صف الإدخال؛ يُعرض محسوباً داخل صندوق تفاصيل الفاتورة بالأسفل.
        line.addView(total,new LinearLayout.LayoutParams(0,dp(60),1.0f));
        line.addView(qty,new LinearLayout.LayoutParams(0,dp(60),0.72f));
        line.addView(item,new LinearLayout.LayoutParams(0,dp(60),1.35f));
        entry.addView(line);
        Button add=action("＋  إضافة الصنف / التعامل",GREEN);
        entry.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));
        addCard(entry,130);

        section("صندوق عرض الفاتورة");
        LinearLayout invoiceBox=card();
        invoiceBox.setPadding(dp(6),dp(6),dp(6),dp(8));

        // جدول مرتب بخلايا متساوية ومن دون حدود مرئية؛ الأعمدة ثابتة وواضحة بصرياً.
        LinearLayout table=new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head=new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] heads={"الإجمالي","الكمية","اسم الصنف","سعر الوحدة","حذف"};
        float[] weights={1.0f,.72f,1.35f,.9f,.55f};
        for(int i=0;i<heads.length;i++){
            TextView hv=tv(heads[i],11);
            hv.setTextColor(GREEN);hv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            hv.setGravity(Gravity.CENTER);hv.setSingleLine(true);
            hv.setBackgroundColor(Color.TRANSPARENT);
            head.addView(hv,new LinearLayout.LayoutParams(0,dp(38),weights[i]));
        }
        table.addView(head,new LinearLayout.LayoutParams(-1,dp(38)));

        HorizontalScrollView tableScroll=new HorizontalScrollView(this);
        tableScroll.setHorizontalScrollBarEnabled(false);
        tableScroll.setFillViewport(true);
        LinearLayout rows=new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tableScroll.addView(rows,new ViewGroup.LayoutParams(-1,-2));
        invoiceBox.addView(tableScroll,new LinearLayout.LayoutParams(-1,dp(260)));

        TextView boxTotal=tv("إجمالي الأصناف: 0 ريال",21);
        boxTotal.setTextColor(GREEN); boxTotal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        boxTotal.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        boxTotal.setPadding(dp(12),dp(8),dp(12),dp(8));
        invoiceBox.addView(boxTotal,new LinearLayout.LayoutParams(-1,dp(58)));

        TextView totalView=tv("الإجمالي: 0 ريال",24);
        totalView.setTextColor(GREEN);
        totalView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        totalView.setGravity(Gravity.CENTER);
        totalView.setBackground(bg(Color.rgb(255,249,226),14));
        invoiceBox.addView(totalView,new LinearLayout.LayoutParams(-1,dp(66)));
        content.addView(invoiceBox,new LinearLayout.LayoutParams(-1,-2)); space(10);

        final ArrayList<Line> lines=new ArrayList<>();
        if(edit){Cursor c=db.invoiceLines(invoiceId);while(c.moveToNext())lines.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}

        Runnable redraw=()->{
            rows.removeAllViews();
            double run=0,baseBal=db.balanceByName(customer.getText().toString().trim());
            for(Line l:lines){run+=l.total;addRow(rows,l,run,baseBal,lines);}
            totalView.setText("الإجمالي: "+fmt(run)+" ريال");
            boxTotal.setText("إجمالي الأصناف: "+fmt(run)+" ريال");
        };

        add.setOnClickListener(v->{
            try{
                double t=Double.parseDouble(total.getText().toString().trim());
                double q=Double.parseDouble(qty.getText().toString().trim());
                String n=item.getText().toString().trim();
                if(n.isEmpty()||q<=0||t<0)throw new Exception();
                lines.add(new Line(n,q,t));
                redraw.run();
                total.setText("");qty.setText("1");item.setText("");total.requestFocus();
            }catch(Exception e){
                Toast.makeText(this,"أدخل الإجمالي والكمية واسم الصنف بشكل صحيح",Toast.LENGTH_SHORT).show();
            }
        });

        Button clear=btn("مسح الأصناف");clear.setTextColor(MUTED);
        content.addView(clear,new LinearLayout.LayoutParams(-1,dp(44)));
        clear.setOnClickListener(v->{lines.clear();redraw.run();});
        addSpace(6);

        Button save=action(edit?"💾  حفظ التعديل":"💾  حفظ الفاتورة",GREEN);
        content.addView(save,new LinearLayout.LayoutParams(-1,dp(56)));addSpace(6);
        Button print=btn("🖨  طباعة مباشرة — بلوتوث 58mm");print.setTextColor(GREEN);
        content.addView(print,new LinearLayout.LayoutParams(-1,dp(46)));

        save.setOnClickListener(v->{
            if(lines.isEmpty()){Toast.makeText(this,"أضف صنفاً واحداً على الأقل",Toast.LENGTH_SHORT).show();return;}
            String cn=customer.getText().toString().trim();
            if(cn.isEmpty()){Toast.makeText(this,"اكتب اسم العميل، أو اتركه للفاتورة النقدية",Toast.LENGTH_SHORT).show();return;}
            showPhoneDialog(cn,no.getText().toString(),lines,totalOf(lines),edit,invoiceId);
        });
        print.setOnClickListener(v->printInvoiceBluetooth(no.getText().toString(),customer.getText().toString(),lines,totalOf(lines)));
        item.setOnEditorActionListener((v,a,e)->{add.performClick();return true;});
        redraw.run();
    }

    double totalOf(ArrayList<Line> ls){double x=0;for(Line l:ls)x+=l.total;return x;}
    void showPhoneDialog(String name,String no,ArrayList<Line> lines,double total,boolean edit,long oldId){
        EditText phone=field("رقم هاتف العميل (اختياري)");phone.setText(db.phoneByName(name));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        box.addView(tv("إن كان العميل جديداً سيُضاف تلقائياً. بدون هاتف سيُحفظ رقم عميل داخلي مثل C00001، وليس رقماً هاتفياً وهمياً.",13));
        box.addView(phone,new LinearLayout.LayoutParams(-1,dp(58)));
        new AlertDialog.Builder(this).setTitle("تأكيد بيانات العميل").setView(box).setPositiveButton("حفظ",(d,w)->{
            String p=phone.getText().toString().trim();long cid=db.customer(name,p);String date=db.now();
            if(edit){String oldNo=db.invoiceNo(oldId);db.deleteInvoiceTransaction(oldNo);db.updateInvoice(oldId,no,name,total,date);db.replaceInvoiceLines(oldId,lines);}
            else{long id=db.addInvoice(no,name,total,date);db.replaceInvoiceLines(id,lines);}
            db.addTransactionOnce(cid,total,"فاتورة مبيعات رقم "+no,date);
            invoiceHistory(); showPostSaveActions(no,name,lines,total,cid);
        }).setNegativeButton("إلغاء",null).show();
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable bg(int color,float radius){return rounded(color,dp((int)radius));}
    GradientDrawable outline(int color,float radius){return outlined(color,1,dp((int)radius));}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(outline(CARD,16));c.setElevation(dp(1));return c;}
    void addCard(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(h)));space(8);}
    void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(h)));space(6);}
    void space(int h){addSpace(dp(h));}
    void spaceInside(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    Button action(String text,int color){Button b=button(text);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setBackground(rounded(color,dp(14)));return b;}
    Button btn(String text){Button b=button(text);b.setTextColor(TEXT);b.setBackground(outline(CARD,14));return b;}
    static class Line{String name;double qty,total;Line(String n,double q,double t){name=n;qty=q;total=t;}}
    void addRow(LinearLayout parent,Line l,double running,double baseBal,ArrayList<Line> all){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(3),dp(2),dp(3));
        float[] w={1.0f,.72f,1.35f,.9f,.55f};
        TextView total=tv(fmt(l.total),13);total.setGravity(Gravity.CENTER);total.setSingleLine(true);
        TextView qty=tv(fmt(l.qty),13);qty.setGravity(Gravity.CENTER);qty.setSingleLine(true);
        TextView item=tv(l.name,12);item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);item.setMaxLines(2);item.setEllipsize(TextUtils.TruncateAt.END);
        TextView unit=tv(fmt(l.total/l.qty),12);unit.setTextColor(MUTED);unit.setGravity(Gravity.CENTER);unit.setSingleLine(true);
        Button del=button("حذف");del.setTextSize(11);del.setTextColor(Color.RED);del.setBackgroundColor(Color.TRANSPARENT);
        View[] cells={total,qty,item,unit,del};for(int i=0;i<cells.length;i++)r.addView(cells[i],new LinearLayout.LayoutParams(0,dp(56),w[i]));
        del.setOnClickListener(v->{all.remove(l);redrawInvoiceRows(parent,all,baseBal);});parent.addView(r,new LinearLayout.LayoutParams(-1,dp(56)));
    }
    void redrawInvoiceRows(LinearLayout parent,ArrayList<Line> all,double baseBal){parent.removeAllViews();double run=0;for(Line x:all){run+=x.total;addRow(parent,x,run,baseBal,all);}}
    void spaceTo(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    void preview(String no,String customer,ArrayList<Line> lines,double total){
        String s=receiptTextFromLines(no,customer,lines,total,customer.isEmpty()?-1:db.customer(customer));
        TextView v=tv(s,12);v.setTypeface(Typeface.MONOSPACE);v.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this).setTitle("معاينة إيصال 58mm").setView(v)
            .setPositiveButton("مشاركة",(d,w)->shareReceiptImageAndText(no,customer,lines,total))
            .setNeutralButton("طباعة",(d,w)->printInvoiceBluetooth(no,customer,lines,total))
            .setNegativeButton("إغلاق",null).show();
    }

    String receiptTextFromLines(String no,String customer,ArrayList<Line> lines,double total,long cid){
        StringBuilder s=new StringBuilder("بقالة العزي\nفاتورة رقم: ").append(no).append("\nالتاريخ: ").append(db.now()).append("\n");
        if(!customer.trim().isEmpty())s.append("العميل: ").append(customer).append("\n");
        s.append("------------------------------\n");
        for(Line l:lines)s.append(l.name).append("\nالكمية: ").append(fmt(l.qty)).append("   سعر الوحدة: ").append(fmt(l.total/l.qty)).append("   الإجمالي: ").append(fmt(l.total)).append(" ريال\n");
        s.append("------------------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\n");if(cid>0)s.append(balanceText(db.balance(cid))).append("\n");s.append("شكراً لتعاملكم معنا");return s.toString();
    }

    void showPostSaveActions(String no,String customer,ArrayList<Line> lines,double total,long cid){
        final Dialog dialog=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(12),dp(16),dp(10));box.setBackground(rounded(CARD,dp(18)));
        TextView title=tv("تم حفظ الفاتورة",17);title.setTextColor(GREEN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView sub=tv("الرصيد بعد الفاتورة: "+balanceText(db.balance(cid)),12);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button share=button("مشاركة");share.setTextColor(Color.WHITE);share.setBackgroundColor(GREEN);Button hide=button("إخفاء");hide.setTextColor(MUTED);
        actions.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(hide,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(actions);
        share.setOnClickListener(v->{dialog.dismiss();shareReceiptImageAndText(no,customer,lines,total);});hide.setOnClickListener(v->dialog.dismiss());
        dialog.setContentView(box);dialog.setCanceledOnTouchOutside(true);dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(dp(320),WindowManager.LayoutParams.WRAP_CONTENT);dialog.getWindow().setGravity(Gravity.CENTER);}
    }

    void invoiceHistory(){base("الفواتير");section("سجل الفواتير");Cursor c=db.invoices();while(c.moveToNext()){long id=c.getLong(0);String no=c.getString(1),cn=c.getString(2),date=c.getString(3);double total=c.getDouble(4);LinearLayout r=card();r.addView(tv("فاتورة "+no+"\n"+(cn==null||cn.isEmpty()?"نقدي":cn)+"   •   "+fmt(total)+" ريال\n"+date,14));LinearLayout a=new LinearLayout(this);Button edit=button("تعديل"),del=button("حذف");edit.setTextColor(GREEN);del.setTextColor(Color.RED);a.addView(edit,new LinearLayout.LayoutParams(0,46,1));a.addView(del,new LinearLayout.LayoutParams(0,46,1));r.addView(a);edit.setOnClickListener(v->invoice(true,id));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف الفاتورة؟").setMessage("سيتم حذف الفاتورة وحركتها من حساب العميل.").setPositiveButton("حذف",(d,w)->{db.deleteInvoice(id);invoiceHistory();}).setNegativeButton("إلغاء",null).show());addCard(r,128);}c.close();}
    String receiptText(String no,String customer,LinearLayout rows,double total,long cid){StringBuilder s=new StringBuilder("بقالة العزي\nفاتورة رقم: ").append(no).append("\nالتاريخ: ").append(db.now()).append("\n");if(!customer.isEmpty())s.append("العميل: ").append(customer).append("\n");s.append("------------------------------\n");for(int i=0;i<rows.getChildCount();i++){View ch=rows.getChildAt(i);if(ch instanceof LinearLayout){LinearLayout r=(LinearLayout)ch;StringBuilder q=new StringBuilder();for(int j=0;j<r.getChildCount();j++){View x=r.getChildAt(j);if(x instanceof TextView){String z=((TextView)x).getText().toString().trim();if(!z.isEmpty()){if(q.length()>0)q.append(" | ");q.append(z);}}}if(q.length()>0)s.append(q).append("\n");}}s.append("------------------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\n");if(cid>0)s.append(balanceText(db.balance(cid))).append("\n");s.append("شكراً لتعاملكم معنا");return s.toString();}
    String balanceText(double b){return b>0?"رصيد العميل عليه: "+fmt(b)+" ريال":b<0?"رصيد العميل له: "+fmt(Math.abs(b))+" ريال":"رصيد العميل: 0 ريال";}
    void shareAccountPdfToWhatsApp(long id,String name){
        try{
            String pdfText=statement(id,name);
            File dir=new File(getCacheDir(),"statements"); if(!dir.exists())dir.mkdirs();
            File file=new File(dir,"statement_"+id+"_"+System.currentTimeMillis()+".pdf");
            android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo info=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create();
            android.graphics.pdf.PdfDocument.Page page=pdf.startPage(info);
            Canvas canvas=page.getCanvas(); Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(Color.BLACK); paint.setTextSize(dp(13));
            int y=dp(36); String[] ls=pdfText.split("\\n");
            for(String line:ls){ if(y>800){pdf.finishPage(page);info=new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pdf.getPages().size()+1).create();page=pdf.startPage(info);canvas=page.getCanvas();y=dp(36);} canvas.drawText(new StringBuilder(line).reverse().toString(),575,y,paint); y+=dp(22);}
            pdf.finishPage(page); FileOutputStream out=new FileOutputStream(file);pdf.writeTo(out);out.close();pdf.close();
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            String phone=db.phoneByName(name); Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.putExtra(Intent.EXTRA_TEXT,"كشف حساب العميل: "+name);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String p=phone==null?"":phone.replaceAll("[^0-9+]","");if(!p.isEmpty())i.putExtra("jid",p.replace("+","")+"@s.whatsapp.net");
            try{i.setPackage("com.whatsapp");startActivity(i);}catch(Exception e){i.setPackage(null);startActivity(Intent.createChooser(i,"إرسال كشف الحساب"));}
        }catch(Exception e){Toast.makeText(this,"تعذر إنشاء ملف PDF",Toast.LENGTH_LONG).show();}
    }
    void shareText(String s){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"إرسال الفاتورة"));}
    void shareWhatsAppToCustomer(String phone,String text,Uri image){
        String p=phone==null?"":phone.replaceAll("[^0-9+]","");
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("image/png");
        i.putExtra(Intent.EXTRA_TEXT,text); if(image!=null){i.putExtra(Intent.EXTRA_STREAM,image);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        if(!p.isEmpty()) i.putExtra("jid",p.replace("+","")+"@s.whatsapp.net");
        try{ i.setPackage("com.whatsapp"); startActivity(i); }catch(Exception e){ i.setPackage(null); startActivity(Intent.createChooser(i,"مشاركة الفاتورة")); }
    }
    Bitmap receiptBitmap(String text){int width=384,pad=dp(8);TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(dp(15));paint.setTypeface(Typeface.create("sans",Typeface.NORMAL));StaticLayout layout=new StaticLayout(text,paint,width-pad*2,Layout.Alignment.ALIGN_CENTER,1.0f,dp(4),false);Bitmap b=Bitmap.createBitmap(width,Math.max(dp(80),layout.getHeight()+dp(20)),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);canvas.save();canvas.translate(pad,dp(8));layout.draw(canvas);canvas.restore();return b;}
    Uri saveReceiptBitmap(Bitmap bitmap,String no)throws Exception{File dir=new File(getCacheDir(),"receipts");if(!dir.exists())dir.mkdirs();File file=new File(dir,"invoice_"+no+"_"+System.currentTimeMillis()+".png");FileOutputStream out=new FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.PNG,100,out);out.close();return FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);}
    void shareReceiptImageAndText(String no,String customer,ArrayList<Line> lines,double total){try{long cid=customer.trim().isEmpty()?-1:db.customer(customer);String text=receiptTextFromLines(no,customer,lines,total,cid);Uri uri=saveReceiptBitmap(receiptBitmap(text),no);String phone=db.phoneByName(customer);shareWhatsAppToCustomer(phone,text,uri);}catch(Exception e){shareText(receiptTextFromLines(no,customer,lines,total,customer.isEmpty()?-1:db.customer(customer)));}}
    String pendingPrintNo="",pendingPrintCustomer="";ArrayList<Line> pendingPrintLines;double pendingPrintTotal;
    void printInvoiceBluetooth(String no,String customer,ArrayList<Line> lines,double total){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED){pendingPrintNo=no;pendingPrintCustomer=customer;pendingPrintLines=new ArrayList<>(lines);pendingPrintTotal=total;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},5101);return;}
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();if(adapter==null){Toast.makeText(this,"هذا الجهاز لا يدعم البلوتوث",Toast.LENGTH_LONG).show();return;}if(!adapter.isEnabled()){Toast.makeText(this,"فعّل البلوتوث ثم أعد الضغط على الطباعة",Toast.LENGTH_LONG).show();return;}
        Set<BluetoothDevice> paired=adapter.getBondedDevices();if(paired==null||paired.isEmpty()){Toast.makeText(this,"لا توجد طابعة مقترنة. اقترن بالطابعة من إعدادات البلوتوث أولاً.",Toast.LENGTH_LONG).show();return;}
        BluetoothDevice[] devices=paired.toArray(new BluetoothDevice[0]);String[] names=new String[devices.length];for(int i=0;i<devices.length;i++)names[i]=(devices[i].getName()==null?"طابعة بلوتوث":devices[i].getName())+"\n"+devices[i].getAddress();
        new AlertDialog.Builder(this).setTitle("اختر طابعة 58mm").setItems(names,(d,w)->printToBluetooth(devices[w],no,customer,lines,total)).setNegativeButton("إلغاء",null).show();
    }
    void printToBluetooth(BluetoothDevice device,String no,String customer,ArrayList<Line> lines,double total){String text=receiptTextFromLines(no,customer,lines,total,customer.isEmpty()?-1:db.customer(customer));Bitmap bitmap=receiptBitmap(text);new Thread(()->{BluetoothSocket socket=null;OutputStream out=null;try{socket=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();out=socket.getOutputStream();out.write(new byte[]{0x1B,0x40});out.write(rasterBytes(bitmap));out.write(new byte[]{0x0A,0x0A,0x0A});out.flush();runOnUiThread(()->Toast.makeText(this,"تم إرسال الفاتورة إلى الطابعة",Toast.LENGTH_SHORT).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"تعذر الاتصال بالطابعة: "+(e.getMessage()==null?"تحقق من الاقتران":e.getMessage()),Toast.LENGTH_LONG).show());}finally{try{if(out!=null)out.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}}).start();}
    byte[] rasterBytes(Bitmap bitmap){int width=bitmap.getWidth(),height=bitmap.getHeight(),bpr=(width+7)/8;byte[] out=new byte[8+bpr*height];out[0]=0x1D;out[1]=0x76;out[2]=0x30;out[3]=0;out[4]=(byte)(bpr&255);out[5]=(byte)((bpr>>8)&255);out[6]=(byte)(height&255);out[7]=(byte)((height>>8)&255);int p=8;for(int y=0;y<height;y++)for(int xb=0;xb<bpr;xb++){int v=0;for(int bit=0;bit<8;bit++){int x=xb*8+bit;if(x<width){int px=bitmap.getPixel(x,y);int g=(Color.red(px)+Color.green(px)+Color.blue(px))/3;if(g<180)v|=1<<(7-bit);}}out[p++]=(byte)v;}return out;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_CONTACTS){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)importContact();else Toast.makeText(this,"يلزم السماح بالوصول إلى جهات الاتصال",Toast.LENGTH_LONG).show();}else if(requestCode==5101&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&pendingPrintLines!=null){printInvoiceBluetooth(pendingPrintNo,pendingPrintCustomer,pendingPrintLines,pendingPrintTotal);}}
    void thermalPreview(String no,String customer,LinearLayout rows,double total){preview(no,customer,new ArrayList<Line>(),total);}
    static String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("الحسابات"); section("حسابات العملاء");
        EditText search=field("بحث بالاسم أو الهاتف"); addField(search);

        LinearLayout addBox=new LinearLayout(this); addBox.setOrientation(LinearLayout.VERTICAL);
        addBox.setPadding(dp(12),dp(10),dp(12),dp(10)); addBox.setBackground(outlined(CARD,1,16));
        TextView addTitle=tv("إضافة عميل جديد",17); addTitle.setTextColor(GREEN); addTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        addBox.addView(addTitle,new LinearLayout.LayoutParams(-1,dp(38)));
        EditText name=field("اسم العميل"), phone=field("رقم الهاتف");
        customerNameInput=name; customerPhoneInput=phone;
        addBox.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(7)));
        addBox.addView(phone,new LinearLayout.LayoutParams(-1,dp(56)));
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(8)));
        LinearLayout contactActions=new LinearLayout(this); contactActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("👤 جهات الاتصال"); pick.setTextColor(GREEN); pick.setOnClickListener(v->importContact());
        Button add=button("＋ إضافة العميل"); add.setTextColor(Color.WHITE); add.setBackgroundColor(GREEN);
        contactActions.addView(pick,new LinearLayout.LayoutParams(0,dp(52),1));
        contactActions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1)); addBox.addView(contactActions);
        content.addView(addBox,new LinearLayout.LayoutParams(-1,-2)); addSpace(12);

        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); content.addView(list);
        Runnable refresh=()->{
            list.removeAllViews(); Cursor c=db.customers(search.getText().toString());
            while(c.moveToNext()){
                long id=c.getLong(0); String n=c.getString(1),p=c.getString(2); double bal=db.balance(id);
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14),dp(10),dp(14),dp(10)); card.setBackground(outlined(CARD,1,16)); card.setOnClickListener(v->account(id,n));
                TextView title=tv(n,18); title.setTextColor(GREEN); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                card.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
                TextView sub=tv((p==null||p.isEmpty()?"بدون رقم":p)+"   •   "+db.transactionCount(id)+" عملية",12); sub.setTextColor(MUTED);
                card.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
                TextView balance=tv(balanceText(bal),15); balance.setTextColor(bal>0?Color.rgb(190,55,45):GREEN);
                balance.setTypeface(Typeface.DEFAULT,Typeface.BOLD); card.addView(balance,new LinearLayout.LayoutParams(-1,dp(34)));
                list.addView(card,new LinearLayout.LayoutParams(-1,dp(108))); addSpaceTo(list,8);
            } c.close();
        };
        add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم العميل",Toast.LENGTH_SHORT).show();return;}db.addCustomer(n,phone.getText().toString().trim());name.setText("");phone.setText("");refresh.run();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}public void afterTextChanged(android.text.Editable e){}});
        refresh.run();
    }
    void addSpaceTo(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,h));}
    void account(long id,String name){
        base("حساب العميل"); section(name);
        LinearLayout summary=card();
        TextView bal=tv(balanceText(db.balance(id)),22); bal.setTextColor(GREEN); bal.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bal.setGravity(Gravity.CENTER);
        summary.addView(bal,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView hint=tv("الأحدث أولاً • يظهر تاريخ وبيانات العملية والرصيد التراكمي بعد كل عملية",11); hint.setTextColor(MUTED); hint.setGravity(Gravity.CENTER);
        summary.addView(hint,new LinearLayout.LayoutParams(-1,dp(34))); addCard(summary,94);

        EditText amount=field("المبلغ"); addField(amount); EditText details=field("التفاصيل"); addField(details);
        LinearLayout acts=new LinearLayout(this); acts.setOrientation(LinearLayout.HORIZONTAL);
        Button debit=button("عليه"),credit=button("له / دفعة"); debit.setTextColor(Color.RED);credit.setTextColor(GREEN);
        acts.addView(debit,new LinearLayout.LayoutParams(0,dp(50),1));acts.addView(credit,new LinearLayout.LayoutParams(0,dp(50),1));content.addView(acts);addSpace(8);
        Button share=button("📄 PDF + واتساب");share.setTextColor(GREEN);content.addView(share,new LinearLayout.LayoutParams(-1,dp(54)));share.setOnClickListener(v->shareAccountPdfToWhatsApp(id,name));
        section("سجل العمليات"); LinearLayout history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);content.addView(history);

        Runnable refresh=()->{
            history.removeAllViews(); double runningAfter=db.balance(id); Cursor c=db.transactions(id);
            while(c.moveToNext()){
                String date=c.getString(1),d=c.getString(2);double amountValue=c.getDouble(3);int type=c.getInt(4);
                LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(12),dp(8),dp(12),dp(8));r.setBackground(outlined(CARD,1,14));
                TextView dateV=tv(date,11);dateV.setTextColor(MUTED);r.addView(dateV,new LinearLayout.LayoutParams(-1,dp(30)));
                TextView detV=tv(d==null||d.trim().isEmpty()?"عملية مالية":d,13);detV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(detV,new LinearLayout.LayoutParams(-1,dp(40)));
                TextView amtV=tv((type==1?"عليه: ":"له: ")+fmt(amountValue)+" ريال",13);amtV.setTextColor(type==1?Color.rgb(190,55,45):GREEN);r.addView(amtV,new LinearLayout.LayoutParams(-1,dp(30)));
                TextView balV=tv("الرصيد بعد العملية: "+balanceText(runningAfter),12);balV.setTextColor(GREEN);r.addView(balV,new LinearLayout.LayoutParams(-1,dp(30)));
                String invNo=db.invoiceNoFromTransaction(d);
                if(!TextUtils.isEmpty(invNo)){TextView iv=tv(db.invoiceCompactDetails(invNo),10);iv.setTextColor(MUTED);iv.setMaxLines(2);iv.setEllipsize(TextUtils.TruncateAt.END);r.addView(iv,new LinearLayout.LayoutParams(-1,dp(42)));}
                history.addView(r,new LinearLayout.LayoutParams(-1,-2));addSpaceTo(history,8);
                runningAfter-=(type==1?amountValue:-amountValue);
            } c.close(); bal.setText(balanceText(db.balance(id)));
        };
        View.OnClickListener add=v->{try{double a=Double.parseDouble(amount.getText().toString());if(a<=0)throw new Exception();db.addTransaction(id,a,details.getText().toString().trim(),v==debit?1:0,db.now());amount.setText("");details.setText("");refresh.run();}catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}};
        debit.setOnClickListener(add);credit.setOnClickListener(add);refresh.run();
    }
    String statement(long id,String name){StringBuilder s=new StringBuilder("بقالة العزي\nكشف حساب العميل: ").append(name).append("\n");double runningAfter=db.balance(id);Cursor c=db.transactions(id);while(c.moveToNext()){String d=c.getString(1),x=c.getString(2);double a=c.getDouble(3);int t=c.getInt(4);s.append(d).append(" | ").append(x==null?"":x).append(" | ").append(t==1?"عليه":"له").append(": ").append(fmt(a)).append(" ريال | ").append(balanceText(runningAfter)).append("\n");runningAfter-=(t==1?a:-a);}c.close();s.append("--------------------\n").append(balanceText(db.balance(id)));return s.toString();}

    void inventory(){
        base("المخزون");section("إضافة صنف");
        EditText name=field("اسم الصنف");EditText qty=field("الكمية");EditText min=field("الحد الأدنى");addField(name);addField(qty);addField(min);
        Button add=button("＋ حفظ الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);content.addView(add,new LinearLayout.LayoutParams(-1,dp(48)));addSpace(8);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable refresh=()->{list.removeAllViews();Cursor c=db.items();while(c.moveToNext()){double q=c.getDouble(2),m=c.getDouble(3);TextView r=tv(c.getString(1)+"\nالكمية: "+fmt(q)+"   •   الحد الأدنى: "+fmt(m)+(q<=m?"   ⚠ منخفض":""),
                14);r.setBackgroundColor(CARD);r.setTextColor(q<=m?Color.rgb(170,75,35):TEXT);list.addView(r,new LinearLayout.LayoutParams(-1,dp(68)));addSpaceTo(list,5);}c.close();};
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
        String invoiceNoFromTransaction(String details){if(details==null)return "";String p="فاتورة مبيعات رقم ";return details.startsWith(p)?details.substring(p.length()).trim():"";}
        String invoiceCompactDetails(String no){Cursor c=getReadableDatabase().rawQuery("SELECT name,qty,total FROM invoice_items WHERE invoice_id=(SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1) ORDER BY id",new String[]{no});StringBuilder s=new StringBuilder("تفاصيل: ");int n=0;while(c.moveToNext()&&n<6){if(n>0)s.append(" • ");s.append(c.getString(0)).append(" × ").append(fmt(c.getDouble(1))).append(" = ").append(fmt(c.getDouble(2)));n++;}c.close();return n==0?"تفاصيل الفاتورة غير متاحة":s.toString();}
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
