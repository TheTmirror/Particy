package com.mocoteam1.musicmatch;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

//import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

//TODO: Lizenzen beachten und noch drum kümmern
public class ShowQRCode extends AppCompatActivity {

    /*
    private static final String DEBUG_TAG = ShowQRCode.class.getSimpleName();

    public static final String SEPERATION_TAG = "%2307seperation2307%";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_qrcode);

        QRCodeHelper helper = new QRCodeHelper();
        Bundle bundle = getIntent().getExtras();
        String name = bundle.getString(PartyDetailview.PARTY_ID);
        String owner = bundle.getString(PartyDetailview.PARTY_OWNER);
        helper.setContent(name + SEPERATION_TAG + owner);
        Resources r = getResources();
        int dpi = 256;
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpi, r.getDisplayMetrics());
        int height = width;
        helper.setWidthAndHeight(width, height);
        helper.setErrorCorrectionLevel(ErrorCorrectionLevel.L);

        ImageView code = (ImageView) findViewById(R.id.showQRCodeQRCode);
        //TODO: id reafactoren
        FrameLayout codeFrame = (FrameLayout) findViewById(R.id.showQRCodeCode);
        code.setImageBitmap(helper.generate());
        codeFrame.setVisibility(View.VISIBLE);
    }
    */
}
