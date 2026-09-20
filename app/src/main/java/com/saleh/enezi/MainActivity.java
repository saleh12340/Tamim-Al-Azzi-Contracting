package com.saleh.enezi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Rect;
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
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setPadding(16,10,16,10);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); v.setTextDirection(View.TEXT_DIRECTION_RTL); return v;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(0);
        b.setMinimumHeight(0); b.setPadding(12,4,12,4); b.setGravity(Gravity.CENTER); b.setStateListAnimator(null);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return b;
    }
    EditText field(String h){
        EditText e=new EditText(this); e.setHint(h); e.setTextSize(textSize); e.setSingleLine(true);
        e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setPadding(14,7,14,7); e.setBackground(outlined(CARD,1,14)); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setSelectAllOnFocus(true); e.setOnClickListener(v -> e.selectAll());
        e.setOnFocusChangeListener((v,has)->{ if(has) e.postDelayed(() -> { e.selectAll(); },60); });
        return e;
    }
    void addField(EditText e){content.addView(e,new LinearLayout.LayoutParams(-1,52)); addSpace(6);}
    void addSpace(int h){Space s=new Space(this); content.addView(s,new LinearLayout.LayoutParams(1,h));}
    TextView section(String s){TextView v=tv(s,14);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(4,12,4,6);content.addView(v);return v;}

    void base(String title){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(12,6,12,6); bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        TextView logo=tv("بقالة العزي",20); logo.setTextColor(Color.WHITE); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bar.addView(logo,new LinearLayout.LayoutParams(0,64,1));
        TextView pt=tv(title,15); pt.setTextColor(Color.WHITE); pt.setGravity(Gravity.CENTER); bar.addView(pt,new LinearLayout.LayoutParams(120,64)); root.addView(bar);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.setClipToPadding(false);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(14,14,14,22); content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER); bottom.setPadding(5,5,5,5); bottom.setBackground(outlined(CARD,1,20)); bottom.setElevation(8);
        String[] ns={"الرئيسية","العملاء","الفواتير","المخزون","التقارير"};
        for(String n:ns){Button b=button(n); b.setTextSize(12); b.setTextColor(n.equals(title)?GREEN:MUTED); b.setBackgroundColor(Color.TRANSPARENT); b.setOnClickListener(v->navigate(n)); bottom.addView(b,new LinearLayout.LayoutParams(0,62,1));}
        root.addView(bottom); setContentView(root);
        root.getViewTreeObserver().addOnGlobalLayoutListener(()->{Rect rr=new Rect();root.getWindowVisibleDisplayFrame(rr);int diff=root.getRootView().getHeight()-rr.bottom;if(bottom!=null)bottom.setVisibility(diff>root.getRootView().getHeight()*0.18?View.GONE:View.VISIBLE);});
    }
    void navigate(String n){hideKeyboard(); if(n.equals("الرئيسية"))home();else if(n.equals("العملاء")||n.equals("الحسابات"))customers();else if(n.equals("الفواتير"))invoice();else if(n.equals("المخزون"))inventory();else reports();}
    void importContact(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.READ_CONTACTS")!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{"android.permission.READ_CONTACTS"},REQ_CONTACTS); return; }
        try{ Intent i=new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI); startActivityForResult(i,PICK_CONTACT); }catch(Exception e){ Toast.makeText(this,"تعذر فتح جهات الاتصال",Toast.LENGTH_SHORT).show(); }
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==REQ_CONTACTS){ if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) importContact(); else Toast.makeText(this,"يلزم السماح بالوصول إلى جهات الاتصال لاستيراد الاسم والرقم",Toast.LENGTH_LONG).show(); } }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data); if(requestCode==PICK_CONTACT&&resultCode==RESULT_OK&&data!=null){ Cursor c=null; try{ c=getContentResolver().query(data.getData(),new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null); if(c!=null&&c.moveToFirst()){ String n=c.getString(0),p=c.getString(1); if(customerNameInput!=null)customerNameInput.setText(n==null?"":n); if(customerPhoneInput!=null)customerPhoneInput.setText(p==null?"":p); if(customerNameInput!=null)customerNameInput.requestFocus(); Toast.makeText(this,"تم استيراد اسم العميل ورقم الهاتف",Toast.LENGTH_SHORT).show(); } }catch(Exception e){Toast.makeText(this,"تعذر قراءة بيانات جهة الاتصال",Toast.LENGTH_SHORT).show();}finally{if(c!=null)c.close();} } }
    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}

    TextView cardTitle(String title,String sub){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(14,8,14,8);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        TextView a=tv(title,17);a.setTextColor(GREEN);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(a);
        TextView b=tv(sub,12);b.setTextColor(MUTED);c.addView(b);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,72); cp.setMargins(0,0,0,7); content.addView(c,cp);return a;
    }
    void addAction(String a,String sub,View.OnClickListener l){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,5,12,5);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        Button b=button(a);b.setTextSize(16);b.setTextColor(TEXT);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setOnClickListener(l);c.addView(b,new LinearLayout.LayoutParams(-1,48));
        TextView s=tv(sub,12);s.setTextColor(MUTED);c.addView(s,new LinearLayout.LayoutParams(-1,30));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,82); ap.setMargins(0,0,0,7); content.addView(c,ap);
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

        LinearLayout rows=new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        table.addView(rows,new LinearLayout.LayoutParams(-1,-2));

        invoiceBox.addView(table,new LinearLayout.LayoutParams(-1,-2));

        TextView totalView=tv("الإجمالي: 0 ريال",24);
        totalView.setTextColor(GREEN);
        totalView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        totalView.setGravity(Gravity.CENTER);
        totalView.setBackground(bg(Color.rgb(255,249,226),14));
        invoiceBox.addView(totalView,new LinearLayout.LayoutParams(-1,dp(66)));
        addCard(invoiceBox,102);

        final ArrayList<Line> lines=new ArrayList<>();
        if(edit){Cursor c=db.invoiceLines(invoiceId);while(c.moveToNext())lines.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}

        Runnable redraw=()->{
            rows.removeAllViews();
            double run=0,baseBal=db.balanceByName(customer.getText().toString().trim());
            for(Line l:lines){run+=l.total;addRow(rows,l,run,baseBal,lines);}
            totalView.setText("الإجمالي: "+fmt(run)+" ريال");
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
        Button print=btn("🖨  معاينة إيصال 58mm");print.setTextColor(GREEN);
        content.addView(print,new LinearLayout.LayoutParams(-1,dp(46)));

        save.setOnClickListener(v->{
            if(lines.isEmpty()){Toast.makeText(this,"أضف صنفاً واحداً على الأقل",Toast.LENGTH_SHORT).show();return;}
            String cn=customer.getText().toString().trim();
            if(cn.isEmpty()){Toast.makeText(this,"اكتب اسم العميل، أو اتركه للفاتورة النقدية",Toast.LENGTH_SHORT).show();return;}
            showPhoneDialog(cn,no.getText().toString(),lines,totalOf(lines),edit,invoiceId);
        });
        print.setOnClickListener(v->preview(no.getText().toString(),customer.getText().toString(),lines,totalOf(lines)));
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
            Toast.makeText(this,"تم حفظ الفاتورة ورصيد العميل",Toast.LENGTH_LONG).show();invoiceHistory();
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
    void addRow(LinearLayout parent,Line l,double running,double baseBal,ArrayList<Line> all){LinearLayout r=card();TextView t=tv(l.name+"\nالكمية: "+fmt(l.qty)+"   •   الإجمالي: "+fmt(l.total)+" ريال\nسعر الوحدة: "+fmt(l.total/l.qty)+" ريال   •   الرصيد بعد العملية: "+fmt(baseBal+running)+" ريال",13);r.addView(t,new LinearLayout.LayoutParams(0,dp(72),1));Button del=button("حذف");del.setTextColor(Color.RED);del.setOnClickListener(v->{all.remove(l);redrawInvoiceRows(parent,all,baseBal);});r.addView(del,new LinearLayout.LayoutParams(dp(62),dp(72)));parent.addView(r);spaceTo(parent,6);}
    void redrawInvoiceRows(LinearLayout parent,ArrayList<Line> all,double baseBal){parent.removeAllViews();double run=0;for(Line x:all){run+=x.total;addRow(parent,x,run,baseBal,all);}}
    void spaceTo(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    void preview(String no,String customer,ArrayList<Line> lines,double total){StringBuilder s=new StringBuilder("بقالة العزي\nفاتورة رقم: ").append(no).append("\n");if(!customer.trim().isEmpty())s.append("العميل: ").append(customer).append("\n");for(Line l:lines)s.append(l.name).append(" | ").append(fmt(l.qty)).append(" | ").append(fmt(l.total)).append(" ريال\n");s.append("--------------------\nالإجمالي: ").append(fmt(total)).append(" ريال");new AlertDialog.Builder(this).setTitle("معاينة إيصال 58mm").setMessage(s.toString()).setPositiveButton("مشاركة",(d,w)->shareWhatsApp(s.toString())).setNegativeButton("إغلاق",null).show();}
    void invoiceHistory(){base("الفواتير");section("سجل الفواتير");Cursor c=db.invoices();while(c.moveToNext()){long id=c.getLong(0);String no=c.getString(1),cn=c.getString(2),date=c.getString(3);double total=c.getDouble(4);LinearLayout r=card();r.addView(tv("فاتورة "+no+"\n"+(cn==null||cn.isEmpty()?"نقدي":cn)+"   •   "+fmt(total)+" ريال\n"+date,14));LinearLayout a=new LinearLayout(this);Button edit=button("تعديل"),del=button("حذف");edit.setTextColor(GREEN);del.setTextColor(Color.RED);a.addView(edit,new LinearLayout.LayoutParams(0,46,1));a.addView(del,new LinearLayout.LayoutParams(0,46,1));r.addView(a);edit.setOnClickListener(v->invoice(true,id));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف الفاتورة؟").setMessage("سيتم حذف الفاتورة وحركتها من حساب العميل.").setPositiveButton("حذف",(d,w)->{db.deleteInvoice(id);invoiceHistory();}).setNegativeButton("إلغاء",null).show());addCard(r,128);}c.close();}
    String receiptText(String no,String customer,LinearLayout rows,double total,long cid){StringBuilder s=new StringBuilder("بقالة العزي\n");s.append("فاتورة رقم: ").append(no).append("\nالتاريخ: ").append(db.now()).append("\n");if(!customer.isEmpty())s.append("العميل: ").append(customer).append("\n");s.append("--------------------\n");for(int i=0;i<rows.getChildCount();i++)s.append(((TextView)rows.getChildAt(i)).getText()).append("\n");s.append("--------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\n");if(cid>0)s.append(balanceText(db.balance(cid))).append("\n");s.append("شكراً لتعاملكم معنا");return s.toString();}
    String balanceText(double b){return b>0?"رصيد العميل عليه: "+fmt(b)+" ريال":b<0?"رصيد العميل له: "+fmt(Math.abs(b))+" ريال":"رصيد العميل: 0 ريال";}
    void showShareDialog(String title,String text,String receipt){new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("واتساب",(d,w)->shareWhatsApp(receipt)).setNeutralButton("مشاركة النص",(d,w)->shareText(receipt)).setNegativeButton("إيصال 58mm",null).show();}
    void shareText(String s){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"إرسال الفاتورة"));}
    void shareWhatsApp(String s){try{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.setPackage("com.whatsapp");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(i);}catch(Exception e){shareText(s);}}
    void thermalPreview(String no,String customer,LinearLayout rows,double total){String s=receiptText(no,customer,rows,total,customer.isEmpty()?-1:db.customer(customer));TextView v=tv(s,12);v.setTypeface(Typeface.MONOSPACE);v.setGravity(Gravity.CENTER);new AlertDialog.Builder(this).setTitle("معاينة 58mm").setView(v).setPositiveButton("مشاركة", (d,w)->shareWhatsApp(s)).setNegativeButton("إغلاق",null).show();}
    String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("الحسابات");section("بحث وإضافة عميل");
        EditText search=field("بحث بالاسم أو الهاتف");addField(search);
        LinearLayout addBox=new LinearLayout(this); addBox.setOrientation(LinearLayout.VERTICAL); addBox.setPadding(14,12,14,12); addBox.setBackground(outlined(CARD,1,16));
        TextView addTitle=tv("بيانات العميل",17); addTitle.setTextColor(GREEN); addTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); addBox.addView(addTitle,new LinearLayout.LayoutParams(-1,40));
        EditText name=field("اسم العميل"); EditText phone=field("رقم الهاتف"); customerNameInput=name; customerPhoneInput=phone; addBox.addView(name,new LinearLayout.LayoutParams(-1,56)); addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,7)); addBox.addView(phone,new LinearLayout.LayoutParams(-1,56)); addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,8));
        LinearLayout contactActions=new LinearLayout(this); contactActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("👤 استيراد من جهات الاتصال"); pick.setTextColor(GREEN); pick.setOnClickListener(v->importContact());
        Button add=button("＋ إضافة العميل"); add.setTextColor(Color.WHITE); add.setBackgroundColor(GREEN);
        contactActions.addView(pick,new LinearLayout.LayoutParams(0,54,1.15f)); contactActions.addView(add,new LinearLayout.LayoutParams(0,54,1)); addBox.addView(contactActions); content.addView(addBox,new LinearLayout.LayoutParams(-1,-2)); addSpace(12);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable refresh=()->{list.removeAllViews();Cursor c=db.customers(search.getText().toString());while(c.moveToNext()){long id=c.getLong(0);String n=c.getString(1),p=c.getString(2);double bal=db.balance(id);Button b=button(n+"\n"+(p.isEmpty()?"بدون رقم":p)+"   •   "+balanceText(bal));b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setTextSize(14);b.setBackgroundColor(CARD);b.setOnClickListener(v->account(id,n));list.addView(b,new LinearLayout.LayoutParams(-1,68));addSpaceTo(list,6);}c.close();};
        add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم العميل",Toast.LENGTH_SHORT).show();return;}db.addCustomer(n,phone.getText().toString().trim());name.setText("");phone.setText("");refresh.run();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh.run();}public void afterTextChanged(android.text.Editable e){}});
        refresh.run();
    }
    void addSpaceTo(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,h));}
    void account(long id,String name){
        base("حساب العميل");section(name);TextView bal=tv(balanceText(db.balance(id)),22);bal.setTextColor(GREEN);bal.setGravity(Gravity.CENTER);content.addView(bal,new LinearLayout.LayoutParams(-1,60));
        EditText amount=field("المبلغ");addField(amount);EditText details=field("التفاصيل");addField(details);
        LinearLayout acts=new LinearLayout(this);acts.setOrientation(LinearLayout.HORIZONTAL);Button debit=button("عليه");Button credit=button("له / دفعة");debit.setTextColor(Color.RED);credit.setTextColor(GREEN);acts.addView(debit,new LinearLayout.LayoutParams(0,48,1));acts.addView(credit,new LinearLayout.LayoutParams(0,48,1));content.addView(acts);addSpace(6);
        Button share=button("📲  مشاركة كشف الحساب");content.addView(share,new LinearLayout.LayoutParams(-1,48));share.setOnClickListener(v->shareWhatsApp(statement(id,name)));
        LinearLayout history=new LinearLayout(this);history.setOrientation(LinearLayout.VERTICAL);content.addView(history);Runnable refresh=()->{history.removeAllViews();Cursor c=db.transactions(id);while(c.moveToNext()){TextView r=tv(c.getString(1)+"\n"+c.getString(2)+"\n"+(c.getInt(4)==1?"عليه":"له")+"  "+fmt(c.getDouble(3))+" ريال",14);r.setBackgroundColor(CARD);history.addView(r,new LinearLayout.LayoutParams(-1,70));addSpaceTo(history,5);}c.close();bal.setText(balanceText(db.balance(id)));};View.OnClickListener add=v->{try{double a=Double.parseDouble(amount.getText().toString());if(a<=0)throw new Exception();db.addTransaction(id,a,details.getText().toString(),v==debit?1:0,db.now());amount.setText("");details.setText("");refresh.run();}catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}};debit.setOnClickListener(add);credit.setOnClickListener(add);refresh.run();
    }
    String statement(long id,String name){StringBuilder s=new StringBuilder("بقالة العزي\nكشف حساب العميل: ").append(name).append("\n");Cursor c=db.transactions(id);while(c.moveToNext())s.append(c.getString(1)).append(" | ").append(c.getString(2)).append(" | ").append(c.getInt(4)==1?"عليه":"له").append(": ").append(fmt(c.getDouble(3))).append(" ريال\n");c.close();s.append("--------------------\n").append(balanceText(db.balance(id)));return s.toString();}

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
