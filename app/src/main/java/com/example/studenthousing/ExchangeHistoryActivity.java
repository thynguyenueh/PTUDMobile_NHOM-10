package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ExchangeHistoryActivity extends AppCompatActivity {

    private SessionManager session;
    private ExchangeRepository exchangeRepository;
    private LinearLayout layoutExchangesContainer;
    private TextView tvTotalExchanges;
    private ChipGroup chipGroupExchangeStatus;

    private List<ExchangeRequest> allRequests = new ArrayList<>();
    private String currentFilterStatus = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_history);

        session = new SessionManager(this);
        exchangeRepository = new ExchangeRepository(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutExchangesContainer = findViewById(R.id.layoutExchangesContainer);
        tvTotalExchanges = findViewById(R.id.tvTotalExchanges);
        chipGroupExchangeStatus = findViewById(R.id.chipGroupExchangeStatus);

        setupFilterChips();

        View btnCreateProposal = findViewById(R.id.btnCreateProposal);
        if (btnCreateProposal != null) {
            btnCreateProposal.setOnClickListener(v ->
                    startActivity(new Intent(ExchangeHistoryActivity.this, ProposalActivity.class)));
        }

        loadExchangeHistory();
    }

    private void setupFilterChips() {
        if (chipGroupExchangeStatus != null) {
            chipGroupExchangeStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipExchangePending) {
                    currentFilterStatus = "PENDING";
                } else if (checkedId == R.id.chipExchangeApproved) {
                    currentFilterStatus = "ACCEPTED";
                } else if (checkedId == R.id.chipExchangeCompleted) {
                    currentFilterStatus = "COMPLETED";
                } else if (checkedId == R.id.chipExchangeClosed) {
                    currentFilterStatus = "CLOSED";
                } else {
                    currentFilterStatus = "ALL";
                }
                applyFilterAndDisplay();
            });
        }
    }

    private void loadExchangeHistory() {
        String userId = session.getUserId() != null ? session.getUserId() : "current_user";

        exchangeRepository.getBySender(userId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                allRequests = list != null ? list : new ArrayList<>();
                runOnUiThread(() -> applyFilterAndDisplay());
            }

            @Override
            public void onError(Exception e) {
                allRequests = new ArrayList<>();
                runOnUiThread(() -> applyFilterAndDisplay());
            }
        });
    }

    private void applyFilterAndDisplay() {
        if (layoutExchangesContainer == null) return;
        layoutExchangesContainer.removeAllViews();

        List<ExchangeRequest> filtered = new ArrayList<>();
        if (allRequests != null && !allRequests.isEmpty()) {
            for (ExchangeRequest req : allRequests) {
                String st = req.getStatus() != null ? req.getStatus() : "PENDING";
                if ("ALL".equals(currentFilterStatus)) {
                    filtered.add(req);
                } else if ("CLOSED".equals(currentFilterStatus)) {
                    if ("REJECTED".equals(st) || "CANCELLED".equals(st)) {
                        filtered.add(req);
                    }
                } else if (currentFilterStatus.equalsIgnoreCase(st)) {
                    filtered.add(req);
                }
            }
        }

        if (!filtered.isEmpty()) {
            tvTotalExchanges.setText(filtered.size() + " lượt đổi nhà");
            for (ExchangeRequest req : filtered) {
                addExchangeItemView(
                        "Yêu cầu đổi trọ #" + req.getRequestId().substring(0, Math.min(6, req.getRequestId().length())),
                        "Đối tác ID: " + req.getReceiverId(),
                        formatStatusDisplay(req.getStatus()),
                        "Thời gian đổi: " + formatPeriodDisplay(req.getExchangePeriod()),
                        req.getMessage() != null ? req.getMessage() : "Thỏa thuận đổi nhà 2 chiều.",
                        req.getReceiverId(),
                        req.getRequestId()
                );
            }
        } else {
            // Mẫu thử nghiệm nếu chưa có dữ liệu
            tvTotalExchanges.setText("3 lượt đổi nhà ghi nhận");

            addExchangeItemView("Căn hộ Thảo Điền, Q.2",
                    "Gia đình Lê Văn Nam",
                    "✓ Hoàn tất",
                    "Thời gian đổi: Học kỳ 1 (SEMESTER_1)",
                    "Thỏa thuận đổi nhà 2 chiều đã hoàn tất thành công.",
                    "user_co_mai", "req_001");

            addExchangeItemView("Nhà trọ Linh Trung, Thủ Đức",
                    "Gia đình Trần Thị Mai",
                    "✓ Đã đồng ý",
                    "Thời gian đổi: Học kỳ 2 (SEMESTER_2)",
                    "Thỏa thuận đổi nhà cho sinh viên đang chờ bàn giao.",
                    "user_chu_nam", "req_002");

            addExchangeItemView("Phòng trọ Linh Xuân, Thủ Đức",
                    "Gia đình Phạm Hoàng",
                    "Đang chờ duyệt",
                    "Thời gian đổi: Cả năm (FULL_YEAR)",
                    "Đã gửi yêu cầu đổi phòng thời hạn 1 năm.",
                    "user_tuan", "req_003");
        }
    }

    private String formatPeriodDisplay(String period) {
        if (period == null) return "Học kỳ 1";
        switch (period.toUpperCase()) {
            case "SEMESTER_1": return "Học kỳ 1";
            case "SEMESTER_2": return "Học kỳ 2";
            case "FULL_YEAR":   return "Cả năm";
            default:           return period;
        }
    }

    private String formatStatusDisplay(String status) {
        if (status == null) return "Chờ duyệt";
        switch (status.toUpperCase()) {
            case "PENDING":   return "Đang chờ duyệt";
            case "ACCEPTED":  return "✓ Đã đồng ý";
            case "REJECTED":  return "✕ Đã từ chối";
            case "CANCELLED": return "✕ Đã hủy";
            case "COMPLETED": return "✓ Hoàn tất";
            default:          return status;
        }
    }

    private void addExchangeItemView(String title, String partner, String status, String period, String message, String targetUserId, String requestId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_exchange_history, layoutExchangesContainer, false);

        TextView tvTitle = itemView.findViewById(R.id.tvExchangeTitle);
        TextView tvPartner = itemView.findViewById(R.id.tvExchangePartner);
        TextView tvStatus = itemView.findViewById(R.id.tvExchangeStatus);
        TextView tvPeriod = itemView.findViewById(R.id.tvExchangePeriod);
        TextView tvMessage = itemView.findViewById(R.id.tvExchangeMessage);
        View btnReviewPartner = itemView.findViewById(R.id.btnReviewPartner);

        tvTitle.setText(title);
        tvPartner.setText(partner.startsWith("Đối tác") ? partner : "Đối tác: " + partner);
        tvStatus.setText(status);
        tvPeriod.setText(period);
        tvMessage.setText(message);

        if (btnReviewPartner != null) {
            btnReviewPartner.setOnClickListener(v -> {
                Intent intent = new Intent(ExchangeHistoryActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_ID, requestId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, targetUserId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, partner);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ROLE, "ĐỐI TÁC ĐỔI NHÀ");
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_INFO, period + " · " + status);
                startActivity(intent);
            });
        }

        layoutExchangesContainer.addView(itemView);
    }
}
