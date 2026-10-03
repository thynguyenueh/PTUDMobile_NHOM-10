package com.example.homeexchange;

import com.example.studenthousing.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.DrawableRes;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.databinding.ActivityProposalBinding;
import com.example.studenthousing.databinding.ItemHomeCardBinding;

/** Màn hình tạo đề xuất đổi nhà (proposal). */
public class ProposalActivity extends AppCompatActivity {

    private static final String EXTRA_HOST_NAME = "extra_host_name";

    private ActivityProposalBinding binding;
    private String hostName;

    public static void start(@NonNull Context context, @NonNull String hostName) {
        Intent intent = new Intent(context, ProposalActivity.class);
        intent.putExtra(EXTRA_HOST_NAME, hostName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProposalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hostName = resolveHostName();

        bindHomes();
        binding.tvMessageTitle.setText(getString(R.string.proposal_message_title, hostName));
        setupListeners();
    }

    private String resolveHostName() {
        String name = getIntent().getStringExtra(EXTRA_HOST_NAME);
        return name != null ? name : getString(R.string.detail_host_name);
    }

    private void bindHomes() {
        // TODO: thay bằng dữ liệu thật (nhà của người dùng và nhà đối tác).
        bindHomeCard(binding.cardMyHome,
                R.drawable.home_binh_thanh,
                R.string.proposal_my_home_label,
                R.string.proposal_my_home_name,
                R.string.proposal_my_home_area);
        bindHomeCard(binding.cardPartnerHome,
                R.drawable.home_linh_trung,
                R.string.proposal_partner_label,
                R.string.proposal_partner_name,
                R.string.proposal_partner_area);
    }

    private void bindHomeCard(ItemHomeCardBinding card,
                              @DrawableRes int photo,
                              @StringRes int label,
                              @StringRes int name,
                              @StringRes int area) {
        card.ivHomePhoto.setImageResource(photo);
        card.tvHomeLabel.setText(label);
        card.tvHomeName.setText(name);
        card.tvHomeArea.setText(area);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> submitProposal());
    }

    private void submitProposal() {
        String message = binding.etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            binding.etMessage.setError(getString(R.string.proposal_message_required));
            return;
        }

        // TODO: gọi API/ViewModel gửi đề xuất khi merge.
        String period = getString(getSelectedPeriodLabel());
        Toast.makeText(this,
                getString(R.string.proposal_sent, hostName, period),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @StringRes
    private int getSelectedPeriodLabel() {
        int checkedId = binding.togglePeriod.getCheckedButtonId();
        if (checkedId == R.id.btnSemester2) {
            return R.string.proposal_period_semester_2;
        }
        if (checkedId == R.id.btnFullYear) {
            return R.string.proposal_period_full_year;
        }
        return R.string.proposal_period_semester_1;
    }
}
