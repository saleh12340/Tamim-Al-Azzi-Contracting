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
import android.text.InputType;
import android.text.method.DigitsKeyListener;
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
import android.graphics.drawable.Drawable;
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
    DB db; LinearLayout root,content,bottom; TextView pageTitle; int textSize=16; String currentPage="الرئيسية"; ArrayDeque<String> pageStack=new ArrayDeque<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        db=new DB(this); home();
    }

    void confirmExit(){
        new AlertDialog.Builder(this)
            .setTitle("تأكيد الخروج")
            .setMessage("هل تريد الخروج من التطبيق؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("خروج",(d,w)->finish())
            .show();
    }
    @Override public void onBackPressed(){ goBack(); }
    void goBack(){
        if(pageStack.isEmpty()){ confirmExit(); return; }
        String prev=pageStack.pop();
        if(prev.equals("الرئيسية")) home();
        else if(prev.equals("الحسابات")||prev.equals("العملاء")) customers();
        else if(prev.equals("الفواتير")) invoiceHistory();
        else if(prev.equals("المخزون")) inventory();
        else if(prev.equals("التقارير")) reports();
        else home();
    }

    GradientDrawable rounded(int color,float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); return g; }
    GradientDrawable outlined(int color,int stroke,float radius){ GradientDrawable g=rounded(color,radius); g.setStroke(stroke,Color.rgb(224,230,225)); return g; }
    float fitText(float z){return Math.max(9f,Math.min(z,16f));}
    void fitInside(View v,float maxSp,float minSp){
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setIncludeFontPadding(true);
            t.setHorizontallyScrolling(false);
            t.setEllipsize(null);
            t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            if(android.os.Build.VERSION.SDK_INT>=26){
                t.setAutoSizeTextTypeUniformWithConfiguration(dp((int)minSp),dp((int)maxSp),1,android.util.TypedValue.COMPLEX_UNIT_PX);
            }
        }
    }
    TextView tv(String s,float z){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(fitText(z)); v.setTextColor(TEXT);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setPadding(dp(6),dp(2),dp(6),dp(2));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); v.setTextDirection(View.TEXT_DIRECTION_RTL);
        fitInside(v,fitText(z),8f); return v;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(fitText(11)); b.setAllCaps(false); b.setMinHeight(0);
        b.setMinimumHeight(0); b.setPadding(dp(5),dp(0),dp(5),dp(0)); b.setGravity(Gravity.CENTER); b.setStateListAnimator(null);
        b.setIncludeFontPadding(true); b.setMaxLines(3); b.setEllipsize(null);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); fitInside(b,12f,8f); return b;
    }
    EditText field(String h){
        EditText e=new EditText(this); e.setHint(h); e.setTextSize(13); e.setSingleLine(true);
        e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setPadding(dp(7),dp(2),dp(7),dp(2)); e.setBackground(outlined(CARD,1,10)); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setSelectAllOnFocus(true); e.setOnClickListener(v -> e.selectAll());
        e.setOnFocusChangeListener((v,has)->{ if(has) e.postDelayed(() -> { e.selectAll(); },60); });
        return e;
    }
    EditText numberField(String h){
        EditText e=field(h);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        return e;
    }
    EditText phoneField(String h){
        EditText e=field(h);
        e.setInputType(InputType.TYPE_CLASS_PHONE);
        e.setRawInputType(InputType.TYPE_CLASS_PHONE);
        return e;
    }
    void addField(EditText e){content.addView(e,new LinearLayout.LayoutParams(-1,dp(38))); addSpace(2);}
    void addSpace(int h){Space s=new Space(this); content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    TextView section(String s){TextView v=tv(s,11);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(false);v.setMaxLines(2);v.setEllipsize(null);v.setPadding(dp(3),dp(4),dp(3),dp(2));content.addView(v,new LinearLayout.LayoutParams(-1,dp(26)));return v;}

    void base(String title){
        if(!title.equals(currentPage)){
            pageStack.push(currentPage);
            currentPage=title;
        }
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(6),dp(3),dp(6),dp(3)); bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        Button back=button("‹");
        back.setTextColor(Color.WHITE); back.setTextSize(28); back.setBackgroundColor(Color.TRANSPARENT);
        back.setContentDescription("رجوع للشاشة السابقة"); back.setOnClickListener(v->goBack());
        bar.addView(back,new LinearLayout.LayoutParams(dp(46),dp(40)));
        TextView logo=tv("بقالة العزي",19); logo.setTextColor(Color.WHITE); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bar.addView(logo,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView pt=tv(title,14); pt.setTextColor(Color.WHITE); pt.setGravity(Gravity.CENTER);
        bar.addView(pt,new LinearLayout.LayoutParams(dp(105),dp(40))); root.addView(bar);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.setClipToPadding(false);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(5),dp(4),dp(5),dp(8)); content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView operationChip=tv("العملية الحالية: "+title,10); operationChip.setTextColor(GREEN); operationChip.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); operationChip.setSingleLine(false); operationChip.setMaxLines(2); operationChip.setEllipsize(null); operationChip.setPadding(dp(8),0,dp(8),0); operationChip.setBackground(outline(Color.rgb(241,247,242),8)); content.addView(operationChip,new LinearLayout.LayoutParams(-1,dp(24))); addSpace(2);
        sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
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
    }    void addAction(String a,String sub,View.OnClickListener l){        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,5,12,5);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        Button b=button(a);b.setTextSize(13);b.setTextColor(TEXT);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setOnClickListener(l);c.addView(b,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView s=tv(sub,12);s.setTextColor(MUTED);c.addView(s,new LinearLayout.LayoutParams(-1,30));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(82)); ap.setMargins(0,0,0,7); content.addView(c,ap);
    }

    void home(){
        base("الرئيسية");

        LinearLayout hero=card(); hero.setPadding(dp(20),dp(16),dp(20),dp(16));
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        TextView h=tv("بقالة العزي",26);h.setTextColor(Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        hero.addView(h,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView hs=tv("المبيعات • حسابات العملاء • المخزون • التقارير\nيعمل محلياً بدون إنترنت ويحفظ بياناتك على الجهاز",14);
        hs.setTextColor(Color.WHITE);hero.addView(hs,new LinearLayout.LayoutParams(-1,dp(36)));
        addCard(hero,120);

        LinearLayout quick=card();
        quick.addView(tv("إجراء سريع",17),new LinearLayout.LayoutParams(-1,dp(34)));
        Button ni=action("＋  إضافة فاتورة جديدة",GOLD);ni.setTextSize(18);ni.setOnClickListener(v->invoice());
        quick.addView(ni,new LinearLayout.LayoutParams(-1,dp(36)));
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
        content.addView(general,new LinearLayout.LayoutParams(-1,dp(40)));
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
        metaRow.addView(no,new LinearLayout.LayoutParams(0,dp(42),0.72f));

        AutoCompleteTextView customer=new AutoCompleteTextView(this);
        customer.setHint("اسم العميل");customer.setTextSize(14);customer.setSingleLine(true);
        customer.setTextColor(TEXT);customer.setHintTextColor(MUTED);
        customer.setPadding(dp(10),dp(6),dp(10),dp(6));customer.setBackground(outline(CARD,14));
        customer.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        customer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);customer.setTextDirection(View.TEXT_DIRECTION_RTL);
        customer.setThreshold(1);customer.setSelectAllOnFocus(true);
        customer.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,db.customerNames()));
        metaRow.addView(customer,new LinearLayout.LayoutParams(0,dp(42),1.35f));

        TextView dt=tv(db.now(),12);dt.setTextColor(MUTED);dt.setGravity(Gravity.CENTER);
        dt.setBackground(outline(Color.rgb(248,250,248),14));
        metaRow.addView(dt,new LinearLayout.LayoutParams(0,dp(42),1.15f));
        content.addView(metaRow);space(8);
        if(edit) customer.setText(db.invoiceCustomer(invoiceId));
        section("إدخال الصنف");
        LinearLayout entry=card();entry.setPadding(dp(10),dp(10),dp(10),dp(10));
        LinearLayout line=new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText total=numberField("الإجمالي");
        EditText qty=numberField("الكمية");
        AutoCompleteTextView item=new AutoCompleteTextView(this);
        item.setHint("اسم الصنف / التفاصيل"); item.setTextSize(13); item.setSingleLine(true); item.setTextColor(TEXT); item.setHintTextColor(MUTED);
        item.setPadding(dp(7),dp(2),dp(7),dp(2)); item.setBackground(outlined(CARD,1,10)); item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        item.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); item.setTextDirection(View.TEXT_DIRECTION_RTL); item.setSelectAllOnFocus(true);
        item.setOnClickListener(v->item.selectAll());
        item.setOnFocusChangeListener((v,has)->{if(has)item.postDelayed(()->item.selectAll(),60);});
        ArrayList<String> itemSuggestions=new ArrayList<>(Arrays.asList("السمن"));
        Cursor itemCursor=db.items(); while(itemCursor.moveToNext()) itemSuggestions.add(itemCursor.getString(1)); itemCursor.close();
        item.setThreshold(1); item.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,itemSuggestions));
        total.setInputType(2|8192);qty.setInputType(2|8192);qty.setText("1");

        // لا يوجد سعر وحدة في صف الإدخال؛ يُعرض محسوباً داخل صندوق تفاصيل الفاتورة بالأسفل.
        line.addView(total,new LinearLayout.LayoutParams(0,dp(36),1.0f));
        line.addView(qty,new LinearLayout.LayoutParams(0,dp(36),0.72f));
        line.addView(item,new LinearLayout.LayoutParams(0,dp(36),1.35f));
        entry.addView(line);
        Button add=action("＋  إضافة الصنف / التعامل",GREEN);
        entry.addView(add,new LinearLayout.LayoutParams(-1,dp(42)));
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
            TextView hv=tv(heads[i],9);
            hv.setTextColor(GREEN);hv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            hv.setGravity(Gravity.CENTER);hv.setSingleLine(true);
            hv.setBackgroundColor(Color.TRANSPARENT);
            head.addView(hv,new LinearLayout.LayoutParams(0,dp(38),weights[i]));
        }
        table.addView(head,new LinearLayout.LayoutParams(-1,dp(28)));

        ScrollView tableScroll=new ScrollView(this);
        tableScroll.setFillViewport(true);
        tableScroll.setVerticalScrollBarEnabled(true);
        LinearLayout rows=new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tableScroll.addView(rows,new ViewGroup.LayoutParams(-1,-2));
        table.addView(tableScroll,new LinearLayout.LayoutParams(-1,dp(248))); invoiceBox.addView(table,new LinearLayout.LayoutParams(-1,-2));

        TextView boxTotal=tv("الإجمالي: 0 ريال",20);
        boxTotal.setTextColor(GREEN); boxTotal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        boxTotal.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        boxTotal.setPadding(dp(10),dp(4),dp(10),dp(4));
        boxTotal.setBackground(bg(Color.rgb(255,249,226),12));
        invoiceBox.addView(boxTotal,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView remainingLabel=tv("المتبقي: 0 ريال",12);remainingLabel.setTextColor(MUTED);remainingLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);remainingLabel.setPadding(dp(10),0,dp(10),0);
        invoiceBox.addView(remainingLabel,new LinearLayout.LayoutParams(-1,dp(30)));
        content.addView(invoiceBox,new LinearLayout.LayoutParams(-1,-2)); space(5);

        final ArrayList<Line> lines=new ArrayList<>();
        if(edit){Cursor c=db.invoiceLines(invoiceId);while(c.moveToNext())lines.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}

        Runnable redraw=()->{
            rows.removeAllViews();
            double run=0,baseBal=db.balanceByName(customer.getText().toString().trim());
            for(Line l:lines){run+=l.total;addRow(rows,l,run,baseBal,lines);}
            boxTotal.setText("الإجمالي: "+fmt(run)+" ريال");
            double currentBalance=customer.getText().toString().trim().isEmpty()?0:db.balanceByName(customer.getText().toString().trim());
            double remaining=currentBalance+run;if(Math.abs(remaining)<0.005)remaining=0;
            remainingLabel.setText("المتبقي: "+fmt(remaining)+" ريال");
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
        content.addView(clear,new LinearLayout.LayoutParams(-1,dp(36)));
        clear.setOnClickListener(v->{lines.clear();redraw.run();});
        addSpace(6);

        Button save=action(edit?"💾  حفظ التعديل":"💾  حفظ الفاتورة",GREEN);
        content.addView(save,new LinearLayout.LayoutParams(-1,dp(36)));addSpace(6);
        Button print=btn("🖨  طباعة مباشرة — بلوتوث 58mm");print.setTextColor(GREEN);        content.addView(print,new LinearLayout.LayoutParams(-1,dp(38))); addSpace(5);
        LinearLayout invoiceBottom=new LinearLayout(this);invoiceBottom.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel=button(edit?"↩ إلغاء التعديل":"↩ إلغاء العملية");cancel.setTextColor(Color.RED);cancel.setBackground(outline(CARD,10));
        Button homeBtn=button("⌂ الرجوع للرئيسية");homeBtn.setTextColor(GREEN);homeBtn.setBackground(outline(CARD,10));
        invoiceBottom.addView(cancel,new LinearLayout.LayoutParams(0,dp(40),1));invoiceBottom.addView(homeBtn,new LinearLayout.LayoutParams(0,dp(40),1));content.addView(invoiceBottom);
        cancel.setOnClickListener(v->invoiceHistory());homeBtn.setOnClickListener(v->home());

        save.setOnClickListener(v->{
            if(lines.isEmpty()){Toast.makeText(this,"أضف صنفاً واحداً على الأقل",Toast.LENGTH_SHORT).show();return;}
            String cn=customer.getText().toString().trim();
            if(cn.isEmpty()){Toast.makeText(this,"اكتب اسم العميل، أو اتركه للفاتورة النقدية",Toast.LENGTH_SHORT).show();return;}
            showPhoneDialog(cn,no.getText().toString(),lines,totalOf(lines),edit,invoiceId);
        });
        print.setOnClickListener(v->preview(no.getText().toString(),customer.getText().toString(),lines,totalOf(lines),edit,invoiceId));
        item.setOnEditorActionListener((v,a,e)->{add.performClick();return true;});
        customer.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){redraw.run();}public void afterTextChanged(android.text.Editable e){}});
        redraw.run();
    }

    double totalOf(ArrayList<Line> ls){double x=0;for(Line l:ls)x+=l.total;return x;}
    void showPhoneDialog(String name,String no,ArrayList<Line> lines,double total,boolean edit,long oldId){
        EditText phone=phoneField("رقم هاتف العميل (اختياري)");phone.setText(db.phoneByName(name));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        box.addView(tv("إن كان العميل جديداً سيُضاف تلقائياً. بدون هاتف سيُحفظ رقم عميل داخلي مثل C00001، وليس رقماً هاتفياً وهمياً.",13));
        box.addView(phone,new LinearLayout.LayoutParams(-1,dp(36)));
        new AlertDialog.Builder(this).setTitle("تأكيد بيانات العميل").setView(box).setPositiveButton("حفظ",(d,w)->{
            String p=phone.getText().toString().trim();long cid=db.customer(name,p);String date=db.now();
            if(edit){String oldNo=db.invoiceNo(oldId);db.deleteInvoiceTransaction(oldNo);db.updateInvoice(oldId,no,name,total,date);db.replaceInvoiceLines(oldId,lines);}
            else{long id=db.addInvoice(no,name,total,date);db.replaceInvoiceLines(id,lines);}
            db.addTransactionOnce(cid,total,"فاتورة مبيعات رقم "+no,date);
            cacheLastInvoice(no,name,lines,total,date);
            Toast.makeText(this,"تم حفظ الفاتورة في قاعدة بيانات الجهاز ونسخة مؤقتة للاسترجاع السريع.",Toast.LENGTH_SHORT).show();
            invoiceHistory(); showPostSaveActions(no,name,lines,total,cid);
        }).setNegativeButton("إلغاء",null).show();
    }

    void cacheLastInvoice(String no,String customer,ArrayList<Line> lines,double total,String date){
        try{
            File dir=new File(getCacheDir(),"invoices"); if(!dir.exists())dir.mkdirs();
            File file=new File(dir,"last_invoice.txt");
            StringBuilder x=new StringBuilder();
            x.append("رقم الفاتورة: ").append(no).append("\nالعميل: ").append(customer).append("\nالتاريخ: ").append(date).append("\n");
            for(Line l:lines)x.append(l.name).append(" | ").append(fmt(l.qty)).append(" | ").append(fmt(l.total)).append("\n");
            x.append("الإجمالي: ").append(fmt(total)).append(" ريال");
            FileOutputStream out=new FileOutputStream(file,false);out.write(x.toString().getBytes("UTF-8"));out.close();
        }catch(Exception ignored){}
    }
    void notifyNewOperation(String title,String text){
        try{
            if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7201); return;
            }
            String channelId="operations";
            android.app.NotificationManager nm=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(android.os.Build.VERSION.SDK_INT>=26){
                android.app.NotificationChannel ch=new android.app.NotificationChannel(channelId,"إشعارات العمليات",android.app.NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("إشعار عند إضافة فاتورة أو عملية جديدة");nm.createNotificationChannel(ch);
            }
            android.app.Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,channelId):new android.app.Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_menu_info_details).setContentTitle(title).setContentText(text).setAutoCancel(true);
            nm.notify((int)(System.currentTimeMillis()%100000),b.build());
        }catch(Exception ignored){}
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable bg(int color,float radius){return rounded(color,dp((int)radius));}
    GradientDrawable outline(int color,float radius){return outlined(color,1,dp((int)radius));}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(8),dp(6),dp(8),dp(6));c.setBackground(outline(CARD,12));c.setElevation(dp(1));return c;}
    void addCard(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(Math.max(50,h-18))));space(4);}
    void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(Math.max(42,h-12))));space(4);}
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
        TextView item=tv(l.name,12);item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);item.setMaxLines(3);item.setEllipsize(null);
        TextView unit=tv(fmt(l.total/l.qty),12);unit.setTextColor(MUTED);unit.setGravity(Gravity.CENTER);unit.setSingleLine(true);
        Button del=button("حذف");del.setTextSize(10);del.setTextColor(Color.RED);del.setBackgroundColor(Color.TRANSPARENT); qty.setContentDescription("تعديل كمية الصنف"); qty.setBackground(outline(Color.rgb(248,250,248),6));
        View[] cells={total,qty,item,unit,del};for(int i=0;i<cells.length;i++)r.addView(cells[i],new LinearLayout.LayoutParams(0,dp(36),w[i]));
        qty.setOnClickListener(v->editLineQuantity(l,parent,all,baseBal)); del.setOnClickListener(v->{all.remove(l);redrawInvoiceRows(parent,all,baseBal);});parent.addView(r,new LinearLayout.LayoutParams(-1,dp(36)));
    }
    void editLineQuantity(Line line,LinearLayout parent,ArrayList<Line> all,double baseBal){
        EditText q=numberField("الكمية");q.setText(fmt(line.qty));q.setSelectAllOnFocus(true);
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(8),dp(4),dp(8),dp(2));box.addView(q,new LinearLayout.LayoutParams(-1,dp(42)));
        new AlertDialog.Builder(this).setTitle("تعديل كمية الصنف").setMessage(line.name+" — الكمية الحالية: "+fmt(line.qty)).setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{double value=Double.parseDouble(q.getText().toString().trim());if(value<=0)throw new Exception();line.qty=value;redrawInvoiceRows(parent,all,baseBal);}catch(Exception e){Toast.makeText(this,"أدخل كمية صحيحة",Toast.LENGTH_SHORT).show();}}).show();
    }
    void redrawInvoiceRows(LinearLayout parent,ArrayList<Line> all,double baseBal){parent.removeAllViews();double run=0;for(Line x:all){run+=x.total;addRow(parent,x,run,baseBal,all);}}
    void spaceTo(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    void preview(String no,String customer,ArrayList<Line> lines,double total,boolean edit,long oldId){
        String cleanCustomer=customer==null?"":customer.trim();
        long cid=cleanCustomer.isEmpty()?-1:db.customerIdByName(cleanCustomer);
        double balanceAfter=0;
        if(cid>0){
            balanceAfter=db.balance(cid)+total;
            if(edit){
                String oldCustomer=db.invoiceCustomer(oldId);
                double oldTotal=db.invoiceTotal(oldId);
                if(oldCustomer.equals(cleanCustomer)) balanceAfter-=oldTotal;
            }
            if(Math.abs(balanceAfter)<0.005) balanceAfter=0;
        }
        String s=receiptTextFromLines(no,cleanCustomer,lines,total,cid,balanceAfter);
        TextView v=tv(s,11);v.setTypeface(Typeface.MONOSPACE);v.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this).setTitle("معاينة إيصال 58mm").setView(v)
            .setPositiveButton("مشاركة واتساب",(d,w)->shareReceiptImageAndText(no,cleanCustomer,lines,total))
            .setNeutralButton("طباعة",(d,w)->printInvoiceBluetooth(no,cleanCustomer,lines,total))
            .setNegativeButton("إغلاق",null).show();
    }

    String receiptTextFromLines(String no,String customer,ArrayList<Line> lines,double total,long cid){
        return receiptTextFromLines(no,customer,lines,total,cid,cid>0?db.balance(cid):0);
    }
    String receiptTextFromLines(String no,String customer,ArrayList<Line> lines,double total,long cid,double balanceAfter){
        StringBuilder s=new StringBuilder();
        s.append("بقالة العزي\n");
        s.append("فاتورة مبيعات رقم: ").append(no).append("\n");
        s.append("التاريخ: ").append(db.now()).append("\n");
        if(!customer.trim().isEmpty())s.append("العميل: ").append(customer.trim()).append("\n");
        s.append("------------------------------\n");
        s.append("الصنف | الكمية | الإجمالي\n");
        for(Line l:lines){
            String n=l.name==null?"":l.name.trim();
            s.append(n).append(" | ").append(fmt(l.qty)).append(" | ").append(fmt(l.total)).append("\n");
        }
        s.append("------------------------------\n");
        s.append("الإجمالي: ").append(fmt(total)).append(" ريال\n");
        if(cid>0 && Math.abs(balanceAfter)>=0.005){if(balanceAfter>0)s.append("رصيدكم عليكم: ").append(fmt(balanceAfter)).append(" ريال\n");else s.append("رصيدكم لكم: ").append(fmt(Math.abs(balanceAfter))).append(" ريال\n");}
        s.append("شكراً لتعاملكم معنا");
        return s.toString();
    }

    void showPostSaveActions(String no,String customer,ArrayList<Line> lines,double total,long cid){
        final Dialog dialog=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(12),dp(16),dp(10));box.setBackground(rounded(CARD,dp(18)));
        TextView title=tv("تم حفظ الفاتورة",17);title.setTextColor(GREEN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView sub=tv("الرصيد بعد الفاتورة: "+balanceText(db.balance(cid)),12);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button share=button("مشاركة");share.setTextColor(Color.WHITE);share.setBackgroundColor(GREEN);Button hide=button("إخفاء");hide.setTextColor(MUTED);
        actions.addView(share,new LinearLayout.LayoutParams(0,dp(38),1));actions.addView(hide,new LinearLayout.LayoutParams(0,dp(38),1));box.addView(actions);
        share.setOnClickListener(v->{dialog.dismiss();shareReceiptImageAndText(no,customer,lines,total);});hide.setOnClickListener(v->dialog.dismiss());
        dialog.setContentView(box);dialog.setCanceledOnTouchOutside(true);dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(dp(320),WindowManager.LayoutParams.WRAP_CONTENT);dialog.getWindow().setGravity(Gravity.CENTER);}
    }

    void showInvoiceDialog(long id,String no,String customer,double total,String date){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(10),dp(5),dp(10),dp(5));
        TextView head=tv("فاتورة رقم "+no,18); head.setTextColor(GREEN); head.setTypeface(Typeface.DEFAULT,Typeface.BOLD); box.addView(head,new LinearLayout.LayoutParams(-1,dp(38)));
        box.addView(tv("العميل: "+(customer==null||customer.isEmpty()?"نقدي":customer)+"\nالتاريخ والوقت: "+date,12),new LinearLayout.LayoutParams(-1,dp(50)));
        sectionInside(box,"الأصناف");
        Cursor c=db.invoiceLines(id); int count=0;
        while(c.moveToNext()){
            String n=c.getString(1); double q=c.getDouble(2), t=c.getDouble(3);
            TextView row=tv(n+"  ×  "+fmt(q)+"  =  "+fmt(t)+" ريال",12);
            row.setBackground(outline(Color.rgb(248,250,248),8)); box.addView(row,new LinearLayout.LayoutParams(-1,dp(34))); count++;
        }
        c.close();
        if(count==0) box.addView(tv("لا توجد تفاصيل أصناف محفوظة لهذه الفاتورة.",11),new LinearLayout.LayoutParams(-1,dp(34)));
        TextView totalV=tv("الإجمالي: "+fmt(total)+" ريال",17); totalV.setTextColor(GREEN); totalV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(totalV,new LinearLayout.LayoutParams(-1,dp(42)));
        AlertDialog dialog=new AlertDialog.Builder(this).setView(box)
            .setPositiveButton("تعديل",null).setNegativeButton("إغلاق",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{dialog.dismiss();invoice(true,id);}));
        dialog.show();
    }

    void showOperationDetails(String customer,long tid,String details,double amount,int type){
        String inv=db.invoiceNoFromTransaction(details);
        if(!inv.isEmpty()){
            long iid=db.invoiceIdByNo(inv);
            if(iid>0){Cursor c=db.invoiceLines(iid); ArrayList<Line> ls=new ArrayList<>(); while(c.moveToNext())ls.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3))); c.close();
                new AlertDialog.Builder(this).setTitle("تفاصيل العملية")
                    .setMessage("العميل: "+customer+"\nالفاتورة: "+inv+"\nالإجمالي: "+fmt(amount)+" ريال\n"+(ls.isEmpty()?"":db.invoiceCompactDetails(inv)))
                    .setPositiveButton("إغلاق",null).show(); return;
            }
        }
        new AlertDialog.Builder(this).setTitle("تفاصيل العملية")
            .setMessage("العميل: "+customer+"\n"+(details==null?"عملية مالية":details)+"\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال")
            .setPositiveButton("إغلاق",null).show();
    }

    void sectionInside(LinearLayout box,String title){
        TextView v=tv(title,11);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(v,new LinearLayout.LayoutParams(-1,dp(26)));
    }

    void invoiceHistory(){
        base("الفواتير"); section("سجل الفواتير");
        Cursor c=db.invoices();
        while(c.moveToNext()){
            long id=c.getLong(0); String no=c.getString(1),cn=c.getString(2),date=c.getString(4); double total=c.getDouble(3);
            LinearLayout r=card(); r.setPadding(dp(7),dp(3),dp(7),dp(3));
            TextView info=tv("فاتورة "+no+"  •  "+(cn==null||cn.isEmpty()?"نقدي":cn)+"  •  "+fmt(total)+" ريال\n"+date,12);
            info.setMaxLines(3); info.setEllipsize(null); info.setIncludeFontPadding(true);
            r.addView(info,new LinearLayout.LayoutParams(-1,dp(42)));
            r.setOnClickListener(v->showInvoiceDialog(id,no,cn,total,date));
            LinearLayout a=new LinearLayout(this); a.setOrientation(LinearLayout.HORIZONTAL);
            Button edit=button("تعديل"),del=button("حذف"); edit.setTextColor(GREEN);del.setTextColor(Color.RED);
            a.addView(edit,new LinearLayout.LayoutParams(0,dp(34),1));a.addView(del,new LinearLayout.LayoutParams(0,dp(34),1));r.addView(a);
            edit.setOnClickListener(v->invoice(true,id));
            del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف الفاتورة؟").setMessage("سيتم حذف الفاتورة وحركتها من حساب العميل.").setPositiveButton("حذف",(d,w)->{db.deleteInvoice(id);invoiceHistory();}).setNegativeButton("إلغاء",null).show());
            content.addView(r,new LinearLayout.LayoutParams(-1,dp(78))); addSpace(3);
        } c.close();
    }
    String receiptText(String no,String customer,LinearLayout rows,double total,long cid){StringBuilder s=new StringBuilder("بقالة العزي\nفاتورة رقم: ").append(no).append("\nالتاريخ: ").append(db.now()).append("\n");if(!customer.isEmpty())s.append("العميل: ").append(customer).append("\n");s.append("------------------------------\n");for(int i=0;i<rows.getChildCount();i++){View ch=rows.getChildAt(i);if(ch instanceof LinearLayout){LinearLayout r=(LinearLayout)ch;StringBuilder q=new StringBuilder();for(int j=0;j<r.getChildCount();j++){View x=r.getChildAt(j);if(x instanceof TextView){String z=((TextView)x).getText().toString().trim();if(!z.isEmpty()){if(q.length()>0)q.append(" | ");q.append(z);}}}if(q.length()>0)s.append(q).append("\n");}}s.append("------------------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\n");if(cid>0)s.append(balanceText(db.balance(cid))).append("\n");s.append("شكراً لتعاملكم معنا");return s.toString();}
    String balanceText(double b){double x=Math.abs(b)<0.005?0:b;if(x>0)return "رصيدكم عليكم: "+fmt(x)+" ريال";if(x<0)return "رصيدكم لكم: "+fmt(Math.abs(x))+" ريال";return "رصيدكم عليكم: 0 ريال";}
    void shareAccountPdfToWhatsApp(long id,String name){
        try{
            String pdfText=statement(id,name);
            File dir=new File(getCacheDir(),"statements");if(!dir.exists())dir.mkdirs();
            File file=new File(dir,"statement_"+id+"_"+System.currentTimeMillis()+".pdf");
            android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();            int pageW=595,pageH=842,margin=24,pageNo=1,y=margin;
            android.graphics.pdf.PdfDocument.PageInfo info=new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create();
            android.graphics.pdf.PdfDocument.Page page=pdf.startPage(info);Canvas canvas=page.getCanvas();
            TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(dp(12));
            for(String line:pdfText.split("\\n")){
                StaticLayout layout=new StaticLayout(line,paint,pageW-margin*2,Layout.Alignment.ALIGN_OPPOSITE,1.0f,2,false);
                if(y+layout.getHeight()>pageH-margin){
                    pdf.finishPage(page);pageNo++;info=new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create();
                    page=pdf.startPage(info);canvas=page.getCanvas();y=margin;
                }
                canvas.save();canvas.translate(margin,y);layout.draw(canvas);canvas.restore();y+=layout.getHeight()+3;
            }
            pdf.finishPage(page);FileOutputStream out=new FileOutputStream(file);pdf.writeTo(out);out.close();pdf.close();
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.putExtra(Intent.EXTRA_TEXT,"كشف حساب العميل: "+name);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String p=normalizeWhatsAppPhone(db.phoneByName(name));if(!p.isEmpty())i.putExtra("jid",p+"@s.whatsapp.net");
            try{i.setPackage("com.whatsapp");startActivity(i);}
            catch(Exception e){try{Intent chat=new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+p+"?text="+Uri.encode("كشف حساب العميل: "+name)));chat.setPackage("com.whatsapp");startActivity(chat);}catch(Exception ignored){i.setPackage(null);startActivity(Intent.createChooser(i,"إرسال كشف الحساب"));}}
        }catch(Exception e){Toast.makeText(this,"تعذر إنشاء ملف PDF",Toast.LENGTH_LONG).show();}
    }
    void shareText(String s){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"إرسال الفاتورة"));}
    String normalizeWhatsAppPhone(String phone){
        String p=phone==null?"":phone.replaceAll("[^0-9+]","");
        if(p.startsWith("+"))p=p.substring(1);
        if(p.startsWith("00"))p=p.substring(2);
        if(p.startsWith("0")&&p.length()>=8)p="967"+p.substring(1);
        else if(p.matches("\\d{9}"))p="967"+p;
        return p;
    }
    void shareWhatsAppToCustomer(String phone,String text,Uri image){
        String p=normalizeWhatsAppPhone(phone);
        Intent i=new Intent(Intent.ACTION_SEND);i.setType(image!=null?"image/png":"text/plain");
        i.putExtra(Intent.EXTRA_TEXT,text);
        if(image!=null){i.putExtra(Intent.EXTRA_STREAM,image);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        if(!p.isEmpty())i.putExtra("jid",p+"@s.whatsapp.net");
        try{
            i.setPackage("com.whatsapp");startActivity(i);
        }catch(Exception e){
            try{
                Intent chat=new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+p+"?text="+Uri.encode(text)));
                chat.setPackage("com.whatsapp");startActivity(chat);
            }catch(Exception ignored){
                i.setPackage(null);startActivity(Intent.createChooser(i,"مشاركة الفاتورة"));
            }
        }
    }
    Bitmap receiptBitmap(String text){
        final int width=384;
        final int margin=16;
        final int black=Color.BLACK;
        final int gray=Color.rgb(90,90,90);
        final int green=Color.rgb(24,112,61);
        final int lineH=24;
        String[] ls=text.split("\\n",-1);
        int rows=0;
        for(String s:ls) if(s.contains(" | ")) rows++;
        int height=150+rows*lineH+ls.length*16;
        Bitmap b=Bitmap.createBitmap(width,Math.max(240,height),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);

        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        p.setColor(black);
        p.setTextAlign(Paint.Align.CENTER);

        // شعار التطبيق الحقيقي في أعلى الإيصال.
        try{
            Drawable d=getResources().getDrawable(com.saleh.enezi.R.drawable.ic_store);
            int size=54;
            d.setBounds((width-size)/2,6,(width+size)/2,6+size);
            d.draw(canvas);
        }catch(Exception ignored){}

        p.setTypeface(Typeface.create("sans",Typeface.BOLD));
        p.setTextSize(18);p.setColor(green);canvas.drawText("بقالة العزي",width/2,82,p);
        p.setTextSize(14);p.setColor(black);canvas.drawText("فاتورة مبيعات",width/2,103,p);

        int y=124;
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(10);p.setColor(gray);
        for(String line:ls){
            if(line.equals("بقالة العزي")) continue;
            if(line.startsWith("فاتورة مبيعات رقم:")){
                p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(10);p.setColor(gray);
                canvas.drawText(line,width/2,y,p);y+=17;continue;
            }
            if(line.equals("------------------------------")) continue;
            if(line.equals("الصنف | الكمية | الإجمالي")){
                y+=5;
                p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(11);p.setColor(green);
                canvas.drawText("الصنف",125,y,p);canvas.drawText("الكمية",250,y,p);canvas.drawText("الإجمالي",335,y,p);
                y+=16;
                canvas.drawLine(margin,y,width-margin,y,p); y+=14;
                p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(11);p.setColor(black);
                continue;
            }
            if(line.contains(" | ")){
                String[] q=line.split(" \\| ",-1);
                if(q.length>=3){
                    String item=q[0].trim();
                    if(item.length()>17)item=item.substring(0,17)+"…";
                    p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(item,215,y,p);
                    p.setTextAlign(Paint.Align.CENTER);canvas.drawText(q[1],250,y,p);
                    p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(q[2],365,y,p);
                    p.setTextAlign(Paint.Align.CENTER);y+=lineH;continue;
                }
            }
            p.setTextAlign(Paint.Align.CENTER);
            if(line.startsWith("التاريخ:")||line.startsWith("العميل:")){p.setColor(gray);canvas.drawText(line,width/2,y,p);y+=17;continue;}
            if(line.startsWith("الإجمالي:")||line.startsWith("المتبقي")){
                y+=5;p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(14);p.setColor(green);
                canvas.drawText(line,width/2,y,p);y+=21;p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(11);p.setColor(black);continue;
            }
            if(line.startsWith("شكراً")){y+=7;p.setTextSize(10);p.setColor(gray);canvas.drawText(line,width/2,y,p);y+=18;continue;}
            p.setColor(black);canvas.drawText(line,width/2,y,p);y+=17;
        }
        return Bitmap.createBitmap(b,0,0,width,Math.min(y+12,b.getHeight()));
    }
    Uri saveReceiptBitmap(Bitmap bitmap,String no)throws Exception{File dir=new File(getCacheDir(),"receipts");if(!dir.exists())dir.mkdirs();File file=new File(dir,"invoice_"+no+"_"+System.currentTimeMillis()+".png");FileOutputStream out=new FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.PNG,100,out);out.close();return FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);}
    void shareReceiptImageAndText(String no,String customer,ArrayList<Line> lines,double total){try{long cid=customer==null||customer.trim().isEmpty()?-1:db.customerIdByName(customer.trim());String text=receiptTextFromLines(no,customer,lines,total,cid);Uri uri=saveReceiptBitmap(receiptBitmap(text),no);String phone=db.phoneByName(customer);shareWhatsAppToCustomer(phone,text,uri);}catch(Exception e){shareText(receiptTextFromLines(no,customer,lines,total,customer==null||customer.isEmpty()?-1:db.customerIdByName(customer)));}}
    String pendingPrintNo="",pendingPrintCustomer="";ArrayList<Line> pendingPrintLines;double pendingPrintTotal;
    String pendingPrintText="";
    void printTextBluetooth(String text){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED){
            pendingPrintText=text;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},5102);return;
        }
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null){Toast.makeText(this,"هذا الجهاز لا يدعم البلوتوث",Toast.LENGTH_LONG).show();return;}
        if(!adapter.isEnabled()){Toast.makeText(this,"فعّل البلوتوث ثم أعد الضغط على الطباعة",Toast.LENGTH_LONG).show();return;}
        Set<BluetoothDevice> paired=adapter.getBondedDevices();
        if(paired==null||paired.isEmpty()){Toast.makeText(this,"لا توجد طابعة مقترنة. اقترن بالطابعة من إعدادات البلوتوث أولاً.",Toast.LENGTH_LONG).show();return;}
        BluetoothDevice[] devices=paired.toArray(new BluetoothDevice[0]);String[] names=new String[devices.length];
        for(int i=0;i<devices.length;i++)names[i]=(devices[i].getName()==null?"طابعة بلوتوث":devices[i].getName())+"\n"+devices[i].getAddress();
        new AlertDialog.Builder(this).setTitle("اختر طابعة 58mm").setItems(names,(d,w)->{
            Bitmap bitmap=receiptBitmap(text);
            new Thread(()->sendBitmapToBluetooth(devices[w],bitmap)).start();
        }).setNegativeButton("إلغاء",null).show();
    }
    void sendBitmapToBluetooth(BluetoothDevice device,Bitmap bitmap){
        BluetoothSocket socket=null;OutputStream out=null;
        try{
            socket=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();
            out=socket.getOutputStream();out.write(new byte[]{0x1B,0x40});out.write(rasterBytes(bitmap));out.write(new byte[]{0x0A,0x0A,0x0A});out.flush();
            runOnUiThread(()->Toast.makeText(this,"تم إرسال العملية إلى الطابعة",Toast.LENGTH_SHORT).show());
        }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"تعذر الاتصال بالطابعة: "+(e.getMessage()==null?"تحقق من الاقتران":e.getMessage()),Toast.LENGTH_LONG).show());}
        finally{try{if(out!=null)out.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}
    }
    void printInvoiceBluetooth(String no,String customer,ArrayList<Line> lines,double total){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED){pendingPrintNo=no;pendingPrintCustomer=customer;pendingPrintLines=new ArrayList<>(lines);pendingPrintTotal=total;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},5101);return;}
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();if(adapter==null){Toast.makeText(this,"هذا الجهاز لا يدعم البلوتوث",Toast.LENGTH_LONG).show();return;}if(!adapter.isEnabled()){Toast.makeText(this,"فعّل البلوتوث ثم أعد الضغط على الطباعة",Toast.LENGTH_LONG).show();return;}
        Set<BluetoothDevice> paired=adapter.getBondedDevices();if(paired==null||paired.isEmpty()){Toast.makeText(this,"لا توجد طابعة مقترنة. اقترن بالطابعة من إعدادات البلوتوث أولاً.",Toast.LENGTH_LONG).show();return;}
        BluetoothDevice[] devices=paired.toArray(new BluetoothDevice[0]);String[] names=new String[devices.length];for(int i=0;i<devices.length;i++)names[i]=(devices[i].getName()==null?"طابعة بلوتوث":devices[i].getName())+"\n"+devices[i].getAddress();
        new AlertDialog.Builder(this).setTitle("اختر طابعة 58mm").setItems(names,(d,w)->printToBluetooth(devices[w],no,customer,lines,total)).setNegativeButton("إلغاء",null).show();
    }
    void printToBluetooth(BluetoothDevice device,String no,String customer,ArrayList<Line> lines,double total){long cid=customer==null||customer.trim().isEmpty()?-1:db.customerIdByName(customer.trim());String text=receiptTextFromLines(no,customer,lines,total,cid);Bitmap bitmap=receiptBitmap(text);new Thread(()->{BluetoothSocket socket=null;OutputStream out=null;try{socket=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();out=socket.getOutputStream();out.write(new byte[]{0x1B,0x40});out.write(rasterBytes(bitmap));out.write(new byte[]{0x0A,0x0A,0x0A});out.flush();runOnUiThread(()->Toast.makeText(this,"تم إرسال الفاتورة إلى الطابعة",Toast.LENGTH_SHORT).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"تعذر الاتصال بالطابعة: "+(e.getMessage()==null?"تحقق من الاقتران":e.getMessage()),Toast.LENGTH_LONG).show());}finally{try{if(out!=null)out.close();}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}}}).start();}
    byte[] rasterBytes(Bitmap bitmap){int width=bitmap.getWidth(),height=bitmap.getHeight(),bpr=(width+7)/8;byte[] out=new byte[8+bpr*height];out[0]=0x1D;out[1]=0x76;out[2]=0x30;out[3]=0;out[4]=(byte)(bpr&255);out[5]=(byte)((bpr>>8)&255);out[6]=(byte)(height&255);out[7]=(byte)((height>>8)&255);int p=8;for(int y=0;y<height;y++)for(int xb=0;xb<bpr;xb++){int v=0;for(int bit=0;bit<8;bit++){int x=xb*8+bit;if(x<width){int px=bitmap.getPixel(x,y);int g=(Color.red(px)+Color.green(px)+Color.blue(px))/3;if(g<180)v|=1<<(7-bit);}}out[p++]=(byte)v;}return out;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_CONTACTS){if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)importContact();else Toast.makeText(this,"يلزم السماح بالوصول إلى جهات الاتصال",Toast.LENGTH_LONG).show();}else if(requestCode==5101&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&pendingPrintLines!=null){printInvoiceBluetooth(pendingPrintNo,pendingPrintCustomer,pendingPrintLines,pendingPrintTotal);}else if(requestCode==5102&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&!pendingPrintText.isEmpty()){String x=pendingPrintText;pendingPrintText="";printTextBluetooth(x);}}
    void thermalPreview(String no,String customer,LinearLayout rows,double total){preview(no,customer,new ArrayList<Line>(),total,false,-1);}
    static String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("الحسابات"); section("حسابات العملاء");
        EditText search=field("بحث بالاسم أو الهاتف"); addField(search);

        LinearLayout addBox=new LinearLayout(this); addBox.setOrientation(LinearLayout.VERTICAL);
        addBox.setPadding(dp(12),dp(10),dp(12),dp(10)); addBox.setBackground(outlined(CARD,1,16));
        TextView addTitle=tv("إضافة عميل جديد",17); addTitle.setTextColor(GREEN); addTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        addBox.addView(addTitle,new LinearLayout.LayoutParams(-1,dp(38)));
        EditText name=field("اسم العميل"), phone=phoneField("رقم الهاتف");
        customerNameInput=name;customerPhoneInput=phone;
        LinearLayout customerFields=new LinearLayout(this);customerFields.setOrientation(LinearLayout.HORIZONTAL);customerFields.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        customerFields.addView(name,new LinearLayout.LayoutParams(0,dp(36),1.35f));customerFields.addView(phone,new LinearLayout.LayoutParams(0,dp(36),1f));addBox.addView(customerFields);
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(8)));
        LinearLayout contactActions=new LinearLayout(this); contactActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("👤 جهات الاتصال"); pick.setTextColor(GREEN); pick.setOnClickListener(v->importContact());
        Button add=button("＋ إضافة العميل"); add.setTextColor(Color.WHITE); add.setBackgroundColor(GREEN);
        contactActions.addView(pick,new LinearLayout.LayoutParams(0,dp(42),1));
        contactActions.addView(add,new LinearLayout.LayoutParams(0,dp(42),1)); addBox.addView(contactActions);
        content.addView(addBox,new LinearLayout.LayoutParams(-1,-2)); addSpace(12);

        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); content.addView(list);
        final Runnable[] refresh={null};
        refresh[0]=()->{
            list.removeAllViews(); Cursor c=db.customers(search.getText().toString());
            while(c.moveToNext()){
                long id=c.getLong(0); String n=c.getString(1),p=c.getString(2); double bal=db.balance(id);
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14),dp(10),dp(14),dp(10)); card.setBackground(outlined(CARD,1,16)); card.setOnClickListener(v->account(id,n));
                card.setOnLongClickListener(v->{customerActions(id,n);return true;});
                TextView title=tv(n,18); title.setTextColor(GREEN); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                card.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
                TextView sub=tv((p==null||p.isEmpty()?"بدون رقم":p)+"   •   "+db.transactionCount(id)+" عملية",12); sub.setTextColor(MUTED);
                card.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));                TextView balance=tv(balanceText(bal),15); balance.setTextColor(bal>0?Color.rgb(190,55,45):GREEN);
                balance.setTypeface(Typeface.DEFAULT,Typeface.BOLD); card.addView(balance,new LinearLayout.LayoutParams(-1,dp(34)));
                list.addView(card,new LinearLayout.LayoutParams(-1,dp(108))); addSpaceTo(list,8);
            } c.close();
        };
        add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم العميل",Toast.LENGTH_SHORT).show();return;}db.addCustomer(n,phone.getText().toString().trim());name.setText("");phone.setText("");refresh[0].run();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh[0].run();}public void afterTextChanged(android.text.Editable e){}});
        refresh[0].run();
    }
    void addSpaceTo(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,h));}
    void account(long id,String name){
        base("حساب العميل");
        Button customerLabel=button("👤  "+name);
        customerLabel.setTextSize(18);customerLabel.setTextColor(GREEN);customerLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        customerLabel.setBackground(outline(CARD,14));customerLabel.setOnClickListener(v->customerActions(id,name));
        content.addView(customerLabel,new LinearLayout.LayoutParams(-1,dp(48)));space(5);

        LinearLayout summary=card();
        TextView bal=tv(balanceText(db.balance(id)),20);bal.setTextColor(GREEN);bal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bal.setGravity(Gravity.CENTER);
        summary.addView(bal,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView hint=tv("اضغط على العملية لعرض التفاصيل • اضغط مطولاً لفتح خيارات العملية.",11);hint.setTextColor(MUTED);hint.setGravity(Gravity.CENTER);
        summary.addView(hint,new LinearLayout.LayoutParams(-1,dp(30)));addCard(summary,84);

        EditText amount=numberField("المبلغ"),details=field("التفاصيل");addField(amount);addField(details);
        LinearLayout acts=new LinearLayout(this);acts.setOrientation(LinearLayout.HORIZONTAL);
        Button debit=button("عليه"),credit=button("له / دفعة");debit.setTextColor(Color.RED);credit.setTextColor(GREEN);
        acts.addView(debit,new LinearLayout.LayoutParams(0,dp(40),1));acts.addView(credit,new LinearLayout.LayoutParams(0,dp(40),1));content.addView(acts);addSpace(6);

        Button sharePdf=button("📄 كشف الحساب PDF + واتساب");sharePdf.setTextColor(GREEN);
        content.addView(sharePdf,new LinearLayout.LayoutParams(-1,dp(42)));sharePdf.setOnClickListener(v->shareAccountPdfToWhatsApp(id,name));

        section("سجل العمليات");
        LinearLayout selectedActions=new LinearLayout(this);selectedActions.setOrientation(LinearLayout.HORIZONTAL);
        Button shareSelected=button("📤 مشاركة المحدد");shareSelected.setTextColor(GREEN);shareSelected.setBackground(outline(CARD,12));
        Button printSelected=button("🖨 طباعة المحدد");printSelected.setTextColor(GREEN);printSelected.setBackground(outline(CARD,12));
        selectedActions.addView(shareSelected,new LinearLayout.LayoutParams(0,dp(40),1));
        selectedActions.addView(printSelected,new LinearLayout.LayoutParams(0,dp(40),1));
        content.addView(selectedActions);addSpace(5);
        LinearLayout history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);content.addView(history);
        final ArrayList<Long> selected=new ArrayList<>();

        Runnable refresh=()->{
            history.removeAllViews();selected.clear();
            double runningAfter=db.balance(id);Cursor c=db.transactions(id);
            while(c.moveToNext()){
                long tid=c.getLong(0);String date=c.getString(1),d=c.getString(2);double a=c.getDouble(3);int type=c.getInt(4);
                LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(8),dp(6),dp(8),dp(6));r.setBackground(outlined(CARD,1,12));
                LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
                CheckBox check=new CheckBox(this);check.setText("");TextView dateV=tv(date,10);dateV.setTextColor(MUTED);
                top.addView(check,new LinearLayout.LayoutParams(dp(40),dp(32)));top.addView(dateV,new LinearLayout.LayoutParams(0,dp(32),1));r.addView(top);
                TextView detV=tv(d==null||d.trim().isEmpty()?"عملية مالية":d,13);detV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(detV,new LinearLayout.LayoutParams(-1,dp(34)));
                TextView amtV=tv((type==1?"عليه: ":"له: ")+fmt(a)+" ريال",13);amtV.setTextColor(type==1?Color.rgb(190,55,45):GREEN);r.addView(amtV,new LinearLayout.LayoutParams(-1,dp(28)));
                TextView balV=tv("الرصيد بعد العملية: "+balanceText(runningAfter),11);balV.setTextColor(GREEN);r.addView(balV,new LinearLayout.LayoutParams(-1,dp(28)));
                String invNo=db.invoiceNoFromTransaction(d);
                if(!TextUtils.isEmpty(invNo)){TextView iv=tv(db.invoiceCompactDetails(invNo),10);iv.setTextColor(MUTED);iv.setMaxLines(4);iv.setEllipsize(null);r.addView(iv,new LinearLayout.LayoutParams(-1,dp(44)));}
                check.setOnCheckedChangeListener((b,is)->{if(is){if(!selected.contains(tid))selected.add(tid);}else selected.remove(tid);});
                r.setOnClickListener(v->showOperationDetails(customerName,tid,d,a,type));
                r.setOnLongClickListener(v->{operationActions(id,name,tid,d,a,type);return true;});
                history.addView(r,new LinearLayout.LayoutParams(-1,-2));addSpaceTo(history,6);
                runningAfter-=(type==1?a:-a);
            }
            c.close();bal.setText(balanceText(db.balance(id)));
        };
        View.OnClickListener addOp=v->{
            try{double a=Double.parseDouble(amount.getText().toString().trim());if(a<=0)throw new Exception();
                db.addTransaction(id,a,details.getText().toString().trim(),v==debit?1:0,db.now());
                amount.setText("");details.setText("");refresh.run();
            }catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}
        };
        debit.setOnClickListener(addOp);credit.setOnClickListener(addOp);
        shareSelected.setOnClickListener(v->{if(selected.isEmpty())Toast.makeText(this,"حدد عملية واحدة أو أكثر أولاً",Toast.LENGTH_SHORT).show();else shareSelectedTransactions(id,name,new ArrayList<>(selected));});
        printSelected.setOnClickListener(v->{if(selected.isEmpty())Toast.makeText(this,"حدد عملية واحدة أو أكثر أولاً",Toast.LENGTH_SHORT).show();else printSelectedTransactions(id,name,new ArrayList<>(selected));});
        refresh.run();
    }

    void customerActions(long id,String name){
        String[] choices={"✏ تعديل بيانات العميل","📄 كشف الحساب PDF + واتساب","📞 اتصال بالعميل","🗑 حذف حساب العميل"};
        new AlertDialog.Builder(this).setTitle("حساب: "+name).setItems(choices,(d,w)->{
            if(w==0)editCustomer(id,name);
            else if(w==1)shareAccountPdfToWhatsApp(id,name);
            else if(w==2){
                String p=db.phoneByName(name).replaceAll("[^0-9+]","");
                if(p.isEmpty()){Toast.makeText(this,"لا يوجد رقم هاتف للعميل",Toast.LENGTH_SHORT).show();return;}
                try{startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+p)));}catch(Exception ignored){}
            }else new AlertDialog.Builder(this).setTitle("حذف حساب العميل؟").setMessage("سيتم حذف الحساب وجميع عملياته وفواتيره المرتبطة به.").setPositiveButton("حذف",(x,y)->{db.deleteCustomer(id);customers();}).setNegativeButton("إلغاء",null).show();
        }).setNegativeButton("إغلاق",null).show();
    }

    void editCustomer(long id,String oldName){
        EditText name=field("اسم العميل"); name.setText(oldName);
        EditText phone=phoneField("رقم الهاتف"); phone.setText(db.phoneByName(oldName));
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(4),dp(8),dp(4));
        box.addView(name,new LinearLayout.LayoutParams(-1,dp(40))); spaceInside(box,5); box.addView(phone,new LinearLayout.LayoutParams(-1,dp(40)));
        new AlertDialog.Builder(this).setTitle("تعديل بيانات العميل").setView(box)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ",(d,w)->{
                String n=name.getText().toString().trim(), p=phone.getText().toString().trim();
                if(n.isEmpty()){Toast.makeText(this,"اسم العميل مطلوب",Toast.LENGTH_SHORT).show();return;}
                db.updateCustomer(id,oldName,n,p); customers();
                Toast.makeText(this,"تم تعديل بيانات العميل",Toast.LENGTH_SHORT).show();
            }).show();
    }

    void operationActions(long customerId,String customerName,long tid,String details,double amount,int type){
        String invNo=db.invoiceNoFromTransaction(details);ArrayList<String> choices=new ArrayList<>();
        if(!invNo.isEmpty())choices.add("🧾 تعديل الفاتورة");
        choices.add("✏ تعديل العملية");choices.add("📤 مشاركة العملية واتساب");choices.add("🖼 مشاركة صورة");choices.add("🖨 طباعة الحالية");choices.add("🗑 حذف العملية");
        String[] a=choices.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("خيارات العملية").setItems(a,(d,w)->{
            int i=0;
            if(!invNo.isEmpty()&&w==i++){long iid=db.invoiceIdByNo(invNo);if(iid>0)invoice(true,iid);return;}
            if(w==i++){editTransaction(customerId,customerName,tid,amount,details,type);return;}
            if(w==i++){shareOperation(customerName,details,amount,type,invNo);return;}
            if(w==i++){shareOperationImage(customerName,details,amount,type,invNo);return;}
            if(w==i++){printOperation(customerName,details,amount,type,invNo);return;}
            new AlertDialog.Builder(this).setTitle("حذف العملية؟").setPositiveButton("حذف",(x,y)->{db.deleteTransaction(tid);account(customerId,customerName);}).setNegativeButton("إلغاء",null).show();
        }).setNegativeButton("إغلاق",null).show();
    }

    void editTransaction(long id,String name,long tid,double oldAmount,String oldDetails,int oldType){
        EditText amount=numberField("المبلغ");amount.setText(fmt(oldAmount));EditText details=field("التفاصيل");details.setText(oldDetails==null?"":oldDetails);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));box.addView(amount);spaceInside(box,4);box.addView(details);        new AlertDialog.Builder(this).setTitle("تعديل العملية").setView(box).setPositiveButton("حفظ",(d,w)->{
            try{double a=Double.parseDouble(amount.getText().toString().trim());if(a<=0)throw new Exception();db.updateTransaction(tid,a,details.getText().toString().trim(),oldType,db.now());account(id,name);}
            catch(Exception e){Toast.makeText(this,"بيانات العملية غير صحيحة",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("إلغاء",null).show();
    }

    void shareOperation(String customer,String details,double amount,int type,String invNo){
        String text="بقالة العزي\\n"+(invNo.isEmpty()?"عملية مالية":"فاتورة مبيعات رقم "+invNo)+"\\nالعميل: "+customer+"\\n"+(details==null?"":details)+"\\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال\\n"+balanceText(db.balanceByName(customer));
        shareWhatsAppToCustomer(db.phoneByName(customer),text,null);
    }

    void shareOperationImage(String customer,String details,double amount,int type,String invNo){
        try{
            ArrayList<Line> ls=new ArrayList<>();
            if(!invNo.isEmpty()){long iid=db.invoiceIdByNo(invNo);if(iid>0){Cursor c=db.invoiceLines(iid);while(c.moveToNext())ls.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}}
            String text=!ls.isEmpty()?receiptTextFromLines(invNo,customer,ls,totalOf(ls),db.customer(customer)):
                "بقالة العزي\\nعملية مالية\\nالعميل: "+customer+"\\n"+(details==null?"":details)+"\\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال\\n"+balanceText(db.balanceByName(customer));
            Uri uri=saveReceiptBitmap(receiptBitmap(text),invNo.isEmpty()?String.valueOf(System.currentTimeMillis()):invNo);shareWhatsAppToCustomer(db.phoneByName(customer),text,uri);
        }catch(Exception e){shareOperation(customer,details,amount,type,invNo);}
    }

    void shareSelectedTransactions(long customerId,String name,ArrayList<Long> ids){
        StringBuilder text=new StringBuilder("بقالة العزي\\nكشف عمليات: ").append(name).append("\\n");
        double debit=0,credit=0;
        for(Long tid:ids){Cursor c=db.transactionById(tid);if(c.moveToFirst()){String d=c.getString(3);double a=c.getDouble(4);int t=c.getInt(5);text.append(c.getString(2)).append(" | ").append(d==null?"":d).append(" | ").append(t==1?"عليه: ":"له: ").append(fmt(a)).append(" ريال\\n");if(t==1)debit+=a;else credit+=a;}c.close();}
        text.append("إجمالي المحدد عليه: ").append(fmt(debit)).append(" ريال\\nإجمالي المحدد له: ").append(fmt(credit)).append(" ريال\\n").append(balanceText(db.balance(customerId)));
        shareWhatsAppToCustomer(db.phoneByName(name),text.toString(),null);
    }

    void printSelectedTransactions(long customerId,String name,ArrayList<Long> ids){
        StringBuilder text=new StringBuilder("بقالة العزي\\nكشف عمليات: ").append(name).append("\\n");
        double debit=0,credit=0;
        for(Long tid:ids){
            Cursor c=db.transactionById(tid);
            if(c.moveToFirst()){
                String d=c.getString(3);double a=c.getDouble(4);int t=c.getInt(5);
                text.append(c.getString(2)).append(" | ").append(d==null?"":d).append(" | ").append(t==1?"عليه: ":"له: ").append(fmt(a)).append(" ريال\\n");
                if(t==1)debit+=a;else credit+=a;
            }c.close();
        }
        text.append("------------------------------\\nإجمالي المحدد عليه: ").append(fmt(debit)).append(" ريال\\n");
        text.append("إجمالي المحدد له: ").append(fmt(credit)).append(" ريال\\n");
        text.append("الرصيد الحالي: ").append(balanceText(db.balance(customerId)));
        previewTextForPrint(text.toString(),name);
    }

    void printOperation(String customer,String details,double amount,int type,String invNo){
        try{
            ArrayList<Line> ls=new ArrayList<>();
            if(!invNo.isEmpty()){long iid=db.invoiceIdByNo(invNo);if(iid>0){Cursor c=db.invoiceLines(iid);while(c.moveToNext())ls.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}}
            String text=!ls.isEmpty()?receiptTextFromLines(invNo,customer,ls,totalOf(ls),db.customer(customer)):
                "بقالة العزي\\nعملية مالية\\nالعميل: "+customer+"\\n"+details+"\\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال\\n"+balanceText(db.balanceByName(customer));
            previewTextForPrint(text,customer);
        }catch(Exception e){Toast.makeText(this,"تعذر تجهيز العملية للطباعة",Toast.LENGTH_LONG).show();}
    }

    void previewTextForPrint(String text,String customer){
        TextView v=tv(text,11);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.MONOSPACE);
        new AlertDialog.Builder(this).setTitle("معاينة العملية 58mm").setView(v).setPositiveButton("طباعة",(d,w)->printTextBluetooth(text)).setNegativeButton("إغلاق",null).show();
    }
    String statement(long id,String name){
        StringBuilder s=new StringBuilder("بقالة العزي\\nكشف حساب العميل: ").append(name).append("\\n");
        String phone=db.phoneByName(name);if(!phone.isEmpty())s.append("الهاتف: ").append(phone).append("\\n");
        s.append("التاريخ: ").append(db.now()).append("\\n--------------------------------\\n");
        double runningAfter=db.balance(id),totalDebit=0,totalCredit=0;Cursor c=db.transactions(id);
        while(c.moveToNext()){
            String d=c.getString(1),x=c.getString(2);double a=c.getDouble(3);int t=c.getInt(4);
            if(t==1)totalDebit+=a;else totalCredit+=a;
            s.append(d).append(" | ").append(x==null?"":x).append(" | ").append(t==1?"عليه: ":"له: ").append(fmt(a)).append(" ريال | ").append(balanceText(runningAfter)).append("\\n");
            String inv=db.invoiceNoFromTransaction(x);if(!inv.isEmpty())s.append("تفاصيل الفاتورة: ").append(db.invoiceCompactDetails(inv)).append("\\n");
            runningAfter-=(t==1?a:-a);
        }c.close();
        s.append("--------------------------------\\nإجمالي عليه: ").append(fmt(totalDebit)).append(" ريال\\n");
        s.append("إجمالي له: ").append(fmt(totalCredit)).append(" ريال\\n");
        s.append("المتبقي: ").append(balanceText(db.balance(id)));
        return s.toString();
    }

    void inventory(){
        base("المخزون");section("إضافة / تعديل صنف");
        EditText name=field("اسم الصنف");EditText qty=numberField("الكمية");EditText min=numberField("الحد الأدنى");
        addField(name);addField(qty);addField(min);
        Button add=button("＋ حفظ الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);
        content.addView(add,new LinearLayout.LayoutParams(-1,dp(38)));addSpace(8);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);

        final long[] editingId={-1};

        Runnable clearForm=()->{
            editingId[0]=-1;
            name.setText("");qty.setText("");min.setText("");
            add.setText("＋ حفظ الصنف");
        };

        final Runnable[] refresh={null};
        refresh[0]=()->{
            list.removeAllViews();
            Cursor c=db.items();
            while(c.moveToNext()){
                long id=c.getLong(0);
                String itemName=c.getString(1);
                double q=c.getDouble(2),m=c.getDouble(3);

                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(8),dp(5),dp(8),dp(5));
                row.setBackground(outlined(CARD,1,10));

                TextView info=tv(itemName+"\nالكمية: "+fmt(q)+"   •   الحد الأدنى: "+fmt(m)+(q<=m?"   ⚠ منخفض":""),13);
                info.setTextColor(q<=m?Color.rgb(170,75,35):TEXT);
                row.addView(info,new LinearLayout.LayoutParams(-1,dp(42)));

                LinearLayout actions=new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setGravity(Gravity.CENTER);

                Button editBtn=button("✎ تعديل");
                editBtn.setTextColor(GREEN);
                Button deleteBtn=button("حذف");
                deleteBtn.setTextColor(Color.rgb(170,55,55));

                editBtn.setOnClickListener(v->{
                    editingId[0]=id;
                    name.setText(itemName);qty.setText(fmt(q));min.setText(fmt(m));
                    add.setText("✓ حفظ التعديل");
                    name.requestFocus();
                    Toast.makeText(this,"تم تحميل الصنف للتعديل",Toast.LENGTH_SHORT).show();
                });

                deleteBtn.setOnClickListener(v->new AlertDialog.Builder(this)
                    .setTitle("حذف الصنف")
                    .setMessage("هل تريد حذف «"+itemName+"» نهائياً؟")
                    .setNegativeButton("إلغاء",null)
                    .setPositiveButton("حذف",(d,w)->{
                        db.deleteItem(id);
                        if(editingId[0]==id)clearForm.run();
                        refresh[0].run();
                        Toast.makeText(this,"تم حذف الصنف",Toast.LENGTH_SHORT).show();
                    }).show());

                actions.addView(editBtn,new LinearLayout.LayoutParams(0,dp(34),1));
                actions.addView(deleteBtn,new LinearLayout.LayoutParams(0,dp(34),1));
                row.addView(actions);
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(82)));
                addSpaceTo(list,5);
            }
            c.close();
        };

        add.setOnClickListener(v->{
            try{
                String n=name.getText().toString().trim();
                double q=Double.parseDouble(qty.getText().toString().trim());
                double m=Double.parseDouble(min.getText().toString().trim());
                if(n.isEmpty()||q<0||m<0)throw new Exception();

                if(editingId[0]>0){
                    db.updateItem(editingId[0],n,q,m);
                    Toast.makeText(this,"تم تعديل الصنف وحفظه",Toast.LENGTH_SHORT).show();
                }else{
                    boolean existed=db.itemExists(n);
                    db.addItem(n,q,m);
                    Toast.makeText(this,existed?"الصنف موجود؛ تم تحديث بياناته":"تم حفظ الصنف",Toast.LENGTH_SHORT).show();
                }
                clearForm.run();
                refresh[0].run();
            }catch(Exception e){
                Toast.makeText(this,"أدخل بيانات الصنف بشكل صحيح",Toast.LENGTH_SHORT).show();
            }
        });
        refresh[0].run();
    }
    void reports(){
        base("التقارير");section("ملخص سريع");
        try{
            cardTitle("المبيعات","عدد الفواتير: "+db.invoiceCount()+"   •   إجمالي المبيعات: "+fmt(db.sales())+" ريال");
            cardTitle("العملاء","عدد العملاء: "+db.customerCount());
            section("كل الفواتير والعمليات — الأحدث أولاً");
            Cursor c=db.recentActivity();
            while(c.moveToNext()){
                String title=c.getString(2),date=c.getString(4);double amount=c.getDouble(3);
                LinearLayout r=card(); r.setPadding(dp(8),dp(3),dp(8),dp(3));
                TextView v=tv(title+"\n"+fmt(amount)+" ريال  •  "+(date==null?"":date),11);v.setMaxLines(3);v.setEllipsize(null);
                r.addView(v,new LinearLayout.LayoutParams(-1,dp(46)));content.addView(r,new LinearLayout.LayoutParams(-1,dp(52)));addSpace(2);
            }
            c.close();
        }catch(Exception e){
            TextView err=tv("تعذر تحميل بعض بيانات التقارير. تم الحفاظ على البيانات.",12);err.setTextColor(Color.rgb(170,75,35));
            content.addView(err,new LinearLayout.LayoutParams(-1,dp(48)));
        }
    }

    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"enezi.db",null,6);}
        public void onCreate(SQLiteDatabase d){create(d);}
        void create(SQLiteDatabase d){d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,customer TEXT,total REAL,date TEXT)");d.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER,amount REAL,details TEXT,type INTEGER,date TEXT)");d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,min_qty REAL)");d.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<6){try{d.execSQL("ALTER TABLE customers ADD COLUMN phone TEXT");}catch(Exception ignored){}}if(o<2){try{d.execSQL("ALTER TABLE invoices ADD COLUMN date TEXT");}catch(Exception ignored){}}if(o<5){d.execSQL("CREATE TABLE IF NOT EXISTS invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");}}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
        long customer(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);return getWritableDatabase().insert("customers",null,v);}
        void updateCustomer(long id,String oldName,String newName,String phone){
            SQLiteDatabase d=getWritableDatabase(); ContentValues v=new ContentValues();v.put("name",newName);v.put("phone",phone);
            d.update("customers",v,"id=?",new String[]{String.valueOf(id)});
            if(oldName!=null&&!oldName.equals(newName)){ContentValues iv=new ContentValues();iv.put("customer",newName);d.update("invoices",iv,"customer=?",new String[]{oldName});}
        }
        void addCustomer(String n,String p){ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);getWritableDatabase().insert("customers",null,v);}
        long addInvoice(String no,String c,double t,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",c);v.put("total",t);v.put("date",date);return getWritableDatabase().insert("invoices",null,v);}
        void addTransaction(long id,double a,String d,int type,String date){if(id<1)return;ContentValues v=new ContentValues();v.put("customer_id",id);v.put("amount",a);v.put("details",d);v.put("type",type);v.put("date",date);getWritableDatabase().insert("transactions",null,v);}
        double balance(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE -amount END),0) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        Cursor customers(String q){return getReadableDatabase().rawQuery("SELECT id,name,COALESCE(phone,'') FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});}
        Cursor transactions(long id){return getReadableDatabase().rawQuery("SELECT id,date,details,amount,type FROM transactions WHERE customer_id=? ORDER BY datetime(date) DESC, id DESC",new String[]{String.valueOf(id)});}
        Cursor items(){return getReadableDatabase().rawQuery("SELECT id,name,qty,min_qty FROM items ORDER BY name",null);}
        boolean itemExists(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM items WHERE name=? LIMIT 1",new String[]{n});boolean x=c.moveToFirst();c.close();return x;}
        void addItem(String n,double q,double m){if(n.isEmpty()||q<0||m<0)throw new IllegalArgumentException();SQLiteDatabase d=getWritableDatabase();Cursor c=d.rawQuery("SELECT id FROM items WHERE name=? LIMIT 1",new String[]{n});if(c.moveToFirst()){long id=c.getLong(0);c.close();ContentValues v=new ContentValues();v.put("qty",q);v.put("min_qty",m);d.update("items",v,"id=?",new String[]{String.valueOf(id)});return;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("qty",q);v.put("min_qty",m);d.insert("items",null,v);}
        void updateItem(long id,String n,double q,double m){if(id<1||n==null||n.trim().isEmpty()||q<0||m<0)throw new IllegalArgumentException();ContentValues v=new ContentValues();v.put("name",n.trim());v.put("qty",q);v.put("min_qty",m);getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteItem(long id){if(id>0)getWritableDatabase().delete("items","id=?",new String[]{String.valueOf(id)});}
        int transactionCount(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        String invoiceNoFromTransaction(String details){if(details==null)return "";String p="فاتورة مبيعات رقم ";return details.startsWith(p)?details.substring(p.length()).trim():"";}
        String invoiceCompactDetails(String no){Cursor c=getReadableDatabase().rawQuery("SELECT name,qty,total FROM invoice_items WHERE invoice_id=(SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1) ORDER BY id",new String[]{no});StringBuilder s=new StringBuilder("تفاصيل: ");int n=0;while(c.moveToNext()&&n<6){if(n>0)s.append(" • ");s.append(c.getString(0)).append(" × ").append(fmt(c.getDouble(1))).append(" = ").append(fmt(c.getDouble(2)));n++;}c.close();return n==0?"تفاصيل الفاتورة غير متاحة":s.toString();}
        Cursor invoices(){return getReadableDatabase().rawQuery("SELECT id,no,customer,total,date FROM invoices ORDER BY datetime(date) DESC, id DESC LIMIT 100",null);}
        Cursor recentActivity(){
            return getReadableDatabase().rawQuery(
                "SELECT kind,ref,title,amount,date,sort_id FROM ("+
                "SELECT 1 AS kind,no AS ref,'فاتورة '+no+' • '+CASE WHEN customer IS NULL OR customer='' THEN 'نقدي' ELSE customer END AS title,total AS amount,date,id AS sort_id FROM invoices "+
                "UNION ALL "+
                "SELECT 2 AS kind,'' AS ref,CASE WHEN details IS NULL OR details='' THEN 'عملية مالية' ELSE details END || ' • ' || CASE WHEN c.name IS NULL THEN '' ELSE c.name END AS title,amount,date,id AS sort_id FROM transactions t LEFT JOIN customers c ON c.id=t.customer_id WHERE details NOT LIKE 'فاتورة مبيعات رقم %'"+
                ") ORDER BY datetime(date) DESC, sort_id DESC LIMIT 200",null);
        }
        int invoiceCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        int customerCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        double sales(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM invoices",null);double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        String[] customerNames(){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM customers ORDER BY name",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
        long customerIdByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=? ORDER BY id DESC LIMIT 1",new String[]{n});long x=c.moveToFirst()?c.getLong(0):-1;c.close();return x;}
        double invoiceTotal(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(total,0) FROM invoices WHERE id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        String phoneByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(phone,'') FROM customers WHERE name=? LIMIT 1",new String[]{n});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        long customer(String n,String p){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();ContentValues v=new ContentValues();v.put("phone",p);getWritableDatabase().update("customers",v,"id=?",new String[]{String.valueOf(x)});return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);return getWritableDatabase().insert("customers",null,v);}
        double balanceByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=? ORDER BY id DESC LIMIT 1",new String[]{n});if(!c.moveToFirst()){c.close();return 0;}long id=c.getLong(0);c.close();return balance(id);}
        String invoiceNo(long id){Cursor c=getReadableDatabase().rawQuery("SELECT no FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        String invoiceCustomer(long id){Cursor c=getReadableDatabase().rawQuery("SELECT customer FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        void updateInvoice(long id,String no,String customer,double total,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",customer);v.put("total",total);v.put("date",date);getWritableDatabase().update("invoices",v,"id=?",new String[]{String.valueOf(id)});}
        Cursor invoiceLines(long id){return getReadableDatabase().rawQuery("SELECT id,name,qty,total FROM invoice_items WHERE invoice_id=? ORDER BY id",new String[]{String.valueOf(id)});}
        void replaceInvoiceLines(long id,ArrayList<Line> ls){SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});for(Line l:ls){ContentValues v=new ContentValues();v.put("invoice_id",id);v.put("name",l.name);v.put("qty",l.qty);v.put("total",l.total);d.insert("invoice_items",null,v);}}
        void deleteInvoice(long id){String no=invoiceNo(id);deleteInvoiceTransaction(no);SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});d.delete("invoices","id=?",new String[]{String.valueOf(id)});}
        void deleteInvoiceTransaction(String no){getWritableDatabase().delete("transactions","details=?",new String[]{"فاتورة مبيعات رقم "+no});}
        void addTransactionOnce(long id,double a,String details,String date){if(id>0)addTransaction(id,a,details,1,date);}
        int nextInvoice(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(CAST(no AS INTEGER)),0)+1 FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):1;c.close();return x;}
        long invoiceIdByNo(String no){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1",new String[]{no});long x=c.moveToFirst()?c.getLong(0):-1;c.close();return x;}
        Cursor transactionById(long id){return getReadableDatabase().rawQuery("SELECT id,customer_id,date,details,amount,type FROM transactions WHERE id=?",new String[]{String.valueOf(id)});}
        void updateTransaction(long id,double amount,String details,int type,String date){ContentValues v=new ContentValues();v.put("amount",amount);v.put("details",details);v.put("type",type);v.put("date",date);getWritableDatabase().update("transactions",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteTransaction(long id){getWritableDatabase().delete("transactions","id=?",new String[]{String.valueOf(id)});}
        void deleteCustomer(long id){
            SQLiteDatabase d=getWritableDatabase();
            Cursor c=d.rawQuery("SELECT id FROM invoices WHERE customer=(SELECT name FROM customers WHERE id=?)",new String[]{String.valueOf(id)});
            ArrayList<Long> invoiceIds=new ArrayList<>();while(c.moveToNext())invoiceIds.add(c.getLong(0));c.close();
            d.delete("transactions","customer_id=?",new String[]{String.valueOf(id)});
            for(Long iid:invoiceIds)d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(iid)});
            d.delete("invoices","customer=(SELECT name FROM customers WHERE id=?)",new String[]{String.valueOf(id)});
            d.delete("customers","id=?",new String[]{String.valueOf(id)});
        }
    }
}
