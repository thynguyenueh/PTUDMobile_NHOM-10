package com.example.studenthousing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.databinding.ActivityProposalBinding;
import com.example.studenthousing.databinding.ItemHomeCardBinding;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.util.List;
import java.util.concurrent.Executors;

/** Màn hình tạo đề xuất đổi nhà (proposal). */
public class ProposalActivity extends AppCompatActivity {

    public static final String EXTRA_HOST_NAME = "extra_host_name";
    public static final String EXTRA_PARTNER_POST_ID = "extra_partner_post_id";
    public static final String EXTRA_PARTNER_POST_TITLE = "extra_partner_post_title";
    public static final String EXTRA_PARTNER_POST_AREA = "extra_partner_post_area";
    public static final String EXTRA_PARTNER_PHOTO_RES = "extra_partner_photo_res";

    private ActivityProposalBinding binding;
    private SessionManager session;
    private ExchangeRepository exchangeRepository;
    private PostDao postDao;

    private String hostName;
    private String partnerPostId;
    private String partnerPostTitle;
    private String partnerPostArea;
    private int partnerPhotoRes;

    private String myPostId = "my_post_default";
    private String myPostTitle = "Căn hộ của tôi";
    private String myPostArea = "45 m²";
    private int myPhotoRes = R.drawable.home_binh_thanh;

    public static void start(@NonNull Context context, @NonNull String hostName) {
        start(context, hostName, "partner_post_1", "Căn hộ cao cấp Thảo Điền", 65.0, R.drawable.house_thao_dien);
    }

    public static void start(@NonNull Context context,
                             @NonNull String hostName,
                             @NonNull String partnerPostId,
                             @NonNull String partnerPostTitle,
                             double partnerArea,
                             @DrawableRes int partnerPhotoRes) {
        Intent intent = new Intent(context, ProposalActivity.class);
        intent.putExtra(EXTRA_HOST_NAME, hostName);
        intent.putExtra(EXTRA_PARTNER_POST_ID, partnerPostId);
        intent.putExtra(EXTRA_PARTNER_POST_TITLE, partnerPostTitle);
        intent.putExtra(EXTRA_PARTNER_POST_AREA, partnerArea + " m²");
        intent.putExtra(EXTRA_PARTNER_PHOTO_RES, partnerPhotoRes);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProposalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        exchangeRepository = new ExchangeRepository(this);
        postDao = new PostDao(this);

        resolveIntentExtras();
        loadMyHouseData();

        binding.tvMessageTitle.setText(getString(R.string.proposal_message_title, hostName));
        setupListeners();
    }

    private void resolveIntentExtras() {
        Intent intent = getIntent();
        hostName = intent.getStringExtra(EXTRA_HOST_NAME);
        if (hostName == null || hostName.isEmpty()) {
            hostName = getString(R.string.detail_host_name);
        }

        partnerPostId = intent.getStringExtra(EXTRA_PARTNER_POST_ID);
        if (partnerPostId == null) partnerPostId = "partner_house_1";

        partnerPostTitle = intent.getStringExtra(EXTRA_PARTNER_POST_TITLE);
        if (partnerPostTitle == null) partnerPostTitle = "Căn hộ đối tác";

        partnerPostArea = intent.getStringExtra(EXTRA_PARTNER_POST_AREA);
        if (partnerPostArea == null) partnerPostArea = "65 m²";

        partnerPhotoRes = intent.getIntExtra(EXTRA_PARTNER_PHOTO_RES, R.drawable.home_linh_trung);
    }

    private void loadMyHouseData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (session.isLoggedIn()) {
                List<AccommodationPost> posts = postDao.getAll();
                String userId = session.getUserId();
                for (AccommodationPost p : posts) {
                    if (userId.equals(p.getUserId())) {
                        myPostId = p.getPostId();
                        myPostTitle = p.getTitle();
                        myPostArea = p.getArea() + " m²";
                        break;
                    }
                }
            }
            runOnUiThread(this::bindHomeCards);
        });
    }

    private void bindHomeCards() {
        // Bind My Home
        bindHomeCard(
                binding.cardMyHome,
                myPhotoRes,
                R.string.proposal_my_home_label,
                myPostTitle,
                myPostArea
        );

        // Bind Partner Home
        bindHomeCard(
                binding.cardPartnerHome,
                partnerPhotoRes,
                R.string.proposal_partner_label,
                partnerPostTitle,
                partnerPostArea
        );
    }

    private void bindHomeCard(ItemHomeCardBinding card,
                              @DrawableRes int photo,
                              int labelRes,
                              String title,
                              String area) {
        card.ivHomePhoto.setImageResource(photo);
        card.tvHomeLabel.setText(labelRes);
        card.tvHomeName.setText(title);
        card.tvHomeArea.setText(area);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> submitProposal());
    }

    private void submitProposal() {
        String message = binding.etMessage.getText() != null ? binding.etMessage.getText().toString().trim() : "";
        if (message.isEmpty()) {
            binding.etMessage.setError(getString(R.string.proposal_message_required));
            return;
        }

        String periodCode = getSelectedPeriodCode();
        String periodDisplay = formatPeriodDisplay(periodCode);
        String senderId = session.isLoggedIn() ? session.getUserId() : "user_guest";
        String receiverId = "host_" + hostName.hashCode();

        ExchangeRequest request = new ExchangeRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setSenderPostId(myPostId);
        request.setReceiverPostId(partnerPostId);
        request.setExchangePeriod(periodCode);
        request.setMessage(message);
        request.setStatus("PENDING");
        request.setCreatedAt(System.currentTimeMillis());

        exchangeRepository.create(request, new AppCallback<String>() {
            @Override
            public void onSuccess(String id) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalActivity.this,
                            getString(R.string.proposal_sent, hostName, periodDisplay),
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProposalActivity.this, ExchangeHistoryActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalActivity.this,
                            getString(R.string.proposal_sent, hostName, periodDisplay),
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProposalActivity.this, ExchangeHistoryActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private String getSelectedPeriodCode() {
        int checkedId = binding.togglePeriod.getCheckedButtonId();
        if (checkedId == R.id.btnSemester2) {
            return "SEMESTER_2";
        }
        if (checkedId == R.id.btnFullYear) {
            return "FULL_YEAR";
        }
        return "SEMESTER_1";
    }

    private String formatPeriodDisplay(String code) {
        if ("SEMESTER_2".equals(code)) return "Học kỳ 2";
        if ("FULL_YEAR".equals(code)) return "Cả năm";
        return "Học kỳ 1";
    }
}
