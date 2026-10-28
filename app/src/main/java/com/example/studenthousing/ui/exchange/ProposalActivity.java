package com.example.studenthousing.ui.exchange;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.studenthousing.R;
import com.example.studenthousing.data.local.PostDao;
import com.example.studenthousing.data.model.AccommodationPost;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.data.repository.PostRepository;
import com.example.studenthousing.databinding.ActivityProposalBinding;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ProposalActivity extends AppCompatActivity {

    public static final String EXTRA_HOST_NAME = "extra_host_name";

    private ActivityProposalBinding binding;
    private String hostName;
    private String receiverPostId;
    private String receiverUserId;

    private String myCurrentPostId;
    private final List<AccommodationPost> myAvailablePosts = new ArrayList<>();
    private boolean resumedOnce = false;

    private PostRepository postRepository;
    private ExchangeRepository exchangeRepository;
    private SessionManager session;
    private PostDao postDao;

    public static void start(@NonNull Context context, @NonNull String hostName) {
        start(context, hostName, null, null, 0.0, 0);
    }

    public static void start(@NonNull Context context,
                             @NonNull String hostName,
                             String receiverPostId,
                             String receiverPostTitle,
                             double partnerArea,
                             int partnerPhotoRes) {
        Intent intent = new Intent(context, ProposalActivity.class);
        intent.putExtra(EXTRA_HOST_NAME, hostName);
        if (receiverPostId != null) intent.putExtra("receiver_post_id", receiverPostId);
        if (receiverPostTitle != null) intent.putExtra("receiver_post_title", receiverPostTitle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProposalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        postRepository = new PostRepository(this);
        exchangeRepository = new ExchangeRepository(this);
        postDao = new PostDao(this);

        receiverPostId = getIntent().getStringExtra("receiver_post_id");
        receiverUserId = getIntent().getStringExtra("receiver_user_id");

        hostName = getIntent().getStringExtra(EXTRA_HOST_NAME);
        if (hostName == null || hostName.isEmpty()) {
            hostName = getIntent().getStringExtra("HOST_NAME");
        }
        if (hostName == null || hostName.isEmpty()) {
            hostName = getString(R.string.detail_host_name);
        }

        loadHomesData();
        binding.tvMessageTitle.setText(getString(R.string.proposal_message_title, hostName));
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumedOnce) {
            loadHomesData();
        }
        resumedOnce = true;
    }

    private void loadHomesData() {
        String currentUserId = session.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            List<AccommodationPost> allMyPosts = postDao.getByUserId(currentUserId);

            myAvailablePosts.clear();
            if (allMyPosts != null) {
                for (AccommodationPost p : allMyPosts) {
                    if (currentUserId.equals(p.getUserId()) && (p.getStatus() == null || Constants.POST_AVAILABLE.equalsIgnoreCase(p.getStatus()))) {
                        myAvailablePosts.add(p);
                    }
                }
            }

            runOnUiThread(() -> {
                if (!myAvailablePosts.isEmpty()) {
                    selectMyPost(myAvailablePosts.get(0));
                } else {
                    binding.cardMyHome.tvHomeLabel.setText("Nhà của tôi");
                    binding.cardMyHome.tvHomeName.setText("Chưa có bài đăng nào");
                    binding.cardMyHome.ivHomePhoto.setImageResource(R.drawable.ic_add);
                }
            });
        }).start();

        if (receiverPostId != null && !receiverPostId.isEmpty()) {
            postRepository.getPostById(receiverPostId, new AppCallback<AccommodationPost>() {
                @Override
                public void onSuccess(AccommodationPost partnerPost) {
                    if (partnerPost != null) {
                        if (receiverUserId == null || receiverUserId.isEmpty()) {
                            receiverUserId = partnerPost.getUserId();
                        }
                        runOnUiThread(() -> bindPartnerHome(partnerPost));
                    }
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(ProposalActivity.this, "Không tải được bài đăng đối tác", Toast.LENGTH_SHORT).show());
                }
            });
        } else if (receiverUserId != null && !receiverUserId.isEmpty()) {
            postRepository.getPostsByUserId(receiverUserId, new AppCallback<List<AccommodationPost>>() {
                @Override
                public void onSuccess(List<AccommodationPost> partnerPosts) {
                    if (partnerPosts != null && !partnerPosts.isEmpty()) {
                        AccommodationPost p = partnerPosts.get(0);
                        receiverPostId = p.getPostId();
                        runOnUiThread(() -> bindPartnerHome(p));
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private void selectMyPost(AccommodationPost post) {
        if (post == null) return;
        myCurrentPostId = post.getPostId();
        binding.cardMyHome.tvHomeLabel.setText("Nhà của tôi (Chạm để đổi)");
        binding.cardMyHome.tvHomeName.setText(post.getTitle() != null ? post.getTitle() : "Căn hộ của tôi");

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            Glide.with(this)
                    .load(post.getImages().get(0))
                    .into(binding.cardMyHome.ivHomePhoto);
        }
    }

    private void bindPartnerHome(AccommodationPost post) {
        binding.cardPartnerHome.tvHomeLabel.setText("Nhà đối tác");
        binding.cardPartnerHome.tvHomeName.setText(post.getTitle() != null ? post.getTitle() : "Nhà đối tác");

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            Glide.with(this)
                    .load(post.getImages().get(0))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(binding.cardPartnerHome.ivHomePhoto);
        } else {
            binding.cardPartnerHome.ivHomePhoto.setImageResource(R.drawable.bg_image_placeholder);
        }
    }

    private void showSelectMyPostDialog() {
        if (myAvailablePosts.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Bạn chưa có bài đăng nào")
                    .setMessage("Để tạo đề xuất đổi chỗ ở, bạn cần đăng thông tin phòng/nhà của mình trước để đối tác có thể xem xét và hoán đổi. Bạn có muốn tạo bài đăng ngay không?")
                    .setPositiveButton("Tạo bài đăng ngay", (dialog, which) -> {
                        Intent createIntent = new Intent(ProposalActivity.this, com.example.studenthousing.ui.post.CreatePostActivity.class);
                        startActivity(createIntent);
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }
        String[] titles = new String[myAvailablePosts.size()];
        for (int i = 0; i < myAvailablePosts.size(); i++) {
            titles[i] = myAvailablePosts.get(i).getTitle();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn bài đăng nhà của bạn để đề xuất")
                .setItems(titles, (dialog, which) -> selectMyPost(myAvailablePosts.get(which)))
                .show();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.cardMyHome.getRoot().setOnClickListener(v -> {
            if (myAvailablePosts.isEmpty()) {
                Intent createIntent = new Intent(ProposalActivity.this, com.example.studenthousing.ui.post.CreatePostActivity.class);
                startActivity(createIntent);
            } else {
                showSelectMyPostDialog();
            }
        });
        binding.togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (binding.etCustomPeriodInput != null) {
                    binding.etCustomPeriodInput.setText("");
                }
            }
        });
        if (binding.etCustomPeriodInput != null) {
            binding.etCustomPeriodInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() > 0) {
                        int checkedBtnId = binding.togglePeriod.getCheckedButtonId();
                        if (checkedBtnId != View.NO_ID) {
                            binding.togglePeriod.uncheck(checkedBtnId);
                        }
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
        binding.btnSend.setOnClickListener(v -> submitProposal());
    }

    private void submitProposal() {
        Editable editable = binding.etMessage.getText();
        String message = editable != null ? editable.toString().trim() : "";
        if (message.isEmpty()) {
            binding.etMessage.setError(getString(R.string.proposal_message_required));
            return;
        }

        String currentUserId = session.getUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myCurrentPostId == null || myCurrentPostId.isEmpty()
                || receiverPostId == null || receiverPostId.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn bài đăng nhà của bạn trước khi gửi", Toast.LENGTH_LONG).show();
            return;
        }

        String period = getExchangePeriod();
        if (period.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập hoặc chọn thời gian đổi", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatRoomId = new com.example.studenthousing.data.repository.ChatRepository(this)
                .getRoomId(currentUserId, receiverUserId != null ? receiverUserId : "partner");

        String agreementText = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n" +
                "Độc lập - Tự do - Hạnh phúc\n" +
                "o0o\n\n" +
                "BIÊN BẢN THỎA THUẬN TRAO ĐỔI NHÀ Ở\n\n" +
                "1. BÊN GỬI ĐỀ XUẤT (BÊN A):\n" +
                "- Họ và tên: " + (session.getUserFamilyName() != null ? session.getUserFamilyName() : "Thành viên") + "\n" +
                "- Số điện thoại: " + (session.getUserPhone() != null ? session.getUserPhone() : "Chưa cập nhật") + "\n\n" +
                "2. BÊN NHẬN ĐỀ XUẤT (BÊN B):\n" +
                "- Họ và tên / Đối tác: " + hostName + "\n\n" +
                "3. NỘI DUNG TRAO ĐỔI VÀ THỜI GIAN:\n" +
                "- Thời gian dự kiến đổi: " + period + "\n" +
                "- Lời nhắn / Ghi chú: " + message + "\n\n" +
                "4. ĐIỀU KHOẢN, GIAO NHẬN VÀ CAM KẾT:\n" +
                "- Hai bên (Bên A và Bên B) đồng ý tiến hành khảo sát thực tế, bàn giao hiện trạng nhà ở, trang thiết bị nội thất và các giấy tờ liên quan đúng thời hạn thỏa thuận.\n" +
                "- Việc bàn giao nhà, giấy tờ tùy thân, biên bản bàn giao và thanh toán chi phí chênh lệch (nếu có) do hai bên tự thỏa thuận trực tiếp và chịu trách nhiệm trước pháp luật.\n" +
                "- Văn bản này được hệ thống StudentHousing tự động tổng hợp từ thông tin đề xuất của người dùng nhằm làm cơ sở tham khảo và phối hợp thực hiện giữa các bên.";

        ExchangeRequest req = new ExchangeRequest();
        req.setSenderId(currentUserId);
        req.setReceiverId(receiverUserId != null ? receiverUserId : "");
        req.setSenderPostId(myCurrentPostId);
        req.setReceiverPostId(receiverPostId);
        req.setChatRoomId(chatRoomId);
        req.setExchangePeriod(period);
        req.setMessage(message);
        req.setAgreementText(agreementText);
        req.setStatus(Constants.REQ_PENDING);
        req.setCreatedAt(System.currentTimeMillis());

        exchangeRepository.create(req, new AppCallback<String>() {
            @Override
            public void onSuccess(String requestId) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalActivity.this,
                            getString(R.string.proposal_sent, hostName, period),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalActivity.this,
                            "Gửi đề xuất thất bại: " + (e == null ? "Lỗi máy chủ" : e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String getExchangePeriod() {
        Editable ed = binding.etCustomPeriodInput.getText();
        String custom = ed != null ? ed.toString().trim() : "";
        if (!custom.isEmpty()) {
            return custom;
        }

        int checkedId = binding.togglePeriod.getCheckedButtonId();
        if (checkedId == R.id.btn6Months) {
            return "6 tháng";
        }
        if (checkedId == R.id.btn1Year) {
            return "1 năm";
        }
        return "3 tháng";
    }
}
