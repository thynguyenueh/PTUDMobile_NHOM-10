package com.example.studenthousing.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studenthousing.R;
import com.example.studenthousing.ui.adapter.PhotoPagerAdapter;

import java.util.List;

public class FullScreenImageDialog extends Dialog {

    private final List<String> imageUrls;
    private final int initialPosition;

    public FullScreenImageDialog(@NonNull Context context, @NonNull List<String> imageUrls, int initialPosition) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.imageUrls = imageUrls;
        this.initialPosition = Math.max(0, Math.min(initialPosition, imageUrls.size() - 1));
    }

    public static void show(Context context, List<String> imageUrls, int initialPosition) {
        if (context == null || imageUrls == null || imageUrls.isEmpty()) return;
        FullScreenImageDialog dialog = new FullScreenImageDialog(context, imageUrls, initialPosition);
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fullscreen_photo);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ViewPager2 pager = findViewById(R.id.fullScreenPager);
        ImageButton btnClose = findViewById(R.id.btnCloseFullScreen);
        TextView tvCounter = findViewById(R.id.tvFullScreenCounter);

        if (pager != null) {
            pager.setAdapter(new PhotoPagerAdapter(imageUrls));
            pager.setCurrentItem(initialPosition, false);
            if (tvCounter != null) {
                tvCounter.setText((initialPosition + 1) + " / " + imageUrls.size());
                pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tvCounter.setText((position + 1) + " / " + imageUrls.size());
                    }
                });
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }
}