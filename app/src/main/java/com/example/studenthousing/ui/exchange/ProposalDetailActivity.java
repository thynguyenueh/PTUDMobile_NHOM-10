package com.example.studenthousing.ui.exchange;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.ui.review.ReviewActivity;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;

public class ProposalDetailActivity extends AppCompatActivity {

    private ExchangeRepository exchangeRepository;
    private SessionManager sessionManager;

    private View btnReject;
    private View btnAccept;
    private TextView tvProposalMessage;
    private TextView tvTransactionId;
    private ExchangeRequest currentRequest;

    public static void start(android.content.Context context, String requestId, String agreementText) {
        Intent intent = new Intent(context, ProposalDetailActivity.class);
        intent.putExtra("request_id", requestId);
        intent.putExtra("agreement_text", agreementText);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_detail);

        exchangeRepository = new ExchangeRepository(this);
        sessionManager = new SessionManager(this);

        btnReject = findViewById(R.id.btnReject);
        btnAccept = findViewById(R.id.btnAccept);
        ImageView btnBack = findViewById(R.id.btnBack);
        tvProposalMessage = findViewById(R.id.tvProposalMessage);
        tvTransactionId = findViewById(R.id.tvTransactionId);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String agreementText = getIntent().getStringExtra("agreement_text");
        String requestId = getIntent().getStringExtra("request_id");

        if (tvProposalMessage != null && agreementText != null && !agreementText.trim().isEmpty()) {
            tvProposalMessage.setText(agreementText);
        }

        if (requestId != null && !requestId.isEmpty()) {
            if (tvTransactionId != null) {
                tvTransactionId.setText("Mã giao dịch: " + requestId);
            }
            loadRequestDetail(requestId);
        }

        View btnExportContract = findViewById(R.id.btnExportContract);
        if (btnExportContract != null) {
            btnExportContract.setOnClickListener(v -> {
                String text = tvProposalMessage != null ? tvProposalMessage.getText().toString() : "";
                if (text.isEmpty()) {
                    Toast.makeText(this, "Không có nội dung hợp đồng", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Hợp đồng thỏa thuận trao đổi nhà ở - StudentHousing");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareIntent, "Chia sẻ / In hợp đồng"));
            });
        }
    }

    private void loadRequestDetail(String requestId) {
        exchangeRepository.getById(requestId, new AppCallback<ExchangeRequest>() {
            @Override
            public void onSuccess(ExchangeRequest request) {
                if (request == null) return;
                currentRequest = request;
                runOnUiThread(() -> setupUIForRequest(request));
            }

            @Override
            public void onError(Exception e) {
                // Keep default layout
            }
        });
    }

    private void setupUIForRequest(ExchangeRequest request) {
        String currentUserId = sessionManager.getUserId();
        String status = request.getStatus() != null ? request.getStatus().toUpperCase() : Constants.REQ_PENDING;

        if (tvProposalMessage != null) {
            if (request.getAgreementText() != null && !request.getAgreementText().trim().isEmpty()) {
                tvProposalMessage.setText(request.getAgreementText());
            } else if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
                tvProposalMessage.setText(request.getMessage());
            }
        }

        if (tvTransactionId != null) {
            tvTransactionId.setText("Mã giao dịch: " + request.getRequestId() + " (" + getDisplayStatusText(status) + ")");
        }

        boolean isReceiver = currentUserId != null && currentUserId.equals(request.getReceiverId());

        if (Constants.REQ_PENDING.equals(status)) {
            if (btnReject != null) {
                btnReject.setVisibility(View.VISIBLE);
                btnReject.setOnClickListener(v -> handleReject(request.getRequestId()));
            }
            if (btnAccept != null) {
                btnAccept.setVisibility(isReceiver ? View.VISIBLE : View.GONE);
                btnAccept.setOnClickListener(v -> handleAccept(request.getRequestId()));
            }
        } else if (Constants.REQ_EXCHANGING.equals(status)) {
            if (btnAccept != null) {
                btnAccept.setVisibility(View.GONE);
            }

            if (btnReject != null) {
                btnReject.setVisibility(View.VISIBLE);
                btnReject.setOnClickListener(v -> handleMutualCancel(request));
            }
        } else {
            if (btnReject != null) btnReject.setVisibility(View.GONE);
            if (btnAccept != null) btnAccept.setVisibility(View.GONE);
        }
    }

    private void handleAccept(String requestId) {
        exchangeRepository.updateStatus(requestId, Constants.REQ_EXCHANGING, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalDetailActivity.this, "Đã chấp nhận đề xuất đổi nhà!", Toast.LENGTH_SHORT).show();
                    loadRequestDetail(requestId);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ProposalDetailActivity.this,
                        "Không thể đồng ý đề xuất: " + (e == null ? "Lỗi máy chủ" : e.getMessage()),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleReject(String requestId) {
        exchangeRepository.updateStatus(requestId, Constants.REQ_REJECTED, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    Toast.makeText(ProposalDetailActivity.this, "Đã từ chối đề xuất đổi nhà.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ProposalDetailActivity.this,
                        "Không thể cập nhật trạng thái: " + (e == null ? "Lỗi máy chủ" : e.getMessage()),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleMutualCancel(ExchangeRequest request) {
        String currentUserId = sessionManager.getUserId();
        exchangeRepository.requestOrConfirmCancel(request, currentUserId, new AppCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isCancelled) {
                runOnUiThread(() -> {
                    if (isCancelled) {
                        Toast.makeText(ProposalDetailActivity.this, "Đã đồng ý hủy thỏa thuận.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ProposalDetailActivity.this, "Đã gửi yêu cầu hủy thỏa thuận. Đang chờ đối tác xác nhận.", Toast.LENGTH_LONG).show();
                        loadRequestDetail(request.getRequestId());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ProposalDetailActivity.this,
                        e == null ? "Lỗi kết nối" : e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String getDisplayStatusText(String rawStatus) {
        switch (rawStatus) {
            case Constants.REQ_EXCHANGING:   return "Đang trao đổi";
            case Constants.REQ_COMPLETED:    return "Hoàn thành";
            case Constants.REQ_REJECTED:     return "Bị từ chối";
            case Constants.REQ_CANCELLED:    return "Đã huỷ";
            case Constants.REQ_WITHDRAWN:    return "Đã rút lại";
            case Constants.REQ_EXPIRED:      return "Hết hạn";
            default:                         return "Đang chờ phản hồi";
        }
    }
}
